package it.unisa.diem.wordageddon_g16.models;

public enum  Difficulty {
    EASY("Facile"),
    MEDIUM("Medio"),
    HARD("Difficile");

    private final String label;

    Difficulty(String label) {
        this.label = label;
    }
    public static int getMaxScoreDifficulty(Difficulty d) {
        return switch (d) {
            case EASY -> 100;
            case MEDIUM -> 200;
            case HARD -> 300;
        };
    }
    
    @Override
    public String toString() {
        return label;
    }
}
