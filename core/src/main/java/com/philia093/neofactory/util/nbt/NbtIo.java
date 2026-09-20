package com.philia093.neofactory.util.nbt;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Reads and writes tag trees in the named binary tag format.
 * <p>
 * The format is big endian and writes the type byte first, then the name, then the
 * payload, which is why {@link DataOutputStream} is used directly: it is big
 * endian as well and it needs no dependency beyond the JDK, so stored worlds can
 * be read and written without the engine running.
 * <p>
 * A text is written as its length in bytes followed by its UTF-8 bytes, which is
 * what keeps a world name with Chinese characters intact. A list writes the type
 * of its entries and its size, then the entries without their names.
 * <p>
 * Every file is written through {@link #writeGzip} and read through
 * {@link #readGzip}: block data and the generated-cell bitmap of a chunk compress
 * well, so a stored world is a good deal smaller than the raw data.
 */
public final class NbtIo {

    /** Highest amount of bytes a text may hold, the limit of the format itself. */
    private static final int MAX_STRING_LENGTH = 65535;

    /** Guard against a damaged file that claims to hold a huge array. */
    private static final int MAX_ARRAY_LENGTH = 16 * 1024 * 1024;

    private NbtIo() {
        // Utility class: never instantiated.
    }

    /**
     * Writes a tag tree into a stream.
     *
     * @param root tag to write, usually a compound
     * @param out stream to write into, not closed by this method
     * @throws NbtException when the data cannot be written
     */
    public static void write(NbtTag root, OutputStream out) {
        try {
            DataOutputStream data = out instanceof DataOutputStream
                    ? (DataOutputStream) out
                    : new DataOutputStream(out);
            writeTag(data, root);
            data.flush();
        } catch (IOException e) {
            throw new NbtException("Unable to write tag " + root, e);
        }
    }

    /**
     * Writes a tag tree into a compressed file, creating the parent folder.
     *
     * @param root tag to write
     * @param file file to write, replaced when it exists
     * @throws NbtException when the file cannot be written
     */
    public static void writeGzip(NbtTag root, File file) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new NbtException("Unable to create folder " + parent);
        }
        // Both streams are declared as resources: a compression stream refuses to be
        // created when the header cannot be written, and the file handle it would
        // wrap has to be closed even then, otherwise the file stays locked.
        try (OutputStream raw = new FileOutputStream(file);
                DataOutputStream data = new DataOutputStream(new BufferedOutputStream(
                        new GZIPOutputStream(raw)))) {
            writeTag(data, root);
        } catch (IOException e) {
            throw new NbtException("Unable to write " + file, e);
        }
    }

    /**
     * Reads a tag tree from a stream.
     *
     * @param in stream to read from, not closed by this method
     * @return the root tag
     * @throws NbtException when the data is damaged or incomplete
     */
    public static NbtTag read(InputStream in) {
        try {
            DataInputStream data = in instanceof DataInputStream
                    ? (DataInputStream) in
                    : new DataInputStream(in);
            NbtType type = NbtType.byId(data.readByte());
            String name = readText(data);
            return readPayload(data, type, name);
        } catch (EOFException e) {
            throw new NbtException("Tag data ends before the tag is complete", e);
        } catch (IOException e) {
            throw new NbtException("Unable to read tag data", e);
        }
    }

    /**
     * Reads a compressed tag file.
     *
     * @param file file to read
     * @return the root tag
     * @throws NbtException when the file is missing or damaged
     */
    public static NbtTag readGzip(File file) {
        if (!file.isFile()) {
            throw new NbtException("Save game is missing: " + file);
        }
        // The plain stream is a resource of its own: a file that is not compressed at
        // all makes the gzip stream fail, and the handle has to be closed even then.
        // A leaked handle keeps the file locked, which would stop a damaged save game
        // from being moved aside.
        try (InputStream raw = new FileInputStream(file);
                DataInputStream data = new DataInputStream(new BufferedInputStream(
                        new GZIPInputStream(raw)))) {
            NbtType type = NbtType.byId(data.readByte());
            String name = readText(data);
            return readPayload(data, type, name);
        } catch (EOFException e) {
            throw new NbtException("Save game is cut off: " + file, e);
        } catch (IOException e) {
            throw new NbtException("Unable to read " + file, e);
        }
    }

    /**
     * Writes one tag with its type byte and its name.
     *
     * @param data stream to write into
     * @param tag tag to write
     * @throws IOException when the stream fails
     */
    private static void writeTag(DataOutputStream data, NbtTag tag) throws IOException {
        data.writeByte(tag.type().id());
        writeText(data, tag.name());
        writePayload(data, tag);
    }

    /**
     * Writes the payload of a tag, the part behind its name.
     *
     * @param data stream to write into
     * @param tag tag to write
     * @throws IOException when the stream fails
     */
    private static void writePayload(DataOutputStream data, NbtTag tag) throws IOException {
        switch (tag.type()) {
            case BYTE:
                data.writeByte(((NbtByte) tag).value());
                break;
            case SHORT:
                data.writeShort(((NbtShort) tag).value());
                break;
            case INT:
                data.writeInt(((NbtInt) tag).value());
                break;
            case LONG:
                data.writeLong(((NbtLong) tag).value());
                break;
            case FLOAT:
                data.writeFloat(((NbtFloat) tag).value());
                break;
            case DOUBLE:
                data.writeDouble(((NbtDouble) tag).value());
                break;
            case BYTE_ARRAY:
                writeBytes(data, ((NbtByteArray) tag).toArray());
                break;
            case INT_ARRAY: {
                int[] numbers = ((NbtIntArray) tag).toArray();
                data.writeInt(numbers.length);
                for (int number : numbers) {
                    data.writeInt(number);
                }
                break;
            }
            case STRING:
                writeText(data, ((NbtString) tag).value());
                break;
            case LIST: {
                NbtList list = (NbtList) tag;
                data.writeByte(list.elementType().id());
                data.writeInt(list.size());
                for (NbtTag entry : list.entries()) {
                    writePayload(data, entry);
                }
                break;
            }
            case COMPOUND: {
                NbtCompound compound = (NbtCompound) tag;
                for (String key : compound.keys()) {
                    writeTag(data, compound.get(key));
                }
                // A type byte of zero ends the compound, it carries no name.
                data.writeByte(0);
                writeText(data, "");
                break;
            }
            default:
                throw new NbtException("Cannot write tag of type " + tag.type().typeName());
        }
    }

    /**
     * Reads the payload of a tag whose type and name are already known.
     *
     * @param data stream to read from
     * @param type type of the tag
     * @param name name of the tag
     * @return the tag
     * @throws IOException when the stream fails or the data is damaged
     */
    private static NbtTag readPayload(DataInputStream data, NbtType type, String name)
            throws IOException {
        switch (type) {
            case BYTE:
                return new NbtByte(name, data.readByte());
            case SHORT:
                return new NbtShort(name, data.readShort());
            case INT:
                return new NbtInt(name, data.readInt());
            case LONG:
                return new NbtLong(name, data.readLong());
            case FLOAT:
                return new NbtFloat(name, data.readFloat());
            case DOUBLE:
                return new NbtDouble(name, data.readDouble());
            case BYTE_ARRAY:
                return new NbtByteArray(name, readBytes(data));
            case INT_ARRAY: {
                int length = checkArrayLength(data.readInt(), "int array");
                int[] numbers = new int[length];
                for (int i = 0; i < length; i++) {
                    numbers[i] = data.readInt();
                }
                return new NbtIntArray(name, numbers);
            }
            case STRING:
                return new NbtString(name, readText(data));
            case LIST: {
                NbtType elementType = NbtType.byId(data.readByte());
                int size = checkArrayLength(data.readInt(), "list");
                NbtList list = new NbtList(name);
                for (int i = 0; i < size; i++) {
                    // Entries carry no name of their own, the format stores it once.
                    list.add(readPayload(data, elementType, ""));
                }
                return list;
            }
            case COMPOUND: {
                NbtCompound compound = new NbtCompound(name);
                while (true) {
                    int typeByte = data.readByte();
                    if (typeByte == 0) {
                        // A type byte of zero ends the compound.
                        readText(data);
                        return compound;
                    }
                    String key = readText(data);
                    compound.put(readPayload(data, NbtType.byId(typeByte), key));
                }
            }
            default:
                throw new NbtException("Cannot read tag of type " + type.typeName());
        }
    }

    /**
     * Writes a text as its byte length followed by its UTF-8 bytes.
     *
     * @param data stream to write into
     * @param text text to write
     * @throws IOException when the stream fails
     */
    private static void writeText(DataOutputStream data, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_STRING_LENGTH) {
            throw new NbtException("Text needs " + bytes.length + " bytes and does not fit into "
                    + MAX_STRING_LENGTH + " bytes");
        }
        data.writeShort(bytes.length);
        data.write(bytes);
    }

    /**
     * Reads a text written by {@link #writeText(DataOutputStream, String)}.
     *
     * @param data stream to read from
     * @return the text
     * @throws IOException when the stream fails
     */
    private static String readText(DataInputStream data) throws IOException {
        int length = data.readUnsignedShort();
        if (length == 0) {
            return "";
        }
        byte[] bytes = new byte[length];
        data.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Writes a byte array with its length in front.
     *
     * @param data stream to write into
     * @param bytes bytes to write
     * @throws IOException when the stream fails
     */
    private static void writeBytes(DataOutputStream data, byte[] bytes) throws IOException {
        data.writeInt(bytes.length);
        data.write(bytes);
    }

    /**
     * Reads a byte array written by {@link #writeBytes(DataOutputStream, byte[])}.
     *
     * @param data stream to read from
     * @return the bytes
     * @throws IOException when the stream fails
     */
    private static byte[] readBytes(DataInputStream data) throws IOException {
        int length = checkArrayLength(data.readInt(), "byte array");
        byte[] bytes = new byte[length];
        data.readFully(bytes);
        return bytes;
    }

    /**
     * Rejects a length that cannot come from a healthy file.
     *
     * @param length length read from the file
     * @param what name of the array, used in the message
     * @return the length, when it is plausible
     * @throws NbtException when the length is negative or absurdly large
     */
    private static int checkArrayLength(int length, String what) {
        if (length < 0 || length > MAX_ARRAY_LENGTH) {
            throw new NbtException("A " + what + " claims to hold " + length
                    + " entries, the file is damaged");
        }
        return length;
    }
}
