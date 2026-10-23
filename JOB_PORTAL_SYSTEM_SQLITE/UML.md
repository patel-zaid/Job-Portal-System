# UML Diagrams – Job Portal System

Ready-made images are in the `docs/` folder. Editable Mermaid versions are given below each image
(they render automatically on GitHub, or paste them into https://mermaid.live).

---

## 1. Class Diagram (model / OOP structure)

![Class diagram](docs/class_diagram.png)

```mermaid
classDiagram
    class User {
        <<abstract>>
        -int userId
        -String fullName
        -String email
        -String passwordHash
        -String phone
        -String status
        +getRole()* String
        +getDashboardTitle()* String
        +getWelcomeMessage() String
        +isBlocked() boolean
        +hashPassword(String plain)$ String
    }
    class JobSeeker {
        -String skills
        -String education
        -int experienceYears
        -String city
        -String resumePath
        +calculateMatch(Job job) int
    }
    class Recruiter {
        -Company company
        -String designation
    }
    class Admin {
        <<final>>
    }
    class Company {
        -int companyId
        -String name
        -String industry
        -String location
        -String website
        -String description
    }
    class Exportable {
        <<interface>>
        +getCsvHeader() String
        +toCsvRow() String
    }
    class Job {
        -int jobId
        -String title
        -String location
        -String jobType
        -double salaryLpa
        -String requiredSkills
        -String status
        +isOpen() boolean
    }
    class Application {
        -int applicationId
        -String status
        -String appliedDate
        -Job job
        -JobSeeker seeker
        +getMatchPercent() int
    }
    User <|-- JobSeeker
    User <|-- Recruiter
    User <|-- Admin
    Exportable <|.. Job
    Exportable <|.. Application
    Recruiter --> Company : has-a
    Application --> Job : for
    Application --> JobSeeker : made by
```

---

## 2. Architecture / Layer Diagram (UI → DAO → Database)

![Architecture diagram](docs/architecture_diagram.png)

Package summary:

| Package | Purpose |
|---|---|
| `jobportal` | `MainApp` – starting point |
| `jobportal.model` | Data classes (User hierarchy, Job, Application, Company …) |
| `jobportal.dao` | JDBC code – every SQL statement lives here |
| `jobportal.service` | Threads (`NotificationService`) and REST API (`RestApiServer`) |
| `jobportal.ui` | All Swing screens |
| `jobportal.util` | Validation (regex), text helpers, file handling |
| `jobportal.exception` | Our own exceptions |

---

## 3. Use Case Diagram

![Use case diagram](docs/use_case_diagram.png)

```mermaid
flowchart LR
    JS([Job Seeker])
    RC([Recruiter])
    AD([Admin])
    EX([External Client])

    JS --> A1(Register / Login)
    JS --> A2(Manage profile)
    JS --> A3(Search and filter jobs)
    JS --> A4(View job details and skill match)
    JS --> A5(Apply for job)
    JS --> A6(Track application status)
    JS --> A7(Save / bookmark job)
    JS --> A8(View notifications)

    RC --> A1
    RC --> B1(Manage company profile)
    RC --> B2(Post job)
    RC --> B3(Edit / close / delete job)
    RC --> B4(View applicants)
    RC --> B5(Change application status)
    RC --> B6(Export CSV)
    RC --> B7(View statistics)
    RC --> A8

    AD --> C0(Login)
    AD --> C1(Manage users)
    AD --> C2(Manage all jobs)
    AD --> C3(View system statistics)
    AD --> C4(View activity log)
    AD --> B6

    EX --> D1(GET /jobs and /jobs/id)
```

---

## 4. ER Diagram (database)

![ER diagram](docs/er_diagram.png)

```mermaid
erDiagram
    users ||--o| job_seekers : "is a"
    users ||--o| recruiters : "is a"
    companies ||--o{ recruiters : employs
    recruiters ||--o{ jobs : posts
    jobs ||--o{ applications : receives
    job_seekers ||--o{ applications : makes
    job_seekers ||--o{ saved_jobs : bookmarks
    jobs ||--o{ saved_jobs : "is saved in"
    users ||--o{ notifications : gets
```
