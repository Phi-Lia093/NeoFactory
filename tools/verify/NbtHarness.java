import com.philia093.neofactory.util.nbt.NbtByte;
import com.philia093.neofactory.util.nbt.NbtByteArray;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.util.nbt.NbtDouble;
import com.philia093.neofactory.util.nbt.NbtException;
import com.philia093.neofactory.util.nbt.NbtInt;
import com.philia093.neofactory.util.nbt.NbtIntArray;
import com.philia093.neofactory.util.nbt.NbtIo;
import com.philia093.neofactory.util.nbt.NbtList;
import com.philia093.neofactory.util.nbt.NbtShort;
import com.philia093.neofactory.util.nbt.NbtString;
import com.philia093.neofactory.util.nbt.NbtTag;
import com.philia093.neofactory.util.nbt.NbtType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/** Temporary harness for the tag format, no window needed. */
public final class NbtHarness {

    private static int checks;
    private static int failures;
    private static final List<String> FAILED = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        checkRoundTripOfEveryType();
        checkNestingAndLists();
        checkTextEncoding();
        checkDefaultsOnMissingEntries();
        checkDamagedData();
        checkFileRoundTrip();

        System.out.println("checks: " + checks + ", failures: " + failures);
        for (String failure : FAILED) {
            System.out.println("FAILED: " + failure);
        }
    }

    private static void checkRoundTripOfEveryType() {
        NbtCompound root = new NbtCompound("NeoFactory");
        root.put(new NbtByte("flag", (byte) 7));
        root.put(new NbtShort("small", (short) -1234));
        root.putInt("seed", 20260918);
        root.putLong("time", 1_700_000_000_000L);
        root.putFloat("x", 12.5f);
        root.put(new NbtDouble("precise", 1.0 / 3.0));
        root.put(new NbtString("name", "NeoFactory World"));
        root.put(new NbtByteArray("blocks", new byte[] {0, 1, 4, (byte) 0xFF, 0, 0, 9}));
        root.put(new NbtIntArray("table", new int[] {1, -2, 3, Integer.MIN_VALUE}));
        root.putBoolean("hardcore", true);
        root.putBoolean("cheats", false);

        NbtCompound read = roundTrip(root);
        check("byte value survives", ((NbtByte) read.get("flag")).value() == 7);
        check("short survives", ((NbtShort) read.get("small")).value() == -1234);
        check("int survives", read.getInt("seed", 0) == 20260918);
        check("long survives", read.getLong("time", 0L) == 1_700_000_000_000L);
        check("float survives", read.getFloat("x", 0.0f) == 12.5f);
        check("double survives", ((NbtDouble) read.get("precise")).value() == 1.0 / 3.0);
        check("string survives", "NeoFactory World".equals(read.getString("name", "")));
        NbtByteArray blocks = read.getByteArray("blocks");
        check("byte array survives", blocks != null && blocks.length() == 7
                && blocks.get(3) == (byte) 0xFF && blocks.get(6) == 9);
        NbtIntArray table = (NbtIntArray) read.get("table");
        check("int array survives", table.length() == 4 && table.get(3) == Integer.MIN_VALUE);
        check("true flag survives", read.getBoolean("hardcore", false));
        check("false flag survives", !read.getBoolean("cheats", true));
        check("root name survives", "NeoFactory".equals(read.name()));
        check("entry order is kept",
                String.join(",", read.keys()).equals(String.join(",", root.keys())));
    }

    private static void checkNestingAndLists() {
        NbtCompound root = new NbtCompound("root");
        NbtCompound player = new NbtCompound("Player");
        player.putFloat("X", 320.5f);
        player.putInt("SelectedSlot", 4);
        NbtList inventory = new NbtList("Inventory");
        for (int slot = 0; slot < 3; slot++) {
            NbtCompound entry = new NbtCompound("");
            entry.putInt("Slot", slot);
            entry.putString("id", "stone");
            inventory.add(entry);
        }
        player.put(inventory);
        root.put(player);

        NbtList chunks = new NbtList("Chunks");
        for (int i = 0; i < 2; i++) {
            NbtCompound chunk = new NbtCompound("");
            chunk.putInt("X", i);
            chunk.putInt("Z", -i);
            chunk.put(new NbtByteArray("Blocks", new byte[] {(byte) i, (byte) (i + 1)}));
            chunks.add(chunk);
        }
        root.put(chunks);
        root.put(new NbtList("Empty"));

        NbtCompound read = roundTrip(root);
        NbtCompound readPlayer = read.getCompound("Player");
        check("nested compound survives", readPlayer != null);
        check("nested float survives", readPlayer.getFloat("X", 0.0f) == 320.5f);
        check("nested int survives", readPlayer.getInt("SelectedSlot", -1) == 4);
        NbtList readInventory = readPlayer.getList("Inventory");
        check("nested list survives", readInventory != null && readInventory.size() == 3);
        check("list entry order survives", readInventory.getCompound(2).getInt("Slot", -1) == 2);
        check("list entry string survives",
                "stone".equals(readInventory.getCompound(0).getString("id", "")));
        NbtList readChunks = read.getList("Chunks");
        check("list of compounds survives", readChunks != null && readChunks.size() == 2);
        check("chunk payload survives",
                readChunks.getCompound(1).getByteArray("Blocks").get(1) == 2);
        NbtList empty = read.getList("Empty");
        check("empty list survives", empty != null && empty.isEmpty());
        check("empty list counts as bytes", empty.elementType() == NbtType.BYTE);
    }

    private static void checkTextEncoding() {
        NbtCompound root = new NbtCompound("root");
        root.putString("chinese", "新的工厂");
        root.putString("mixed", "World \u4e16\u754c 42");
        root.putString("empty", "");
        root.putString("quotes", "a \"quoted\" name\\with slash");

        NbtCompound read = roundTrip(root);
        check("chinese text survives", "新的工厂".equals(read.getString("chinese", "")));
        check("mixed text survives", "World \u4e16\u754c 42".equals(read.getString("mixed", "")));
        check("empty text survives", "".equals(read.getString("empty", "x")));
        check("quotes survive", "a \"quoted\" name\\with slash".equals(read.getString("quotes", "")));
        check("byte length counts UTF-8 bytes", new NbtString("name", "世界").byteLength() == 6);
    }

    private static void checkDefaultsOnMissingEntries() {
        NbtCompound root = new NbtCompound("root");
        root.putInt("seed", 5);
        root.putString("name", "x");
        NbtCompound read = roundTrip(root);

        check("missing int gives the default", read.getInt("missing", 42) == 42);
        check("missing long gives the default", read.getLong("missing", 7L) == 7L);
        check("missing float gives the default", read.getFloat("missing", 1.5f) == 1.5f);
        check("missing string gives the default",
                "fallback".equals(read.getString("missing", "fallback")));
        check("missing flag gives the default", read.getBoolean("missing", true));
        check("missing compound is null", read.getCompound("missing") == null);
        check("missing list is null", read.getList("missing") == null);
        check("missing byte array is null", read.getByteArray("missing") == null);
        check("a wrongly typed entry gives the default", read.getInt("name", 9) == 9);
        check("a wrongly typed compound is null", read.getCompound("seed") == null);
        check("contains sees stored entries", read.contains("seed") && !read.contains("missing"));
    }

    private static void checkDamagedData() {
        check("an empty stream is refused",
                throwsNbt(() -> NbtIo.read(new ByteArrayInputStream(new byte[0]))));

        byte[] unknownType = {99, 0, 0};
        check("an unknown type byte is refused",
                throwsNbt(() -> NbtIo.read(new ByteArrayInputStream(unknownType))));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        NbtCompound root = new NbtCompound("root");
        root.putString("name", "NeoFactory");
        NbtIo.write(root, out);
        byte[] full = out.toByteArray();
        byte[] cut = new byte[full.length - 3];
        System.arraycopy(full, 0, cut, 0, cut.length);
        check("a cut off stream is refused",
                throwsNbt(() -> NbtIo.read(new ByteArrayInputStream(cut))));

        NbtList list = new NbtList("list");
        list.add(new NbtInt("", 1));
        check("a list refuses foreign types", throwsNbt(() -> list.add(new NbtString("", "x"))));
        check("a list reports its element type", list.elementType() == NbtType.INT);
    }

    private static void checkFileRoundTrip() throws Exception {
        File file = new File("build/verify/nbt/nested/level.dat");
        if (file.exists() && !file.delete()) {
            throw new IllegalStateException("cannot clean " + file);
        }

        NbtCompound root = new NbtCompound("NeoFactory");
        root.putInt("DataVersion", 1);
        root.putLong("Seed", 20260918L);
        root.putString("WorldName", "测试世界");
        NbtCompound player = new NbtCompound("Player");
        player.putFloat("X", 1.25f);
        root.put(player);
        NbtIo.writeGzip(root, file);

        check("the file was created", file.isFile() && file.length() > 0);
        NbtCompound read = (NbtCompound) NbtIo.readGzip(file);
        check("file round trip keeps the seed", read.getLong("Seed", 0L) == 20260918L);
        check("file round trip keeps the name", "测试世界".equals(read.getString("WorldName", "")));
        check("file round trip keeps nested data",
                read.getCompound("Player").getFloat("X", 0f) == 1.25f);

        check("a missing file is reported",
                throwsNbt(() -> NbtIo.readGzip(new File("build/verify/nbt/missing.dat"))));

        byte[] raw = Files.readAllBytes(file.toPath());
        raw[raw.length / 2] ^= 0xFF;
        File damaged = new File("build/verify/nbt/damaged.dat");
        Files.write(damaged.toPath(), raw);
        check("a damaged file fails instead of crashing",
                throwsNbt(() -> NbtIo.readGzip(damaged)));
    }

    private static NbtCompound roundTrip(NbtCompound root) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        NbtIo.write(root, out);
        NbtTag read = NbtIo.read(new ByteArrayInputStream(out.toByteArray()));
        if (!(read instanceof NbtCompound)) {
            throw new IllegalStateException("root is not a compound: " + read);
        }
        return (NbtCompound) read;
    }

    /** An action that may fail with an {@link NbtException}. */
    private interface Action {
        void run() throws Exception;
    }

    private static boolean throwsNbt(Action action) {
        try {
            action.run();
            return false;
        } catch (NbtException expected) {
            return true;
        } catch (Exception other) {
            FAILED.add("unexpected exception type: " + other);
            failures++;
            checks++;
            return false;
        }
    }

    private static void check(String label, boolean ok) {
        checks++;
        if (!ok) {
            failures++;
            FAILED.add(label);
        }
    }
}
