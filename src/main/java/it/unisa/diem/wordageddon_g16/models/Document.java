package it.unisa.diem.wordageddon_g16.models;

import java.util.Objects;

public record Document(long id, String title, String filename, int wordCount) {

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
}
