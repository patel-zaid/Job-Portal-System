package jobportal.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import jobportal.exception.DuplicateRecordException;
import jobportal.exception.JobPortalException;
import jobportal.model.Admin;
import jobportal.model.Company;
import jobportal.model.JobSeeker;
import jobportal.model.Recruiter;
import jobportal.model.User;
import jobportal.util.TextUtil;

/**
 * DAO (Data Access Object) for the tables: users, job_seekers, recruiters, companies.
 * Every SQL statement of these tables is written in this class.
 * All queries use PreparedStatement (safe against SQL injection).
 */
public class UserDao {

    // One SELECT that joins all user tables. We add WHERE / ORDER BY when we use it.
    private static final String USER_SELECT =
            "SELECT u.user_id, u.full_name, u.email, u.password_hash, u.phone, u.role, u.status, "
          + "s.skills, s.education, s.experience_years, s.city, s.resume_path, "
          + "r.designation, c.company_id, c.name AS company_name, c.industry, "
          + "c.location AS company_location, c.website, c.description AS company_description "
          + "FROM users u "
          + "LEFT JOIN job_seekers s ON u.user_id = s.user_id "
          + "LEFT JOIN recruiters r ON u.user_id = r.user_id "
          + "LEFT JOIN companies c ON r.company_id = c.company_id ";

    // ------------------------------------------------------------------
    // LOGIN
    // ------------------------------------------------------------------

