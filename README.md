\# EduVibe — Student Social Media Platform



EduVibe is a desktop-based social media application designed for students. It combines academic collaboration, entertainment, social interaction, and gamified rewards in one platform.



\## Features



\- \*\*User Authentication\*\* — Registration and login

\- \*\*Academic Feed\*\* — Share and explore academic content

\- \*\*Entertainment Feed\*\* — Discover entertainment posts

\- \*\*Reels\*\* — Short-form video content

\- \*\*Create Posts\*\* — Publish text, images, and videos

\- \*\*User Profiles\*\* — Manage personal information

\- \*\*Social Interaction\*\* — Likes, comments, follows, and saved posts

\- \*\*Notifications\*\* — Receive activity updates

\- \*\*Points \& Rewards\*\* — Earn points through platform activities

\- \*\*Leaderboard\*\* — View user rankings

\- \*\*Admin Panel\*\* — Manage users, posts, and reports



\## Technologies Used



\- Java 21

\- Java Swing

\- MySQL

\- Maven

\- MySQL Connector/J

\- jBCrypt

\- NetBeans IDE



\## Requirements



\- Java Development Kit (JDK) 21

\- MySQL Server

\- NetBeans IDE

\- Git



\## Installation and Setup



1\. Clone the repository:



&#x20;  ```bash

&#x20;  git clone https://github.com/sven0829/EduVibe.git

&#x20;  ```



2\. Open the project in NetBeans IDE.



3\. Configure your MySQL database.



4\. Create or import the required EduVibe database schema.



5\. Configure the database environment variables:



&#x20;  - `EDUVIBE\_DB\_URL`

&#x20;  - `EDUVIBE\_DB\_USER`

&#x20;  - `EDUVIBE\_DB\_PASSWORD`



6\. Build and run the project using NetBeans or the provided batch scripts.



\## Database



EduVibe uses MySQL for storing application data.



Default database name: `eduvibe`



Default connection URL:



```text

jdbc:mysql://localhost:3306/eduvibe?useSSL=false\&serverTimezone=Asia/Manila

```



Database credentials should be configured through environment variables. Never commit real passwords or private credentials.



\## Project Structure



```text

EduVibe/

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



\## Project Status



EduVibe is an academic capstone project and desktop application prototype.



\## Author



Developed as a student capstone project.



\---



\*\*EduVibe — Learn, Connect, and Enjoy.\*\*

