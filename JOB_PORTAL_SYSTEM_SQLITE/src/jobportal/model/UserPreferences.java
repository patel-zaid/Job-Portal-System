package jobportal.model;

import java.io.Serializable;

/**
 * Small object that remembers the last search of a job seeker.
 * It implements Serializable, so FileHandler can save it to a file (serialization)
 * and read it back later (deserialization).
 */
public class UserPreferences implements Serializable {

    private static final long serialVersionUID = 1L;

    private String lastKeyword = "";
    private String lastLocation = "";
    private String lastJobType = "All";

    public String getLastKeyword() {
        return lastKeyword;
    }

    public void setLastKeyword(String lastKeyword) {
        this.lastKeyword = lastKeyword;
    }

    public String getLastLocation() {
        return lastLocation;
    }

    public void setLastLocation(String lastLocation) {
        this.lastLocation = lastLocation;
    }

    public String getLastJobType() {
        return lastJobType;
    }

    public void setLastJobType(String lastJobType) {
        this.lastJobType = lastJobType;
    }
}
