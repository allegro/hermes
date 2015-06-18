package pl.allegro.tech.hermes.integration.env;

import com.github.fakemongo.Fongo;
import com.mongodb.DB;

public class FongoFactory {

    private static final Fongo FONGO = new Fongo("test");

    public static Fongo getInstance() {
        return FONGO;
    }

    public static DB hermesDB() {
        return getInstance().getDB("hermesMessages");
    }

}
