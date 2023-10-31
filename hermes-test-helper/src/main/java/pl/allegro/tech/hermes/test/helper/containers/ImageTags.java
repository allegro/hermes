package pl.allegro.tech.hermes.test.helper.containers;

public class ImageTags {
    static String confluentImagesTag() {
        return System.getProperty("confluentImagesTag", "7.5.1");
    }
}
