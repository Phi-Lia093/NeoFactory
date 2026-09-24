package com.philia093.neofactory.chat.command;

import com.philia093.neofactory.entity.Player;
import com.philia093.neofactory.util.Constants;

import java.util.List;
import java.util.Locale;

/**
 * Moves the player to a place of the world.
 * <p>
 * The two numbers are block coordinates, the very same ones the debug log prints
 * for the player and a block, so {@code /tp 12 34} puts the player on block
 * {@code 12, 34}. Decimals are allowed, which makes the position exact; the values
  * are multiplied by {@link Constants#BLOCK_SIZE} because entities live in world
 * units. The speed is dropped as well, so the player never keeps the momentum of
 * the walk that came before the jump.
 */
public final class TpCommand implements Command {

    /** Amount of numbers the command needs. */
    private static final int ARGUMENTS = 2;

    /**
     * Largest coordinate a line may name.
     * <p>
     * Terrain is generated around the player, so a wild number would start to build
     * chunks far outside anything the player could ever visit.
     */
    private static final float COORDINATE_LIMIT = 100000.0f;

    @Override
    public String name() {
        return "tp";
    }

    @Override
    public String usage() {
        return "/tp <x> <y>";
    }

    @Override
    public String description() {
        return "Moves the player to a block position, for example \"/tp 0 0\"";
    }

    @Override
    public void run(CommandContext context, List<String> args) {
        if (args.size() < ARGUMENTS) {
            context.log().addError("Usage: " + usage());
            return;
        }
        Float x = parseCoordinate(args.get(0));
        Float y = parseCoordinate(args.get(1));
        if (x == null || y == null) {
            String wrong = x == null ? args.get(0) : args.get(1);
            context.log().addError("Usage: " + usage() + " - \"" + wrong
                    + "\" is not a block coordinate.");
            return;
        }
        Player player = context.player();
        player.position().set(x * Constants.BLOCK_SIZE, player.position().y, y * Constants.BLOCK_SIZE);
        player.velocity().setZero();
        player.halt();
        context.log().addSystem("Teleported to block (" + formatCoordinate(x) + ", "
                + formatCoordinate(y) + ").");
    }

    /** Reads a block coordinate, {@code null} when the word is not a sane number. */
    private static Float parseCoordinate(String value) {
        try {
            float coordinate = Float.parseFloat(value);
            if (!Float.isFinite(coordinate) || Math.abs(coordinate) > COORDINATE_LIMIT) {
                return null;
            }
            return coordinate;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Writes a coordinate without a pointless {@code .00} behind a whole number. */
    private static String formatCoordinate(float coordinate) {
        if (coordinate == Math.rint(coordinate)) {
            return Integer.toString((int) coordinate);
        }
        return String.format(Locale.ROOT, "%.2f", coordinate);
    }
}
