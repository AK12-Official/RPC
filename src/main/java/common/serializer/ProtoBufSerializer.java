package common.serializer;

import common.Message.MessageType;
import common.Message.RpcRequest;
import common.Message.RpcResponse;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

import java.nio.charset.StandardCharsets;

/**
 * ClassName：ProtoBufSerializer
 * Package: common.serializer
 *
 * @ Author：zh
 * @ Create: 2026/1/22 14:13
 * @ Version: 1.0
 * @ Description:protobuf序列化器
 */
public class ProtoBufSerializer implements Serializer {

    private static final byte KIND_NULL = 0;
    private static final byte KIND_PROTOSTUFF = 1;
    private static final byte KIND_STRING = 2;
    private static final byte KIND_NUMBER = 3;
    private static final byte KIND_BOOLEAN = 4;
    private static final byte KIND_CHAR = 5;

    @Override
    public byte[] serialize(Object obj) {
        if (obj == null) {
            return new byte[0];
        }

        if (obj instanceof RpcRequest request) {
            RpcRequestWrapper wrapper = wrapRequest(request);
            return protostuffSerialize(wrapper, RpcRequestWrapper.class);
        }

        if (obj instanceof RpcResponse response) {
            RpcResponseWrapper wrapper = wrapResponse(response);
            return protostuffSerialize(wrapper, RpcResponseWrapper.class);
        }

        throw new RuntimeException("ProtoBufSerializer 暂时只支持 RpcRequest / RpcResponse");
    }

    @Override
    public Object deserialize(byte[] bytes, int messageType) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        if (messageType == MessageType.REQUEST.getCode()) {
            RpcRequestWrapper wrapper = protostuffDeserialize(bytes, RpcRequestWrapper.class);
            return unwrapRequest(wrapper);
        }

        if (messageType == MessageType.RESPONSE.getCode()) {
            RpcResponseWrapper wrapper = protostuffDeserialize(bytes, RpcResponseWrapper.class);
            return unwrapResponse(wrapper);
        }

