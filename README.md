# EduVibe — Student Social Media Platform

EduVibe is a desktop-based social media application designed for students. It combines academic collaboration, entertainment, social interaction, and gamified rewards in one platform.

## Features

- **User Authentication** — Registration and login
- **Academic Feed** — Share and explore academic content
- **Entertainment Feed** — Discover entertainment posts
- **Reels** — Short-form video content
- **Create Posts** — Publish text, images, and videos
- **User Profiles** — Manage personal information
- **Social Interaction** — Likes, comments, follows, and saved posts
- **Notifications** — Receive activity updates
- **Points & Rewards** — Earn points through platform activities
- **Leaderboard** — View user rankings
- **Admin Panel** — Manage users, posts, and reports

## Technologies Used

- Java 21
- Java Swing
- MySQL
- Maven
- MySQL Connector/J
- jBCrypt
- NetBeans IDE

## Requirements

- Java Development Kit (JDK) 21
- MySQL Server
- MySQL Workbench
- Apache Maven (or the Maven installation bundled with NetBeans)
- Git (for cloning the repository)

## Installation and Setup

### 1. Clone the Repository

```bash
git clone https://github.com/sven0829/EduVibe.git
```

### 2. Open the Project

Open the cloned `EduVibe` folder in NetBeans IDE as a Maven project.

### 3. Set Up MySQL

Make sure MySQL Server is running.

Create the database:

```sql
CREATE DATABASE eduvibe;
```

### 4. Import the Database Schema

1. Open MySQL Workbench.
2. Select **Server → Data Import**.
3. Choose **Import from Self-Contained File**.
4. Select `dumps/eduvibe_schema.sql`.
5. Set the target schema to `eduvibe`.
6. Click **Start Import**.

The schema export contains the database structure, not existing user accounts, posts, or other personal data.

### 5. Configure Database Environment Variables

Configure these Windows environment variables:

| Variable | Value |
|---|---|
| `EDUVIBE_DB_URL` | `jdbc:mysql://localhost:3306/eduvibe?useSSL=false&allowPublicKeyRetrieval=true` |
| `EDUVIBE_DB_USER` | Your MySQL username |
| `EDUVIBE_DB_PASSWORD` | Your MySQL password |

Never commit real database passwords or private credentials to GitHub.

### 6. Build and Run EduVibe

Use the provided Windows batch files:

| File | Purpose |
|---|---|
| `build-jar.bat` | Builds the executable JAR with dependencies |
| `run-eduvibe.bat` | Launches EduVibe |
| `build-app.bat` | Builds the Windows application package |

Run `build-jar.bat` before `run-eduvibe.bat`.

To create the Windows application package, run `build-app.bat`.

**Important:** MySQL Server must be running and the database environment variables must be configured before launching the application.

## Database

- Database name: `eduvibe`
- Database server: MySQL
- Schema file: `dumps/eduvibe_schema.sql`

The project uses environment variables for database credentials rather than storing passwords directly in the source code.

## Project Structure

```text
EduVibe/
├── dumps/
│   └── eduvibe_schema.sql
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/eduvibe/
│   │   │       ├── config/
│   │   │       ├── dao/
│   │   │       ├── model/
│   │   │       ├── service/
│   │   │       ├── ui/
│   │   │       └── utils/
│   │   └── resources/
│   └── test/
├── build-app.bat
├── build-jar.bat
├── run-eduvibe.bat
├── pom.xml
└── README.md
```

## Project Status

EduVibe is an academic capstone project and desktop application prototype.

## Author

Developed as a student capstone project.

---

**EduVibe — Learn, Connect, and Enjoy.**