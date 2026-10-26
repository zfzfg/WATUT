package com.corosus.watut.common.nbt;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Pure Java, zero-dependency binary NBT compound model and codec.
 * Fully compatible with Minecraft FriendlyByteBuf.readNbt() and writeNbt().
 */
public class NbtCompound {

    public static final byte TAG_END = 0;
    public static final byte TAG_BYTE = 1;
    public static final byte TAG_SHORT = 2;
    public static final byte TAG_INT = 3;
    public static final byte TAG_LONG = 4;
    public static final byte TAG_FLOAT = 5;
    public static final byte TAG_DOUBLE = 6;
    public static final byte TAG_BYTE_ARRAY = 7;
    public static final byte TAG_STRING = 8;
    public static final byte TAG_LIST = 9;
    public static final byte TAG_COMPOUND = 10;
    public static final byte TAG_INT_ARRAY = 11;
    public static final byte TAG_LONG_ARRAY = 12;

    static final int MAX_DEPTH = 32;
    static final int MAX_COLLECTION_LENGTH = 1_048_576;

    private final Map<String, Object> tags = new LinkedHashMap<>();
    private final Map<String, Byte> tagTypes = new HashMap<>();

    public NbtCompound() {}

    public boolean contains(String key) {
        return tags.containsKey(key);
    }

    public void remove(String key) {
        tags.remove(key);
        tagTypes.remove(key);
    }

    public void putByte(String key, byte value) {
        tags.put(key, value);
        tagTypes.put(key, TAG_BYTE);
    }

    public byte getByte(String key) {
        Object val = tags.get(key);
        return val instanceof Number ? ((Number) val).byteValue() : 0;
    }

    public void putBoolean(String key, boolean value) {
        putByte(key, (byte) (value ? 1 : 0));
    }

    public boolean getBoolean(String key) {
        return getByte(key) != 0;
    }

    public void putShort(String key, short value) {
        tags.put(key, value);
        tagTypes.put(key, TAG_SHORT);
    }

    public short getShort(String key) {
        Object val = tags.get(key);
        return val instanceof Number ? ((Number) val).shortValue() : 0;
    }

    public void putInt(String key, int value) {
        tags.put(key, value);
        tagTypes.put(key, TAG_INT);
    }

    public int getInt(String key) {
        Object val = tags.get(key);
        return val instanceof Number ? ((Number) val).intValue() : 0;
    }

    public void putLong(String key, long value) {
        tags.put(key, value);
        tagTypes.put(key, TAG_LONG);
    }

    public long getLong(String key) {
        Object val = tags.get(key);
        return val instanceof Number ? ((Number) val).longValue() : 0L;
    }

    public void putFloat(String key, float value) {
        tags.put(key, value);
        tagTypes.put(key, TAG_FLOAT);
    }

    public float getFloat(String key) {
        Object val = tags.get(key);
        return val instanceof Number ? ((Number) val).floatValue() : 0.0f;
    }

    public void putDouble(String key, double value) {
        tags.put(key, value);
        tagTypes.put(key, TAG_DOUBLE);
    }

    public double getDouble(String key) {
        Object val = tags.get(key);
        return val instanceof Number ? ((Number) val).doubleValue() : 0.0;
    }

    public void putString(String key, String value) {
        if (value == null) {
            remove(key);
            return;
        }
        tags.put(key, value);
        tagTypes.put(key, TAG_STRING);
    }

    public String getString(String key) {
        Object val = tags.get(key);
        return val instanceof String ? (String) val : "";
    }

    public void putUUID(String key, UUID uuid) {
        if (uuid == null) {
            remove(key);
            return;
        }
        putString(key, uuid.toString());
    }

