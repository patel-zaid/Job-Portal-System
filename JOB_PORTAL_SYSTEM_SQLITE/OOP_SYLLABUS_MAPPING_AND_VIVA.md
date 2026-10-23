# OOP & Syllabus Mapping + Viva Questions

All class names are inside `src/jobportal/…`. Open the file named in **Where used** to see the real code.

## 1. Concept Mapping Table

### Java basics and OOP

| CONCEPT | WHERE USED | SIMPLE EXPLANATION |
|---|---|---|
| Variables / data types | everywhere, e.g. `Job` (`int jobId`, `double salaryLpa`, `boolean`, `String`) | Each field has a type that decides what it can store. |
| Input / output | Swing forms (input) · dialogs and tables (output) · `System.out.println` in `Database`, `RestApiServer` | Users type in text fields and read results in tables and message boxes. |
| Control structures | `if / else if` in `JobSeekerDashboard.onPageShown`; `for`, `for-each`, `while` in `JobDao.searchJobs`, `TextUtil` | Decisions and loops. |
| Methods | every class, e.g. `JobSeeker.calculateMatch(Job)` | A named block of code that does one job. |
| Classes / objects | `model/*`, e.g. `new Job(...)`, `new JobSeeker(...)` | A class is a blueprint; an object is one real thing made from it. |
| Encapsulation | `User`, `Job`, `Company` – `private` fields + getters/setters | Data is hidden; it is changed only through methods. |
| Inheritance | `User` → `JobSeeker`, `Recruiter`, `Admin` · `BaseDashboard` → 3 dashboards · `JobPortalException` → `DuplicateRecordException` | A child class reuses fields and methods of its parent (`extends`). |
| Abstraction | `User` and `BaseDashboard` | Show only *what* must be done, hide *how*. |
| Abstract class / method | `User.getRole()`, `User.getDashboardTitle()`, `BaseDashboard.onPageShown()` | The parent declares a method without body; each child must write it. |
| Interface | `Exportable` (implemented by `Job`, `Application`); `Runnable` in `SendTask`; `Serializable` in `UserPreferences`; `HttpHandler` in `RestApiServer` | A contract: a class promises to provide certain methods. |
| Association | `Recruiter` has-a `Company` · `Application` has-a `Job` and a `JobSeeker` | One object holds a reference to another object ("has-a"). |
| Polymorphism | `User user = userDao.login(...)` returns a child object · `LoginFrame.openDashboard` uses a `BaseDashboard` variable · `FileHandler.exportToCsv(Exportable[])` | One parent type, many child behaviours; the correct child method runs at run time. |
| Method overriding | `getRole()`, `getDashboardTitle()`, `getWelcomeMessage()` in children; `toCsvRow()`; `paintComponent()` in `UIHelper.BarChart`; `isCellEditable()` in `UIHelper.createTableModel` | A child gives its own body to a parent's method (`@Override`). |
| Method overloading | `User.getWelcomeMessage()` / `(String)` · `JobDao.searchJobs(keyword)` / `(keyword, location, type)` · `JobDao.countJobs()` / `(status)` · `FileHandler.writeLog(String)` / `(User, String)` · `UIHelper.createButton(...)` · constructors of `JobSeeker`, `Job` | Same method name, different parameters. |
| `super` keyword | `super(userId, fullName, …)` in `JobSeeker`, `Recruiter`, `Admin` constructors · `super.getWelcomeMessage()` in `JobSeeker`, `Recruiter` | Calls the parent's constructor / method. |
| `final` keyword | constants `User.ROLE_ADMIN`, `Job.STATUS_OPEN` · `final class Admin` · `final boolean isBlocked()` in `User` · `final` utility classes `Validator`, `TextUtil`, `FileHandler`, `UIHelper` | `final` variable = constant; `final` method = cannot be overridden; `final` class = cannot be extended. |

### Collections

| CONCEPT | WHERE USED | SIMPLE EXPLANATION |
|---|---|---|
| Array | `Job.JOB_TYPES`, `Application.STATUS_LIST`, `Exportable[]` in CSV export, `int[]` column widths in `UIHelper.setColumnWidths` | Fixed-size list of the same type. |
| List | `ArrayList<Job>` in `JobDao`, `List<Application>` in `ApplicationDao` | Ordered, growing list. |
| Set | `TextUtil.toSkillSet()` (`HashSet`), used by `JobSeeker.calculateMatch()` | No duplicates; `contains()` is used to compare skills. |
| Map | `LinkedHashMap<String,Integer>` status counts in `ApplicationDao`; `HashMap` of menu buttons in `BaseDashboard` | Key → value pairs. |
| Vector | `NotificationDao.getNotifications()` returns `Vector<String>` shown in a `JList` | Old thread-safe growable array; Swing `JList` accepts it directly. |

### Strings

