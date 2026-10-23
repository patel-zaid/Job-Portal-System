package jobportal.model;

import java.util.Set;

import jobportal.util.TextUtil;

/**
 * A job posted by a recruiter.
 * Job implements the Exportable interface, so it must write getCsvHeader() and toCsvRow().
 */
public class Job implements Exportable {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_CLOSED = "CLOSED";

    // Array of the allowed job types (also used to fill combo boxes)
    public static final String[] JOB_TYPES = {"Full-time", "Part-time", "Internship", "Contract"};

    private int jobId;
    private int recruiterId;
    private String companyName = "";
    private String title = "";
    private String description = "";
    private String location = "";
    private String jobType = "Full-time";
    private double salaryLpa;
    private String requiredSkills = "";      // comma separated
    private int experienceRequired;
    private String status = STATUS_OPEN;
    private String postedDate = "";
    private int applicantCount;              // filled by a SQL COUNT when jobs are loaded

    public Job() {
    }

    // Overloaded constructor: used when a recruiter posts a new job
    public Job(int recruiterId, String title, String description, String location, String jobType,
               double salaryLpa, String requiredSkills, int experienceRequired) {
        this.recruiterId = recruiterId;
        this.title = title;
        this.description = description;
        this.location = location;
        this.jobType = jobType;
        this.salaryLpa = salaryLpa;
        this.requiredSkills = requiredSkills;
        this.experienceRequired = experienceRequired;
    }

    public boolean isOpen() {
        return STATUS_OPEN.equals(status);
    }

    /** The required skills as a Set, for example {"java", "sql"}. */
    public Set<String> getSkillSet() {
        return TextUtil.toSkillSet(requiredSkills);
    }

    // ---------- Exportable interface methods ----------

    @Override
    public String getCsvHeader() {
        return "Job ID,Title,Company,Location,Type,Salary (LPA),Required Skills,Experience,Status,Posted On";
    }

    @Override
    public String toCsvRow() {
        StringBuilder row = new StringBuilder();
        row.append(jobId).append(",");
        row.append(TextUtil.csvSafe(title)).append(",");
        row.append(TextUtil.csvSafe(companyName)).append(",");
        row.append(TextUtil.csvSafe(location)).append(",");
        row.append(TextUtil.csvSafe(jobType)).append(",");
        row.append(salaryLpa).append(",");
        row.append(TextUtil.csvSafe(requiredSkills)).append(",");
        row.append(experienceRequired).append(",");
        row.append(status).append(",");
        row.append(postedDate);
        return row.toString();
    }

    // ---------- getters and setters ----------

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public int getRecruiterId() {
        return recruiterId;
    }

    public void setRecruiterId(int recruiterId) {
        this.recruiterId = recruiterId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public double getSalaryLpa() {
        return salaryLpa;
    }

    public void setSalaryLpa(double salaryLpa) {
        this.salaryLpa = salaryLpa;
    }

    public String getRequiredSkills() {
        return requiredSkills;
    }

    public void setRequiredSkills(String requiredSkills) {
        this.requiredSkills = requiredSkills;
    }

    public int getExperienceRequired() {
        return experienceRequired;
    }

    public void setExperienceRequired(int experienceRequired) {
        this.experienceRequired = experienceRequired;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPostedDate() {
        return postedDate;
    }

    public void setPostedDate(String postedDate) {
        this.postedDate = postedDate;
    }

    public int getApplicantCount() {
        return applicantCount;
    }

    public void setApplicantCount(int applicantCount) {
        this.applicantCount = applicantCount;
    }

    @Override
    public String toString() {
        return "#" + jobId + " " + title;
    }
}
