package jobportal.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jobportal.exception.DuplicateRecordException;
import jobportal.model.Application;
import jobportal.model.Job;
import jobportal.model.JobSeeker;
import jobportal.util.TextUtil;

/**
 * DAO for the applications table.
 */
public class ApplicationDao {

    // Base SELECT: application + job + company + applicant details
    private static final String APP_SELECT =
            "SELECT a.application_id, a.status, a.applied_date, "
          + "j.job_id, j.recruiter_id, j.title, j.location, j.required_skills, c.name AS company_name, "
          + "u.user_id AS seeker_id, u.full_name, u.email, u.phone, "
          + "s.skills, s.education, s.experience_years, s.city, s.resume_path "
          + "FROM applications a "
          + "JOIN jobs j ON a.job_id = j.job_id "
          + "JOIN recruiters r ON j.recruiter_id = r.user_id "
          + "JOIN companies c ON r.company_id = c.company_id "
          + "JOIN users u ON a.seeker_id = u.user_id "
          + "JOIN job_seekers s ON u.user_id = s.user_id ";

    // ------------------------------------------------------------------
    // APPLY
    // ------------------------------------------------------------------

    public boolean hasApplied(int jobId, int seekerId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM applications WHERE job_id = ? AND seeker_id = ?";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, jobId);
            ps.setInt(2, seekerId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    /** Applies for a job. Throws DuplicateRecordException if the seeker already applied. */
    public void applyForJob(int jobId, int seekerId) throws SQLException, DuplicateRecordException {
        // First check: friendly message
        if (hasApplied(jobId, seekerId)) {
            throw new DuplicateRecordException("You have already applied for this job.");
        }
        String sql = "INSERT INTO applications (job_id, seeker_id, status, applied_date) "
                   + "VALUES (?, ?, 'APPLIED', date('now', 'localtime'))";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, jobId);
            ps.setInt(2, seekerId);
            ps.executeUpdate();
        } catch (SQLException e) {
            // Second protection: the UNIQUE (job_id, seeker_id) constraint of the database.
            // SQLite reports it as "UNIQUE constraint failed" (checked by Database.isUniqueViolation).
            if (Database.isUniqueViolation(e)) {
                throw new DuplicateRecordException("You have already applied for this job.");
            }
            throw e;                         // any other database problem is passed on
        }
    }

    // ------------------------------------------------------------------
    // READ APPLICATIONS
    // ------------------------------------------------------------------

    /** All applications made by one job seeker. */
    public List<Application> getApplicationsBySeeker(int seekerId) throws SQLException {
        String sql = APP_SELECT + "WHERE a.seeker_id = ? ORDER BY a.application_id DESC";
        return runQuery(sql, seekerId);
    }

    /** All applications received by one recruiter (for all of his/her jobs). */
    public List<Application> getApplicationsForRecruiter(int recruiterId) throws SQLException {
        String sql = APP_SELECT + "WHERE j.recruiter_id = ? ORDER BY a.application_id DESC";
        return runQuery(sql, recruiterId);
    }

    /** All applications received for one job. */
    public List<Application> getApplicationsForJob(int jobId) throws SQLException {
        String sql = APP_SELECT + "WHERE a.job_id = ? ORDER BY a.application_id DESC";
        return runQuery(sql, jobId);
    }

    // ------------------------------------------------------------------
    // UPDATE STATUS
    // ------------------------------------------------------------------

    public void updateStatus(int applicationId, String newStatus) throws SQLException {
        String sql = "UPDATE applications SET status = ? WHERE application_id = ?";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, applicationId);
            ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------
    // STATISTICS  (returned as a Map:  status -> number of applications)
    // ------------------------------------------------------------------

    public Map<String, Integer> getStatusCountsForSeeker(int seekerId) throws SQLException {
        String sql = "SELECT a.status, COUNT(*) AS total FROM applications a "
                   + "WHERE a.seeker_id = ? GROUP BY a.status";
        return readStatusCounts(sql, seekerId);
    }

    public Map<String, Integer> getStatusCountsForRecruiter(int recruiterId) throws SQLException {
        String sql = "SELECT a.status, COUNT(*) AS total FROM applications a "
                   + "JOIN jobs j ON a.job_id = j.job_id "
                   + "WHERE j.recruiter_id = ? GROUP BY a.status";
        return readStatusCounts(sql, recruiterId);
    }

    public int countApplications() throws SQLException {
        String sql = "SELECT COUNT(*) FROM applications";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    // ------------------------------------------------------------------
    // PRIVATE HELPERS
    // ------------------------------------------------------------------

    /** Runs a query that has exactly one ? (an id) and returns the applications. */
    private List<Application> runQuery(String sql, int id) throws SQLException {
        List<Application> list = new ArrayList<>();
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(buildApplication(rs));
                }
            }
        }
        return list;
    }

    private Map<String, Integer> readStatusCounts(String sql, int id) throws SQLException {
        // LinkedHashMap keeps the insertion order, so the chart shows statuses in process order
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String status : Application.STATUS_LIST) {
            counts.put(status, 0);                 // start every status with 0
        }
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    counts.put(rs.getString("status"), rs.getInt("total"));
                }
            }
        }
        return counts;
    }

    /** Converts one row into an Application object that holds a Job and a JobSeeker (association). */
    private Application buildApplication(ResultSet rs) throws SQLException {
        Job job = new Job();
        job.setJobId(rs.getInt("job_id"));
        job.setRecruiterId(rs.getInt("recruiter_id"));
        job.setTitle(rs.getString("title"));
        job.setLocation(rs.getString("location"));
        job.setRequiredSkills(TextUtil.clean(rs.getString("required_skills")));
        job.setCompanyName(rs.getString("company_name"));

        JobSeeker seeker = new JobSeeker();
        seeker.setUserId(rs.getInt("seeker_id"));
        seeker.setFullName(rs.getString("full_name"));
        seeker.setEmail(rs.getString("email"));
        seeker.setPhone(rs.getString("phone"));
        seeker.setSkills(TextUtil.clean(rs.getString("skills")));
        seeker.setEducation(TextUtil.clean(rs.getString("education")));
        seeker.setExperienceYears(rs.getInt("experience_years"));
        seeker.setCity(TextUtil.clean(rs.getString("city")));
        seeker.setResumePath(TextUtil.clean(rs.getString("resume_path")));

        return new Application(rs.getInt("application_id"), rs.getString("status"),
                rs.getString("applied_date"), job, seeker);
    }
}
