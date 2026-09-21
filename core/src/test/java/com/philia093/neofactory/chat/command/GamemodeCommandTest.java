package com.philia093.neofactory.chat.command;

import com.philia093.neofactory.chat.ChatMessage;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.support.TestCommandContext;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.world.GameMode;
import com.philia093.neofactory.world.save.LevelData;
import com.philia093.neofactory.world.save.SaveTags;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@code /gamemode} and the mode a world is stored with.
 * <p>
 * The command is driven through a hand written context, so no window is needed; the
 * stored mode is checked by writing level data and reading it back, which is the same
 * way {@code WorldSaver} and the loader do it.
 */
class GamemodeCommandTest {

    private final CommandRegistry registry = CommandRegistry.withDefaults();
    private final TestCommandContext context = new TestCommandContext();

    @Test
    void aWorldIsPlayedInSurvivalUntilAPlayerSaysOtherwise() {
        assertEquals(GameMode.SURVIVAL, new LevelData().gameMode());
    }

    @Test
    void theCommandSwitchesToCreativeAndBack() {
        registry.run("/gamemode creative", context);
        assertEquals(GameMode.CREATIVE, context.gameMode());
        assertEquals(ChatMessage.Kind.SYSTEM, context.lastLine().kind());

        registry.run("/gamemode survival", context);
        assertEquals(GameMode.SURVIVAL, context.gameMode());
    }

    @Test
    void theCommandIgnoresTheCaseOfTheName() {
        registry.run("/gamemode CREATIVE", context);

        assertEquals(GameMode.CREATIVE, context.gameMode());
    }

    @Test
    void theCommandAnswersWithTheModeWhenItGetsNoName() {
        registry.run("/gamemode", context);

        assertEquals(ChatMessage.Kind.ERROR, context.lastLine().kind());
        assertTrue(context.lastLine().text().contains("survival"), "the answer names the mode");
    }

    @Test
    void theCommandRefusesANameItDoesNotKnow() {
        registry.run("/gamemode flying", context);

        assertEquals(ChatMessage.Kind.ERROR, context.lastLine().kind());
        assertEquals(GameMode.SURVIVAL, context.gameMode(), "the mode stays as it was");
    }

    @Test
    void switchingToTheModeThatIsAlreadySetSaysSo() {
        registry.run("/gamemode survival", context);

        assertTrue(context.lastLine().text().contains("already"), context.lastLine().text());
    }

    @Test
    void theModeIsStoredWithTheWorld() {
        LevelData data = new LevelData();
        data.setGameMode(GameMode.CREATIVE);

        LevelData read = LevelData.read(data.write(new PlayerInventory()));

        assertEquals(GameMode.CREATIVE, read.gameMode());
    }

    @Test
    void aWorldWithoutAModeIsReadAsSurvival() {
        LevelData data = new LevelData();
        data.setGameMode(GameMode.CREATIVE);
        NbtCompound root = data.write(new PlayerInventory());
        // A stored world may miss the entry of the mode; the reader falls back to
        // survival instead of refusing the whole file.
        root.remove(SaveTags.GAME_MODE);

        assertEquals(GameMode.SURVIVAL, LevelData.read(root).gameMode());
    }

    @Test
    void aModeIsFoundByTheNameAPlayerTypes() {
        assertEquals(GameMode.CREATIVE, GameMode.byName("creative"));
        assertEquals(GameMode.SURVIVAL, GameMode.byName(" Survival "));
        assertNull(GameMode.byName("flying"));
        assertNull(GameMode.byName(null));
    }
}
