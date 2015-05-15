package pl.allegro.tech.hermes.api.helpers;

import java.util.ArrayList;
import java.util.List;

public class Replacer {
    public static List<String> replaceInAll(String regex, String replacement, String... strings) {
        List<String> result = new ArrayList<String>();
        for (String string : strings) {
            result.add(string.replaceAll(regex, replacement));
        }
        return result;
    }
}
