package it.unisa.diem.wordageddon_g16.utility;

import java.io.IOException;
import java.util.logging.*;

/**
 * Logger centralizzato per la gestione degli errori nell'applicazione.
 * <p>
 * Configura un {@link Logger} personalizzato che scrive solo messaggi di livello {@code SEVERE}
 * in un file chiamato {@code error.log}, evitando l'output sulla console. Include anche lo stack trace.
 */
public class SystemLogger {
    /**
     * Istanza del logger Java associata alla classe {@code SystemLogger}.
     * <p>
     * Configurata per registrare solo eventi gravi ({@code Level.SEVERE}) su file.
     */
    private static final Logger logger = Logger.getLogger(SystemLogger.class.getName());

    /**
     * Registra un errore con un messaggio personalizzato e una {@link Throwable}.
     *
     * @param msg messaggio esplicativo dell'errore
     * @param e   eccezione da registrare nel log
     */
    public static void log(String msg, Throwable e) {
        logger.log(Level.SEVERE, msg, e);
    }

    /**
     * Registra un errore generico con messaggio predefinito e una {@link Throwable}.
     *
     * @param e eccezione da registrare nel log
     */
    public static void log(Throwable e) {
        logger.log(Level.SEVERE, "An error occurred", e);
    }

    static {
        try {
            // Imposta livello minimo a SEVERE (errori ed eccezioni)
            logger.setLevel(Level.SEVERE);

            // Rimuove handler di default (console)
            Handler[] handlers = logger.getHandlers();
            for (Handler h : handlers) {
                logger.removeHandler(h);
            }

            // Crea un FileHandler che scrive su "error.log"
            FileHandler fileHandler = new FileHandler("error.log");
            fileHandler.setLevel(Level.SEVERE);

            // Formatter semplice per scrivere solo stacktrace
            fileHandler.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("[").append(record.getLevel()).append("] ");
                    sb.append(record.getLoggerName()).append(": ");
                    sb.append(record.getMessage()).append("\n");

                    if (record.getThrown() != null) {
                        Throwable t = record.getThrown();
                        sb.append(t.toString()).append("\n");
                        for (StackTraceElement ste : t.getStackTrace()) {
                            sb.append("\tat ").append(ste.toString()).append("\n");
                        }
                    }
                    return sb.toString();
                }
            });

            logger.addHandler(fileHandler);
            logger.setUseParentHandlers(false); // evita stampa su console

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}