        throw new RuntimeException("暂不支持此种消息类型: " + messageType);
    }

    @Override
    public int getType() {
        return 2;
    }

    private static RpcRequestWrapper wrapRequest(RpcRequest request) {
        RpcRequestWrapper wrapper = new RpcRequestWrapper();
        wrapper.interfaceName = request.getInterfaceName();
        wrapper.methodName = request.getMethodName();

        Object[] params = request.getParams();
        Class<?>[] paramsType = request.getParamsType();

        int count = (params == null) ? 0 : params.length;
        wrapper.paramsTypeNames = new String[count];
        wrapper.paramsBytes = new byte[count][];
        wrapper.paramsKinds = new byte[count];

        for (int i = 0; i < count; i++) {
            Class<?> type = (paramsType != null && paramsType.length > i) ? paramsType[i] : (params[i] == null ? Object.class : params[i].getClass());
            wrapper.paramsTypeNames[i] = type.getName();

            EncodedValue encoded = encodeValue(params[i], type);
            wrapper.paramsKinds[i] = encoded.kind;
            wrapper.paramsBytes[i] = encoded.bytes;
        }
        return wrapper;
    }

    private static RpcRequest unwrapRequest(RpcRequestWrapper wrapper) {
        RpcRequest request = new RpcRequest();
        request.setInterfaceName(wrapper.interfaceName);
        request.setMethodName(wrapper.methodName);

        int count = (wrapper.paramsTypeNames == null) ? 0 : wrapper.paramsTypeNames.length;
        Object[] params = new Object[count];
        Class<?>[] paramsType = new Class<?>[count];

        for (int i = 0; i < count; i++) {
            Class<?> type = classForName(wrapper.paramsTypeNames[i]);
            paramsType[i] = type;
            byte kind = (wrapper.paramsKinds != null && wrapper.paramsKinds.length > i) ? wrapper.paramsKinds[i] : KIND_PROTOSTUFF;
            byte[] raw = (wrapper.paramsBytes != null && wrapper.paramsBytes.length > i) ? wrapper.paramsBytes[i] : null;
            params[i] = decodeValue(raw, kind, type);
        }

        request.setParamsType(paramsType);
        request.setParams(params);
        return request;
    }

    private static RpcResponseWrapper wrapResponse(RpcResponse response) {
        RpcResponseWrapper wrapper = new RpcResponseWrapper();
        wrapper.code = response.getCode();
        wrapper.message = response.getMessage();

        Object data = response.getData();
        Class<?> dataType = response.getDataType();
        if (data == null) {
            wrapper.dataTypeName = (dataType == null) ? Object.class.getName() : dataType.getName();
            wrapper.dataKind = KIND_NULL;
            wrapper.dataBytes = new byte[0];
            return wrapper;
        }

        Class<?> type = (dataType == null) ? data.getClass() : dataType;
        wrapper.dataTypeName = type.getName();
        EncodedValue encoded = encodeValue(data, type);
        wrapper.dataKind = encoded.kind;
        wrapper.dataBytes = encoded.bytes;
        return wrapper;
    }

    private static RpcResponse unwrapResponse(RpcResponseWrapper wrapper) {
        RpcResponse response = new RpcResponse();
        response.setCode(wrapper.code);
        response.setMessage(wrapper.message);

        Class<?> dataType = classForName(wrapper.dataTypeName);
        response.setDataType(dataType);
        response.setData(decodeValue(wrapper.dataBytes, wrapper.dataKind, dataType));
        return response;
    }

    private static EncodedValue encodeValue(Object value, Class<?> declaredType) {
        if (value == null) {
            return new EncodedValue(KIND_NULL, new byte[0]);
        }

        Class<?> type = declaredType == null ? value.getClass() : declaredType;

        if (type == String.class) {
            return new EncodedValue(KIND_STRING, ((String) value).getBytes(StandardCharsets.UTF_8));
        }

        if (type == boolean.class || type == Boolean.class) {
            return new EncodedValue(KIND_BOOLEAN, new byte[]{(byte) (((Boolean) value) ? 1 : 0)});
        }

        if (type == char.class || type == Character.class) {
            return new EncodedValue(KIND_CHAR, String.valueOf(value).getBytes(StandardCharsets.UTF_8));
        }

        if (isNumberType(type)) {
            return new EncodedValue(KIND_NUMBER, String.valueOf(value).getBytes(StandardCharsets.UTF_8));
        }

        @SuppressWarnings("unchecked")
        Class<Object> runtimeType = (Class<Object>) value.getClass();
        return new EncodedValue(KIND_PROTOSTUFF, protostuffSerialize(value, runtimeType));
    }

    private static Object decodeValue(byte[] bytes, byte kind, Class<?> targetType) {
        if (kind == KIND_NULL) {
            return null;
        }
        if (bytes == null) {
            bytes = new byte[0];
        }

        if (kind == KIND_STRING) {
            return new String(bytes, StandardCharsets.UTF_8);
        }

        if (kind == KIND_BOOLEAN) {
            return bytes.length > 0 && bytes[0] == 1;
        }

        if (kind == KIND_CHAR) {
            String s = new String(bytes, StandardCharsets.UTF_8);
            return s.isEmpty() ? '\0' : s.charAt(0);
        }

        if (kind == KIND_NUMBER) {
            String s = new String(bytes, StandardCharsets.UTF_8);
            return parseNumber(s, targetType);
        }

        if (kind == KIND_PROTOSTUFF) {
            if (targetType == null || targetType == Object.class) {
                throw new RuntimeException("ProtoBufSerializer 反序列化需要明确的目标类型");
            }
            @SuppressWarnings("unchecked")
            Class<Object> clazz = (Class<Object>) targetType;
            Object obj;
            try {
                obj = clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("类型缺少无参构造，无法使用 protostuff 反序列化: " + clazz.getName(), e);
            }
            Schema<Object> schema = RuntimeSchema.getSchema(clazz);
            ProtostuffIOUtil.mergeFrom(bytes, obj, schema);
            return obj;
        }

        throw new RuntimeException("未知的编码类型: " + kind);
    }

    private static boolean isNumberType(Class<?> type) {
        return type == byte.class || type == Byte.class
                || type == short.class || type == Short.class
                || type == int.class || type == Integer.class
                || type == long.class || type == Long.class
                || type == float.class || type == Float.class
                || type == double.class || type == Double.class;
    }

    private static Object parseNumber(String s, Class<?> targetType) {
        if (targetType == null) {
            return Double.valueOf(s);
        }
        if (targetType == byte.class || targetType == Byte.class) {
            return Byte.valueOf(s);
        }
        if (targetType == short.class || targetType == Short.class) {
            return Short.valueOf(s);
        }
        if (targetType == int.class || targetType == Integer.class) {
            return Integer.valueOf(s);
        }
        if (targetType == long.class || targetType == Long.class) {
            return Long.valueOf(s);
        }
        if (targetType == float.class || targetType == Float.class) {
            return Float.valueOf(s);
        }
        if (targetType == double.class || targetType == Double.class) {
            return Double.valueOf(s);
        }
        return Double.valueOf(s);
    }

    private static Class<?> classForName(String name) {
        if (name == null || name.isBlank()) {
            return Object.class;
        }

        switch (name) {
            case "boolean":
                return boolean.class;
            case "byte":
                return byte.class;
            case "short":
                return short.class;
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "float":
                return float.class;
            case "double":
                return double.class;
            case "char":
                return char.class;
            default:
                try {
                    return Class.forName(name);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("找不到类型: " + name, e);
                }
        }
    }

    private static <T> byte[] protostuffSerialize(T obj, Class<T> clazz) {
        Schema<T> schema = RuntimeSchema.getSchema(clazz);
        LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
        try {
            return ProtostuffIOUtil.toByteArray(obj, schema, buffer);
        } finally {
            buffer.clear();
        }
    }

    private static <T> T protostuffDeserialize(byte[] bytes, Class<T> clazz) {
        T obj;
        try {
            obj = clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("无法创建对象: " + clazz.getName(), e);
        }
        Schema<T> schema = RuntimeSchema.getSchema(clazz);
        ProtostuffIOUtil.mergeFrom(bytes, obj, schema);
        return obj;
    }

    private static class EncodedValue {
        final byte kind;
        final byte[] bytes;

        EncodedValue(byte kind, byte[] bytes) {
            this.kind = kind;
            this.bytes = bytes;
        }
    }

    /**
     * 为了绕过 Object/Class<?> 等字段的 protostuff 兼容性问题，
     * 这里把 RpcRequest/RpcResponse 转成“纯字符串 + 字节数组”的 wrapper 再序列化。
     */
    private static class RpcRequestWrapper {
        public String interfaceName;
        public String methodName;
        public String[] paramsTypeNames;
        public byte[][] paramsBytes;
        public byte[] paramsKinds;
    }

    private static class RpcResponseWrapper {
        public int code;
        public String message;
        public String dataTypeName;
        public byte[] dataBytes;
        public byte dataKind;
    }
}
