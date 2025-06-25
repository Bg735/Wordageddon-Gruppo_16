package it.unisa.diem.wordageddon_g16.controllers;

import it.unisa.diem.wordageddon_g16.db.DAO;
import it.unisa.diem.wordageddon_g16.db.UserDAO;
import it.unisa.diem.wordageddon_g16.models.User;
import it.unisa.diem.wordageddon_g16.services.AuthService;

public class AuthController {

    private final AuthService authService;

    public <T> AuthController(AuthService authService) {
        this.authService = authService;
    }



    // aggiungi checker per username e password

    // no utenti? solo registrazione e mettilo admin
}
