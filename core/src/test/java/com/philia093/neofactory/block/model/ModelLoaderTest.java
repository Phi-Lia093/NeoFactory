package com.philia093.neofactory.block.model;

import com.philia093.neofactory.block.BlockFace;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks what the loader makes of a model file, without opening a window.
 * <p>
 * The interesting part is the chain of parents: a template is written once and every block names its
 * pictures, so the boxes of the parent have to be read with the pictures of the child. The rest of a
 * file - the window of a face, the layer above it, the direction a face is culled in, the turn of a
 * box - is checked here as well, because a mistake in any of them shows up as a wrong picture in the
 * world and nowhere else.
 */
class ModelLoaderTest {

    @Test
    void aModelInheritsTheBoxesOfItsParent() {
        Map<String, BlockModel> models = ModelLoader.read(Map.of(
                "template_cube_all", """
                        { "template": true,
                          "elements": [ { "from": [0, 0, 0], "to": [16, 16, 16],
                            "faces": { "up": { "texture": "#all", "cullface": "up" },
                                       "down": { "texture": "#all", "cullface": "down" } } } ] }
                        """,
                "stone", """
                        { "parent": "template_cube_all", "textures": { "all": "stone" } }
                        """));

        assertFalse(models.containsKey("template_cube_all"),
                "a template is no model a block could be drawn with");
        BlockModel stone = models.get("stone");
        assertNotNull(stone, "the child of the template was read");
        assertEquals(1, stone.boxes().size(), "the child takes the boxes of its parent");
        assertEquals("stone", stone.boxes().get(0).face(BlockFace.TOP).picture(),
                "and the picture of the child reaches the faces of the parent");
        assertNull(stone.boxes().get(0).face(BlockFace.NORTH),
                "a face the parent does not name stays out");
    }

    @Test
    void aChildOverridesThePicturesOfItsParent() {
        Map<String, BlockModel> models = ModelLoader.read(Map.of(
                "template_cube_bottom_top", """
                        { "template": true,
                          "elements": [ { "from": [0, 0, 0], "to": [16, 16, 16],
                            "faces": { "up": { "texture": "#top", "cullface": "up" },
                                       "down": { "texture": "#bottom", "cullface": "down" },
                                       "north": { "texture": "#side", "cullface": "north" } } } ] }
                        """,
                "grass", """
                        { "parent": "template_cube_bottom_top",
                          "textures": { "top": "grass_top", "bottom": "dirt", "side": "grass_side" },
                          "tint": true }
                        """));

        ModelBox box = models.get("grass").boxes().get(0);
        assertEquals("grass_top", box.face(BlockFace.TOP).picture());
        assertEquals("dirt", box.face(BlockFace.BOTTOM).picture());
        assertEquals("grass_side", box.face(BlockFace.NORTH).picture());
        assertTrue(box.face(BlockFace.TOP).tinted(),
                "the tint of the model reaches the faces of its parent");
        assertEquals(BlockFace.TOP, box.face(BlockFace.TOP).cullface(),
                "the direction a face is culled in is the one the file writes");
    }

    @Test
    void aPictureBehindAHashPointsAtTheNamedOne() {
        Map<String, BlockModel> models = ModelLoader.read(Map.of(
                "log_oak", """
                        { "textures": { "all": "log_oak", "end": "#all" },
                          "elements": [ { "from": [0, 0, 0], "to": [16, 16, 16],
                            "faces": { "up": { "texture": "#end" } } } ] }
                        """));

        assertEquals("log_oak", models.get("log_oak").boxes().get(0).face(BlockFace.TOP).picture(),
                "a name behind a hash stands for the picture it points at");
    }

    @Test
    void aFaceKeepsTheWindowAndTheOverlayOfItsFile() {
        Map<String, BlockModel> models = ModelLoader.read(Map.of(
                "grass", """
                        { "textures": { "side": "grass_side", "layer": "grass_side_overlay" },
                          "elements": [ { "from": [0, 0, 0], "to": [16, 16, 16],
                            "faces": { "north": { "texture": "#side", "overlay": "#layer",
                                                  "uv": [4, 8, 12, 16] } } } ] }
                        """));

        ModelFace face = models.get("grass").boxes().get(0).face(BlockFace.NORTH);
        assertEquals(0.25f, face.u0(), 1.0e-6f, "the window is read in sixteenths of the picture");
        assertEquals(0.5f, face.v0(), 1.0e-6f);
        assertEquals(0.75f, face.u1(), 1.0e-6f);
        assertEquals(1.0f, face.v1(), 1.0e-6f);
        assertEquals("grass_side_overlay", face.overlay(), "and the layer above it is kept");
    }

    @Test
    void aBoxIsTurnedByTheFile() {
        Map<String, BlockModel> models = ModelLoader.read(Map.of(
                "tall_grass", """
                        { "textures": { "cross": "tallgrass" },
                          "elements": [ { "from": [0.8, 0, 8], "to": [15.2, 16, 8],
                                          "rotation": { "origin": [8, 8, 8], "axis": "y",
                                                        "angle": 45 },
                                          "shade": false,
                                          "faces": { "north": { "texture": "#cross" },
                                                     "south": { "texture": "#cross" } } } ] }
                        """));

        ModelBox box = models.get("tall_grass").boxes().get(0);
        assertNotNull(box.rotation(), "the turn of the box was read");
        assertEquals(45.0f, box.rotation().angle(), 1.0e-6f);
        assertEquals('y', box.rotation().axis());
        assertFalse(box.shaded(), "a plant is drawn without the fall off of a cube");
        assertFalse(box.isWholeCube(), "a crossed plane is no whole cube and is never culled");
    }

    @Test
    void aFileThatCannotBeReadIsSkipped() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("broken", "{ this is not json");
        files.put("no_boxes", "{ \"textures\": { \"all\": \"stone\" } }");
        files.put("no_picture", """
                { "elements": [ { "from": [0, 0, 0], "to": [16, 16, 16],
                    "faces": { "up": { "texture": "#missing" } } } ] }
                """);
        files.put("no_such_parent", "{ \"parent\": \"nowhere\" }");
        files.put("outside", """
                { "textures": { "all": "stone" },
                  "elements": [ { "from": [0, 0, 0], "to": [17, 16, 16],
                    "faces": { "up": { "texture": "#all" } } } ] }
                """);
        files.put("good", """
                { "textures": { "all": "stone" },
                  "elements": [ { "from": [0, 0, 0], "to": [16, 16, 16],
                    "faces": { "up": { "texture": "#all" } } } ] }
                """);

        Map<String, BlockModel> models = ModelLoader.read(files);

        assertEquals(1, models.size(), "only the file that can be read survives");
        assertTrue(models.containsKey("good"));
    }
}
