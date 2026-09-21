package com.philia093.neofactory.chat.command;

import java.util.List;
import java.util.Objects;

/**
 * Lists the commands of the game.
 * <p>
 * Without an argument every command is listed, with a name behind it only that one
 * is described. The registry is handed in instead of being reached statically,
 * which keeps the command and its test free of a global state.
 */
public final class HelpCommand implements Command {

    private final CommandRegistry registry;

    /**
     * Creates the command.
     *
     * @param registry registry the listing is taken from
     */
    public HelpCommand(CommandRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @Override
    public String name() {
        return "help";
    }

    @Override
    public String usage() {
        return "/help [command]";
    }

    @Override
    public String description() {
        return "Lists the commands, or explains one of them";
    }

    @Override
    public void run(CommandContext context, List<String> args) {
        if (args.isEmpty()) {
            context.log().addSystem("Commands:");
            for (Command command : registry.all()) {
                context.log().addSystem("  " + command.usage() + " - " + command.description());
            }
            return;
        }
        Command command = registry.byName(args.get(0));
        if (command == null) {
            context.log().addError("Unknown command \"/" + args.get(0) + "\". Type \"/help\" for a list.");
            return;
        }
        context.log().addSystem(command.usage() + " - " + command.description());
    }
}
