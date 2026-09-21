package com.philia093.neofactory.chat.command;

import java.util.List;

/**
 * One command the player can type behind a slash, for example {@code /give}.
 * <p>
 * A command receives the words that followed its name and does its work through
 * {@link CommandContext}: it never talks to a screen or to the chat itself, so a
 * test drives it with a hand written context, see
 * {@link CommandRegistry#run(String, CommandContext)}.
 */
public interface Command {

    /** Name the command is called with, without the slash, lower case. */
    String name();

    /** How the command is written, shown by {@code /help} and on a wrong call. */
    String usage();

    /** One sentence telling what the command does. */
    String description();

    /**
     * Runs the command.
     * <p>
     * Everything the player should read is written into
     * {@link CommandContext#log()}. A command that does not understand its arguments
     * writes an error line instead of throwing.
     *
     * @param context world, player and chat the command works with
     * @param args the words behind the name, possibly empty
     */
    void run(CommandContext context, List<String> args);
}
