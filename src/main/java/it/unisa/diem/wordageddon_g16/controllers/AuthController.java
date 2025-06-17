package it.unisa.diem.wordageddon_g16.controllers;

import it.unisa.diem.wordageddon_g16.db.DAO;

public class AuthController {
    private DAO<?> userDAO;

    public <T> AuthController(DAO<T> userDAO) {
        this.userDAO = userDAO;
    }
}
