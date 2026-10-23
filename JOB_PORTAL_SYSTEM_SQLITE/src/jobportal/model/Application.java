package jobportal.model;

import jobportal.util.TextUtil;

/**
 * One job application: a JobSeeker applied for a Job.
 *
 * ASSOCIATION: an Application is linked to one Job and one JobSeeker object
 * (Application "has a" Job and "has a" JobSeeker).
 */
public class Application implements Exportable {

    public static final String STATUS_APPLIED = "APPLIED";
    public static final String STATUS_SHORTLISTED = "SHORTLISTED";
    public static final String STATUS_INTERVIEW = "INTERVIEW";
    public static final String STATUS_SELECTED = "SELECTED";
    public static final String STATUS_REJECTED = "REJECTED";

    // Array with every status, in the order of the hiring process
    public static final String[] STATUS_LIST = {
        STATUS_APPLIED, STATUS_SHORTLISTED, STATUS_INTERVIEW, STATUS_SELECTED, STATUS_REJECTED
    };

    private int applicationId;
    private String status = STATUS_APPLIED;
    private String appliedDate = "";
    private Job job;              // association
    private JobSeeker seeker;     // association

    public Application() {
    }

    public Application(int applicationId, String status, String appliedDate, Job job, JobSeeker seeker) {
        this.applicationId = applicationId;
        this.status = status;
        this.appliedDate = appliedDate;
        this.job = job;
        this.seeker = seeker;
    }

    /** Skill match between the applicant and the job (0 to 100). */
    public int getMatchPercent() {
        return seeker.calculateMatch(job);
    }

    @Override
    public String getCsvHeader() {
        return "Application ID,Applicant,Email,Phone,Job,Company,Experience (yrs),Skills,Match %,Applied On,Status";
    }

    @Override
    public String toCsvRow() {
        StringBuilder row = new StringBuilder();
        row.append(applicationId).append(",");
        row.append(TextUtil.csvSafe(seeker.getFullName())).append(",");
        row.append(TextUtil.csvSafe(seeker.getEmail())).append(",");
        row.append(TextUtil.csvSafe(seeker.getPhone())).append(",");
        row.append(TextUtil.csvSafe(job.getTitle())).append(",");
        row.append(TextUtil.csvSafe(job.getCompanyName())).append(",");
        row.append(seeker.getExperienceYears()).append(",");
        row.append(TextUtil.csvSafe(seeker.getSkills())).append(",");
        row.append(getMatchPercent()).append(",");
        row.append(appliedDate).append(",");
        row.append(status);
        return row.toString();
    }

    public int getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(int applicationId) {
        this.applicationId = applicationId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAppliedDate() {
        return appliedDate;
    }

    public void setAppliedDate(String appliedDate) {
        this.appliedDate = appliedDate;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    public JobSeeker getSeeker() {
        return seeker;
    }

    public void setSeeker(JobSeeker seeker) {
        this.seeker = seeker;
    }
}
