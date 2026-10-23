# Job Portal System

**Subject:** Object Oriented Programming using Java (OOPJ) – B.Tech Mini Project
**Technology:** Java 17+ · Java Swing/AWT · SQLite · JDBC (no frameworks, no web technologies, no database server)

Other documents: [`UML.md`](UML.md) (class / use case / ER diagrams) ·
[`OOP_SYLLABUS_MAPPING_AND_VIVA.md`](OOP_SYLLABUS_MAPPING_AND_VIVA.md) (concept mapping + viva Q&A)

---

## 1. Problem Statement

Finding a job and hiring people are still done through scattered emails, spreadsheets and notice boards.
Job seekers cannot easily search, apply and track applications in one place, and recruiters
have no simple way to publish jobs, compare applicants and update their status.

## 2. Objectives

1. Build a desktop **Job Portal** with separate roles: Job Seeker, Recruiter and Admin.
2. Store all data in a **SQLite** database (one file, created automatically) and access it with **JDBC** and `PreparedStatement`.
3. Apply core **OOP concepts** (inheritance, abstraction, interface, polymorphism, encapsulation …) in a real project.
4. Use Java features from the syllabus: collections, strings/regex, exceptions, threads, file handling and a small REST API.
5. Provide a clean, easy-to-use **Swing** interface.

## 3. Features

| Role | Features |
|---|---|
| **Job Seeker** | Register / login · profile (skills, education, resume upload) · search & filter jobs · job details · apply · duplicate application blocked · track status · save (bookmark) jobs · **skill-match %** · recommended jobs · notifications |
| **Recruiter** | Register / login · company profile · post / edit / close / reopen / delete jobs · view applicants · view applicant profile · change status (Applied → Shortlisted → Interview → Selected / Rejected) · export applicants to CSV · statistics with bar chart · notifications |
| **Admin** | Login · block / unblock / delete users · close / delete any job · export jobs to CSV · system statistics with chart · activity log viewer · REST API info |
| **Extras** | Passwords stored as SHA-256 hash · last search remembered (serialization) · background notification thread · small REST API · input validation with regex |

## 4. Technology Used

| Item | Used |
|---|---|
| Language | Java 17 or newer (tested with JDK 21) |
| UI | Swing + AWT (`JFrame`, `JTable`, `CardLayout`, custom painted bar chart) |
| Database | **SQLite** – a single file, `data/job_portal.db`, created automatically. No server, no user name, no password |
| Connectivity | JDBC with the SQLite JDBC driver (`lib/sqlite-jdbc-3.53.4.0.jar`, bundled) |
| REST | `com.sun.net.httpserver.HttpServer` (built into the JDK) |
| Build | plain `javac` (scripts provided) – works in VS Code / IntelliJ / Eclipse too |

## 5. Project Structure

```
JOB_PORTAL_SYSTEM_SQLITE/
├── src/jobportal/
│   ├── MainApp.java                 starting point (run this)
│   ├── model/    User (abstract), JobSeeker, Recruiter, Admin, Company,
│   │             Job, Application, Exportable (interface), UserPreferences
│   ├── dao/      Database (SQLite connection + automatic set-up), UserDao, JobDao,
│   │             ApplicationDao, NotificationDao
│   ├── service/  NotificationService (threads), RestApiServer (REST)
│   ├── ui/       LoginFrame, RegisterDialog, BaseDashboard (abstract),
│   │             JobSeekerDashboard, RecruiterDashboard, AdminDashboard,
│   │             JobFormDialog, UIHelper
│   ├── util/     Validator (regex), TextUtil (String helpers), FileHandler (files)
│   └── exception/ JobPortalException, DuplicateRecordException, ValidationException
├── test/jobportal/                  JobPortalTests (database, DAO, REST, persistence), UiSmokeTest (screens)
├── database/
│   ├── schema_sqlite.sql            tables + indexes  (run automatically, safe to repeat)
│   └── sample_data_sqlite.sql       demo accounts / jobs (loaded once into a new database)
├── data/                            created at the first start: job_portal.db, activity.log, resumes/
├── docs/                            diagram images (class, architecture, use case, ER)
├── lib/                             sqlite-jdbc-3.53.4.0.jar (+ its licence files)
├── db.properties                    the SQLite URL (default jdbc:sqlite:data/job_portal.db)
├── compile_and_run.bat / .sh        compile + run
├── run_tests.bat / .sh              compile + run all automatic tests
├── reset_database.bat / .sh         delete the database (a fresh one is created at the next start)
├── README.md · UML.md · OOP_SYLLABUS_MAPPING_AND_VIVA.md
```

