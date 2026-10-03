package com.philia093.neofactory.world.interaction;

import java.util.Locale;

/**
 * The way a player works on a face of a block: which button of the mouse was pressed, and whether the
 * modifier key was held while it was.
 * <p>
 * The grid of nine cells is picked with a tool, see {@link FaceOperable}, and the wrench of the workshop is
 * the tool that does four things with those cells - which of them a click means is read from the button and
 * from the key:
 * <ul>
 *     <li><b>the left button takes the block apart</b>, which is what a wrench is for, so the click never
 *         reaches the block at all;</li>
 *     <li><b>the right button turns the machine</b>: the side a player clicked is the side it looks in from
 *         then on;</li>
 *     <li><b>the modifier key with the left button gives the side a job of taking something in</b> - the
 *         steam of a machine blows out, the power of a machine comes in;</li>
 *     <li><b>the modifier key with the right button gives the side a job of giving something out</b> - the
 *         plug a generator feeds the line of the power through.</li>
 * </ul>
 * <p>
 * <b>The two keys are not redundant.</b> A machine has four things a player sets and only two buttons, so
 * the modifier key is what separates the sides that take from the sides that give: without it the left
 * button and the right one would have to carry four meanings and no player could tell them apart.
 * <p>
 * A pipe and a cable answer the right button alone - a line is turned, and it is the machine at its end
 * that has sides to give a job to - so a click of the left button with the modifier held reaches a machine
 * and nothing else.
 */
public enum FaceClick {

    /** The left button, the one that takes a block apart. */
    LEFT,

    /** The right button, the one that turns a machine and joins a line. */
    RIGHT,

    /** The left button with the modifier key held, which gives a side a job of taking something in. */
    SHIFT_LEFT,

    /** The right button with the modifier key held, which gives a side a job of giving something out. */
    SHIFT_RIGHT;

    /**
     * The click a button and a key stand for.
     *
     * @param left {@code true} for the left button, {@code false} for the right one
     * @param modifier {@code true} while the player holds the modifier key
     * @return the click
     */
    public static FaceClick of(boolean left, boolean modifier) {
        if (left) {
            return modifier ? SHIFT_LEFT : LEFT;
        }
        return modifier ? SHIFT_RIGHT : RIGHT;
    }

    /** {@code true} when this click was made with the left button. */
    public boolean isLeft() {
        return this == LEFT || this == SHIFT_LEFT;
    }

    /** {@code true} when this click was made with the right button. */
    public boolean isRight() {
        return this == RIGHT || this == SHIFT_RIGHT;
    }

    /** {@code true} when the modifier key was held while this click was made. */
    public boolean modifier() {
        return this == SHIFT_LEFT || this == SHIFT_RIGHT;
    }

    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT);
    }
}
