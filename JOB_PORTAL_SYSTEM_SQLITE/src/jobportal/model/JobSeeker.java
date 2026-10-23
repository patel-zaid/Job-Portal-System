package jobportal.model;

import java.util.Set;

import jobportal.util.TextUtil;

/**
 * A person who searches and applies for jobs.
 * JobSeeker IS-A User (inheritance): it gets name, email, phone ... from User
 * and adds its own data: skills, education, experience, city, resume.
 */
public class JobSeeker extends User {

    private String skills = "";           // comma separated, example: "java, sql, git"
    private String education = "";
    private int experienceYears = 0;
    private String city = "";
    private String resumePath = "";

    // Constructor 1: empty object (used by the DAO before setters are called)
    public JobSeeker() {
        super();
    }

    // Constructor 2 (overloaded): used when a new seeker registers
    public JobSeeker(String fullName, String email, String passwordHash, String phone) {
        super(0, fullName, email, passwordHash, phone, STATUS_ACTIVE);   // super(...) calls the User constructor
    }

    // Constructor 3 (overloaded): used when a seeker is loaded from the database
    public JobSeeker(int userId, String fullName, String email, String passwordHash, String phone,
                     String status, String skills, String education, int experienceYears,
                     String city, String resumePath) {
        super(userId, fullName, email, passwordHash, phone, status);
        this.skills = skills;
        this.education = education;
        this.experienceYears = experienceYears;
        this.city = city;
        this.resumePath = resumePath;
    }

    // Method OVERRIDING: we replace the abstract methods of User with real code
    @Override
    public String getRole() {
        return ROLE_JOB_SEEKER;
    }

    @Override
    public String getDashboardTitle() {
        return "Job Seeker Dashboard";
    }

    @Override
    public String getWelcomeMessage() {
        return super.getWelcomeMessage() + "! Find your next opportunity.";   // super.method() = parent's version
    }

    /**
     * Simple skill-match percentage.
     * Example: job needs {java, sql, git}, seeker knows {java, sql} -> 2 of 3 match -> 66%
     */
    public int calculateMatch(Job job) {
        Set<String> requiredSkills = job.getSkillSet();
        Set<String> mySkills = TextUtil.toSkillSet(skills);
        if (requiredSkills.isEmpty()) {
            return 0;
        }
        int matched = 0;
        for (String skill : requiredSkills) {
            if (mySkills.contains(skill)) {
                matched++;
            }
        }
        return matched * 100 / requiredSkills.size();
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getEducation() {
        return education;
    }

    public void setEducation(String education) {
        this.education = education;
    }

    public int getExperienceYears() {
        return experienceYears;
    }

    public void setExperienceYears(int experienceYears) {
        this.experienceYears = experienceYears;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getResumePath() {
        return resumePath;
    }

    public void setResumePath(String resumePath) {
        this.resumePath = resumePath;
    }
}