## 6. Database Structure (SQLite, file `data/job_portal.db`)

| Table | Important columns | Notes |
|---|---|---|
| `users` | user_id (PK), full_name, email (**UNIQUE**), password_hash, phone, role, status, created_at | common data of all roles |
| `job_seekers` | user_id (PK, FK→users), skills, education, experience_years, city, resume_path | extra data of seekers |
| `companies` | company_id (PK), name, industry, location, website, description | |
| `recruiters` | user_id (PK, FK→users), company_id (FK→companies), designation | extra data of recruiters |
| `jobs` | job_id (PK), recruiter_id (FK→recruiters), title, description, location, job_type, salary_lpa, required_skills, experience_required, status, posted_date | |
| `applications` | application_id (PK), job_id (FK), seeker_id (FK), status, applied_date | **UNIQUE (job_id, seeker_id)** stops duplicates |
| `saved_jobs` | saved_id (PK), seeker_id (FK), job_id (FK), saved_date | **UNIQUE (seeker_id, job_id)** |
| `notifications` | notification_id (PK), user_id (FK), message, is_read (0/1), created_at | |

Foreign keys use `ON DELETE CASCADE`, so deleting a recruiter automatically removes his/her jobs and their applications.
See the ER diagram in [`UML.md`](UML.md).

---

## 7. Setup and Run (Windows + VS Code)

**You only need a JDK. You do NOT need to install or start any database.**

1. **Install a JDK, version 17 or newer** (for example *Eclipse Temurin* from <https://adoptium.net>; tick
   *"Add to PATH"* in the installer). Check in a new terminal: `javac -version` must show 17 or higher.
2. **Install VS Code** and the extension **"Extension Pack for Java"** (optional – only needed if you want to run with the
   Run button instead of the batch file).
3. **Extract the ZIP** to any folder (for example `C:\Projects\JOB_PORTAL_SYSTEM_SQLITE`) and open that folder in VS Code
   (*File → Open Folder…*).
