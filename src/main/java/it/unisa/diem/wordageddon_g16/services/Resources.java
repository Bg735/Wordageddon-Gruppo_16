package it.unisa.diem.wordageddon_g16.services;

import java.io.InputStream;
import java.net.URL;

public class Resources {

    private Resources(){}

    static final String RES_PATH = "/it/unisa/diem/wordageddon_g16/";

    public static InputStream getAsset(String filename){
        return Resources.class.getResourceAsStream(RES_PATH+"assets/"+filename);
    }

    public static String getStyle(String name) {
        return  Resources.class.getResource(RES_PATH + "style/" + name + ".css").toExternalForm();
    }


}
