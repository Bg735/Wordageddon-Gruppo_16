package it.unisa.diem.wordageddon_g16.utility;

import it.unisa.diem.wordageddon_g16.models.Document;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Utility centralizzata per la gestione delle risorse statiche dell'applicazione Wordageddon.
 * <p>
 * Fornisce accesso ad asset grafici, fogli di stile, file di documento e vocabolario predefinito.
 * Opera sul classpath e su directory di configurazione definite tramite {@link Config}.
 */
public class Resources {
    private static final List<String> VOCABULARY = Arrays.asList(
            "paradosso", "sinestesia", "algoritmo", "resilienza", "simposio",
            "entropia", "frattale", "prerogativa", "aforisma", "labirinto",
            "emendamento", "ipotesi", "prospettiva", "vulnerabilità", "catarsi",
            "introspezione", "palinsesto", "querimonia", "sineddoche", "aplomb"
    );


    /**
     * Costruttore privato per impedire l'istanziazione della classe di utilità.
     */
    private Resources() {}

    /** Percorso base delle risorse nel classpath. */
    static final String RES_PATH = "/it/unisa/diem/wordageddon_g16/";

    /**
     * Restituisce uno {@link InputStream} per un asset (es. immagini, icone) contenuto nella directory {@code assets}.
     *
     * @param filename nome del file asset da recuperare (es. {@code "logo.png"})
     * @return stream per leggere il contenuto del file, oppure {@code null} se non trovato
     */
    public static InputStream getAsset(String filename) {
        return Resources.class.getResourceAsStream(RES_PATH + "assets/" + filename);
    }

    /**
     * Restituisce l'URL esterno di un file CSS presente nella directory {@code style}.
     * <p>
     * Utile per aggiungere fogli di stile alla scena con {@code Scene.getStylesheets().add(...)}.
     * </p>
     *
     * @param name nome del file di stile (senza estensione)
     * @return URL esterno del file CSS da usare come stringa
     * @throws NullPointerException se il file non viene trovato
     */
    public static String getStyle(String name) {
        return Objects.requireNonNull(Resources.class.getResource(RES_PATH + "style/" + name + ".css")).toExternalForm();
    }

    /**
     * Restituisce il {@link Path} completo al file associato a un documento.
     * <p>
     * Il percorso è calcolato sulla base della directory definita in {@link Config.Props#DOCUMENTS_DIR}.
     * </p>
     *
     * @param document oggetto {@link Document} contenente il nome del file
     * @return percorso completo al file di documento
     */
    public static Path getDocPath(Document document) {
        return Path.of(Config.get(Config.Props.DOCUMENTS_DIR), document.filename());
    }

    /**
     * Restituisce il {@link Path} della directory dei documenti configurata.
     *
     * @return percorso alla directory contenente i documenti caricati
     */
    public static Path getDocsDirPath() {
        return Path.of(Config.get(Config.Props.DOCUMENTS_DIR));
    }

    /**
     * Legge il contenuto di un file di documento come stringa.
     * <p>
     * Costruisce il percorso completo per ottenere la directory dei documenti
     * e concatena il {@code filename} fornito. Il contenuto viene letto con {@link Files#readString(Path)}.
     * </p>
     *
     * @param filename nome del file da leggere
     * @return contenuto testuale del file sotto forma di {@code String}
     * @throws IOException se il file non è accessibile o non può essere letto
     */
    public static String getDocumentContent(String filename) throws IOException {
        return Files.readString(Path.of(Config.get(Config.Props.DOCUMENTS_DIR), filename));
    }

    public static List<String> getVocabulary() {
        return VOCABULARY;
    }
}