    public UUID getUUID(String key) {
        String val = getString(key);
        if (val == null || val.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(val);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void putByteArray(String key, byte[] bytes) {
        if (bytes == null) {
            remove(key);
            return;
        }
        tags.put(key, bytes.clone());
        tagTypes.put(key, TAG_BYTE_ARRAY);
    }

    public byte[] getByteArray(String key) {
        Object val = tags.get(key);
        return val instanceof byte[] ? (byte[]) val : new byte[0];
    }

    public void putCompound(String key, NbtCompound compound) {
        if (compound == null) {
            remove(key);
            return;
        }
        tags.put(key, compound);
        tagTypes.put(key, TAG_COMPOUND);
    }

    public NbtCompound getCompound(String key) {
        Object val = tags.get(key);
        return val instanceof NbtCompound ? (NbtCompound) val : new NbtCompound();
    }

    public Map<String, Object> getTags() {
        return Collections.unmodifiableMap(tags);
    }

    public byte getTagType(String key) {
        Byte b = tagTypes.get(key);
        return b != null ? b : TAG_END;
    }

    /**
     * Serializes this compound into standard uncompressed Minecraft network NBT bytes.
     */
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        write(out);
        out.flush();
        return baos.toByteArray();
    }

    /**
     * Writes this compound to a DataOutput stream.
     */
    public void write(DataOutput out) throws IOException {
        // Tag ID for compound
        out.writeByte(TAG_COMPOUND);
        writePayload(out);
    }

    void writePayload(DataOutput out) throws IOException {
        for (Map.Entry<String, Object> entry : tags.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            byte type = tagTypes.get(name);

            out.writeByte(type);
            out.writeUTF(name);
            writeTagValue(out, type, value);
        }
        out.writeByte(TAG_END);
    }

    private static void writeTagValue(DataOutput out, byte type, Object value) throws IOException {
        switch (type) {
            case TAG_BYTE -> out.writeByte(((Number) value).byteValue());
            case TAG_SHORT -> out.writeShort(((Number) value).shortValue());
            case TAG_INT -> out.writeInt(((Number) value).intValue());
            case TAG_LONG -> out.writeLong(((Number) value).longValue());
            case TAG_FLOAT -> out.writeFloat(((Number) value).floatValue());
            case TAG_DOUBLE -> out.writeDouble(((Number) value).doubleValue());
            case TAG_BYTE_ARRAY -> {
                byte[] arr = (byte[]) value;
                out.writeInt(arr.length);
                out.write(arr);
            }
            case TAG_STRING -> out.writeUTF((String) value);
            case TAG_COMPOUND -> ((NbtCompound) value).writePayload(out);
            case TAG_INT_ARRAY -> {
                int[] arr = (int[]) value;
                out.writeInt(arr.length);
                for (int i : arr) out.writeInt(i);
            }
            case TAG_LONG_ARRAY -> {
                long[] arr = (long[]) value;
                out.writeInt(arr.length);
                for (long l : arr) out.writeLong(l);
            }
            case TAG_LIST -> {
                @SuppressWarnings("unchecked")
                NbtList list = (NbtList) value;
                list.write(out);
            }
            default -> throw new IOException("Unsupported NBT tag type: " + type);
        }
    }

    /**
     * Deserializes an NbtCompound from Minecraft network bytes.
     */
    public static NbtCompound fromByteArray(byte[] data) throws IOException {
        if (data == null || data.length == 0) {
            return new NbtCompound();
        }
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
        return read(in);
    }

    public static NbtCompound read(DataInputStream in) throws IOException {
        byte rootType = in.readByte();
        if (rootType == TAG_END) {
            return new NbtCompound();
        }
        if (rootType != TAG_COMPOUND) {
            throw new IOException("Expected NBT root TAG_Compound (10), got " + rootType);
        }

        NbtCompound compound = new NbtCompound();
        compound.readPayload(in, 1);
        return compound;
    }

    void readPayload(DataInputStream in) throws IOException {
        readPayload(in, 1);
    }

    void readPayload(DataInputStream in, int depth) throws IOException {
        if (depth > MAX_DEPTH) {
            throw new IOException("NBT compound nested too deeply");
        }
        while (true) {
            byte type = in.readByte();
            if (type == TAG_END) {
                break;
            }
            String name = in.readUTF();
            Object value = readTagValue(in, type, depth);
            tags.put(name, value);
            tagTypes.put(name, type);
        }
    }

    private static int checkedLength(int len) throws IOException {
        if (len < 0 || len > MAX_COLLECTION_LENGTH) {
            throw new IOException("Invalid NBT collection length: " + len);
        }
        return len;
    }

    private static Object readTagValue(DataInputStream in, byte type, int depth) throws IOException {
        return switch (type) {
            case TAG_BYTE -> in.readByte();
            case TAG_SHORT -> in.readShort();
            case TAG_INT -> in.readInt();
            case TAG_LONG -> in.readLong();
            case TAG_FLOAT -> in.readFloat();
            case TAG_DOUBLE -> in.readDouble();
            case TAG_BYTE_ARRAY -> {
                int len = checkedLength(in.readInt());
                byte[] arr = new byte[len];
                in.readFully(arr);
                yield arr;
            }
            case TAG_STRING -> in.readUTF();
            case TAG_COMPOUND -> {
                NbtCompound child = new NbtCompound();
                child.readPayload(in, depth + 1);
                yield child;
            }
            case TAG_INT_ARRAY -> {
                int len = checkedLength(in.readInt());
                int[] arr = new int[len];
                for (int i = 0; i < len; i++) arr[i] = in.readInt();
                yield arr;
            }
            case TAG_LONG_ARRAY -> {
                int len = checkedLength(in.readInt());
                long[] arr = new long[len];
                for (int i = 0; i < len; i++) arr[i] = in.readLong();
                yield arr;
            }
            case TAG_LIST -> NbtList.read(in, depth);
            case TAG_END -> throw new IOException("TAG_End is not a value");
            default -> throw new IOException("Unknown NBT tag type: " + type);
        };
    }

    /**
     * Minimal list representation for NBT list payloads.
     */
    public static class NbtList {
        private final byte elementType;
        private final List<Object> elements = new ArrayList<>();

        public NbtList(byte elementType) {
            this.elementType = elementType;
        }

        public byte getElementType() {
            return elementType;
        }

        public List<Object> getElements() {
            return elements;
        }

        public void add(Object elem) {
            elements.add(elem);
        }

        public void write(DataOutput out) throws IOException {
            out.writeByte(elementType);
            out.writeInt(elements.size());
            for (Object obj : elements) {
                writeTagValue(out, elementType, obj);
            }
        }

        public static NbtList read(DataInputStream in, int depth) throws IOException {
            if (depth > MAX_DEPTH) {
                throw new IOException("NBT list nested too deeply");
            }
            byte elemType = in.readByte();
            int len = checkedLength(in.readInt());
            if (elemType == TAG_END && len > 0) {
                throw new IOException("TAG_End list cannot contain elements");
            }
            NbtList list = new NbtList(elemType);
            for (int i = 0; i < len; i++) {
                list.add(readTagValue(in, elemType, depth + 1));
            }
            return list;
        }
    }
}
