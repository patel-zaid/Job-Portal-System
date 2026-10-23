package jobportal.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * ABSTRACT parent class of all people using the portal.
 *
 *         User (abstract)
 *          |-- JobSeeker
 *          |-- Recruiter
 *          '-- Admin
 *
 * An abstract class cannot be created with "new User()". Only its children can be created.
 * Data members are PRIVATE (encapsulation) and are used through getters and setters.
 */
public abstract class User {

    // final + static = constants, their value can never change
    public static final String ROLE_JOB_SEEKER = "JOB_SEEKER";
    public static final String ROLE_RECRUITER = "RECRUITER";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_BLOCKED = "BLOCKED";

    // Encapsulation: private data
    private int userId;
    private String fullName;
    private String email;
    private String passwordHash;
    private String phone;
    private String status;

    // Constructor 1: empty user (fields are filled later using setters)
    public User() {
        this.status = STATUS_ACTIVE;
    }

    // Constructor 2 (overloaded): user with all common data
    public User(int userId, String fullName, String email, String passwordHash, String phone, String status) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.phone = phone;
        this.status = status;
    }

    // ---------- abstract methods: every child MUST write its own body ----------

    /** Returns ROLE_JOB_SEEKER, ROLE_RECRUITER or ROLE_ADMIN. */
    public abstract String getRole();

    /** Text shown in the title bar of the dashboard. */
    public abstract String getDashboardTitle();

    // ---------- normal methods (children may override them) ----------

    public String getWelcomeMessage() {
        return "Welcome, " + fullName;
    }

    // Method OVERLOADING: same name, different parameters
    public String getWelcomeMessage(String extraText) {
        return getWelcomeMessage() + " - " + extraText;
    }

    // final method: children are NOT allowed to override it
    public final boolean isBlocked() {
        return STATUS_BLOCKED.equals(status);
    }

    /**
     * Basic password protection: we never store the real password in the database.
     * We store its SHA-256 hash (a fixed-length "fingerprint" that cannot be reversed).
     */
    public static String hashPassword(String plainPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(plainPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));   // one byte -> two hex characters
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 exists in every Java, so this should never happen (unchecked exception)
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    // ---------- getters and setters ----------

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return getRole() + " #" + userId + " (" + fullName + ")";
    }
}
