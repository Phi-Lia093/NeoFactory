package com.philia093.neofactory.chat.command;

import com.philia093.neofactory.world.GameMode;

import java.util.List;

/**
 * Switches how the world is played.
 * <p>
 * The mode decides how a player gets their items: in survival they break blocks and
 * craft, in creative they open the creative inventory with the inventory key and take
 * what they want, see
 * {@link com.philia093.neofactory.gui.CreativeInventoryGui}. The choice is stored with
 * the world, so a world that was switched keeps its mode on the next visit.
 * <p>
 * Without an argument the command answers with the mode the world is in, which is how a
 * player finds out what is going on.
 */
public final class GamemodeCommand implements Command {

    @Override
    public String name() {
        return "gamemode";
    }

    @Override
    public String usage() {
        return "/gamemode <survival|creative>";
    }

    @Override
    public String description() {
        return "Switches how the world is played, \"creative\" grants every item";
    }

    @Override
    public void run(CommandContext context, List<String> args) {
        if (args.isEmpty()) {
            context.log().addError("Usage: " + usage() + " - the world is in "
                    + context.gameMode().modeName() + " mode.");
            return;
        }
        GameMode mode = GameMode.byName(args.get(0));
        if (mode == null) {
            context.log().addError("\"" + args.get(0) + "\" is not a game mode."
                    + " Use \"survival\" or \"creative\".");
            return;
        }
        if (mode == context.gameMode()) {
            context.log().addSystem("The world is already in " + mode.modeName() + " mode.");
            return;
        }
        context.setGameMode(mode);
        if (mode == GameMode.CREATIVE) {
            context.log().addSystem("The world is now in creative mode:"
                    + " press the inventory key for every item of the game.");
        } else {
            context.log().addSystem("The world is now in survival mode.");
        }
    }
}
