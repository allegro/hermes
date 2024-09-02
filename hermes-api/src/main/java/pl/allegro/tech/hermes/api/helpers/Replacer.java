package pl.allegro.tech.hermes.api.helpers;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class Replacer {

  public static List<String> replaceInAll(String regex, String replacement, String... strings) {
    return Arrays.stream(strings)
        .map(s -> s.replaceAll(regex, replacement))
        .collect(Collectors.toList());
  }
}
