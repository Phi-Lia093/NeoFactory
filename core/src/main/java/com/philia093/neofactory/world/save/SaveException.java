package com.philia093.neofactory.world.save;

/**
 * Thrown when a stored world cannot be written or read.
 * <p>
 * Unchecked on purpose: opening a world happens in one place, which is also the
 * place that decides what to do about a damaged save game - report it and keep the
 * file instead of overwriting it.
 */
public class SaveException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception with a message.
     *
     * @param message description of the problem
     */
    public SaveException(String message) {
        super(message);
    }

    /**
     * Creates an exception with a message and a cause.
     *
     * @param message description of the problem
     * @param cause exception that caused the problem
     */
    public SaveException(String message, Throwable cause) {
        super(message, cause);
    }
}
