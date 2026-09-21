package com.philia093.neofactory.world.save;

import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.nbt.NbtCompound;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Checks that a stored world of the right version opens and everything else does
 * not.
 * <p>
 * The game is still being built, so its format changes as the factory systems
 * arrive and nothing is converted: a file of another version has to be refused with
 * a clear reason instead of being read halfway. Catching that here is what keeps a
 * forgotten bump of {@link SaveFormat#DATA_VERSION} from turning into a world that
 * loads and then behaves strangely.
 */
class SaveVersionTest {

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void aLevelFileOfThisBuildIsRead() {
        LevelData data = new LevelData();
        data.setSeed(4711);

        LevelData read = LevelData.read(data.write(new PlayerInventory()));

        assertEquals(4711, read.seed());
    }

    @Test
    void aLevelFileOfAnotherFormatIsRefused() {
        NbtCompound root = new LevelData().write(new PlayerInventory());
        root.putInt(SaveTags.DATA_VERSION, SaveFormat.DATA_VERSION + 1);

        assertThrows(SaveException.class, () -> LevelData.read(root));
    }

    @Test
    void aLevelFileWithoutAVersionIsRefused() {
        NbtCompound root = new LevelData().write(new PlayerInventory());
        root.remove(SaveTags.DATA_VERSION);

        assertThrows(SaveException.class, () -> LevelData.read(root));
    }
}
