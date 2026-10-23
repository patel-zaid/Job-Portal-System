package jobportal.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jobportal.exception.ValidationException;

/**
 * All input validation of the project is here (Regex + simple checks).
 * The check...() methods throw an unchecked ValidationException when the input is wrong.
 */
public final class Validator {

    // Regex for email: something@something.domain
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // Regex for phone: exactly 10 digits, first digit is 6, 7, 8 or 9 (Indian mobile number)
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[6-9][0-9]{9}$");

    private Validator() {
    }

    public static boolean isValidEmail(String email) {
        Matcher matcher = EMAIL_PATTERN.matcher(TextUtil.clean(email));
        return matcher.matches();
    }

    public static boolean isValidPhone(String phone) {
        Matcher matcher = PHONE_PATTERN.matcher(TextUtil.clean(phone));
        return matcher.matches();
    }

    public static void checkNotEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName + " cannot be empty.");
        }
    }

    public static void checkEmail(String email) {
        checkNotEmpty(email, "Email");
        if (!isValidEmail(email)) {
            throw new ValidationException("Please enter a valid email address (example: name@mail.com).");
        }
    }

    public static void checkPhone(String phone) {
        checkNotEmpty(phone, "Phone number");
        if (!isValidPhone(phone)) {
            throw new ValidationException("Phone number must be 10 digits and start with 6, 7, 8 or 9.");
        }
    }

    public static void checkPassword(String password) {
        checkNotEmpty(password, "Password");
        if (password.length() < 6) {
            throw new ValidationException("Password must have at least 6 characters.");
        }
    }

    /** The database columns have a maximum size, so we check the length before saving. */
    public static void checkMaxLength(String value, int maxLength, String fieldName) {
        if (value != null && value.trim().length() > maxLength) {
            throw new ValidationException(fieldName + " is too long (maximum " + maxLength + " characters).");
        }
    }

    /** Converts text like "3" to the number 3. Throws ValidationException for "abc" or "-5". */
    public static int parseNonNegativeInt(String text, String fieldName) {
        try {
            int number = Integer.parseInt(text.trim());
            if (number < 0) {
                throw new ValidationException(fieldName + " cannot be negative.");
            }
            return number;
        } catch (NumberFormatException e) {
            throw new ValidationException(fieldName + " must be a whole number.");
        }
    }

    public static double parseNonNegativeDouble(String text, String fieldName) {
        try {
            double number = Double.parseDouble(text.trim());
            if (number < 0) {
                throw new ValidationException(fieldName + " cannot be negative.");
            }
            return number;
        } catch (NumberFormatException e) {
            throw new ValidationException(fieldName + " must be a number.");
        }
    }
}
