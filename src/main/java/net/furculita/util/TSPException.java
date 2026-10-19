package net.furculita.util;

/** Signals a TSPLIB file that is missing, unreadable or in an unsupported format. */
public class TSPException extends Exception {
    private static final long serialVersionUID = 1L;

    public TSPException() {
        super();
    }

    public TSPException(String message) {
        super(message);
    }
}
