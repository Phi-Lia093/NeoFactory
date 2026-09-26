package com.philia093.neofactory.render;

import com.badlogic.gdx.utils.ObjectMap;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks how the pictures of the world are numbered once a strip of frames takes several layers.
 * <p>
 * A picture that moves is a strip of tiles down its file, one frame below the other, and each of them
 * takes a layer of the texture array, so the picture behind the strip starts as many layers further
 * down as the strip has frames. The numbering of {@link BlockPictures#pictureNames} is what keeps a
 * frame of one picture from being read as a frame of another, and the arithmetic of it never touches a
 * graphics card: the frame count is handed in here, which is the very count
 * {@link BlockPictures#frameCountOf(int, int)} reads out of a file while the game starts.
 */
class PictureFramesTest {

    /** Picture of the game that really is a strip: the gear on the top of a running grinder. */
    private static final String GEAR = "grinder/grinder_top_active";

    /** Frames that strip holds, the sheet of sixteen by sixty four pixels of the art pack. */
    private static final int GEAR_FRAMES = 4;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void aPictureOfOneTileHoldsOneFrame() {
        assertEquals(1, BlockPictures.frameCountOf(16, 16), "a tile is a picture that stands still");
        assertEquals(GEAR_FRAMES, BlockPictures.frameCountOf(16, 4 * 16), "four tiles are four frames");
        assertEquals(16, BlockPictures.frameCountOf(16, 256), "a long strip holds as many frames");
    }

    @Test
    void aPictureThatIsNoStripOfTilesHoldsOneFrame() {
        assertEquals(1, BlockPictures.frameCountOf(16, 0), "a file without height holds nothing");
        assertEquals(1, BlockPictures.frameCountOf(16, 40),
                "forty pixels are two tiles and a half and no strip of whole frames");
        assertEquals(1, BlockPictures.frameCountOf(32, 64),
                "a sheet that is two tiles across is packed differently and is scaled into one layer");
        assertEquals(1, BlockPictures.frameCountOf(8, 16), "a picture smaller than a tile stands still");
    }

    @Test
    void aStripPushesThePicturesBehindItDownTheArray() {
        ObjectMap<String, Integer> layers = new ObjectMap<>();
        ObjectMap<String, Integer> frames = new ObjectMap<>();

        List<String> names = BlockPictures.pictureNames(layers, frames, name -> true,
                name -> name.equals(GEAR) ? GEAR_FRAMES : 1);

        assertTrue(names.size() > 2, "the game holds more than two pictures, a list of two says nothing");
        assertTrue(names.contains(GEAR), "the gear of a grinder is a picture of the world");
        int next = 0;
        for (String name : names) {
            assertEquals(next, layers.get(name).intValue(),
                    "the run of the layers is unbroken at " + name);
            next += frames.get(name).intValue();
        }
        assertEquals(GEAR_FRAMES, frames.get(GEAR).intValue(), "the strip holds four frames");
        assertEquals(names.size() + GEAR_FRAMES - 1, next,
                "every picture takes one layer and the strip takes four of them");

        int gear = names.indexOf(GEAR);
        assertTrue(gear + 1 < names.size(), "the gear is followed by another picture");
        assertEquals(layers.get(GEAR).intValue() + GEAR_FRAMES, layers.get(names.get(gear + 1)).intValue(),
                "the picture behind the strip starts behind its whole run");
    }

    @Test
    void aCornerHasRoomForItsFrames() {
        assertEquals(10, MeshData.FRAMES,
                "the frames are the last number of a corner, which is where the attribute of the "
                        + "shader reads them");
        assertEquals(11, MeshData.FLOATS_PER_VERTEX, "a corner is position, picture, colour and frames");
        assertEquals(6, MeshData.RED, "the colour did not move, so the meshes of the past still fit");
    }
}
