package com.philia093.neofactory.chemistry;

/**
 * Thrown when a SMILES string cannot be read into a molecule.
 * <p>
 * The parser is the gate of the whole chemistry module: a string that cannot be turned into a graph is a
 * species no reaction may ever use, so a broken line has to fail where it is read and not much later
 * inside a balance solver, where the reason is long gone. The exception therefore carries the position of
 * the character that was refused, which is what turns "a molecule could not be read" into a line a player
 * or a developer can walk to.
 * <p>
 * The module refuses a string instead of guessing at it on purpose: a guessed molecule is a wrong species,
 * and a wrong species is worse than a missing one, because it will be balanced against everything else.
 */
public final class SmilesException extends RuntimeException {

    /** Position of the character that was refused, {@code -1} when no position is known. */
    private final int position;

    /**
     * Creates an exception that names no position.
     *
     * @param message what could not be read
     */
    public SmilesException(String message) {
        this(message, -1);
    }

    /**
     * Creates an exception that names the character it stopped at.
     *
     * @param message what could not be read
     * @param position index of the character, {@code -1} when unknown
     */
    public SmilesException(String message, int position) {
        super(position < 0 ? message : message + " at position " + position);
        this.position = position;
    }

    /** Position of the character that was refused, {@code -1} when no position is known. */
    public int position() {
        return position;
    }
}
