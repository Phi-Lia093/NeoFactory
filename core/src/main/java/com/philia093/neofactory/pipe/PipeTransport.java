package com.philia093.neofactory.pipe;

import com.philia093.neofactory.fluid.Fluid;
import com.philia093.neofactory.pipe.Pipes.Pipe;
import com.philia093.neofactory.world.TickClock;

import java.util.Objects;

/**
 * The arithmetic of the fluid that runs through the pipes.
 * <p>
 * A pipe is a tube with a small storage of its own and no pump: every tick it offers what it holds to the
 * pipes it is joined to and each of them takes what fits. How much a pipe offers is what it moves a second,
 * {@link Pipe#flow()}, divided by the ticks of a second, and how that offer is divided between several
 * neighbours is a question of weight, {@link #split(int, int[], int[])}: <b>a junction splits what it has
 * the way the rates of the pipes behind it stand to each other,</b> so a line that meets a wide pipe and a
 * narrow one gives the wide one the larger part of the fluid and not an equal half.
 * <p>
 * <b>Everything here is arithmetic on numbers.</b> The class knows no world and no storage: it answers what
 * an amount divides into, what one tick of a rate comes to, whether a fluid bursts a pipe of a material and
 * what a neighbour weighs, and the pipe entity carries the answer out. That is what makes the rules of the
 * transport checkable without a world, see {@code PipeTransportTest}.
 * <p>
 * <b>A pipe that is filled faster than it can pass the fluid on holds what it has.</b> The storage of a
 * pipe is as large as its own rate, see {@code PipeBlockEntity}, so a wide pipe at the end of a narrow line
 * fills up and the narrow pipe stops being able to push: the line backs up instead of losing the fluid,
 * which is what a machine at the end of it notices.
 */
public final class PipeTransport {

    /**
     * Ticks a second moves of fluid happen in.
     * <p>
     * The world runs on fixed ticks, see {@link TickClock}, and the rate of a pipe is written in
     * millibuckets a second, so one tick of it is {@code rate / TICKS_PER_SECOND}. The number is the one the
     * world itself runs on and not a copy of it, so a world of another tick rate moves fluid with it.
     */
    public static final int TICKS_PER_SECOND = TickClock.TICKS_PER_SECOND;

    private PipeTransport() {
        // Utility class: never instantiated.
    }

    /**
     * How much a pipe of a rate moves in one tick.
     * <p>
     * A pipe that moves less than a tick's worth of fluid still moves one millibucket: a rate that rounded
     * away to nothing would be a pipe that never runs, and no pipe of the game is that.
     *
     * @param rate rate of a pipe in millibuckets a second
     * @return the amount of one tick, at least one millibucket
     */
    public static int perTick(int rate) {
        return Math.max(1, rate / TICKS_PER_SECOND);
    }

    /**
     * How much a pipe of a rate moves in a time.
     *
     * @param rate rate of a pipe in millibuckets a second
     * @param seconds time to move it in, never negative
     * @return the amount in millibuckets, never negative
     */
    public static float inTime(int rate, float seconds) {
        return rate * Math.max(0.0f, seconds);
    }

    /**
     * {@code true} when a fluid bursts a pipe.
     * <p>
     * A pipe carries a fluid while the fluid is no hotter than the material of the pipe takes, see
     * {@link PipeMaterial#maxTemperature()}: the steam of a boiler of three hundred and seventy three
     * kelvin bursts a wooden pipe of three hundred and fifty and runs in a copper one without trouble.
     *
     * @param pipe pipe the fluid is offered to, may be {@code null}
     * @param fluid fluid that is offered, {@code null} for an empty pipe
     * @return {@code true} when the pipe gives way
     */
    public static boolean bursts(Pipe pipe, Fluid fluid) {
        return pipe != null && fluid != null && fluid.hotterThan(pipe.maxTemperature());
    }

    /**
     * The weight a pipe draws fluid with.
     * <p>
     * A pipe is measured by what it moves a second: the wider the pipe behind a junction, the larger its
     * share of what the junction has to give. One is the least any neighbour weighs, so no pipe is ever
     * handed nothing.
     *
     * @param pipe pipe that takes the fluid, never {@code null}
     * @return the weight, at least one
     */
    public static int weightOf(Pipe pipe) {
        return Math.max(1, Objects.requireNonNull(pipe, "pipe").flow());
    }

