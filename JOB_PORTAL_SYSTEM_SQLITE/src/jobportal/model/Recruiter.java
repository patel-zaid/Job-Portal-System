package jobportal.model;

/**
 * A person who posts jobs for a company.
 * Recruiter IS-A User (inheritance) and HAS-A Company (association).
 */
public class Recruiter extends User {

    private Company company = new Company();
    private String designation = "HR Manager";

    public Recruiter() {
        super();
    }

    // Used when a new recruiter registers
    public Recruiter(String fullName, String email, String passwordHash, String phone,
                     Company company, String designation) {
        super(0, fullName, email, passwordHash, phone, STATUS_ACTIVE);
        this.company = company;
        this.designation = designation;
    }

    // Used when a recruiter is loaded from the database
    public Recruiter(int userId, String fullName, String email, String passwordHash, String phone,
                     String status, Company company, String designation) {
        super(userId, fullName, email, passwordHash, phone, status);
        this.company = company;
        this.designation = designation;
    }

    @Override
    public String getRole() {
        return ROLE_RECRUITER;
    }

    @Override
    public String getDashboardTitle() {
        return "Recruiter Dashboard - " + company.getName();
    }

    @Override
    public String getWelcomeMessage() {
        return super.getWelcomeMessage() + " (" + designation + ")";
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }
}
