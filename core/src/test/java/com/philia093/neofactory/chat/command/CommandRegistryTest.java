package com.philia093.neofactory.chat.command;

import com.philia093.neofactory.chat.ChatMessage;
import com.philia093.neofactory.support.TestCommandContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests how a typed line is split, looked up and handed to a command. */
class CommandRegistryTest {

    private final CommandRegistry registry = new CommandRegistry();
    private final TestCommandContext context = new TestCommandContext();

    @Test
    void runsACommandWithoutTheSlash() {
        CountingCommand command = new CountingCommand("count");
        registry.register(command);

        registry.run("count a b", context);

        assertEquals(1, command.runs);
        assertEquals(List.of("a", "b"), command.lastArgs);
    }

    @Test
    void splitsOnAnyAmountOfWhitespace() {
        CountingCommand command = new CountingCommand("count");
        registry.register(command);

        registry.run("   /count    a     b  ", context);

        assertEquals(1, command.runs);
        assertEquals(List.of("a", "b"), command.lastArgs);
    }

    @Test
    void matchesTheNameWithoutRegardToCase() {
        CountingCommand command = new CountingCommand("Count");
        registry.register(command);

        registry.run("/COUNT", context);

        assertEquals(1, command.runs, "the name is matched without regard to case");
        assertNotNull(registry.byName("/CoUnT"), "and the slash is optional in a lookup");
        assertNull(registry.byName("nope"));
        assertNull(registry.byName(null));
    }

    @Test
    void anUnknownCommandIsReported() {
        registry.run("/nope", context);

        assertEquals(ChatMessage.Kind.ERROR, context.lastLine().kind());
        assertEquals("Unknown command \"/nope\". Type \"/help\" for a list.",
                context.lastLine().text());
    }

    @Test
    void aLineWithoutANameIsReported() {
        registry.run("   ", context);

        assertEquals(ChatMessage.Kind.ERROR, context.lastLine().kind());
        assertEquals("Type a command, for example \"/help\".", context.lastLine().text());
    }

    @Test
    void aCommandThatThrowsBecomesAnErrorLine() {
        registry.register(new Command() {
            @Override
            public String name() {
                return "boom";
            }

            @Override
            public String usage() {
                return "/boom";
            }

            @Override
            public String description() {
                return "Always fails";
            }

            @Override
            public void run(CommandContext failingContext, List<String> args) {
                throw new IllegalStateException("no");
            }
        });

        registry.run("/boom", context);

        assertEquals(ChatMessage.Kind.ERROR, context.lastLine().kind());
        assertTrue(context.lastLine().text().startsWith("Command \"/boom\" failed: "),
                "the failure is reported instead of breaking the game");
    }

    @Test
    void aNameCannotBeUsedTwice() {
        registry.register(new CountingCommand("count"));

        assertThrows(IllegalStateException.class, () -> registry.register(new CountingCommand("count")));
    }

    @Test
    void theRegistryOfTheGameHoldsTheFourCommands() {
        CommandRegistry commands = CommandRegistry.withDefaults();

        assertEquals(4, commands.size());
        assertNotNull(commands.byName("help"));
        assertNotNull(commands.byName("/give"));
        assertNotNull(commands.byName("tp"));
        assertNotNull(commands.byName("seed"));
    }

    /** A command that only counts how often it was called. */
    private static final class CountingCommand implements Command {

        private final String name;
        private int runs;
        private List<String> lastArgs = List.of();

        private CountingCommand(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String usage() {
            return "/" + name;
        }

        @Override
        public String description() {
            return "Counts its calls";
        }

        @Override
        public void run(CommandContext commandContext, List<String> args) {
            runs++;
            lastArgs = args;
        }
    }
}
