package com.philia093.neofactory.chat.command;

import com.philia093.neofactory.chat.ChatLog;
import com.philia093.neofactory.chat.ChatMessage;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.support.TestCommandContext;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Tests the four commands of the game, driven through a hand written context. */
class ChatCommandsTest {

    private final CommandRegistry registry = CommandRegistry.withDefaults();
    private final TestCommandContext context = new TestCommandContext();

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void theRegistryHoldsTheFiveCommands() {
        assertEquals(5, registry.size());
        assertEquals(5, registry.all().size());
    }

    @Test
    void helpListsEveryCommand() {
        registry.run("/help", context);

        List<ChatMessage> lines = context.log.visibleLines(true);
        assertEquals(1 + registry.size(), lines.size(), "a heading and one line per command");
        assertEquals("Commands:", lines.get(0).text());
        assertEquals(ChatMessage.Kind.SYSTEM, lines.get(0).kind());
        assertEquals("  /help [command] - Lists the commands, or explains one of them",
                lines.get(1).text(), "every command is listed with its usage");
    }

    @Test
    void helpExplainsASingleCommand() {
        registry.run("/help give", context);

        assertEquals("/give <item> [count] - Puts items into the inventory, "
                + "for example \"/give planks_oak 32\"", context.lastLine().text());
    }

    @Test
    void helpReportsAnUnknownCommand() {
        registry.run("/help nope", context);

        assertEquals(ChatMessage.Kind.ERROR, context.lastLine().kind());
    }

    @Test
    void givePutsItemsIntoTheInventory() {
        registry.run("/give planks_oak 10", context);

        assertEquals(10, context.player.inventory().countOf(Items.PLANKS_OAK));
        assertEquals(ChatMessage.Kind.SYSTEM, context.lastLine().kind());
        assertEquals("Gave 10 Oak Planks.", context.lastLine().text());
    }

    @Test
    void giveHandsOutASingleItemWithoutACount() {
        registry.run("/give planks_oak", context);

        assertEquals(1, context.player.inventory().countOf(Items.PLANKS_OAK));
    }

    @Test
    void giveFillsTheInventoryStackByStack() {
        registry.run("/give dirt 100", context);

        assertEquals(100, context.player.inventory().countOf(Items.DIRT),
                "a count larger than one stack is shared out");
        assertEquals(64, context.player.inventory().get(0).count());
    }

    @Test
    void giveReportsAnUnknownItem() {
        registry.run("/give not_an_item 3", context);

        assertEquals(ChatMessage.Kind.ERROR, context.lastLine().kind());
        assertEquals("There is no item called \"not_an_item\".", context.lastLine().text());
    }

    @Test
    void giveReportsACountThatIsNotANumber() {
        registry.run("/give stone many", context);

        assertEquals(ChatMessage.Kind.ERROR, context.lastLine().kind());
        assertEquals(0, context.player.inventory().countOf(Items.STONE));
    }

    @Test
    void giveReportsAFullInventoryWithoutCopyingAnything() {
        for (int slot = 0; slot < context.player.inventory().size(); slot++) {
            context.player.inventory().set(slot, ItemStack.of(Items.STONE, 64));
        }

        registry.run("/give planks_oak 5", context);

        assertEquals(ChatMessage.Kind.ERROR, context.lastLine().kind());
        assertEquals("Unable to give Oak Planks: the inventory is full.", context.lastLine().text());
        assertEquals(0, context.player.inventory().countOf(Items.PLANKS_OAK));
    }

    @Test
    void giveStopsAtTheAmountTheInventoryCanHold() {
        registry.run("/give planks_oak 100000", context);

        int capacity = Items.PLANKS_OAK.maxStackSize() * context.player.inventory().size();
        assertEquals(capacity, context.player.inventory().countOf(Items.PLANKS_OAK));
        assertEquals(ChatMessage.Kind.SYSTEM, context.lastLine().kind());
    }

    @Test
    void teleportMovesThePlayerToABlock() {
        context.player.position().set(1.0f, 2.0f);
        context.player.velocity().set(5.0f, 5.0f);

        registry.run("/tp 12 34", context);

        assertEquals(192.0f, context.player.position().x, 0.001f);
        assertEquals(544.0f, context.player.position().y, 0.001f);
        assertEquals(0.0f, context.player.velocity().x, 0.001f, "the walk is stopped");
        assertEquals("Teleported to block (12, 34).", context.lastLine().text());
    }

    @Test
    void teleportTakesDecimalBlocks() {
        registry.run("/tp 1.5 -2", context);

        assertEquals(24.0f, context.player.position().x, 0.001f);
        assertEquals(-32.0f, context.player.position().y, 0.001f);
        assertEquals("Teleported to block (1.50, -2).", context.lastLine().text());
    }

    @Test
    void teleportReportsMissingCoordinates() {
        registry.run("/tp 12", context);

        assertEquals(ChatMessage.Kind.ERROR, context.lastLine().kind());
        assertEquals("Usage: /tp <x> <y>", context.lastLine().text());
    }

    @Test
    void teleportReportsACoordinateThatIsNotANumber() {
        registry.run("/tp here 4", context);

        assertEquals(ChatMessage.Kind.ERROR, context.lastLine().kind());
        assertEquals("Usage: /tp <x> <y> - \"here\" is not a block coordinate.",
                context.lastLine().text());
    }

    @Test
    void teleportRefusesAWildCoordinate() {
        registry.run("/tp 999999999 0", context);

        assertEquals(ChatMessage.Kind.ERROR, context.lastLine().kind());
    }

    @Test
    void seedShowsTheSeedOfTheWorld() {
        registry.run("/seed", context);

        assertEquals("Seed: 1234", context.lastLine().text());
    }

    @Test
    void aCommandWritesExactlyOneAnswerLine() {
        int before = context.log.size();

        registry.run("/tp 4 4", context);

        assertEquals(before + 1, context.log.size());
    }
}
