-- ============================================================
--  JOB PORTAL SYSTEM  -  SQLite sample data
--
--  Loaded automatically ONCE, when a new database is created
--  (jobportal.dao.Database checks that the database is still empty).
--  Because every row has a fixed id and uses INSERT OR IGNORE,
--  running this file again can never create duplicate rows.
--
--  Passwords are stored as SHA-256 hashes:
--     admin123   -> admin
--     recruit123 -> all recruiters
--     seeker123  -> all job seekers
--  date('now','localtime','-10 days') means "10 days ago".
-- ============================================================

-- Users (ids 1 to 7)
INSERT OR IGNORE INTO users (user_id, full_name, email, password_hash, phone, role) VALUES
(1, 'System Admin',  'admin@jobportal.com',    '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', '9000000001', 'ADMIN'),
(2, 'Sneha Kapoor',  'hr@technova.com',        '0489b16b827c1631762db7247baa9d876b29f43852503bfbfd9fe52c05729375', '9000000002', 'RECRUITER'),
(3, 'Vikram Rao',    'careers@infobridge.com', '0489b16b827c1631762db7247baa9d876b29f43852503bfbfd9fe52c05729375', '9000000003', 'RECRUITER'),
(4, 'Neha Joshi',    'hiring@greenbyte.com',   '0489b16b827c1631762db7247baa9d876b29f43852503bfbfd9fe52c05729375', '9000000004', 'RECRUITER'),
(5, 'Rahul Sharma',  'rahul@student.com',      '551fc2b3d9634d79ed6191c13446ad02b53d69e0771836e7f86e9ea9171ccba7', '9876543210', 'JOB_SEEKER'),
(6, 'Priya Verma',   'priya@student.com',      '551fc2b3d9634d79ed6191c13446ad02b53d69e0771836e7f86e9ea9171ccba7', '9876501234', 'JOB_SEEKER'),
(7, 'Amit Patel',    'amit@student.com',       '551fc2b3d9634d79ed6191c13446ad02b53d69e0771836e7f86e9ea9171ccba7', '9812345678', 'JOB_SEEKER');

-- Job seekers (user ids 5, 6, 7)
INSERT OR IGNORE INTO job_seekers (user_id, skills, education, experience_years, city) VALUES
(5, 'java, oop, sql, sqlite, jdbc, git',          'B.Tech Computer Engineering', 0, 'Pune'),
(6, 'python, sql, pandas, machine learning',      'B.Tech Information Technology', 1, 'Hyderabad'),
(7, 'html, css, javascript, react',               'B.Tech Computer Science', 0, 'Mumbai');

-- Companies (ids 1 to 3)
INSERT OR IGNORE INTO companies (company_id, name, industry, location, website, description) VALUES
(1, 'TechNova Solutions', 'Software Services', 'Pune',      'www.technova.example',   'Product and services company building enterprise Java applications.'),
(2, 'InfoBridge Systems', 'Data and Analytics', 'Hyderabad', 'www.infobridge.example', 'Analytics company working with data science and AI projects.'),
(3, 'GreenByte Labs',     'Web Technology',     'Mumbai',    'www.greenbyte.example',   'Startup that builds modern web and mobile products.');

-- Recruiters (user ids 2, 3, 4)
INSERT OR IGNORE INTO recruiters (user_id, company_id, designation) VALUES
(2, 1, 'HR Manager'),
(3, 2, 'Talent Acquisition Lead'),
(4, 3, 'Recruitment Officer');