4. **Run:** open the VS Code terminal (*Terminal → New Terminal*) and type

   ```
   .\compile_and_run.bat
   ```

   (or double-click `compile_and_run.bat` in the Explorer). The script checks the JDK, finds the bundled SQLite driver in
   `lib\`, compiles all Java files into `out\`, and starts the program. The login window opens.
5. **Login** with one of the sample accounts in section 8.

Alternative: open `src/jobportal/MainApp.java` in VS Code and press **Run** above `main`. VS Code adds the jar from `lib/`
automatically; the program must run with the **project folder** as working directory (the default).

Linux / macOS: `./compile_and_run.sh`

### How the SQLite database is created

* The first time the program starts, `Database.java` creates the folder `data/` and the file **`data/job_portal.db`**,
  runs `database/schema_sqlite.sql` (all tables and indexes) and loads `database/sample_data_sqlite.sql`
  (demo accounts, jobs, applications, saved jobs, notifications).
* On every later start the schema script runs again but only uses `CREATE … IF NOT EXISTS`, and the sample data is **not**
  loaded again, so nothing is deleted or duplicated and all your changes stay in the file.
* Everything happens in one transaction: a failure never leaves a half-created database.
* `PRAGMA foreign_keys = ON` is executed for every connection (SQLite ignores foreign keys otherwise).
* To use another file, change `db.url` in `db.properties` (path relative to the project folder; only `jdbc:sqlite:` URLs are accepted).

### How to reset the database

Close the program, then run **`reset_database.bat`** (Linux/macOS: `./reset_database.sh`) and type `YES`. It deletes
`data/job_portal.db`; the next start creates a new one with the original sample data. (You can also delete the file
`data\job_portal.db` by hand while the program is closed.) To keep a backup, copy that file while the program is closed.

### REST API

The program also starts a small REST API on port 8080 while it is running:

* <http://localhost:8080/jobs> – all open jobs as JSON
* <http://localhost:8080/jobs/1> – job number 1 (unknown id → 404, text instead of a number → 400)

## 8. Sample Login Credentials

| Role | Email | Password |
|---|---|---|
| Admin | `admin@jobportal.com` | `admin123` |
| Recruiter (TechNova) | `hr@technova.com` | `recruit123` |
| Recruiter (InfoBridge) | `careers@infobridge.com` | `recruit123` |
| Recruiter (GreenByte) | `hiring@greenbyte.com` | `recruit123` |
| Job Seeker | `rahul@student.com` | `seeker123` |
| Job Seeker | `priya@student.com` | `seeker123` |
| Job Seeker | `amit@student.com` | `seeker123` |

(The same demo logins are shown on the login screen.)

## 9. Demo Flow (for the presentation – about 5 minutes)

1. Login as **amit@student.com** → Home shows statistics and *recommended jobs* ordered by skill match.
2. **Browse Jobs** → search `java` → select *Java Intern* → *View Details* (shows skill-match %) → *Apply Now*.
   Press *Apply Now* again → "already applied" message (duplicate protection).
3. *Save Job* on another job → open **Saved Jobs**. Open **My Applications** to see the coloured statuses.
4. Logout → login as **hr@technova.com** → the sidebar counter goes up (e.g. **"Notifications: 2 new"**)
   – a background thread checks the database every 5 seconds.
5. **My Jobs** → *Post New Job*. **Applicants** → select Amit → *Update Status* → *Interview* → *Export CSV*.
6. Login as Amit again → **Notifications** shows the status change (and the thread state).
7. Login as **admin@jobportal.com** → statistics, *Manage Users* (block a user), *Activity Log*.
8. Open `http://localhost:8080/jobs` in a browser → JSON from the REST API.
9. Close the program and start it again → all changes are still there (they are stored in `data/job_portal.db`).

## 10. Automatic Tests

Run **`run_tests.bat`** (Linux/macOS: `./run_tests.sh`). It compiles everything into `out_test/` and runs three test programs.
They use temporary database files – your own `data/job_portal.db` is **not** touched.

| Test program | What it checks |
|---|---|
| `JobPortalTests` | clean database creation, 8 tables + indexes, sample data counts, foreign keys / CHECK / UNIQUE rules, safe repeated initialization, login of every role, job search (case-insensitive, location, type), apply / duplicate apply, saved jobs, recruiter job management, status changes, registration with rollback, profile updates, admin block / delete with cascades, notifications, 8 threads writing at once, the REST API (200 / 400 / 404 / 405) |
| `JobPortalTests persist-write` / `persist-verify` | two **separate program runs**: records written by the first run are still present in the second run and the sample data is not duplicated |
| `UiSmokeTest` | opens the real Swing windows and clicks through login, registration, admin, recruiter and job seeker screens; every database effect is verified in SQLite. It needs a screen (on a Linux server: `xvfb-run -a …`, `run_tests.sh` does this automatically if `xvfb-run` exists). Do not touch the mouse while it runs on Windows. |

Each check prints `PASS` or `FAIL`; the script ends with `ALL TESTS PASSED` or `SOME TESTS FAILED`.
Manual test ideas:

| # | Test | Steps | Expected result |
|---|---|---|---|
| 1 | Valid login | admin@jobportal.com / admin123 | Admin dashboard opens |
| 2 | Invalid login | any email + wrong password | "Invalid email or password." |
| 3 | Empty fields | click Login with empty fields | "Email cannot be empty." |
| 4 | Register – bad email | email `abc@` | "Please enter a valid email address…" |
| 5 | Register – bad phone | phone `12345` | "Phone number must be 10 digits…" |
| 6 | Register – duplicate email | use `rahul@student.com` | "This email is already registered." |
| 7 | Duplicate application | apply twice for one job | "You have already applied for this job." |
| 8 | Duplicate bookmark | save the same job twice | "You have already saved this job." |
| 9 | Closed job | recruiter closes a job | job disappears from seeker's Browse page |
| 10 | Status change | recruiter sets Shortlisted | seeker sees new status + notification |
| 11 | Blocked user | admin blocks a user | that user gets "account has been blocked" at login |
| 12 | REST API | browser: `localhost:8080/jobs/1` | JSON of job 1; `/jobs/999` → 404 |
| 13 | Persistence | change something, close, start again | the change is still there |
| 14 | Case-insensitive search | search `JAVA` or `pune` in capitals | same results as lower case |

