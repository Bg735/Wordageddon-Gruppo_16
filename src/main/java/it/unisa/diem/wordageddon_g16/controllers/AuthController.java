package it.unisa.diem.wordageddon_g16.controllers;

import it.unisa.diem.wordageddon_g16.db.DAO;

public class AuthController {
    private DAO<?> userDAO;

    public <T> AuthController(UserDAO userDAO) {
        this.userDAO = userDAO;
    }
}
