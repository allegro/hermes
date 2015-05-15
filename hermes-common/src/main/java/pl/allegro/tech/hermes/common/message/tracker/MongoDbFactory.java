package pl.allegro.tech.hermes.common.message.tracker;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.WriteConcern;
import org.glassfish.hk2.api.Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;

import javax.inject.Inject;

import static java.lang.String.format;

public class MongoDbFactory implements Factory<DB> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoDbFactory.class);

    private MongoClient mongoClient;
    private MongoClientURI uri;

    @Inject
    public MongoDbFactory(ConfigFactory config) {
        this.uri = new MongoClientURI(config.getStringProperty(Configs.TRACKER_MONGODB_URI));
    }

    @Override
    public DB provide() {
        try {
            mongoClient = new MongoClient(uri);
            mongoClient.setWriteConcern(WriteConcern.ACKNOWLEDGED);
            return mongoClient.getDB(uri.getDatabase());
        } catch (Exception e) {
            LOGGER.error(format("Cannot connect to MongoDB on hosts %s using username: '%s'", uri.getHosts(), uri.getUsername()), e);
            throw new CouldNotProvideMongoException(e);
        }
    }

    @Override
    public void dispose(DB instance) {
        mongoClient.close();
    }

}
