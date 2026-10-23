-- ============================================================
--  JOB PORTAL SYSTEM  -  SQLite schema (tables + indexes)
--
--  You do NOT have to run this file by hand.
--  The application runs it automatically at every start (jobportal.dao.Database).
--  Every statement uses IF NOT EXISTS, so running it again never deletes or
--  duplicates anything.
--
--  SQLite notes (compared with a server database):
--    * INTEGER PRIMARY KEY AUTOINCREMENT = auto-increment id (ids are never re-used).
--    * Dates are stored as TEXT in ISO format: 'YYYY-MM-DD' or 'YYYY-MM-DD HH:MM:SS'
--      (local time of the computer).
--    * Booleans are stored as INTEGER 0 / 1.
--    * SQLite does not enforce VARCHAR(n) lengths, so a CHECK (length(...) <= n)
--      keeps the original size limits.
--    * Foreign keys are only enforced when "PRAGMA foreign_keys = ON" is set for the
--      connection. Database.getConnection() does this for every connection.
-- ============================================================

-- ------------------------------------------------------------
-- 1. users : common data of every person (admin, recruiter, job seeker)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    user_id        INTEGER PRIMARY KEY AUTOINCREMENT,
    full_name      TEXT NOT NULL CHECK (length(full_name) <= 100),
    email          TEXT NOT NULL UNIQUE CHECK (length(email) <= 100),
    password_hash  TEXT NOT NULL CHECK (length(password_hash) <= 64),   -- SHA-256 hash, never plain text
    phone          TEXT NOT NULL CHECK (length(phone) <= 15),
    role           TEXT NOT NULL CHECK (role IN ('JOB_SEEKER', 'RECRUITER', 'ADMIN')),
    status         TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'BLOCKED')),
    created_at     TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);

-- ------------------------------------------------------------
-- 2. job_seekers : extra data only for job seekers (1 row per seeker)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS job_seekers (
    user_id           INTEGER PRIMARY KEY,
    skills            TEXT    NOT NULL DEFAULT '' CHECK (length(skills) <= 300),     -- comma separated
    education         TEXT    NOT NULL DEFAULT '' CHECK (length(education) <= 150),
    experience_years  INTEGER NOT NULL DEFAULT 0,
    city              TEXT    NOT NULL DEFAULT '' CHECK (length(city) <= 60),
    resume_path       TEXT    NOT NULL DEFAULT '' CHECK (length(resume_path) <= 255),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- ------------------------------------------------------------
-- 3. companies : company profile of a recruiter
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS companies (
    company_id   INTEGER PRIMARY KEY AUTOINCREMENT,
    name         TEXT NOT NULL CHECK (length(name) <= 100),
    industry     TEXT NOT NULL DEFAULT '' CHECK (length(industry) <= 80),
    location     TEXT NOT NULL DEFAULT '' CHECK (length(location) <= 80),
    website      TEXT NOT NULL DEFAULT '' CHECK (length(website) <= 120),
    description  TEXT NOT NULL DEFAULT '' CHECK (length(description) <= 500)
);

-- ------------------------------------------------------------
-- 4. recruiters : extra data only for recruiters (belongs to one company)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS recruiters (
    user_id      INTEGER PRIMARY KEY,
    company_id   INTEGER NOT NULL,
    designation  TEXT NOT NULL DEFAULT 'HR Manager' CHECK (length(designation) <= 80),
    FOREIGN KEY (user_id)    REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (company_id) REFERENCES companies(company_id)
);

-- ------------------------------------------------------------
-- 5. jobs : jobs posted by recruiters
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS jobs (
    job_id               INTEGER PRIMARY KEY AUTOINCREMENT,
    recruiter_id         INTEGER NOT NULL,
    title                TEXT NOT NULL CHECK (length(title) <= 100),
    description          TEXT NOT NULL CHECK (length(description) <= 1000),
    location             TEXT NOT NULL CHECK (length(location) <= 80),
    job_type             TEXT NOT NULL
                         CHECK (job_type IN ('Full-time', 'Part-time', 'Internship', 'Contract')),
    salary_lpa           REAL NOT NULL DEFAULT 0,                              -- lakhs per annum
    required_skills      TEXT NOT NULL DEFAULT '' CHECK (length(required_skills) <= 300),  -- comma separated
    experience_required  INTEGER NOT NULL DEFAULT 0,                           -- years
    status               TEXT NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'CLOSED')),
    posted_date          TEXT NOT NULL DEFAULT (date('now', 'localtime')),
    FOREIGN KEY (recruiter_id) REFERENCES recruiters(user_id) ON DELETE CASCADE
);

-- ------------------------------------------------------------
-- 6. applications : a job seeker applying for a job
--    UNIQUE (job_id, seeker_id) prevents duplicate applications
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS applications (
    application_id  INTEGER PRIMARY KEY AUTOINCREMENT,
    job_id          INTEGER NOT NULL,
    seeker_id       INTEGER NOT NULL,
    status          TEXT NOT NULL DEFAULT 'APPLIED'
                    CHECK (status IN ('APPLIED', 'SHORTLISTED', 'INTERVIEW', 'SELECTED', 'REJECTED')),
    applied_date    TEXT NOT NULL DEFAULT (date('now', 'localtime')),
    CONSTRAINT uq_job_seeker UNIQUE (job_id, seeker_id),
    FOREIGN KEY (job_id)    REFERENCES jobs(job_id) ON DELETE CASCADE,
    FOREIGN KEY (seeker_id) REFERENCES job_seekers(user_id) ON DELETE CASCADE
);

-- ------------------------------------------------------------
-- 7. saved_jobs : bookmarks of a job seeker
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS saved_jobs (
    saved_id    INTEGER PRIMARY KEY AUTOINCREMENT,
    seeker_id   INTEGER NOT NULL,
    job_id      INTEGER NOT NULL,
    saved_date  TEXT NOT NULL DEFAULT (date('now', 'localtime')),
    CONSTRAINT uq_seeker_job UNIQUE (seeker_id, job_id),
    FOREIGN KEY (seeker_id) REFERENCES job_seekers(user_id) ON DELETE CASCADE,
    FOREIGN KEY (job_id)    REFERENCES jobs(job_id) ON DELETE CASCADE
);

-- ------------------------------------------------------------
-- 8. notifications : messages shown to seekers / recruiters
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notifications (
    notification_id  INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id          INTEGER NOT NULL,
    message          TEXT NOT NULL CHECK (length(message) <= 255),
    is_read          INTEGER NOT NULL DEFAULT 0 CHECK (is_read IN (0, 1)),   -- 0 = unread, 1 = read
    created_at       TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- ------------------------------------------------------------
-- Indexes (the UNIQUE constraints above already index
-- applications(job_id, seeker_id) and saved_jobs(seeker_id, job_id))
-- ------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_jobs_recruiter         ON jobs (recruiter_id);
CREATE INDEX IF NOT EXISTS idx_jobs_status_posted     ON jobs (status, posted_date);
CREATE INDEX IF NOT EXISTS idx_applications_seeker    ON applications (seeker_id);
CREATE INDEX IF NOT EXISTS idx_saved_jobs_job         ON saved_jobs (job_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user     ON notifications (user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_recruiters_company     ON recruiters (company_id);