-- Jobs (ids 1 to 9)
INSERT OR IGNORE INTO jobs (job_id, recruiter_id, title, description, location, job_type, salary_lpa, required_skills, experience_required, status, posted_date) VALUES
(1, 2, 'Java Developer', 'Develop and maintain Java applications using OOP principles, JDBC and SQLite. Work with a small agile team and write clean, tested code.', 'Pune', 'Full-time', 6.50, 'java, oop, sqlite, jdbc', 1, 'OPEN', date('now', 'localtime', '-10 days')),
(2, 2, 'Junior Software Engineer', 'Fresher role. You will learn our codebase, fix bugs and build small features under a senior developer.', 'Pune', 'Full-time', 4.50, 'java, sql, git', 0, 'OPEN', date('now', 'localtime', '-8 days')),
(3, 2, 'Java Intern', 'Six month internship for final year students. Work on real modules and get mentoring from senior engineers.', 'Remote', 'Internship', 1.80, 'java, oop, git', 0, 'OPEN', date('now', 'localtime', '-5 days')),
(4, 3, 'Python Data Analyst', 'Clean and analyse business data, build dashboards and share insights with managers.', 'Hyderabad', 'Full-time', 5.50, 'python, sql, pandas, excel', 1, 'OPEN', date('now', 'localtime', '-9 days')),
(5, 3, 'Machine Learning Engineer', 'Design, train and deploy machine learning models for prediction problems.', 'Bangalore', 'Full-time', 9.00, 'python, machine learning, tensorflow, sql', 2, 'OPEN', date('now', 'localtime', '-7 days')),
(6, 3, 'QA Test Engineer', 'Write test cases, run manual and automated tests and report bugs to the development team.', 'Hyderabad', 'Contract', 4.00, 'testing, selenium, java, sql', 1, 'OPEN', date('now', 'localtime', '-4 days')),
(7, 4, 'Frontend Developer', 'Build responsive user interfaces for our web products using modern JavaScript.', 'Mumbai', 'Full-time', 6.00, 'html, css, javascript, react', 1, 'OPEN', date('now', 'localtime', '-6 days')),
(8, 4, 'Web Development Intern', 'Three month internship. Help the frontend team with pages, forms and bug fixes.', 'Mumbai', 'Internship', 1.80, 'html, css, javascript', 0, 'OPEN', date('now', 'localtime', '-3 days')),
(9, 4, 'Database Administrator', 'Manage SQL databases, take backups and tune slow queries.', 'Pune', 'Full-time', 7.00, 'sqlite, sql, backup, performance tuning', 2, 'CLOSED', date('now', 'localtime', '-20 days'));

-- Applications
INSERT OR IGNORE INTO applications (application_id, job_id, seeker_id, status, applied_date) VALUES
(1, 1, 5, 'SHORTLISTED', date('now', 'localtime', '-6 days')),
(2, 2, 5, 'APPLIED',     date('now', 'localtime', '-4 days')),
(3, 3, 5, 'INTERVIEW',   date('now', 'localtime', '-3 days')),
(4, 4, 6, 'SELECTED',    date('now', 'localtime', '-7 days')),
(5, 5, 6, 'APPLIED',     date('now', 'localtime', '-2 days')),
(6, 1, 6, 'REJECTED',    date('now', 'localtime', '-5 days')),
(7, 7, 7, 'INTERVIEW',   date('now', 'localtime', '-4 days')),
(8, 8, 7, 'APPLIED',     date('now', 'localtime', '-1 days')),
(9, 2, 7, 'APPLIED',     date('now', 'localtime', '-2 days'));

-- Saved (bookmarked) jobs
INSERT OR IGNORE INTO saved_jobs (saved_id, seeker_id, job_id, saved_date) VALUES
(1, 5, 4, date('now', 'localtime', '-2 days')),
(2, 5, 6, date('now', 'localtime', '-1 days')),
(3, 6, 1, date('now', 'localtime', '-3 days')),
(4, 7, 5, date('now', 'localtime', '-1 days'));

-- Notifications
INSERT OR IGNORE INTO notifications (notification_id, user_id, message, is_read) VALUES
(1, 5, 'Your application for Java Developer at TechNova Solutions is now SHORTLISTED', 0),
(2, 5, 'Your application for Java Intern at TechNova Solutions is now INTERVIEW', 0),
(3, 6, 'Your application for Python Data Analyst at InfoBridge Systems is now SELECTED', 0),
(4, 7, 'Your application for Frontend Developer at GreenByte Labs is now INTERVIEW', 1),
(5, 2, 'Rahul Sharma applied for Java Developer', 0),
(6, 2, 'Amit Patel applied for Junior Software Engineer', 1),
(7, 3, 'Priya Verma applied for Machine Learning Engineer', 0);