## 11. SQLite-specific points (good for viva)

| Topic | In this project |
|---|---|
| No server | The database is one file; the SQLite engine runs inside the Java program through the JDBC driver. |
| Auto-increment ids | `INTEGER PRIMARY KEY AUTOINCREMENT`. After an INSERT, `Database.lastInsertId(con)` (`SELECT last_insert_rowid()`) returns the new id. |
| Fixed value lists | `CHECK (status IN ('OPEN','CLOSED'))`. SQLite does not enforce `VARCHAR(n)`, so `CHECK (length(col) <= n)` keeps the size limits. |
| Dates | stored as text: `'YYYY-MM-DD'` / `'YYYY-MM-DD HH:MM:SS'` in local time. Defaults use `date('now','localtime')`; sample data uses `date('now','localtime','-10 days')`. |
| Booleans | `is_read` is `INTEGER` 0 / 1. |
| Search | `LIKE` – case-insensitive for English letters in SQLite. |
| Date text on screen | the notification time is formatted in Java (`dd MMM yyyy, HH:mm`). |
| Foreign keys | only active with `PRAGMA foreign_keys = ON`, which `Database.getConnection()` runs for every connection. |
| Transactions | `setAutoCommit(false)` starts `BEGIN IMMEDIATE`; a busy timeout of 10 s lets several threads take turns writing. |
| Duplicate detection | SQLite reports "UNIQUE constraint failed"; `Database.isUniqueViolation` lets `ApplicationDao.applyForJob` convert it to our `DuplicateRecordException`. |
| Driver | `org.sqlite.JDBC`, URL `jdbc:sqlite:data/job_portal.db`. |

## 12. About the bundled SQLite JDBC driver

`lib/sqlite-jdbc-3.53.4.0.jar` (Apache License 2.0, see `lib/sqlite-jdbc-LICENSE.txt`) contains the Xerial SQLite JDBC
classes and native SQLite libraries for Windows (x64, x86, ARM64), Linux (x64, ARM64) and macOS (Intel, Apple silicon).
It was assembled from the official source release **3.53.4.0** of <https://github.com/xerial/sqlite-jdbc>
(sources and native libraries from that release tag), because the preparation environment could not reach Maven Central.
If you prefer the jar that is published on Maven Central, delete it from `lib/` and run `compile_and_run.bat`: the script then
downloads `sqlite-jdbc-3.46.1.3.jar` from Maven Central automatically.

## 13. Troubleshooting

| Message | Reason / Fix |
|---|---|
| `ERROR: No JDK found` | install JDK 17+ (see step 1), open a NEW terminal, check `javac -version` |
| `ERROR: Your JDK is too old` | install JDK 17 or newer; make sure it is the first Java in your `PATH` |
| `The SQLite JDBC driver could not be downloaded` | `lib\` has no `sqlite-jdbc-*.jar` and there is no internet; copy a jar from <https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/> into `lib\` |
| `No suitable driver` / `SQLite JDBC driver ... was not found` | the jar is missing from `lib/` or not on the class path (VS Code: *Java Projects → Referenced Libraries* must show it) |
| `Database script not found: database/schema_sqlite.sql` | the program was not started from the project folder – use `compile_and_run.bat` |
| `database is locked` / `SQLITE_BUSY` | another program (DB browser) or a second copy of Job Portal holds the file; close it |
| `attempt to write a readonly database` / `Could not create the database folder` | the folder is read-only or in a protected location – extract the project to a normal folder such as `C:\Projects` |
| You want the original data back | run `reset_database.bat` |
| `REST API could not start` (console) | port 8080 is busy → change `REST_PORT` in `MainApp.java` |
| Windows SmartScreen / antivirus warns about `sqlitejdbc.dll` | the driver unpacks its native SQLite library into your temp folder; this is normal – allow it |