| CONCEPT | WHERE USED | SIMPLE EXPLANATION |
|---|---|---|
| String manipulation | `TextUtil` (`trim`, `split`, `toLowerCase`, `substring`, `replace`), `FileHandler.copyResume` (`lastIndexOf`) | Built-in String methods. |
| StringBuilder | `Job.toCsvRow()`, `JobDao.searchJobs()` (builds SQL), `showJobDetails()`, `User.hashPassword()` | Fast, changeable text (not thread-safe). |
| StringBuffer | `FileHandler.writeLog()` | Like StringBuilder but thread-safe (log lines come from several threads). |
| Regex | `Validator` – `Pattern` / `Matcher` for email and phone | Pattern to check text format. |

### Exception handling

| CONCEPT | WHERE USED | SIMPLE EXPLANATION |
|---|---|---|
| try / catch / finally | `Database.loadSettings()`, `UserDao.registerJobSeeker()` (rollback + close), `FileHandler` | `finally` always runs – used to close files and connections. |
| `throw` | `Validator.checkEmail()`, `UserDao.login()` ("Invalid email or password") | Creates and raises an exception. |
| `throws` | all DAO methods (`throws SQLException, DuplicateRecordException`) | Passes the exception to the caller. |
| Checked exceptions | `SQLException`, `IOException`, `JobPortalException`, `DuplicateRecordException` | Compiler forces us to handle them. |
| Unchecked exceptions | `ValidationException`, `NumberFormatException`, `IllegalStateException` | Extend `RuntimeException`; handling is optional. |
| Exception hierarchy | `Exception → JobPortalException → DuplicateRecordException`; `SQLException` caught in `ApplicationDao.applyForJob` and checked with `Database.isUniqueViolation(e)` (SQLite message "UNIQUE constraint failed") | Child exceptions can be caught by parent type. |
| Custom exceptions | `exception/` package (3 classes) | Our own error types with clear messages. |

### Multithreading

| CONCEPT | WHERE USED | SIMPLE EXPLANATION |
|---|---|---|
| Thread creation – `extends Thread` | `NotificationService.Poller` | Background thread that counts unread notifications every 5 s. |
| Thread creation – `Runnable` | `NotificationService.SendTask` | Task that saves a notification to the SQLite database without freezing the screen. |
| Thread lifecycle | `Poller`: NEW → RUNNABLE → TIMED_WAITING (`sleep`) → TERMINATED. The Notifications page displays `getPollerState()`. | The states a thread passes through. |
| Synchronization | `synchronized` methods `increaseSentCount()`, `getSentCount()`, `FileHandler.writeLog()`, `readRecentLog()` | Only one thread at a time may use shared data / the shared log file. |
| Thread interruption | `stopPolling()` calls `interrupt()`; `Poller.run()` handles `InterruptedException` and ends the loop | Politely asks a thread to stop. |
| Thread pool | `Executors.newFixedThreadPool(2)` in `NotificationService` | Reuses 2 worker threads for many small tasks. |
| Swing thread rule | `SwingUtilities.invokeLater(...)` in `Poller`, `MainApp` | Screen components are changed only on the Swing thread. |

### REST API, Database, Files

| CONCEPT | WHERE USED | SIMPLE EXPLANATION |
|---|---|---|
| REST API | `RestApiServer` – `GET /jobs`, `GET /jobs/{id}` (JSON, status codes 200/400/404/405/500) | A program talks to our app over HTTP using URLs. |
| SQLite + JDBC | `database/schema_sqlite.sql`, `database/sample_data_sqlite.sql`, `dao/*`, `db.properties` | Data is stored in ONE SQLite file and accessed from Java with the SQLite JDBC driver (`lib/sqlite-jdbc-*.jar`). No database server is needed. |
| Connection | `Database.getConnection()` – URL `jdbc:sqlite:data/job_portal.db` read from `db.properties`; `PRAGMA foreign_keys = ON` is set on every connection | Opens the link to the database (and creates the file, tables and sample data automatically the first time). |
| PreparedStatement | every DAO method | Pre-compiled SQL with `?` parameters – safe against SQL injection. |
| ResultSet | `buildJob`, `buildUser`, `buildApplication` | The rows returned by a SELECT. |
| INSERT / SELECT / UPDATE / DELETE | INSERT: `addJob`, `applyForJob` · SELECT: `searchJobs` (uses `LIKE`, case-insensitive in SQLite) · UPDATE: `updateStatus` · DELETE: `deleteJob` | The four basic SQL operations. |
| Transactions | `UserDao.registerRecruiter()` – `setAutoCommit(false)`, `commit()`, `rollback()` | Three inserts succeed together or not at all. |
| File I/O + streams | CSV: `BufferedWriter/FileWriter`; resume copy: `FileInputStream/FileOutputStream`; `db.properties`: `Properties.load()` | Read / write files with character and byte streams. |
| Serialization | `UserPreferences implements Serializable`; `FileHandler.savePreferences()` / `loadPreferences()` (`ObjectOutputStream` / `ObjectInputStream`) | Save an object to a file and read it back. |
| RandomAccessFile | `FileHandler.writeLog()` (`seek` to the end) and `readRecentLog()` (`seek` to the last 20 KB) | Jump to any position in a file. |

### Mini-project deliverables

| CONCEPT | WHERE |
|---|---|
| Problem statement, objectives | `README.md` §1–2 |
| UML class diagram, use case diagram | `UML.md`, `docs/*.png` |
| Implementation | `src/` |

