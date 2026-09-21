package com.philia093.neofactory.world.save;

import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.util.nbt.NbtException;
import com.philia093.neofactory.util.nbt.NbtIo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The chunk files of a stored world.
 * <p>
 * Every chunk lives in its own file below {@link SaveFormat#CHUNK_FOLDER}, named
 * after its coordinates, for example {@code c.-3.7.dat}. The layout has two
 * consequences that both matter:
 * <ul>
 *     <li>a chunk can be written or read without touching any other chunk, which
 *         is what lets the world drop a distant chunk from memory and write it in
 *         the same step</li>
 *     <li>only chunks the player changed are on disk at all, so the size of a
 *         world follows what was built in it instead of how far the player
 *         walked</li>
 * </ul>
 * A file per chunk costs one system call per chunk instead of one per world, which
 * is why this is the layout of a first step. Trading it for region files that pack
 * many chunks into one file later only replaces this class, nothing above it knows
 * the difference.
 */
public final class ChunkStorage {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Pattern of a chunk file name, used to find the stored chunks. */
    private static final Pattern FILE_PATTERN = Pattern.compile("c\\.(-?\\d+)\\.(-?\\d+)\\.dat");

    /** Folder holding the chunk files. */
    private final File folder;

    /**
     * Creates storage for a save game.
     *
     * @param worldFolder folder of the save game
     */
    public ChunkStorage(File worldFolder) {
        this.folder = new File(worldFolder, SaveFormat.CHUNK_FOLDER);
    }

    /** Folder holding the chunk files. */
    public File folder() {
        return folder;
    }

    /**
     * {@code true} when a file for these coordinates exists.
     *
     * @param chunkX chunk coordinate along the first horizontal axis
     * @param chunkY chunk coordinate along the second horizontal axis
     */
    public boolean hasChunk(int chunkX, int chunkY) {
        return fileOf(chunkX, chunkY).isFile();
    }

    /**
     * Reads a chunk.
     *
     * @param chunkX chunk coordinate along the first horizontal axis
     * @param chunkY chunk coordinate along the second horizontal axis
     * @return the stored tag tree, or {@code null} when the chunk cannot be read
     */
    public NbtCompound read(int chunkX, int chunkY) {
        File file = fileOf(chunkX, chunkY);
        if (!file.isFile()) {
            return null;
        }
        try {
            return (NbtCompound) NbtIo.readGzip(file);
        } catch (NbtException | ClassCastException e) {
            // A single damaged chunk is reported and rebuilt from the seed instead
            // of taking the whole world down with it.
            LOGGER.error("Chunk ({}, {}) cannot be read from {}, it is generated again",
                    chunkX, chunkY, file, e);
            return null;
        }
    }

    /**
     * Writes a chunk, replacing the file that is there.
     *
     * @param chunkX chunk coordinate along the first horizontal axis
     * @param chunkY chunk coordinate along the second horizontal axis
     * @param data tag tree of that chunk
     * @throws SaveException when the file cannot be written
     */
    public void write(int chunkX, int chunkY, NbtCompound data) {
        AtomicNbtFile.write(data, fileOf(chunkX, chunkY));
    }

    /**
     * Coordinates of every stored chunk.
     *
     * @return pairs of chunk coordinates, {@code {x, y}}, in file order
     */
    public List<int[]> coordinates() {
        List<int[]> found = new ArrayList<>();
        File[] files = folder.listFiles();
        if (files == null) {
            return found;
        }
        for (File file : files) {
            Matcher matcher = FILE_PATTERN.matcher(file.getName());
            if (!matcher.matches()) {
                continue;
            }
            found.add(new int[] {Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2))});
        }
        return found;
    }

    /**
     * Amount of stored chunks.
     * <p>
     * Counted from the file names instead of from memory so that the number stays
     * right after a world was opened and no chunk was loaded yet.
     */
    public int count() {
        return coordinates().size();
    }

    /**
     * Amount of bytes the chunk files use together.
     *
     * @return the size in bytes, {@code 0} for a world without stored chunks
     */
    public long sizeBytes() {
        File[] files = folder.listFiles();
        if (files == null) {
            return 0L;
        }
        long total = 0L;
        for (File file : files) {
            if (file.isFile()) {
                total += file.length();
            }
        }
        return total;
    }

    /** File holding a chunk. */
    private File fileOf(int chunkX, int chunkY) {
        return new File(folder, "c." + chunkX + "." + chunkY + ".dat");
    }
}
