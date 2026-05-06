package Utils;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class ProfanityFilter {

    private static final List<String> BAD_WORDS = Arrays.asList(
        "trash", "toxic", "donkey"
    );

    public static String filter(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String result = input;
        for (String word : BAD_WORDS) {
            // Case-insensitive word boundary replacement
            Pattern pattern = Pattern.compile("(?i)\\b" + Pattern.quote(word) + "\\b");
            result = pattern.matcher(result).replaceAll("****");
        }
        return result;
    }
}
