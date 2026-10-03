package com.philia093.neofactory.machine;

/**
 * A machine that blows its spent steam out of a side of its block, which has to stay free.
 * <p>
 * The steam a machine of the age of steam spends is gone: it leaves the block through the side a player gave
 * to the vent and is caught by nothing, see {@code FaceConfig}. <b>The way out has to be open</b>, and a
 * machine cannot see the world it stands in: the block entity is what looks at that side and tells the
 * machine, which is what this interface is for.
 * <ul>
 *     <li>{@link #takesAnExhaustCheck()} is the machine saying that it just blew steam out and that the block
 *         has to look at the vent now;</li>
 *     <li>{@link #reportExhaust(boolean)} is the block answering what it found, which the machine keeps and
 *         refuses its next work with, see {@link MachineError#NO_EXHAUST};</li>
 *     <li>{@link #isWaitingForExhaust()} is the machine standing still with a wall in front of its vent,
 *         which makes the block keep looking until the way is open again.</li>
 * </ul>
 * A machine that runs on recipes asks for the check once a craft, because the steam of a craft goes out at
 * once, see {@link SteamMachine}. <b>The machines that make power have no vent at all</b>: the steam a turbine
 * drinks becomes the power of its line, so nothing of it is left to blow out, see {@link SteamTurbineMachine}.
 */
public interface ExhaustMachine {

    /** {@code true} while this machine stands still and waits for its vent to be free. */
    boolean isWaitingForExhaust();

    /**
     * Says how the vent of this machine was found, asked after the machine blew steam out of it.
     *
     * @param blocked {@code true} when a solid block stands in the way of the steam
     */
    void reportExhaust(boolean blocked);

    /**
     * Hands the check of the vent to the block, once after the steam went out.
     *
     * @return {@code true} when the block has to look at the vent of this machine now
     */
    boolean takesAnExhaustCheck();
}
