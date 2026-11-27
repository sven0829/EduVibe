## Database Setup

EduVibe uses MySQL with the database name `eduvibe`.

### 1. Create the database

Open MySQL Workbench and execute:

```sql
CREATE DATABASE eduvibe;
```

### 2. Import the database structure

1. Open MySQL Workbench.
2. Go to **Server → Data Import**.
3. Select **Import from Self-Contained File**.
4. Choose `dumps/eduvibe_schema.sql`.
5. Select the `eduvibe` database as the target schema.
6. Click **Start Import**.

### 3. Configure environment variables

Set these Windows environment variables:

- `EDUVIBE_DB_URL`
  `jdbc:mysql://localhost:3306/eduvibe?useSSL=false&allowPublicKeyRetrieval=true`
- `EDUVIBE_DB_USER`
  Your MySQL username.
- `EDUVIBE_DB_PASSWORD`
  Your MySQL password.

Never upload real database passwords to GitHub.

### 4. Launch EduVibe

Run `run-eduvibe.bat` after building the application using `build-jar.bat`.

To create the Windows application package, run `build-app.bat`.

**Note:** MySQL Server must be running when using EduVibe. The schema export contains database structure, not your existing user accounts or posts.