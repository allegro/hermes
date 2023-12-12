package pl.allegro.tech.hermes.test.helper.containers;

public class ImageTags {
    static String confluentImagesTag() {
        return System.getProperty("confluentImagesTag", "6.1.0");
    }
}