    /** Checks email + password. Returns a JobSeeker, Recruiter or Admin object (as a User). */
    public User login(String email, String plainPassword) throws SQLException, JobPortalException {
        String sql = USER_SELECT + "WHERE u.email = ? AND u.password_hash = ?";

        // try-with-resources: connection, statement and result set are closed automatically
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, email.trim().toLowerCase());
            ps.setString(2, User.hashPassword(plainPassword));

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new JobPortalException("Invalid email or password.");
                }
                User user = buildUser(rs);
                if (user.isBlocked()) {
                    throw new JobPortalException("Your account has been blocked by the admin.");
                }
                return user;
            }
        }
    }

    // ------------------------------------------------------------------
    // REGISTER  (uses a transaction: either all inserts succeed or none)
    // ------------------------------------------------------------------

    public void registerJobSeeker(JobSeeker seeker) throws SQLException, DuplicateRecordException {
        Connection con = null;
        try {
            con = Database.getConnection();
            con.setAutoCommit(false);                       // start transaction

            int newId = insertUser(con, seeker);            // 1st insert: users table

            String sql = "INSERT INTO job_seekers (user_id, skills, education, experience_years, city, resume_path) "
                       + "VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = con.prepareStatement(sql)) {   // 2nd insert: job_seekers table
                ps.setInt(1, newId);
                ps.setString(2, seeker.getSkills());
                ps.setString(3, seeker.getEducation());
                ps.setInt(4, seeker.getExperienceYears());
                ps.setString(5, seeker.getCity());
                ps.setString(6, seeker.getResumePath());
                ps.executeUpdate();
            }

            con.commit();                                   // save both inserts
            seeker.setUserId(newId);
        } catch (SQLException e) {
            rollbackQuietly(con);                           // undo everything
            throw e;
        } catch (DuplicateRecordException e) {
            rollbackQuietly(con);
            throw e;
        } finally {
            closeQuietly(con);
        }
    }

    public void registerRecruiter(Recruiter recruiter) throws SQLException, DuplicateRecordException {
        Connection con = null;
        try {
            con = Database.getConnection();
            con.setAutoCommit(false);

            // Check the email first, so we do not create a company for nothing
            if (emailExists(con, recruiter.getEmail())) {
                throw new DuplicateRecordException("This email is already registered.");
            }

            int companyId = insertCompany(con, recruiter.getCompany());   // 1st insert: companies
            int newId = insertUser(con, recruiter);                       // 2nd insert: users

            String sql = "INSERT INTO recruiters (user_id, company_id, designation) VALUES (?, ?, ?)";
            try (PreparedStatement ps = con.prepareStatement(sql)) {      // 3rd insert: recruiters
                ps.setInt(1, newId);
                ps.setInt(2, companyId);
                ps.setString(3, recruiter.getDesignation());
                ps.executeUpdate();
            }

            con.commit();
            recruiter.setUserId(newId);
            recruiter.getCompany().setCompanyId(companyId);
        } catch (SQLException e) {
            rollbackQuietly(con);
            throw e;
        } catch (DuplicateRecordException e) {
            rollbackQuietly(con);
            throw e;
        } finally {
            closeQuietly(con);
        }
    }

    // ------------------------------------------------------------------
    // UPDATE PROFILE
    // ------------------------------------------------------------------

    public void updateJobSeeker(JobSeeker seeker) throws SQLException {
        Connection con = null;
        try {
            con = Database.getConnection();
            con.setAutoCommit(false);

            updateBasicInfo(con, seeker);

            String sql = "UPDATE job_seekers SET skills = ?, education = ?, experience_years = ?, "
                       + "city = ?, resume_path = ? WHERE user_id = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, seeker.getSkills());
                ps.setString(2, seeker.getEducation());
                ps.setInt(3, seeker.getExperienceYears());
                ps.setString(4, seeker.getCity());
                ps.setString(5, seeker.getResumePath());
                ps.setInt(6, seeker.getUserId());
                ps.executeUpdate();
            }
            con.commit();
        } catch (SQLException e) {
            rollbackQuietly(con);
            throw e;
        } finally {
            closeQuietly(con);
        }
    }

    public void updateRecruiter(Recruiter recruiter) throws SQLException {
        Connection con = null;
        try {
            con = Database.getConnection();
            con.setAutoCommit(false);

            updateBasicInfo(con, recruiter);

            String sql1 = "UPDATE recruiters SET designation = ? WHERE user_id = ?";
            try (PreparedStatement ps = con.prepareStatement(sql1)) {
                ps.setString(1, recruiter.getDesignation());
                ps.setInt(2, recruiter.getUserId());
                ps.executeUpdate();
            }

            Company company = recruiter.getCompany();
            String sql2 = "UPDATE companies SET name = ?, industry = ?, location = ?, website = ?, description = ? "
                        + "WHERE company_id = ?";
            try (PreparedStatement ps = con.prepareStatement(sql2)) {
                ps.setString(1, company.getName());
                ps.setString(2, company.getIndustry());
                ps.setString(3, company.getLocation());
                ps.setString(4, company.getWebsite());
                ps.setString(5, company.getDescription());
                ps.setInt(6, company.getCompanyId());
                ps.executeUpdate();
            }
            con.commit();
        } catch (SQLException e) {
            rollbackQuietly(con);
            throw e;
        } finally {
            closeQuietly(con);
        }
    }

    // ------------------------------------------------------------------
    // ADMIN OPERATIONS
    // ------------------------------------------------------------------

    public List<User> getAllUsers() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = USER_SELECT + "ORDER BY u.user_id";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(buildUser(rs));
            }
        }
        return users;
    }

    public void updateUserStatus(int userId, String newStatus) throws SQLException {
        String sql = "UPDATE users SET status = ? WHERE user_id = ?";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    /** Deletes the user. Foreign keys with ON DELETE CASCADE remove the related rows automatically. */
    public void deleteUser(int userId) throws SQLException {
        try (Connection con = Database.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM users WHERE user_id = ?")) {
                ps.setInt(1, userId);
                ps.executeUpdate();
            }
            // remove companies that no recruiter uses any more
            String cleanup = "DELETE FROM companies WHERE company_id NOT IN (SELECT company_id FROM recruiters)";
            try (PreparedStatement ps = con.prepareStatement(cleanup)) {
                ps.executeUpdate();
            }
        }
    }

    public int countUsers(String role) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE role = ?";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, role);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    // ------------------------------------------------------------------
    // PRIVATE HELPER METHODS
    // ------------------------------------------------------------------

    /** Converts one row of USER_SELECT into the correct child object. */
    private User buildUser(ResultSet rs) throws SQLException {
        String role = rs.getString("role");
        int userId = rs.getInt("user_id");
        String name = rs.getString("full_name");
        String email = rs.getString("email");
        String hash = rs.getString("password_hash");
        String phone = rs.getString("phone");
        String status = rs.getString("status");

        if (role.equals(User.ROLE_JOB_SEEKER)) {
            return new JobSeeker(userId, name, email, hash, phone, status,
                    TextUtil.clean(rs.getString("skills")),
                    TextUtil.clean(rs.getString("education")),
                    rs.getInt("experience_years"),
                    TextUtil.clean(rs.getString("city")),
                    TextUtil.clean(rs.getString("resume_path")));
        }

        if (role.equals(User.ROLE_RECRUITER)) {
            Company company = new Company(rs.getInt("company_id"),
                    TextUtil.clean(rs.getString("company_name")),
                    TextUtil.clean(rs.getString("industry")),
                    TextUtil.clean(rs.getString("company_location")),
                    TextUtil.clean(rs.getString("website")),
                    TextUtil.clean(rs.getString("company_description")));
            return new Recruiter(userId, name, email, hash, phone, status,
                    company, TextUtil.clean(rs.getString("designation")));
        }

        return new Admin(userId, name, email, hash, phone, status);
    }

    private boolean emailExists(Connection con, String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Inserts a row in the users table and returns the new auto-generated user_id
     * (SQLite: INTEGER PRIMARY KEY AUTOINCREMENT, read back with Database.lastInsertId).
     */
    private int insertUser(Connection con, User user) throws SQLException, DuplicateRecordException {
        if (emailExists(con, user.getEmail())) {
            throw new DuplicateRecordException("This email is already registered.");
        }
        String sql = "INSERT INTO users (full_name, email, password_hash, phone, role) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getPhone());
            ps.setString(5, user.getRole());
            ps.executeUpdate();
        }
        return Database.lastInsertId(con);
    }

    private int insertCompany(Connection con, Company company) throws SQLException {
        String sql = "INSERT INTO companies (name, industry, location, website, description) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, company.getName());
            ps.setString(2, company.getIndustry());
            ps.setString(3, company.getLocation());
            ps.setString(4, company.getWebsite());
            ps.setString(5, company.getDescription());
            ps.executeUpdate();
        }
        return Database.lastInsertId(con);
    }

    private void updateBasicInfo(Connection con, User user) throws SQLException {
        String sql = "UPDATE users SET full_name = ?, phone = ? WHERE user_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getPhone());
            ps.setInt(3, user.getUserId());
            ps.executeUpdate();
        }
    }

    private void rollbackQuietly(Connection con) {
        if (con != null) {
            try {
                con.rollback();
            } catch (SQLException e) {
                System.out.println("Rollback failed: " + e.getMessage());
            }
        }
    }

    private void closeQuietly(Connection con) {
        if (con != null) {
            try {
                con.close();
            } catch (SQLException e) {
                System.out.println("Could not close connection: " + e.getMessage());
            }
        }
    }
}
