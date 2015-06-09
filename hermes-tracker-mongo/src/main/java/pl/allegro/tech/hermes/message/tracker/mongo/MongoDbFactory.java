package pl.allegro.tech.hermes.message.tracker.mongo;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.WriteConcern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

public class MongoDbFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoDbFactory.class);

    private MongoClient mongoClient;
    private MongoClientURI uri;

    public MongoDbFactory(String uri) {
        this.uri = new MongoClientURI(uri);
    }

    public DB create() {
        try {
            mongoClient = new MongoClient(uri);
            mongoClient.setWriteConcern(WriteConcern.ACKNOWLEDGED);
            return mongoClient.getDB(uri.getDatabase());
        } catch (Exception e) {
            LOGGER.error(format("Cannot connect to MongoDB on hosts %s using username: '%s'", uri.getHosts(), uri.getUsername()), e);
            throw new CouldNotProvideMongoException(e);
        }
    }

}
