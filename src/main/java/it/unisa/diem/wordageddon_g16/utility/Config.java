package it.unisa.diem.wordageddon_g16.utility;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Classe di configurazione centralizzata per l'applicazione Wordageddon.
 * <p>
 * Carica i parametri definiti nel file {@code config.properties} e li rende accessibili
 * tramite l'enum {@link Config.Props}. Tra le proprietà gestite ci sono URL del database,
 * lunghezza minima/massima dei campi utente, percorso file sessione e directory documenti.
 */
public class Config {
    private static final Properties PROPS = new Properties();

    /**
     * Enum che rappresenta le chiavi di configurazione supportate nel file {@code config.properties}.
     * <p>
     * Ogni elemento fornisce un alias sicuro per accedere ai valori corrispondenti tramite {@link Config#get(Props)}.
     * </p>
     *
     * Valori disponibili:
     * <ul>
     *   <li>{@code DB_URL} → URL del database</li>
     *   <li>{@code PW_CHAR_MIN_LENGTH} → lunghezza minima della password</li>
     *   <li>{@code USR_CHAR_MAX_LENGTH} → lunghezza massima del nome utente</li>
     *   <li>{@code SESSION_FILE} → percorso file sessione</li>
     *   <li>{@code DOCUMENTS_DIR} → directory contenente i documenti del gioco</li>
     * </ul>
     */
    public enum Props {
        DB_URL("db.url"),
        PW_CHAR_MIN_LENGTH("auth.char_min_length"),
        USR_CHAR_MAX_LENGTH("auth.char_max_length"),
        SESSION_FILE("session.url"),
        DOCUMENTS_DIR("docs.dir");

        private final String key;

        /**
         * Costruttore di {@code Props}: costruisce l'enum associando la chiave stringa del file {@code config.properties}.
         *
         * @param key chiave testuale associata a una proprietà
         */

        Props(String key) {
            this.key = key;
        }

        public String get() {
            return key;
        }
    }

    static {
        try (InputStream input = Config.class.getResourceAsStream("/it/unisa/diem/wordageddon_g16/config.properties")) {
            if (input == null) {
                throw new RuntimeException("Could not find config.properties file in classpath");
            }
            PROPS.load(input);
        } catch (IOException e) {
            SystemLogger.log("Failed to find config. file", e);
        }
    }
    /**
     * Restituisce la chiave testuale associata alla proprietà.
     *
     * @return nome della chiave nel file {@code config.properties}
     */
    public static String get(Props key) {
        return PROPS.getProperty(key.get());
    }
}
