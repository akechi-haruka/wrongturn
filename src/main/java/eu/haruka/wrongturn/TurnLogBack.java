package eu.haruka.wrongturn;

/**
 * Interface for receiving log messages from the server.
 */
public interface TurnLogBack {

    void debug(String str);

    void info(String str);

    void warning(String str);

    void error(String str);

    void exception(Throwable tr, String str);
}
