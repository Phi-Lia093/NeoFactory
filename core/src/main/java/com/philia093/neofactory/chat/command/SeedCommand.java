package com.philia093.neofactory.chat.command;

import java.util.List;

/**
 * Shows the seed of the world the player is in.
 * <p>
 * The seed is what the terrain was generated from, so writing it down lets the same
 * landscape be played again.
 */
public final class SeedCommand implements Command {

    @Override
    public String name() {
        return "seed";
    }

    @Override
    public String usage() {
        return "/seed";
    }

    @Override
    public String description() {
        return "Shows the seed of this world";
    }

    @Override
    public void run(CommandContext context, List<String> args) {
        context.log().addSystem("Seed: " + context.seed());
    }
}