    /**
     * The weight a machine draws fluid with, which is the rate of the pipe that pours into it.
     * <p>
     * <b>A machine names no rate of its own</b>, and weighing it as one - which is what this used to do -
     * made its share round away as soon as a branch of the line stood beside it: a pipe that both pours into
     * a machine and carries on to the next pipe hands its fluid out by weight, and a weight of one against
     * the four hundred of a bronze pipe is a share of a quarter of a millibucket, which is no millibucket at
     * all. A machine in the middle of a line was left dry that way, while the machines at the two ends of the
     * line were served.
     * <p>
     * A machine is therefore measured by what the pipe that reaches it moves in one tick: it draws as much as
     * a branch of the line would, so every machine beside a line is served, and a machine whose tank is full
     * takes nothing more while the fluid carries on down the line.
     *
     * @param rate rate of the pipe that pours, in millibuckets a second
     * @return the weight, at least one
     */
    public static int weightOfMachine(int rate) {
        return Math.max(1, rate);
    }

    /**
     * How full a pipe is, the pressure the line is under.
     *
     * @param amount fluid in the pipe
     * @param capacity what the pipe holds when it is full
     * @return the share of the pipe that is filled, {@code 0} to {@code 1}
     */
    public static float level(int amount, int capacity) {
        if (capacity <= 0) {
            return amount > 0 ? 1.0f : 0.0f;
        }
        return Math.max(0.0f, Math.min(1.0f, (float) amount / capacity));
    }

    /**
     * {@code true} when a pipe hands fluid over to a neighbour at all.
     * <p>
     * A pipe carries what it is given and a line has no pump, so a pipe hands nothing <i>uphill</i>: it
     * gives to the pipes that stand under less pressure than it does itself, which is what makes a line
     * fill from the end it is fed at and stop where the fluid stands as high as the pipe that feeds it.
     * Without the rule a pipe and the pipe next to it would hand the same fluid back and forth for ever,
     * because a pipe that was just given something is as willing to give as the one that gave it.
     * <p>
     * A neighbour that is no pipe counts as empty: the tank of a machine is a place fluid may run to, and
     * the transport of a machine asks with a pressure of its own, see {@code FluidNode}.
     *
     * @param amount fluid in the pipe that gives
     * @param capacity what that pipe holds when it is full
     * @param theirs fluid in the pipe that would take it
     * @param theirCapacity what that pipe holds when it is full
     * @return {@code true} when the fluid runs downhill
     */
    public static boolean flowsTo(int amount, int capacity, int theirs, int theirCapacity) {
        return level(amount, capacity) > level(theirs, theirCapacity);
    }

    /**
     * Divides an amount of fluid between neighbours the way their weights stand to each other.
     * <p>
     * The shares are handed out one after the other and every one of them is the part of what is left that
     * its weight has of the weight that is left: a pipe that moves four hundred millibuckets a second and
     * one that moves twelve hundred divide a thousand between them as two hundred and fifty and seven
     * hundred and fifty. The last neighbour takes the odd millibucket that rounding leaves over, so the
     * shares always add up to the amount and never to more, and a neighbour whose weight is zero gets
     * nothing.
     *
     * @param amount amount of fluid to divide, never negative
     * @param weights weight of every neighbour, read up to the length of {@code shares}
     * @param shares array the shares are written into, in the order of the weights
     */
    public static void split(int amount, int[] weights, int[] shares) {
        int left = Math.max(0, amount);
        int leftWeight = 0;
        for (int weight : weights) {
            leftWeight += Math.max(0, weight);
        }
        for (int index = 0; index < shares.length; index++) {
            int weight = index < weights.length ? Math.max(0, weights[index]) : 0;
            int share = 0;
            if (left > 0 && leftWeight > 0 && weight > 0) {
                share = Math.min(left, Math.round(left * (float) weight / leftWeight));
            }
            shares[index] = share;
            left -= share;
            leftWeight -= weight;
        }
    }

    @Override
    public String toString() {
        return "PipeTransport(" + TICKS_PER_SECOND + " ticks a second)";
    }
}
