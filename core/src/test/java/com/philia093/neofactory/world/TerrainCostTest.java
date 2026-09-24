package com.philia093.neofactory.world;

import com.philia093.neofactory.render.MeshData;
import com.philia093.neofactory.render.SectionMesher;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.Constants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Reports how long it takes to make a chunk and to mesh its sections.
 * <p>
 * This is the tool behind the numbers of the terrain: a world of cubes costs work per column when it is
 * made and per section when it is meshed, and both happen while a player walks through the world. The
 * assertions are loose on purpose - they catch a step that became ten times as expensive, not the
 * difference between two machines.
 */
class TerrainCostTest {

    /** Seed the probe measures with. */
    private static final int SEED = 20_260_924;

    /** Chunks measured per run. */
    private static final int CHUNKS = 6;

    /** Heights measured per chunk, the sections a column can hold. */
    private static final int SECTIONS = Constants.SECTION_COUNT;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void makingAChunkAndMeshingItStayWithinTheirBudget() {
        World world = new World(SEED);
        long started = System.nanoTime();
        long madeChunks = 0;
        for (int index = 0; index < CHUNKS; index++) {
            world.loadChunk(index, 0);
            madeChunks++;
        }
        double perChunk = (System.nanoTime() - started) / 1_000_000.0 / madeChunks;

        long meshing = System.nanoTime();
        long sections = 0;
        long cells = 0;
        for (int index = 0; index < CHUNKS; index++) {
            Chunk chunk = world.loadChunk(index, 0);
            for (int sectionY = 0; sectionY < SECTIONS; sectionY++) {
                if (chunk.isEmptySection(sectionY)) {
                    continue;
                }
                int originY = Constants.MIN_Y + sectionY * Section.SIZE;
                SectionMesher.Blocks blocks = (x, y, z) -> world.peekBlock(chunk.originX() + x,
                        originY + y, chunk.originZ() + z);
                List<MeshData> data = SectionMesher.build(chunk.section(sectionY), chunk.originX(),
                        originY, chunk.originZ(), blocks, picture -> 0);
                for (MeshData mesh : data) {
                    cells += mesh.vertexCount() / 4;
                }
                sections++;
            }
        }
        double perSection = (System.nanoTime() - meshing) / 1_000_000.0 / sections;

        System.out.println("A chunk costs " + String.format("%.2f", perChunk)
                + " ms to make, a section costs " + String.format("%.2f", perSection)
                + " ms to mesh (" + sections + " sections, " + cells + " faces)");

        assertTrue(perChunk < 40.0, "making a chunk takes " + perChunk + " ms");
        assertTrue(perSection < 12.0, "meshing a section takes " + perSection + " ms");
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
