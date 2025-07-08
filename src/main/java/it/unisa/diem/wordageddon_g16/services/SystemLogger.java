package it.unisa.diem.wordageddon_g16.services;

import java.io.IOException;
import java.util.logging.*;

public class SystemLogger {
    private static final Logger logger = Logger.getLogger(SystemLogger.class.getName());

    public static void log(String msg, Throwable e) {
        logger.log(Level.SEVERE, msg, e);
    }
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
            FileHandler fileHandler = new FileHandler("logs/error.log");
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
