package com.philia093.neofactory.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the two numbers a frame of the sky is built from: the buffer the pass writes into and the layer of
 * clouds that is written there.
 * <p>
 * A sky is drawn as it is drawn and kept nowhere, so the amount of it is a number of the code: the layer of
 * clouds around the camera and the two bodies of the sky have to fit into the buffer of the pass. The case
 * that once broke every frame - a buffer of three vertices and a layer of a hundred clouds - is the very
 * case of the first check here, and no window is opened to answer it.
 */
class SkyRendererTest {

    @Test
    void theWholeSkyOfAFrameFitsIntoTheBufferOfThePass() {
        int needed = SkyRenderer.bodyVertices() + SkyRenderer.cloudLayerVertices();

        assertTrue(needed <= SkyRenderer.MAX_VERTICES,
                "the sky of a frame needs " + needed + " vertices and the buffer holds "
                        + SkyRenderer.MAX_VERTICES);
    }

    @Test
    void aLayerOfCloudsIsNotBuiltFromSingleTriangles() {
        assertTrue(SkyRenderer.cloudLayerVertices()
                >= SkyRenderer.VERTICES_PER_CELL * SkyRenderer.VERTICES_PER_TRIANGLE,
                "a layer of clouds is at least one whole cloud of triangles");
        assertTrue(SkyRenderer.bodyVertices() >= SkyRenderer.VERTICES_PER_SQUARE,
                "a body of the sky is a square and not a triangle");
    }

    @Test
    void theLayerOfCloudsKeepsItsShapeAndItsGaps() {
        int cells = SkyRenderer.CLOUD_RADIUS * 2 + 1;
        int clouded = 0;
        for (int cellX = 0; cellX < cells; cellX++) {
            for (int cellZ = 0; cellZ < cells; cellZ++) {
                boolean cloud = SkyRenderer.cloudAt(cellX, cellZ);
                assertTrue(cloud == SkyRenderer.cloudAt(cellX, cellZ),
                        "the pattern of the clouds is the same in every frame");
                if (cloud) {
                    clouded++;
                }
            }
        }
        int total = cells * cells;
        assertTrue(clouded > total / 2, "the layer carries more clouds than gaps");
        assertTrue(clouded < total, "and it has gaps at all");
    }
}
