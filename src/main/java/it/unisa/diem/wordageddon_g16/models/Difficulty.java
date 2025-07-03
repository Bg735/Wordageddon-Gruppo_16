package it.unisa.diem.wordageddon_g16.models;

public enum Difficulty {
    EASY("Facile"),
    MEDIUM("Medio"),
    HARD("Difficile");

    private final String label;

    Difficulty(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }

}
