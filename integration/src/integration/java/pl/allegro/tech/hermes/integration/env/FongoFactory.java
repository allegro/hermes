package pl.allegro.tech.hermes.integration.env;

import com.github.fakemongo.Fongo;
import com.mongodb.DB;
import org.glassfish.hk2.api.Factory;

public class FongoFactory implements Factory<DB> {

    private static final Fongo FONGO = new Fongo("test");

    public static Fongo getInstance() {
        return FONGO;
    }

    @Override
    public DB provide() {
        return getInstance().getDB("hermesMessages");
    }

    @Override
    public void dispose(DB instance) {
    }
}
