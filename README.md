# Student Assignment Manager

A Java 17 desktop application using Java Swing/AWT and SQLite.

## Run
1. Install JDK 17+ and Maven.
2. Open this folder in VS Code.
3. Run `mvn clean package`
4. Run `mvn exec:java`

Database file is created at `data/assignments.db` on first launch. This starter includes persistent assignment CRUD, search, status updates, dashboard counts, and CSV export. It is a local single-computer application, not a network-synchronized service.
