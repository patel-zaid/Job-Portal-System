package jobportal.model;

/** Company profile. A Recruiter HAS-A Company (association). */
public class Company {

    private int companyId;
    private String name = "";
    private String industry = "";
    private String location = "";
    private String website = "";
    private String description = "";

    public Company() {
    }

    // Overloaded constructor: only the name is known when a recruiter registers
    public Company(String name) {
        this.name = name;
    }

    public Company(int companyId, String name, String industry, String location,
                   String website, String description) {
        this.companyId = companyId;
        this.name = name;
        this.industry = industry;
        this.location = location;
        this.website = website;
        this.description = description;
    }

    public int getCompanyId() {
        return companyId;
    }

    public void setCompanyId(int companyId) {
        this.companyId = companyId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return name;
    }
}
