package it.unisa.diem.wordageddon_g16.models;

import java.util.Objects;
import java.io.Serializable;


public class User implements Serializable{
    private String name;
    private String password;
    private boolean isAdmin;

    public User(String name, String password, boolean isAdmin) {
        this.name = name;
        this.password = password;
        this.isAdmin = isAdmin;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }


    public boolean isAdmin() {
        return isAdmin;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(name, user.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    public void setAdmin(boolean b) {
        isAdmin = b;
    }
}
