package de.hilling.maven.release.exceptions;

/**
 * General exception thrown during release.
 */
public class ReleaseException extends RuntimeException {
    public ReleaseException(String summary, Throwable cause) {
        super(summary, cause);
    }

    public ReleaseException(String summary) {
        super(summary);
    }

    public ReleaseException(Throwable cause) {
        super(cause);
    }
}
