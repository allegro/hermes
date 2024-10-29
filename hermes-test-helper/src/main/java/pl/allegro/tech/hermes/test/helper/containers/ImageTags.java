package pl.allegro.tech.hermes.test.helper.containers;

public class ImageTags {
  public static String confluentImagesTag() {
    if (System.getProperty("os.arch").equals("aarch64")) {
      return System.getProperty("confluentImagesTag", "7.6.1");
    }
    return System.getProperty("confluentImagesTag", "6.1.0");
  }
}
