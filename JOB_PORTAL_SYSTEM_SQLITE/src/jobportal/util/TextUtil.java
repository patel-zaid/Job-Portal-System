package jobportal.util;

import java.util.HashSet;
import java.util.Set;

/**
 * Small helper methods for working with Strings.
 * The class is final (cannot be extended) and has only static methods.
 */
public final class TextUtil {

    private TextUtil() {
        // private constructor: nobody needs to create an object of this class
    }

    /** Returns "" for null, otherwise the trimmed text. Avoids NullPointerException. */
    public static String clean(String text) {
        if (text == null) {
            return "";
        }
        return text.trim();
    }

    /** "java, SQL ,  git" -> Set of {"java", "sql", "git"}. A Set never keeps duplicates. */
    public static Set<String> toSkillSet(String skills) {
        Set<String> skillSet = new HashSet<>();
        String[] parts = clean(skills).split(",");       // split the text at every comma
        for (String part : parts) {
            String skill = part.trim().toLowerCase();
            if (!skill.isEmpty()) {
                skillSet.add(skill);
            }
        }
        return skillSet;
    }

    /** "full-time" -> "Full-time", "SHORTLISTED" -> "Shortlisted". Uses StringBuilder. */
    public static String capitalizeWords(String text) {
        String lower = clean(text).toLowerCase();
        StringBuilder result = new StringBuilder();
        boolean startOfWord = true;
        for (int i = 0; i < lower.length(); i++) {
            char ch = lower.charAt(i);
            if (startOfWord && Character.isLetter(ch)) {
                result.append(Character.toUpperCase(ch));
                startOfWord = false;
            } else {
                result.append(ch);
            }
            if (ch == ' ') {
                startOfWord = true;
            }
        }
        return result.toString();
    }

    /** Cuts long text and adds "..." at the end. */
    public static String shorten(String text, int maxLength) {
        String cleaned = clean(text);
        if (cleaned.length() <= maxLength) {
            return cleaned;
        }
        return cleaned.substring(0, maxLength) + "...";
    }

    /** Makes a value safe for a CSV file: wraps it in quotes and doubles inner quotes. */
    public static String csvSafe(String value) {
        String cleaned = clean(value).replace("\"", "\"\"");
        return "\"" + cleaned + "\"";
    }
}