---

## 2. Viva Questions and Short Answers

1. **What is your project about?** A desktop Job Portal in Java Swing with SQLite, where job seekers apply for jobs, recruiters post jobs and manage applicants, and the admin manages the system.
2. **Why is `User` abstract?** We never need a "plain" user; every user is a JobSeeker, Recruiter or Admin. The abstract class holds the common data and forces children to implement `getRole()` and `getDashboardTitle()`.
3. **Where do you use inheritance?** `User` → `JobSeeker`/`Recruiter`/`Admin`; `BaseDashboard` → three dashboards; `JobPortalException` → `DuplicateRecordException`.
4. **Difference between abstract class and interface?** An abstract class can have fields, constructors and normal methods and is *extended* (single inheritance). An interface is a pure contract that a class *implements*; a class can implement many. We use both: `User` (abstract) and `Exportable` (interface).
5. **Where is polymorphism used?** `LoginFrame.openDashboard()` stores a `JobSeekerDashboard`, `RecruiterDashboard` or `AdminDashboard` in a `BaseDashboard` variable; `user.getRole()` runs the child's version.
6. **Overloading vs overriding?** Overloading = same name, different parameters, decided at compile time (`searchJobs`). Overriding = child re-writes a parent method, decided at run time (`getRole`).
7. **What does `super` do here?** `super(...)` in `JobSeeker`'s constructor calls `User`'s constructor; `super.getWelcomeMessage()` calls the parent's method.
8. **Where did you use `final`?** Constants (`User.ROLE_ADMIN`), `final class Admin`, `final` method `isBlocked()`, and `final` utility classes.
9. **What is encapsulation?** Fields are `private`; other classes use getters and setters. Example: `Job`.
10. **What is association? Give an example.** A "has-a" relation: `Recruiter` has a `Company`; `Application` has a `Job` and a `JobSeeker`.
11. **Why does `Exportable` exist?** `Job` and `Application` both promise `getCsvHeader()` and `toCsvRow()`, so one method `exportToCsv(Exportable[])` exports both.
12. **How are passwords protected?** Only the SHA-256 hash is stored (`User.hashPassword`). At login we hash the typed password and compare hashes.
13. **How do you prevent SQL injection?** All queries use `PreparedStatement` with `?` parameters.
14. **How do you prevent duplicate applications?** First a `hasApplied()` check in `ApplicationDao`, plus a `UNIQUE (job_id, seeker_id)` constraint in the table as a second protection. If SQLite still rejects the insert, it reports "UNIQUE constraint failed" (checked by `Database.isUniqueViolation`), which we convert to our own `DuplicateRecordException`.
15. **What is a transaction? Where used?** A group of SQL statements that succeed or fail together. `registerRecruiter()` inserts into `companies`, `users` and `recruiters`; on error we `rollback()`.
16. **Checked vs unchecked exception?** Checked (`SQLException`, `DuplicateRecordException`) must be caught or declared. Unchecked (`ValidationException`) extends `RuntimeException`.
17. **What does `finally` do?** It always runs, even after an exception. We use it to close connections and files.
18. **`throw` vs `throws`?** `throw` raises an exception object; `throws` in a method signature declares that the method may pass it to its caller.
19. **Which collections did you use and why?** Array (fixed status list), List (query results), Set (skills without duplicates), Map (status → count), Vector (notifications for `JList`).
20. **How is the skill-match % calculated?** Required skills of the job and skills of the seeker are converted to Sets. Match % = (number of common skills × 100) / number of required skills.
21. **StringBuilder vs StringBuffer?** Both build strings efficiently. StringBuffer is synchronized (thread-safe), StringBuilder is faster. StringBuffer is used in the log method, which is called by several threads.
22. **Where is regex used?** `Validator`: email pattern and 10-digit phone pattern with `Pattern`/`Matcher`.
23. **Which threads exist in the project?** The Swing event thread, the `Poller` thread (extends `Thread`), and two pool threads running `SendTask` (implements `Runnable`).
24. **Why do you use `synchronized`?** Several threads write to the same log file and counter. `synchronized` allows only one thread at a time, so lines never mix.
25. **How is a thread stopped safely?** `interrupt()` is called; the poller's `sleep()` throws `InterruptedException`; we set the interrupt flag again and the `while` loop ends.
26. **Why `SwingUtilities.invokeLater`?** Swing is not thread-safe; only the event dispatch thread should change components. The poller uses it to update the sidebar label.
27. **What is serialization? Where used?** Converting an object to bytes and saving it. `UserPreferences` (last search) is written with `ObjectOutputStream` and read back on the next login.
28. **Why RandomAccessFile?** It can `seek()` to any position. We jump to the end to append log lines, and to the last 20 KB to read recent lines quickly.
29. **How does the REST API work?** `RestApiServer` uses the JDK `HttpServer`. `GET /jobs` returns open jobs as JSON, `GET /jobs/{id}` returns one job; wrong id → 404, non-GET → 405.
30. **Explain the layers of the project.** UI (Swing) → DAO (JDBC/SQL) → SQLite file. Model classes carry the data; util has validation and file helpers; service has threads and REST.
