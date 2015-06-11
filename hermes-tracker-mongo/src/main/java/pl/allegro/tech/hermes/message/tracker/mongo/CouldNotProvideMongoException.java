package pl.allegro.tech.hermes.message.tracker.mongo;

public class CouldNotProvideMongoException extends RuntimeException {
    public CouldNotProvideMongoException(Throwable cause) {
        super(cause);
    }
}
