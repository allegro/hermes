package pl.allegro.tech.hermes.test.helper.containers;

public class ImageTags {
    public static String confluentImagesTag() {
        return System.getProperty("confluentImagesTag", "6.1.0");
    }
}
