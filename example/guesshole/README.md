# Guesshole

A real-time geography guessing game where players compete to identify locations around the world.

## Technology Stack

### Backend
- Java 17
- Spring Boot 3.4.2
- Spring WebFlux and Reactor Netty
- R2DBC for database access
- PostgreSQL with PostGIS extension
- Flyway for db migrations

### Frontend
- Plain JavaScript
- Alpine.js
- Tailwind CSS
- WebSockets

## Development Setup

### Prerequisites
- JDK 17+
- PostgreSQL 14+ with PostGIS extension
- Gradle 8.x

### Setting Up IntelliJ IDEA

1. Clone the repository:
   ```
   git clone https://github.com/MartinDoms/guesshole.git
   cd guesshole
   ```

2. Open the project in IntelliJ IDEA:
   - Select "Open" or "Import Project"
   - Navigate to the project folder and select the `build.gradle` file
   - Choose "Open as Project"

3. Configure application properties:
   - Verify that `src/main/resources/application.properties` contains the correct database configuration

4. Configure Gradle:
   - Make sure IntelliJ uses the Gradle wrapper
   - In IntelliJ, go to Preferences/Settings > Build, Execution, Deployment > Build Tools > Gradle
   - Set "Use Gradle from" to "gradle-wrapper.properties file"

### Setting Up the Database

#### Installing PostgreSQL and PostGIS

1. Install PostgreSQL:
   ```
   # For Ubuntu/Debian
   sudo apt update
   sudo apt install postgresql postgresql-contrib
   
   # For macOS with Homebrew
   brew install postgresql
   
   # For Windows, download and run the installer from https://www.postgresql.org/download/windows/
   ```

2. Install PostGIS extension:
   ```
   # For Ubuntu/Debian
   sudo apt install postgis postgresql-14-postgis-3
   
   # For macOS with Homebrew
   brew install postgis
   
   # For Windows, use the Application Stack Builder that comes with PostgreSQL
   ```

3. Create the database and user:
   ```sql
   CREATE USER guessholedbuser WITH PASSWORD 'guesshole';
   CREATE DATABASE guessholedb;
   GRANT ALL PRIVILEGES ON DATABASE guessholedb TO guessholedbuser;
   ```

4. Enable PostGIS extension in your database:
   ```sql
   \c guessholedb
   CREATE EXTENSION postgis;
   ```

#### Importing Geographic Data

1. Import GADM administrative boundaries:
   * Download the "GADM data for the entire world" from [the GADM website](https://gadm.org/download_world.html)
   * Unzip the downloaded file
   * Import the data into PostgreSQL using ogr2ogr:
     ```bash
     ogr2ogr -f "PostgreSQL" PG:"host=127.0.0.1 user=guessholedbuser dbname=guessholedb password=guesshole" /path_to/gadm_410.gpkg -nln gadm_boundaries -lco GEOMETRY_NAME=geom
     ```
   * Replace `/path_to/gadm_410.gpkg` with the actual path to the extracted GPKG file

2. Initial Flyway migrations:
   If this is a fresh installation, you need to run the template import migration first:

   a. The easiest way is to deploy the app once. This will automatically run the `R__round_template_import` migration.
   ```bash
   ./gradlew bootRun
   ```

   b. After that, you need to run the location points migration. The simplest approach is to:
   - Make a small change to the `R__populate_location_points_for_round_templates` migration file (e.g., update a comment or log statement)
   - Re-deploy the app:
   ```bash
   ./gradlew bootRun
   ```

## Running the Application

### From IntelliJ IDEA

1. Build the Tailwind CSS first (this happens automatically with Gradle but may need to be run manually the first time):
   ```bash
   ./gradlew buildTailwind
   ```

2. Run the Spring Boot application:
   - Find the main application class (usually `com.guesshole.GuessHoleApplication`)
   - Right-click and select "Run 'GuessHoleApplication'"

3. Alternatively, create a Run Configuration:
   - Click on "Add Configuration" in the top right
   - Click the "+" button and select "Spring Boot"
   - Set the Main class to your application's main class
   - Set the name to "GuessHoleApplication"
   - Click "Apply" and "OK"

### From Command Line

```bash
./gradlew bootRun
```

The application will be available at [http://localhost:8080](http://localhost:8080)

## Development Workflow

### Database Migrations

We use Flyway for database migrations. To create a new migration:

1. Create a new SQL file in `src/main/resources/db/migration/`
2. Name it following the Flyway naming convention: `V{version}__{description}.sql` or `R__{description}.sql`
3. Write your SQL migration

To run a Flyway migration manually:

```bash
./gradlew flywayMigrate
```

### Building Frontend Assets

The project uses Tailwind CSS, which needs to be compiled:

```bash
./gradlew buildTailwind
```

This is automatically run as part of the build process.

## License

TODO
