package it.unisa.diem.wordageddon_g16.models;

import java.util.Objects;

public class Document {

    private long id;
    private String title;
    private String path;
    private int wordCount;

    public Document(long id, String title, String path, int wordCount) {
        this.id = id;
        this.title = title;
        this.path = path;
        this.wordCount = wordCount;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Document document = (Document) o;
        return id == document.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getPath() {
        return path;
    }

    public int getWordCount() {
        return wordCount;
    }
}
