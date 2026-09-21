package com.philia093.neofactory.chat.command;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * The commands of the game, looked up by the name a player typed.
 * <p>
 * The words of a line are split on whitespace, the name is matched without regard
 * to case and the leading slash is optional, so {@code /Give}, {@code /give} and
 * {@code give} all reach the same command. Nothing a command does may break the
 * game: an unknown name and a command that throws both end as a line of the chat.
 * <p>
 * The registry writes no text of its own beyond those two failures, it hands the
 * arguments to the command, see {@link Command#run(CommandContext, List)}.
 */
public final class CommandRegistry {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Commands by their lower case name, in the order they were added. */
    private final Map<String, Command> commands = new LinkedHashMap<>();

    /**
     * Creates a registry holding the commands of the game.
     *
     * @return a registry with {@code /help}, {@code /give}, {@code /tp}, {@code /seed}
     *         and {@code /gamemode}
     */
    public static CommandRegistry withDefaults() {
        CommandRegistry registry = new CommandRegistry();
        registry.register(new HelpCommand(registry));
        registry.register(new GiveCommand());
        registry.register(new TpCommand());
        registry.register(new SeedCommand());
        registry.register(new GamemodeCommand());
        return registry;
    }

    /**
     * Adds a command.
     *
     * @param command command to add
     * @throws IllegalStateException when the name is used twice
     */
    public void register(Command command) {
        Objects.requireNonNull(command, "command");
        String name = command.name().toLowerCase(Locale.ROOT);
        if (commands.put(name, command) != null) {
            throw new IllegalStateException("Command /" + name + " is registered twice");
        }
    }

    /**
     * Looks a command up by name.
     *
     * @param name name of the command, a leading slash is ignored
     * @return the command, or {@code null} when no command uses that name
     */
    public Command byName(String name) {
        if (name == null) {
            return null;
        }
        String key = name.trim().toLowerCase(Locale.ROOT);
        if (key.startsWith("/")) {
            key = key.substring(1);
        }
        return commands.get(key);
    }

    /** Every command, in the order it was added. */
    public List<Command> all() {
        return List.copyOf(commands.values());
    }

    /** Amount of commands the registry holds. */
    public int size() {
        return commands.size();
    }

    /**
     * Runs a line the player typed behind a slash.
     * <p>
     * The leading slash is optional, the words may be separated by any amount of
     * whitespace. A line without a name, an unknown name and a command that throws
     * all answer with an error line instead of doing nothing silently.
     *
     * @param line the typed line, with or without the leading slash
     * @param context world, player and chat the command works with
     */
    public void run(String line, CommandContext context) {
        Objects.requireNonNull(context, "context");
        String trimmed = line == null ? "" : line.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1).trim();
        }
        if (trimmed.isEmpty()) {
            context.log().addError("Type a command, for example \"/help\".");
            return;
        }
        String[] parts = trimmed.split("\\s+");
        Command command = byName(parts[0]);
        if (command == null) {
            context.log().addError("Unknown command \"/" + parts[0] + "\". Type \"/help\" for a list.");
            return;
        }
        List<String> args = parts.length > 1
                ? List.of(Arrays.copyOfRange(parts, 1, parts.length))
                : List.of();
        try {
            command.run(context, args);
        } catch (RuntimeException e) {
            LOGGER.warn("Command /{} failed", command.name(), e);
            context.log().addError("Command \"/" + command.name() + "\" failed: " + e);
        }
    }

    @Override
    public String toString() {
        return "CommandRegistry(" + commands.size() + " commands: " + commands.keySet() + ")";
    }
}
