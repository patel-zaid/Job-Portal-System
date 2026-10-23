package jobportal.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import jobportal.exception.DuplicateRecordException;
import jobportal.model.Job;
import jobportal.util.TextUtil;

/**
 * DAO for the tables: jobs and saved_jobs.
 */
public class JobDao {

    // Base SELECT for jobs. It also gets the company name and counts the applicants of each job.
    private static final String JOB_SELECT =
            "SELECT j.job_id, j.recruiter_id, j.title, j.description, j.location, j.job_type, "
          + "j.salary_lpa, j.required_skills, j.experience_required, j.status, j.posted_date, "
          + "c.name AS company_name, "
          + "(SELECT COUNT(*) FROM applications a WHERE a.job_id = j.job_id) AS applicant_count "
          + "FROM jobs j "
          + "JOIN recruiters r ON j.recruiter_id = r.user_id "
          + "JOIN companies c ON r.company_id = c.company_id ";

    // ------------------------------------------------------------------
    // RECRUITER: post / edit / close / delete
    // ------------------------------------------------------------------

    public void addJob(Job job) throws SQLException {
        String sql = "INSERT INTO jobs (recruiter_id, title, description, location, job_type, salary_lpa, "
                   + "required_skills, experience_required, status, posted_date) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'OPEN', date('now', 'localtime'))";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, job.getRecruiterId());
            ps.setString(2, job.getTitle());
            ps.setString(3, job.getDescription());
            ps.setString(4, job.getLocation());
            ps.setString(5, job.getJobType());
            ps.setDouble(6, job.getSalaryLpa());
            ps.setString(7, job.getRequiredSkills());
            ps.setInt(8, job.getExperienceRequired());
            ps.executeUpdate();
        }
    }

    public void updateJob(Job job) throws SQLException {
        String sql = "UPDATE jobs SET title = ?, description = ?, location = ?, job_type = ?, "
                   + "salary_lpa = ?, required_skills = ?, experience_required = ? WHERE job_id = ?";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, job.getTitle());
            ps.setString(2, job.getDescription());
            ps.setString(3, job.getLocation());
            ps.setString(4, job.getJobType());
            ps.setDouble(5, job.getSalaryLpa());
            ps.setString(6, job.getRequiredSkills());
            ps.setInt(7, job.getExperienceRequired());
            ps.setInt(8, job.getJobId());
            ps.executeUpdate();
        }
    }

    /** Used to close a job (status CLOSED) or to open it again (status OPEN). */
    public void updateJobStatus(int jobId, String newStatus) throws SQLException {
        String sql = "UPDATE jobs SET status = ? WHERE job_id = ?";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, jobId);
            ps.executeUpdate();
        }
    }

    /** Applications and saved_jobs of this job are deleted automatically (ON DELETE CASCADE). */
    public void deleteJob(int jobId) throws SQLException {
        String sql = "DELETE FROM jobs WHERE job_id = ?";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, jobId);
            ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------
    // READING JOBS
    // ------------------------------------------------------------------

    public Job getJobById(int jobId) throws SQLException {
        String sql = JOB_SELECT + "WHERE j.job_id = ?";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, jobId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return buildJob(rs);
                }
                return null;      // job not found
            }
        }
    }

    /** Jobs of one recruiter (open and closed). */
    public List<Job> getJobsByRecruiter(int recruiterId) throws SQLException {
        String sql = JOB_SELECT + "WHERE j.recruiter_id = ? ORDER BY j.job_id DESC";
        List<Job> jobs = new ArrayList<>();
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, recruiterId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    jobs.add(buildJob(rs));
                }
            }
        }
        return jobs;
    }

    /** Every job in the system (used by the admin). */
    public List<Job> getAllJobs() throws SQLException {
        String sql = JOB_SELECT + "ORDER BY j.job_id DESC";
        List<Job> jobs = new ArrayList<>();
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                jobs.add(buildJob(rs));
            }
        }
        return jobs;
    }

    // Method OVERLOADING: two methods with the same name and different parameters.

    /** Search only by keyword. */
    public List<Job> searchJobs(String keyword) throws SQLException {
        return searchJobs(keyword, "", "All");
    }

    /**
     * Search OPEN jobs by keyword (title / skills / company), location and job type.
     * The SQL text is built step by step with a StringBuilder.
     * SQLite's LIKE is case-insensitive for English letters, so "java" also finds "Java Developer".
     */
    public List<Job> searchJobs(String keyword, String location, String jobType) throws SQLException {
        StringBuilder sql = new StringBuilder(JOB_SELECT);
        sql.append("WHERE j.status = 'OPEN' ");

        List<String> values = new ArrayList<>();      // the values for the ? marks, in order

        if (!keyword.isEmpty()) {
            sql.append("AND (j.title LIKE ? OR j.required_skills LIKE ? OR c.name LIKE ?) ");
            values.add("%" + keyword + "%");
            values.add("%" + keyword + "%");
            values.add("%" + keyword + "%");
        }
        if (!location.isEmpty()) {
            sql.append("AND j.location LIKE ? ");
            values.add("%" + location + "%");
        }
        if (!jobType.equals("All")) {
            sql.append("AND j.job_type = ? ");
            values.add(jobType);
        }
        sql.append("ORDER BY j.posted_date DESC, j.job_id DESC");

        List<Job> jobs = new ArrayList<>();
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < values.size(); i++) {
                ps.setString(i + 1, values.get(i));   // JDBC parameter numbers start at 1
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    jobs.add(buildJob(rs));
                }
            }
        }
        return jobs;
    }

    // ------------------------------------------------------------------
    // SAVED (BOOKMARKED) JOBS
    // ------------------------------------------------------------------

    public void saveJob(int seekerId, int jobId) throws SQLException, DuplicateRecordException {
        try (Connection con = Database.getConnection()) {
            // 1. check if it is already saved
            String checkSql = "SELECT COUNT(*) FROM saved_jobs WHERE seeker_id = ? AND job_id = ?";
            try (PreparedStatement check = con.prepareStatement(checkSql)) {
                check.setInt(1, seekerId);
                check.setInt(2, jobId);
                try (ResultSet rs = check.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) > 0) {
                        throw new DuplicateRecordException("You have already saved this job.");
                    }
                }
            }
            // 2. insert
            String insertSql = "INSERT INTO saved_jobs (seeker_id, job_id, saved_date) VALUES (?, ?, date('now', 'localtime'))";
            try (PreparedStatement insert = con.prepareStatement(insertSql)) {
                insert.setInt(1, seekerId);
                insert.setInt(2, jobId);
                insert.executeUpdate();
            }
        }
    }

    public void removeSavedJob(int seekerId, int jobId) throws SQLException {
        String sql = "DELETE FROM saved_jobs WHERE seeker_id = ? AND job_id = ?";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, seekerId);
            ps.setInt(2, jobId);
            ps.executeUpdate();
        }
    }

    public List<Job> getSavedJobs(int seekerId) throws SQLException {
        String sql = JOB_SELECT + "JOIN saved_jobs sj ON sj.job_id = j.job_id "
                   + "WHERE sj.seeker_id = ? ORDER BY sj.saved_id DESC";
        List<Job> jobs = new ArrayList<>();
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, seekerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    jobs.add(buildJob(rs));
                }
            }
        }
        return jobs;
    }

    public int countSavedJobs(int seekerId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM saved_jobs WHERE seeker_id = ?";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, seekerId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    // ------------------------------------------------------------------
    // COUNTS (for admin statistics)  - again method overloading
    // ------------------------------------------------------------------

    /** Number of all jobs. */
    public int countJobs() throws SQLException {
        String sql = "SELECT COUNT(*) FROM jobs";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    /** Number of jobs with the given status (OPEN or CLOSED). */
    public int countJobs(String status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM jobs WHERE status = ?";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    // ------------------------------------------------------------------

    /** Converts the current row of the ResultSet into a Job object. */
    private Job buildJob(ResultSet rs) throws SQLException {
        Job job = new Job();
        job.setJobId(rs.getInt("job_id"));
        job.setRecruiterId(rs.getInt("recruiter_id"));
        job.setCompanyName(rs.getString("company_name"));
        job.setTitle(rs.getString("title"));
        job.setDescription(rs.getString("description"));
        job.setLocation(rs.getString("location"));
        job.setJobType(rs.getString("job_type"));
        job.setSalaryLpa(rs.getDouble("salary_lpa"));
        job.setRequiredSkills(TextUtil.clean(rs.getString("required_skills")));
        job.setExperienceRequired(rs.getInt("experience_required"));
        job.setStatus(rs.getString("status"));
        job.setPostedDate(rs.getString("posted_date"));
        job.setApplicantCount(rs.getInt("applicant_count"));
        return job;
    }
}
