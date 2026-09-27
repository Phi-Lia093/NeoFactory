package com.philia093.neofactory.chat.command;

import com.philia093.neofactory.world.DayCycle;

import java.util.List;
import java.util.Locale;

/**
 * Shows the hour of the world and moves it.
 * <p>
 * Without an argument the command answers with the clock of the world, which is how a player finds out
 * whether the sun is about to go down. {@code set} jumps to an hour, either by name - the day starts at
 * {@code dawn}, and {@code morning}, {@code noon}, {@code dusk}, {@code night} and {@code midnight} are the
 * hours the game names itself - or at a tick of the day. {@code add} moves the hour on without touching the
 * day, which is the one thing a player wants while a night is worked through.
 * <p>
 * The sun and the sky follow at once: the light of the sky is scaled in the shader and never written into a
 * mesh, so jumping to the noon of the next day costs no mesh of the world a single triangle, see
 * {@code BlockShader#skyBrightness(float)} and {@code DayCycle}.
 */
public final class TimeCommand implements Command {

    @Override
    public String name() {
        return "time";
    }

    @Override
    public String usage() {
        return "/time [set <dawn|morning|noon|dusk|night|midnight|ticks>|add <ticks>]";
    }

    @Override
    public String description() {
        return "Shows the hour of the world, or moves it";
    }

    @Override
    public void run(CommandContext context, List<String> args) {
        if (args.isEmpty()) {
            context.log().addSystem("It is " + DayCycle.describe(context.worldTime()) + ".");
            return;
        }
        String mode = args.get(0).toLowerCase(Locale.ROOT);
        if (args.size() != 2) {
            context.log().addError("Usage: " + usage());
            return;
        }
        String value = args.get(1);
        if (mode.equals("set")) {
            Long ticks = ticksOf(value);
            if (ticks == null) {
                context.log().addError("\"" + value + "\" is not an hour of the day. Use \"dawn\","
                        + " \"morning\", \"noon\", \"dusk\", \"night\", \"midnight\" or a tick of the day.");
                return;
            }
            // A named hour and a tick of the day both stand inside one day, so the day of the world is kept:
            // a player who jumps to the noon of a new day stays on the day they are on.
            long dayOfTheWorld = Math.floorDiv(context.worldTime(), (long) DayCycle.DAY_TICKS);
            context.setWorldTime(dayOfTheWorld * DayCycle.DAY_TICKS + DayCycle.ofDay(ticks));
            context.log().addSystem("The time is now " + DayCycle.describe(context.worldTime()) + ".");
            return;
        }
        if (mode.equals("add")) {
            Long ticks = number(value);
            if (ticks == null || ticks <= 0L) {
                context.log().addError("\"" + value + "\" is not an amount of ticks to add.");
                return;
            }
            context.setWorldTime(context.worldTime() + ticks);
            context.log().addSystem("The time is now " + DayCycle.describe(context.worldTime()) + ".");
            return;
        }
        context.log().addError("Usage: " + usage());
    }

    /**
     * Reads the hour an argument names.
     *
     * @param value the typed hour, a name or a tick of the day
     * @return the tick of the day, or {@code null} when the argument names no hour
     */
    private static Long ticksOf(String value) {
        String name = value.toLowerCase(Locale.ROOT);
        if (name.equals("dawn") || name.equals("sunrise")) {
            return (long) DayCycle.SUNRISE;
        }
        if (name.equals("morning") || name.equals("day")) {
            return (long) DayCycle.NEW_WORLD_TIME;
        }
        if (name.equals("noon")) {
            return (long) DayCycle.NOON;
        }
        if (name.equals("dusk") || name.equals("sunset")) {
            return (long) DayCycle.SUNSET;
        }
        if (name.equals("night")) {
            return (long) (DayCycle.SUNSET + DayCycle.TWILIGHT_TICKS);
        }
        if (name.equals("midnight")) {
            return (long) DayCycle.MIDNIGHT;
        }
        Long ticks = number(value);
        return ticks == null || ticks < 0L ? null : ticks;
    }

    /**
     * Reads a whole number.
     *
     * @param value the typed number
     * @return the number, or {@code null} when the argument is not a number
     */
    private static Long number(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
