module it.unisa.diem.wordageddongruppo_16 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;

    opens it.unisa.diem.wordageddon_g16 to javafx.fxml;
    opens it.unisa.diem.wordageddon_g16.controllers to javafx.fxml;
    exports it.unisa.diem.wordageddon_g16;
}