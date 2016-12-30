package pl.allegro.tech.hermes.api;

public class Maintainer {

    private final String id;
    private final String name;

    public Maintainer(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

}
