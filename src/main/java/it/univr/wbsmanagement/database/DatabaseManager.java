package it.univr.wbsmanagement.database;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.univr.wbsmanagement.models.User;

/**
 * Provides methods for managing the database connection and operations.
 *
 * <p>This class is responsible for setting up the database structure, validating user credentials,
 * updating user information, managing roles, handling recover credentials requests, and managing
 * privacy policy acceptance. The underlying database is an SQLite database.</p>
 */
public class DatabaseManager {

    /**
     * The currently authenticated user.
     */
    private static User user;

    /**
     * The H2 database URL. Creates a file named "database.db" in the "src/main/resources" folder.
     * DB_CLOSE_ON_EXIT=FALSE means the database won't auto-close when the JVM exits.
     */
    private static final String DB_URL = "jdbc:h2:file:./src/main/resources/database.db;DB_CLOSE_ON_EXIT=FALSE";

    // SQL statements for creating tables and inserting default values, adapted for H2.
    private static final String createRolesTableSQL = """
            CREATE TABLE IF NOT EXISTS roles (
                id INT AUTO_INCREMENT PRIMARY KEY,
                role_name VARCHAR(255) UNIQUE NOT NULL
            );
        """;

    private static final String createUsersTableSQL = """
            CREATE TABLE IF NOT EXISTS users (
                id INT AUTO_INCREMENT PRIMARY KEY,
                email VARCHAR(255) UNIQUE NOT NULL,
                password VARCHAR(255) NOT NULL,
                role_id INT NOT NULL,
                privacy_accepted BOOLEAN DEFAULT FALSE,
                working_hours_weekly INT NOT NULL,
                FOREIGN KEY (role_id) REFERENCES roles(id)
            );
        """;

    private static final String createPriorityTableSQL = """
            CREATE TABLE IF NOT EXISTS priority (
                id INT AUTO_INCREMENT PRIMARY KEY,
                priority_name VARCHAR(255) UNIQUE NOT NULL
            );
        """;

    private static final String createStatusTableSQL = """
            CREATE TABLE IF NOT EXISTS status (
                id INT AUTO_INCREMENT PRIMARY KEY,
                status_name VARCHAR(255) UNIQUE NOT NULL
            );
        """;

    // In H2, "INSERT OR IGNORE" doesn't exist. We split them into conditional inserts.
    private static final String insertRolesSQL = """
            INSERT INTO roles (role_name)
                SELECT 'Researcher'
                WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_name = 'Researcher');
            INSERT INTO roles (role_name)
                SELECT 'Supervisor'
                WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_name = 'Supervisor');
            INSERT INTO roles (role_name)
                SELECT 'Administrator'
                WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_name = 'Administrator');
        """;

    private static final String insertPrioritySQL = """
            INSERT INTO priority (priority_name)
                SELECT 'High'
                WHERE NOT EXISTS (SELECT 1 FROM priority WHERE priority_name = 'High');
            INSERT INTO priority (priority_name)
                SELECT 'Medium'
                WHERE NOT EXISTS (SELECT 1 FROM priority WHERE priority_name = 'Medium');
            INSERT INTO priority (priority_name)
                SELECT 'Low'
                WHERE NOT EXISTS (SELECT 1 FROM priority WHERE priority_name = 'Low');
        """;

    private static final String insertStatusSQL = """
            INSERT INTO status (status_name)
                SELECT 'In Progress'
                WHERE NOT EXISTS (SELECT 1 FROM status WHERE status_name = 'In Progress');
            INSERT INTO status (status_name)
                SELECT 'Waiting dependency'
                WHERE NOT EXISTS (SELECT 1 FROM status WHERE status_name = 'Waiting dependency');
            INSERT INTO status (status_name)
                SELECT 'Blocked'
                WHERE NOT EXISTS (SELECT 1 FROM status WHERE status_name = 'Blocked');
            INSERT INTO status (status_name)
                SELECT 'Completed'
                WHERE NOT EXISTS (SELECT 1 FROM status WHERE status_name = 'Completed');
            INSERT INTO status (status_name)
                SELECT 'Not started'
                WHERE NOT EXISTS (SELECT 1 FROM status WHERE status_name = 'Not started');
        """;

    private static final String insertAdminSQL = """
            -- We do a similar conditional approach for the admin user
            INSERT INTO users (email, password, role_id, privacy_accepted, working_hours_weekly)
            SELECT 'admin', 'admin', (SELECT id FROM roles WHERE role_name = 'Administrator'), TRUE, 0
            WHERE NOT EXISTS (SELECT 1 FROM users WHERE email='admin');
        """;

    private static final String insertTestUsersSQL = """
            INSERT INTO users (email, password, role_id, privacy_accepted, working_hours_weekly)
            SELECT 'supervisor', 'supervisor', (SELECT id FROM roles WHERE role_name = 'Supervisor'), TRUE, 0
            WHERE NOT EXISTS (SELECT 1 FROM users WHERE email='supervisor');
        
            INSERT INTO users (email, password, role_id, privacy_accepted, working_hours_weekly)
            SELECT 'researcher', 'researcher', (SELECT id FROM roles WHERE role_name = 'Researcher'), TRUE, 0
            WHERE NOT EXISTS (SELECT 1 FROM users WHERE email='researcher');
        """;


    private static final String createRecoverCredentialsRequestsTableSQL = """
            CREATE TABLE IF NOT EXISTS recover_credentials_requests (
                id INT AUTO_INCREMENT PRIMARY KEY,
                email VARCHAR(255) NOT NULL,
                evaded BOOLEAN DEFAULT FALSE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );
        """;

    private static final String createProjectsTableSQL = """
            CREATE TABLE IF NOT EXISTS projects (
                id INT AUTO_INCREMENT PRIMARY KEY,
                title VARCHAR(255) NOT NULL,
                description VARCHAR(255),
                created_by_admin_id INT NOT NULL,
                supervisor_id INT NOT NULL,
                created_at DATE DEFAULT CURRENT_DATE,
                archived BOOLEAN DEFAULT FALSE,
                FOREIGN KEY (created_by_admin_id) REFERENCES users(id),
                FOREIGN KEY (supervisor_id) REFERENCES users(id)
            );
        """;

    private static final String createWorkPackagesTableSQL = """
            CREATE TABLE IF NOT EXISTS work_packages (
                id INT AUTO_INCREMENT PRIMARY KEY,
                project_id INT NOT NULL,
                title VARCHAR(255) NOT NULL,
                description VARCHAR(255),
                start_date DATE,
                end_date DATE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
            );
        """;

    private static final String createTasksTableSQL = """
            CREATE TABLE IF NOT EXISTS tasks (
                id INT AUTO_INCREMENT PRIMARY KEY,
                work_package_id INT NOT NULL,
                title VARCHAR(255) NOT NULL,
                description VARCHAR(255) NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                effort_hours INT NOT NULL,
                duration_hours INT NOT NULL,
                deadline TIMESTAMP NOT NULL,
                priority_id INT NOT NULL,
                status_id INT NOT NULL,
                FOREIGN KEY (work_package_id) REFERENCES work_packages(id) ON DELETE CASCADE
            );
        """;

    private static final String createDependenciesTableSQL = """
            CREATE TABLE IF NOT EXISTS dependencies (
                task_id_blocked INT NOT NULL,
                task_id_required INT NOT NULL,
                PRIMARY KEY (task_id_blocked, task_id_required),
                FOREIGN KEY (task_id_blocked) REFERENCES tasks(id),
                FOREIGN KEY (task_id_required) REFERENCES tasks(id) ON DELETE CASCADE
            );
        """;

    private static final String createTaskAssignmentsTableSQL = """
            CREATE TABLE IF NOT EXISTS task_assignments (
                task_id INT NOT NULL,
                user_id INT NOT NULL,
                effort_hypothetic INT NOT NULL,
                effort_consumed INT NOT NULL,
                PRIMARY KEY (task_id, user_id),
                FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            );
        """;

    private static final String createMilestonesTableSQL = """
            CREATE TABLE IF NOT EXISTS milestones (
                id INT AUTO_INCREMENT PRIMARY KEY,
                project_id INT NOT NULL,
                title VARCHAR(255) NOT NULL,
                description VARCHAR(255)
            );
        """;

    private static final String createMilestoneAssignmentsTableSQL = """
            CREATE TABLE IF NOT EXISTS milestone_assignments (
                milestone_id INT NOT NULL,
                task_id INT NOT NULL,
                PRIMARY KEY (milestone_id, task_id),
                FOREIGN KEY (milestone_id) REFERENCES milestones(id) ON DELETE CASCADE,
                FOREIGN KEY (task_id) REFERENCES tasks(id)
            );
        """;

    private static final String createTimeEntriesTableSQL = """
            CREATE TABLE IF NOT EXISTS time_entries (
                user_id INT NOT NULL,
                task_id INT NOT NULL,
                entry_date DATE DEFAULT CURRENT_DATE,
                hours DECIMAL(3,1) NOT NULL,
                PRIMARY KEY (user_id, task_id, entry_date),
                FOREIGN KEY (user_id) REFERENCES users(id),
                FOREIGN KEY (task_id) REFERENCES tasks(id)
            );
        """;

    private static final String createCreateReportsTableSQL = """
            CREATE TABLE IF NOT EXISTS reports (
                id INT AUTO_INCREMENT PRIMARY KEY,
                project_id INT NOT NULL,
                report_data BLOB,  -- or VARCHAR if you prefer
                signed BOOLEAN NOT NULL DEFAULT FALSE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                retention_until DATE,
                FOREIGN KEY (project_id) REFERENCES projects(id)
            );
        """;

    private static final String createProjectVisibilityTableSQL = """
            CREATE TABLE IF NOT EXISTS project_visibility (
                project_id INT NOT NULL,
                user_id INT NOT NULL,
                PRIMARY KEY (project_id, user_id),
                FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            );
        """;

    private static final String queryUsersFromEmail = "SELECT id, role_id, password FROM users WHERE email = ?";
    private static final String queryUsersRoleIdFromEmail = "SELECT role_id FROM users WHERE email = ?";
    private static final String queryUsersUpdatePassword = "UPDATE users SET password = ? WHERE email = ?";
    private static final String queryUsersUpdateRoleId = "UPDATE users SET role_id = ? WHERE email = ?";
    private static final String queryRolesRoleNameFromId = "SELECT role_name FROM roles WHERE id = ?";
    private static final String queryRoles = "SELECT role_name FROM roles";
    private static final String queryPriority = "SELECT id, priority_name FROM priority";
    private static final String queryStatus = "SELECT id, status_name FROM status";
    private static final String insertRolesIdFromRoleName = "SELECT id FROM roles WHERE role_name = ?";
    private static final String insertUser = """
            INSERT INTO users (email, password, role_id, working_hours_weekly, privacy_accepted)
            VALUES (?, ?, (SELECT id FROM roles WHERE role_name = ?), ?, false)
        """;
    private static final String queryRecoverCredentialsRequestsActiveFromEmail = """
            SELECT created_at FROM recover_credentials_requests
            WHERE email = ? AND evaded = false
        """;
    private static final String insertRecoverCredentialsRequests = """
            INSERT INTO recover_credentials_requests (email, evaded, created_at)
            VALUES (?, false, CURRENT_TIMESTAMP)
        """;
    private static final String countRecoverCredentialsRequestsActive = """
            SELECT COUNT(*) FROM recover_credentials_requests
            WHERE evaded = false
            GROUP BY evaded
        """;
    private static final String queryRecoverCredentialsRequestsActiveTSOrdered = """
            SELECT email, created_at FROM recover_credentials_requests
            WHERE evaded = false ORDER BY created_at ASC
        """;
    private static final String queryRecoverCredentialsRequestsClose = """
            UPDATE recover_credentials_requests
            SET evaded = true
            WHERE email = ?
        """;
    private static final String queryUsersUpdatePrivacy = """
            UPDATE users SET privacy_accepted = true
            WHERE email = ?
        """;
    private static final String queryUsersPrivacy = """
            SELECT privacy_accepted FROM users WHERE email = ?
        """;
    private static final String queryTimeEntryWeeklyByUserId = """
            SELECT entry_date, SUM(hours) as total_hours
            FROM time_entries
            WHERE user_id = ? AND entry_date BETWEEN ? AND ?
            GROUP BY entry_date
        """;
    private static final String insertTimeEntry = """
            INSERT INTO time_entries (user_id, task_id, entry_date, hours)
            VALUES (?, ?, ?, ?)
        """;
    private static final String countTimeEntryHoursByDay = """
            SELECT SUM(hours) as total_hours
            FROM time_entries
            WHERE user_id = ? AND entry_date = ?
        """;
    private static final String queryTaskIdFromName = "SELECT id FROM tasks WHERE title = ?";
    private static final String queryTaskAssignmentsByUserId = """
            SELECT title FROM tasks
            INNER JOIN task_assignments ON tasks.id = task_assignments.task_id
            WHERE task_assignments.user_id = ?
        """;
    private static final String insertProject = """
            INSERT INTO projects (title, description, created_by_admin_id, supervisor_id)
            VALUES (?, ?, ?, ?)
        """;
    private static final String queryUsersSupervisors = """
            SELECT id, email FROM users
            WHERE role_id = (SELECT id FROM roles WHERE role_name = 'Supervisor')
        """;
    private static final String queryUsersResearchersByProjectId = """
            SELECT u.id, u.email
            FROM users u
            INNER JOIN project_visibility pv ON u.id = pv.user_id
            WHERE pv.project_id = ?
              AND role_id = (SELECT id FROM roles WHERE role_name = 'Researcher')
        """;
    private static final String queryUsersResearchersExcludingProjectId = """
            SELECT DISTINCT u.id, u.email
            FROM users u
            LEFT JOIN project_visibility pv ON u.id = pv.user_id AND pv.project_id = ?
            WHERE u.role_id = (SELECT id FROM roles WHERE role_name = 'Researcher')
              AND pv.user_id IS NULL
        """;
    private static final String queryProjectsVisibilityByUserId = """
            SELECT DISTINCT p.id, p.title
            FROM projects p
            INNER JOIN project_visibility pv ON p.id = pv.project_id
            WHERE pv.user_id = ?
              AND p.archived = FALSE
        """;
    private static final String insertProjectsVisibility = """
            INSERT INTO project_visibility (project_id, user_id)
            VALUES (?, ?)
        """;
    private static final String insertWorkPackage = """
            INSERT INTO work_packages (project_id, title, description, start_date, end_date)
            VALUES (?, ?, ?, ?, ?)
        """;
    private static final String queryTasksByWorkPackage = """
            SELECT t.id, t.title
            FROM tasks t
            JOIN work_packages wp ON t.work_package_id = wp.id
            WHERE wp.id = ?
        """;
    private static final String queryWorkPackagesByProject = """
            SELECT id, title FROM work_packages
            WHERE project_id = ?
        """;
    private static final String insertTask = """
            INSERT INTO tasks (work_package_id, title, description, effort_hours, duration_hours,
                               deadline, priority_id, status_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
    private static final String queryProjectsActive = "SELECT DISTINCT id, title FROM projects WHERE archived = FALSE";
    private static final String queryProjectsArchived = "SELECT DISTINCT id, title FROM projects WHERE archived = TRUE";
    private static final String insertMilestone = """
            INSERT INTO milestones (project_id, title, description)
            VALUES (?, ?, ?)
        """;
    private static final String insertMilestoneAssignments = """
            INSERT INTO milestone_assignments (milestone_id, task_id)
            VALUES (?, ?)
        """;
    private static final String queryMilestonesByProject = """
            SELECT id, title FROM milestones WHERE project_id = ?
        """;
    private static final String queryTasksByUser = """
            SELECT t.id, t.title
            FROM tasks t
            INNER JOIN task_assignments ta ON t.id = ta.task_id
            WHERE ta.user_id = ?
        """;
    private static final String queryUpdateWorkPackage = """
            UPDATE work_packages
            SET start_date = ?, end_date = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
        """;
    private static final String deleteWorkPackageById = "DELETE FROM work_packages WHERE id = ?";
    private static final String deleteTaskById = "DELETE FROM tasks WHERE id = ?";
    private static final String countTaskDependenciesByTaskId = """
            SELECT COUNT(*) FROM dependencies
            WHERE task_id_blocked = ?
            GROUP BY task_id_blocked
        """;
    private static final String queryWorkPackageTimeRangeById = "SELECT start_date, end_date FROM work_packages WHERE id = ?";
    private static final String queryTasksByProjectId = """
            SELECT DISTINCT t.id, t.title
            FROM work_packages wp
            INNER JOIN tasks t ON wp.id = t.work_package_id
            WHERE wp.project_id = ?
        """;
    private static final String countMilestoneByTaskId = """
            SELECT COUNT(*) FROM milestone_assignments
            WHERE task_id = ?
            GROUP BY task_id
        """;
    private static final String queryUsersAndAssignmentsHoursByTasks = """
            SELECT DISTINCT u.id, u.email, ta.effort_consumed, ta.effort_hypothetic
            FROM users u
            INNER JOIN task_assignments ta ON u.id = ta.user_id
            WHERE task_id = ?
        """;
    private static final String insertTaskAssignments = """
            INSERT INTO task_assignments (task_id, user_id, effort_hypothetic, effort_consumed)
            VALUES (?, ?, ?, 0)
        """;
    private static final String querySingleTaskById = "SELECT * FROM tasks WHERE id = ?";
    private static final String queryProjectTitleById = "SELECT DISTINCT title FROM projects WHERE id = ?";

    /**
     * Sets the currently authenticated user.
     *
     * @param u the User object to store.
     */
    public static void setUser(User u) {
        user = u;
    }

    /**
     * Obtains a connection to the H2 database.
     *
     * @return a {@link Connection} object for interacting with the database.
     * @throws SQLException if a database access error occurs.
     */
    public static Connection getConnection() throws SQLException {
        // Driver is automatically loaded if we have H2 in the classpath.
        // If needed, you could do: Class.forName("org.h2.Driver");
        return DriverManager.getConnection(DB_URL, "sa", "");
    }

    public static Map<String, String> getUserRowByEmail(String email) {
        String sql = """
        SELECT u.email, u.password, r.role_name
        FROM users u
        JOIN roles r ON u.role_id = r.id
        WHERE u.email = ?
    """;

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null; // user non trovato
                }
                Map<String, String> row = new HashMap<>();
                row.put("email", rs.getString("email"));
                row.put("password", rs.getString("password"));
                row.put("role_name", rs.getString("role_name"));
                return row;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Sets up the database structure by creating necessary tables and inserting default values.
     *
     * <p>It creates the 'roles', 'users', 'recover_credentials_requests' tables if they do not exist,
     * inserts default roles and an administrator user, and logs success or error messages to the console.</p>
     */
    public static void setupDatabase() {
        try (Connection conn = getConnection();
             Statement stmtSetupDatabase = conn.createStatement()) {

            // Create all tables
            // 1) Create base tables
            stmtSetupDatabase.execute(createRolesTableSQL);
            stmtSetupDatabase.execute(createUsersTableSQL);
            stmtSetupDatabase.execute(createPriorityTableSQL);
            stmtSetupDatabase.execute(createStatusTableSQL);
            stmtSetupDatabase.execute(createRecoverCredentialsRequestsTableSQL);

            // 2) Create tables that depend on users
            stmtSetupDatabase.execute(createProjectsTableSQL);     // references users
            stmtSetupDatabase.execute(createWorkPackagesTableSQL); // references projects
            stmtSetupDatabase.execute(createTasksTableSQL);        // references work_packages
            stmtSetupDatabase.execute(createDependenciesTableSQL); // references tasks
            stmtSetupDatabase.execute(createTaskAssignmentsTableSQL); // references tasks + users

            // 3) Create additional tables
            stmtSetupDatabase.execute(createMilestonesTableSQL);          // (optional references project?)
            stmtSetupDatabase.execute(createMilestoneAssignmentsTableSQL);// references milestones + tasks
            stmtSetupDatabase.execute(createTimeEntriesTableSQL);         // references tasks + users
            stmtSetupDatabase.execute(createCreateReportsTableSQL);       // references projects
            stmtSetupDatabase.execute(createProjectVisibilityTableSQL);   // references projects + users


            // Insert default data (roles, priority, status, admin)
            // H2 doesn't allow multiple statements in a single execute() by default,
            // so you may need to split them or enable ALLOW_MULTIQUERIES=TRUE.
            // For simplicity, let's split them manually:
            for (String sql : insertRolesSQL.split(";")) {
                if (!sql.trim().isEmpty()) {
                    stmtSetupDatabase.execute(sql);
                }
            }
            for (String sql : insertPrioritySQL.split(";")) {
                if (!sql.trim().isEmpty()) {
                    stmtSetupDatabase.execute(sql);
                }
            }
            for (String sql : insertStatusSQL.split(";")) {
                if (!sql.trim().isEmpty()) {
                    stmtSetupDatabase.execute(sql);
                }
            }
            for (String sql : insertAdminSQL.split(";")) {
                if (!sql.trim().isEmpty()) {
                    stmtSetupDatabase.execute(sql);
                }
            }
            for (String sql : insertTestUsersSQL.split(";")) {
                if (!sql.trim().isEmpty()) {
                    stmtSetupDatabase.execute(sql);
                }
            }

            System.out.println("Database structure set up successfully (H2).");

        } catch (SQLException e) {
            System.err.println("Error setting up the database: " + e.getMessage());
        }
    }
    /**
     * Retrieves the role name of a user based on their email.
     *
     * @param email the email address of the user.
     * @return the role name of the user if found; otherwise, returns an empty string.
     */
    public static String getUserRole(String email) {
        try (Connection conn = getConnection();
             PreparedStatement stmtRoleId = conn.prepareStatement(queryUsersRoleIdFromEmail)) {

            stmtRoleId.setString(1, email);

            try (ResultSet rsRoleId = stmtRoleId.executeQuery()) {
                if (!rsRoleId.next()) {
                    return ""; // No user found.
                }

                int roleId = rsRoleId.getInt("role_id");

                try (PreparedStatement stmtRoleName = conn.prepareStatement(queryRolesRoleNameFromId)) {
                    stmtRoleName.setInt(1, roleId);

                    try (ResultSet rsRoleName = stmtRoleName.executeQuery()) {
                        if (rsRoleName.next()) {
                            return rsRoleName.getString("role_name");
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return ""; // Return empty string in case of error or if no role is found.
    }

    /**
     * Updates the password for a specified user.
     *
     * @param email       the email address of the user whose password will be updated.
     * @param newPassword the new password to set.
     * @return true if the password was successfully updated; false otherwise.
     */
    public static boolean updateUserPassword(String email, String newPassword) {
        try (Connection conn = getConnection();
             PreparedStatement stmtUsersUpdatePassword = conn.prepareStatement(queryUsersUpdatePassword)) {
            stmtUsersUpdatePassword.setString(1, newPassword);
            stmtUsersUpdatePassword.setString(2, email);
            int rowsUpdated = stmtUsersUpdatePassword.executeUpdate();
            return rowsUpdated > 0; // Returns true if at least one row was updated.
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Retrieves all role names available in the system.
     *
     * @return an array of role names.
     */
    public static String[] getAllRoles() {
        List<String> roles = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmtRoles = conn.prepareStatement(queryRoles);
             ResultSet rsRoles = stmtRoles.executeQuery()) {
            while (rsRoles.next()) {
                roles.add(rsRoles.getString("role_name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return roles.toArray(new String[0]);
    }

    /**
     * Retrieves all role names available in the system.
     *
     * @return an array of role names.
     */
    public static String[] getAllPriority() {
        List<String> priority = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmtPriority = conn.prepareStatement(queryPriority);
             ResultSet rsPriority = stmtPriority.executeQuery()) {
            while (rsPriority.next()) {
                priority.add(rsPriority.getInt("id") + " - " + rsPriority.getString("priority_name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return priority.toArray(new String[0]);
    }

    /**
     * Retrieves all role names available in the system.
     *
     * @return an array of role names.
     */
    public static String[] getAllStatus() {
        List<String> status = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmtPriority = conn.prepareStatement(queryStatus);
             ResultSet rsPriority = stmtPriority.executeQuery()) {
            while (rsPriority.next()) {
                status.add(rsPriority.getInt("id") + " - " + rsPriority.getString("status_name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return status.toArray(new String[0]);
    }

    /**
     * Updates the role of a specified user.
     *
     * <p>For security reasons, the current user is not allowed to change their own role.</p>
     *
     * @param email the email address of the user whose role is to be updated.
     * @param role  the new role name to assign to the user.
     * @return true if the role was successfully updated; false otherwise.
     */
    public static boolean updateUserRole(String email, String role) {
        String em = getUser().getEmail();
        if (email.equals(getUser().getEmail())) {
            return false;
        }

        int role_id = getRoleId(role);

        try (Connection conn = getConnection();
             PreparedStatement stmtUsersUpdateRoleId = conn.prepareStatement(queryUsersUpdateRoleId)) {
            stmtUsersUpdateRoleId.setInt(1, role_id);
            stmtUsersUpdateRoleId.setString(2, email);
            int rowsUpdated = stmtUsersUpdateRoleId.executeUpdate();
            return rowsUpdated > 0; // Returns true if at least one row was updated.
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Retrieves the currently authenticated user.
     *
     * @return the current {@link User} object, or null if no user is authenticated.
     */
    public static User getUser() {
        return user;
    }

    /**
     * Retrieves the role name corresponding to a given role ID.
     *
     * @param role_id the role ID.
     * @return the role name if found; otherwise, returns an empty string.
     */
    public static String getRoleName(int role_id) {
        try (Connection conn = getConnection();
             PreparedStatement stmtRoleName = conn.prepareStatement(queryRolesRoleNameFromId)) {

            stmtRoleName.setInt(1, role_id);

            try (ResultSet rsRoleName = stmtRoleName.executeQuery()) {
                if (!rsRoleName.next()) {
                    return ""; // No role found.
                }

                return rsRoleName.getString("role_name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return ""; // No role found.
    }

    /**
     * Retrieves the role ID corresponding to a given role name.
     *
     * @param role_name the role name.
     * @return the role ID if found; otherwise, returns -1.
     */
    public static int getRoleId(String role_name) {
        try (Connection conn = getConnection();
             PreparedStatement stmtRoleId = conn.prepareStatement(insertRolesIdFromRoleName)) {

            stmtRoleId.setString(1, role_name);

            try (ResultSet rsRoleId = stmtRoleId.executeQuery()) {
                if (!rsRoleId.next()) {
                    return -1; // No role found.
                }

                return rsRoleId.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return -1; // No role found.
    }

    /**
     * Adds a new user to the database.
     *
     * @param email     the email address of the new user.
     * @param password  the password for the new user.
     * @param role_name the role name to assign to the new user.
     * @return true if the user was successfully added; false otherwise.
     */
    public static boolean addUser(String email, String password, String role_name, String working_hours_weekly) {
        if (email.isEmpty() || password.isEmpty() || role_name == null) {
            return false;
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmtInsertUser = conn.prepareStatement(insertUser)) {
            stmtInsertUser.setString(1, email);
            stmtInsertUser.setString(2, password);
            stmtInsertUser.setString(3, role_name);
            stmtInsertUser.setString(4, working_hours_weekly);
            stmtInsertUser.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Adds a new recover credentials request for the given email.
     *
     * <p>If an active request already exists, this method returns the creation timestamp
     * of the existing request. Otherwise, it inserts a new request and returns the new
     * request's creation timestamp.</p>
     *
     * @param email the email address for which the recovery request is made.
     * @return a string representation of the creation timestamp of the active recovery request,
     *         or an empty string if the operation fails.
     */
    public static String addRecoverCredentialsRequests(String email) {
        if (email.isEmpty()) {
            return "";
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmtExtractActiveRequest = conn.prepareStatement(queryRecoverCredentialsRequestsActiveFromEmail)) {
            stmtExtractActiveRequest.setString(1, email);

            try (ResultSet rsCreateAt = stmtExtractActiveRequest.executeQuery()) {
                if (rsCreateAt.next()) {
                    return rsCreateAt.getTimestamp("created_at").toString(); // Active request already exists.
                }

                try (PreparedStatement stmtAddRequest = conn.prepareStatement(insertRecoverCredentialsRequests)) {
                    stmtAddRequest.setString(1, email);
                    stmtAddRequest.executeUpdate(); // Generate new request.

                    try (PreparedStatement stmtNewActiveRequest = conn.prepareStatement(queryRecoverCredentialsRequestsActiveFromEmail)) {
                        stmtNewActiveRequest.setString(1, email);
                        ResultSet rsNewCreateAt = stmtNewActiveRequest.executeQuery();
                        rsNewCreateAt.next();
                        Timestamp created_at = rsNewCreateAt.getTimestamp("created_at");
                        return created_at.toString(); // Return the new request's creation timestamp.
                    }
                }
            }
        } catch (SQLException e) {
            return "";
        }
    }

    /**
     * Counts the number of active recover credentials requests.
     *
     * @return the count of active recovery requests, or -1 if an error occurs.
     */
    public static int countRecoverCredentialsRequestsActive() {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(countRecoverCredentialsRequestsActive);
             ResultSet rs = stmt.executeQuery()) {

            // This will always return one row, even if the count is zero:
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0; // should never happen, but defensively return 0
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Retrieves all active recover credentials requests.
     *
     * <p>Each element in the returned list is a string array where the first element is the email and
     * the second element is the creation timestamp of the request.</p>
     *
     * @return a list of string arrays representing the active recovery requests.
     */
    public static List<String[]> getRecoveryCredentialsRequestsActive() {
        List<String[]> requests = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmtRecoverCredentialsRequestsActiveTSOrdered = conn.prepareStatement(queryRecoverCredentialsRequestsActiveTSOrdered);
             ResultSet rs = stmtRecoverCredentialsRequestsActiveTSOrdered.executeQuery()) {

            while (rs.next()) {
                requests.add(new String[]{rs.getString("email"), rs.getString("created_at")});
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return requests;
    }

    /**
     * Marks the recover credentials requests for a given email as evaded (closed).
     *
     * @param email the email address whose recovery requests should be marked as evaded.
     * @return true if the operation was successful; false otherwise.
     */
    public static boolean updateRecoverCredentialsRequests(String email) {
        if (email.isEmpty()) {
            return false;
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmtCloseActiveRequest = conn.prepareStatement(queryRecoverCredentialsRequestsClose)) {
            stmtCloseActiveRequest.setString(1, email);
            stmtCloseActiveRequest.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Marks the privacy policy as accepted for a specified user.
     *
     * @param email the email address of the user accepting the privacy policy.
     * @return true if the operation was successful; false otherwise.
     */
    public static boolean acceptPrivacyPolicy(String email) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmtUsersUpdatePrivacy = conn.prepareStatement(queryUsersUpdatePrivacy)) {
            stmtUsersUpdatePrivacy.setString(1, email);
            stmtUsersUpdatePrivacy.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Retrieves the privacy policy acceptance status for a specified user.
     *
     * @param email the email address of the user.
     * @return true if the user has accepted the privacy policy; false otherwise.
     */
    public static boolean getPrivacyPolicy(String email) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmtPrivacyAccepted = conn.prepareStatement(queryUsersPrivacy)) {
            stmtPrivacyAccepted.setString(1, email);

            try (ResultSet rsPrivacyAccepted = stmtPrivacyAccepted.executeQuery()) {
                if (rsPrivacyAccepted.next()) {
                    return rsPrivacyAccepted.getBoolean("privacy_accepted");
                }
            }
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Checks if an email is already registered in the database.
     *
     * @param email the email address to check.
     * @return true if the email exists in the database; false otherwise.
     */
    public static boolean checkEmailExists(String email) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmtCheckEmail = conn.prepareStatement(queryUsersRoleIdFromEmail)) {
            stmtCheckEmail.setString(1, email);

            try (ResultSet rsCheckEmail = stmtCheckEmail.executeQuery()) {
                return rsCheckEmail.next(); // Returns true if a record exists.
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    public static HashMap<LocalDate, Double> getWeeklyHours(int userId, LocalDate startDate, LocalDate endDate) {
        HashMap<LocalDate, Double> weeklyHours = new HashMap<>();

        try (Connection conn = getConnection();
             PreparedStatement stmtTimeEntryWeeklyByUserId = conn.prepareStatement(queryTimeEntryWeeklyByUserId)) {
            stmtTimeEntryWeeklyByUserId.setInt(1, userId);
            stmtTimeEntryWeeklyByUserId.setString(2, startDate.toString());
            stmtTimeEntryWeeklyByUserId.setString(3, endDate.toString());

            ResultSet rs = stmtTimeEntryWeeklyByUserId.executeQuery();
            while (rs.next()) {
                LocalDate date = LocalDate.parse(rs.getString("entry_date"));
                double hours = rs.getDouble("total_hours");
                weeklyHours.put(date, hours);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return weeklyHours;
    }

    /**
     * Retrieves the total hours recorded for a specific user and date.
     */
    public static double getTotalHoursForDate(int userId, LocalDate entryDate) {
        double totalHours = 0;


        try (Connection conn = getConnection();
             PreparedStatement stmtTimeEntryHoursByDay = conn.prepareStatement(countTimeEntryHoursByDay)) {
            stmtTimeEntryHoursByDay.setInt(1, userId);
            stmtTimeEntryHoursByDay.setString(2, entryDate.toString());

            ResultSet rs = stmtTimeEntryHoursByDay.executeQuery();
            if (rs.next()) {
                totalHours = rs.getDouble("total_hours");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return totalHours;
    }

    /**
     * Retrieves a list of tasks assigned to a user.
     *
     * @param userId The ID of the user.
     * @return A list of task names.
     */
    public static List<String> getUserTasks(int userId) {
        List<String> tasks = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmtTaskAssignmentsByUserId = conn.prepareStatement(queryTaskAssignmentsByUserId)) {
            stmtTaskAssignmentsByUserId.setInt(1, userId);
            ResultSet rs = stmtTaskAssignmentsByUserId.executeQuery();

            while (rs.next()) {
                tasks.add(rs.getString("title"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tasks;
    }

    /**
     * Retrieves the task ID based on the task name.
     *
     * @param taskName The name of the task.
     * @return The task ID or -1 if not found.
     */
    public static int getTaskIdFromName(String taskName) {
        int taskId = -1;

        try (Connection conn = getConnection();
             PreparedStatement stmtTaskIdFromName = conn.prepareStatement(queryTaskIdFromName)) {
            stmtTaskIdFromName.setString(1, taskName);
            ResultSet rs = stmtTaskIdFromName.executeQuery();

            if (rs.next()) {
                taskId = rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return taskId;
    }

    /**
     * Inserts a new time entry for a user and task.
     *
     * @param userId The ID of the user.
     * @param taskId The ID of the task.
     * @param entryDate The date of the time entry.
     * @param hours The number of hours worked.
     * @return true if the entry was inserted successfully, false otherwise.
     */
    public static boolean insertTimeEntry(int userId, int taskId, LocalDate entryDate, double hours) {
        try (Connection conn = getConnection();
             PreparedStatement stmtInsertTimeEntry = conn.prepareStatement(insertTimeEntry)) {
            stmtInsertTimeEntry.setInt(1, userId);
            stmtInsertTimeEntry.setInt(2, taskId);
            stmtInsertTimeEntry.setString(3, entryDate.toString());
            stmtInsertTimeEntry.setDouble(4, hours);

            int rowsInserted = stmtInsertTimeEntry.executeUpdate();
            return rowsInserted > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Retrieves the list of projects assigned to a specific user.
     *
     * @param userId The ID of the user.
     * @param formatWithIndex Define the return format, if true every string has the format "index - value", else "value".
     * @return An array of assigned project names.
     */
    public static String[] getAssignedProjects(int userId, boolean formatWithIndex) {
        ArrayList<String> projects = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmtProjectsVisibilityByUserId = conn.prepareStatement(queryProjectsVisibilityByUserId)) {
            stmtProjectsVisibilityByUserId.setInt(1, userId);
            ResultSet rs = stmtProjectsVisibilityByUserId.executeQuery();

            while (rs.next()) {
                String projectUniqueName = ((formatWithIndex) ? (rs.getInt("id") + " - ") : ("")) + rs.getString("title");
                projects.add(projectUniqueName);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return projects.toArray(new String[0]);
    }

    /**
     * Adds a new project to the database.
     *
     * @param title The title of the project.
     * @param description The project description.
     * @param adminId The ID of the administrator creating the project.
     * @param supervisorId The ID of the supervisor managing the project.
     * @return true if the project was added successfully, false otherwise.
     */
    public static boolean addProject(String title, String description, int adminId, int supervisorId) {
        try (Connection conn = getConnection();
             PreparedStatement stmtAddProject = conn.prepareStatement(insertProject, Statement.RETURN_GENERATED_KEYS)) {
            stmtAddProject.setString(1, title);
            stmtAddProject.setString(2, description);
            stmtAddProject.setInt(3, adminId);
            stmtAddProject.setInt(4, supervisorId);

            int affectedRows = stmtAddProject.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmtAddProject.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int project_id = generatedKeys.getInt(1);

                        try (PreparedStatement stmtSupervisorVisibility = conn.prepareStatement(insertProjectsVisibility)) {
                            stmtSupervisorVisibility.setInt(1, project_id);
                            stmtSupervisorVisibility.setInt(2, supervisorId);

                            stmtSupervisorVisibility.executeUpdate();

                            return true;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    /**
     *
     */
    public static String[] getSupervisors() {
        ArrayList<String> projects = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmtUsersSupervisors = conn.prepareStatement(queryUsersSupervisors)) {
            ResultSet rs = stmtUsersSupervisors.executeQuery();

            while (rs.next()) {
                String supervisorsUniqueName = rs.getString("id") + " - " + rs.getString("email");
                projects.add(supervisorsUniqueName);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return projects.toArray(new String[0]);
    }

    /**
     *
     */
    public static String[] getResearchersByProjectId(int projectId) {
        ArrayList<String> projects = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmtResearchersByProjectId = conn.prepareStatement(queryUsersResearchersByProjectId)) {
            stmtResearchersByProjectId.setInt(1, projectId);
            ResultSet rsResearchersByProjectId = stmtResearchersByProjectId.executeQuery();

            while (rsResearchersByProjectId.next()) {
                String researchersUniqueName = rsResearchersByProjectId.getString("id") + " - " + rsResearchersByProjectId.getString("email");
                projects.add(researchersUniqueName);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return projects.toArray(new String[0]);
    }

    /**
     *
     */
    public static String[] getResearchersExcludingProjectId(int projectId) {
        ArrayList<String> projects = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmtResearchersExcludingProjectId = conn.prepareStatement(queryUsersResearchersExcludingProjectId)) {
            stmtResearchersExcludingProjectId.setInt(1, projectId);
            ResultSet rsResearchersExcludingProjectId = stmtResearchersExcludingProjectId.executeQuery();

            while (rsResearchersExcludingProjectId.next()) {
                String researchersUniqueName = rsResearchersExcludingProjectId.getString("id") + " - " + rsResearchersExcludingProjectId.getString("email");
                projects.add(researchersUniqueName);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return projects.toArray(new String[0]);
    }

    public static boolean addWorkPackage(int project_id, String title, String description, LocalDate start_date, LocalDate end_date) {
        try (Connection conn = getConnection(); //(project_id, title, description, start_date, end_date)
             PreparedStatement stmtAddProject = conn.prepareStatement(insertWorkPackage)) {
            stmtAddProject.setInt(1, project_id);
            stmtAddProject.setString(2, title);
            stmtAddProject.setString(3, description);
            stmtAddProject.setDate(4, Date.valueOf(start_date));
            stmtAddProject.setDate(5, Date.valueOf(end_date));

            int affectedRows = stmtAddProject.executeUpdate();

            if (affectedRows > 0) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    /**
     * Retrieves a list of work packages associated with a specific project.
     *
     * @param projectId The ID of the project.
     * @return A list of work package titles.
     */
    public static List<String> getWorkPackagesByProject(int projectId) {
        List<String> workPackages = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmtWorkPackagesByProject = conn.prepareStatement(queryWorkPackagesByProject)) {
            stmtWorkPackagesByProject.setInt(1, projectId);
            ResultSet rsWorkPackagesByProject = stmtWorkPackagesByProject.executeQuery();

            while (rsWorkPackagesByProject.next()) {
                workPackages.add(rsWorkPackagesByProject.getInt("id") + " - " + rsWorkPackagesByProject.getString("title"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return workPackages;
    }

    /**
     * Retrieves a list of tasks associated with a specific project.
     *
     * @param workPackagesId The ID of the project.
     * @return A list of task titles.
     */
    public static List<String> getTasksByWorkPackages(int workPackagesId) {
        List<String> tasks = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmtTasksByWorkPackages = conn.prepareStatement(queryTasksByWorkPackage)) {
            stmtTasksByWorkPackages.setInt(1, workPackagesId);
            ResultSet rsTasksByWorkPackages = stmtTasksByWorkPackages.executeQuery();

            while (rsTasksByWorkPackages.next()) {
                tasks.add(rsTasksByWorkPackages.getInt("id") + " - " + rsTasksByWorkPackages.getString("title"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tasks;
    }

    /**
     * Adds a new task to the database for a given work package.
     *
     * @param work_package_id      The ID of the project to which the task belongs.
     * @param title                The title of the task.
     * @param description    The description of the task.
     * @param effortHours    Estimated effort in hours.
     * @param durationHours  Estimated duration in hours.
     * @param deadline       The deadline for the task.
     * @param priority_id       The priority of the task (e.g., High, Medium, Low).
     * @param status_id         The status of the task (e.g., Not Started, In Progress, Completed).
     * @return true if the task was successfully added, false otherwise.
     */
    public static int addTask(int work_package_id, String title, String description, int effortHours, int durationHours, LocalDate deadline, int priority_id, int status_id) {
        try (Connection conn = getConnection();
             PreparedStatement stmtAddTask = conn.prepareStatement(insertTask)) {

            stmtAddTask.setInt(1, work_package_id);
            stmtAddTask.setString(2, title);
            stmtAddTask.setString(3, description);
            stmtAddTask.setInt(4, effortHours);
            stmtAddTask.setInt(5, durationHours);
            stmtAddTask.setDate(6, Date.valueOf(deadline));
            stmtAddTask.setInt(7, priority_id);
            stmtAddTask.setInt(8, status_id);

            //return task_id generated
            int affectedRows = stmtAddTask.executeUpdate();
            ResultSet taskId = stmtAddTask.getGeneratedKeys();
            taskId.next();
            return taskId.getInt(1);

        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Retrieves a list of work packages associated with a specific project.
     *
     * @param formatWithIndex Define the return format, if true every string has the format "index - value", else "value".
     * @return A list of work package titles.
     */
    public static String[] getProjectsActive(boolean formatWithIndex) {
        List<String> projectsActive = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmtProjectsActive = conn.prepareStatement(queryProjectsActive)) {
            ResultSet rsProjectsActive = stmtProjectsActive.executeQuery();

            while (rsProjectsActive.next()) {
                String projectUniqueName = ((formatWithIndex) ? (rsProjectsActive.getInt("id") + " - ") : ("")) + rsProjectsActive.getString("title");
                projectsActive.add(projectUniqueName);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return projectsActive.toArray(new String[0]);
    }

    /**
     * Retrieves a list of work packages associated with a specific project.
     *
     * @param formatWithIndex Define the return format, if true every string has the format "index - value", else "value".
     * @return A list of work package titles.
     */
    public static String[] getProjectsArchived(boolean formatWithIndex) {
        List<String> projectsArchived = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmtProjectsArchived = conn.prepareStatement(queryProjectsArchived)) {
            ResultSet rsProjectsArchived = stmtProjectsArchived.executeQuery();

            while (rsProjectsArchived.next()) {
                String projectUniqueName = ((formatWithIndex) ? (rsProjectsArchived.getInt("id") + " - ") : ("")) + rsProjectsArchived.getString("title");
                projectsArchived.add(projectUniqueName);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return projectsArchived.toArray(new String[0]);
    }

    public static boolean addMilestone(int projectId, String title, String description){
        try (Connection conn = getConnection();
             PreparedStatement stmtMilestone = conn.prepareStatement(insertMilestone)) {

            stmtMilestone.setInt(1, projectId);
            stmtMilestone.setString(2, title);
            stmtMilestone.setString(3, description);

            int affectedRows = stmtMilestone.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Retrieves a list of milestones associated with a specific project.
     *
     * @param projectId The ID of the project.
     * @return A list of milestone titles with their IDs.
     */
    public static List<String> getMilestonesByProject(int projectId) {
        List<String> milestones = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmtMilestonesByProject = conn.prepareStatement(queryMilestonesByProject)) {
            stmtMilestonesByProject.setInt(1, projectId);
            ResultSet rsMilestonesByProject = stmtMilestonesByProject.executeQuery();

            while (rsMilestonesByProject.next()) {
                milestones.add(rsMilestonesByProject.getInt("id") + " - " + rsMilestonesByProject.getString("title"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return milestones;
    }

    /**
     * Updates the title and description of an existing milestone.
     *
     * @param milestoneId  The ID of the milestone to update.
     * @param taskId     The new title for the milestone.
     *
     * @return 1 if is created, 0 if already exists and -1 in case of error
     */
    public static int addMilestoneAssignments(int milestoneId, int taskId) {
        try (Connection conn = getConnection();
             PreparedStatement stmtUpdateMilestone = conn.prepareStatement(insertMilestoneAssignments)) {
            stmtUpdateMilestone.setInt(1, milestoneId);
            stmtUpdateMilestone.setInt(2, taskId);

            int affectedRows = stmtUpdateMilestone.executeUpdate();

            return affectedRows;

        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Retrieves a list of work packages associated with a specific project.
     *
     * @param userId The ID of the project.
     * @return A list of work package titles.
     */
    public static List<String> getTasksByUser(int userId) {
        List<String> workPackages = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmtWorkPackagesByProject = conn.prepareStatement(queryTasksByUser)) {
            stmtWorkPackagesByProject.setInt(1, userId);
            ResultSet rsWorkPackagesByProject = stmtWorkPackagesByProject.executeQuery();

            while (rsWorkPackagesByProject.next()) {
                workPackages.add(rsWorkPackagesByProject.getInt("id") + " - " + rsWorkPackagesByProject.getString("title"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return workPackages;
    }

    public static boolean editWorkPackage(int workpackages_id, LocalDate start_date, LocalDate end_date) {
        try (Connection conn = getConnection(); //(project_id, title, description, start_date, end_date)
             PreparedStatement stmtAddProject = conn.prepareStatement(queryUpdateWorkPackage)) {
            stmtAddProject.setDate(1, Date.valueOf(start_date));
            stmtAddProject.setDate(2, Date.valueOf(end_date));
            stmtAddProject.setInt(3, workpackages_id);

            int affectedRows = stmtAddProject.executeUpdate();

            if (affectedRows > 0) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    public static boolean deleteWorkPackageById(int workPackageId) {
        try (Connection conn = getConnection();
             PreparedStatement stmtDeleteWorkPackageById = conn.prepareStatement(deleteWorkPackageById)) {
            stmtDeleteWorkPackageById.setInt(1, workPackageId);
            int affectedRows = stmtDeleteWorkPackageById.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deleteTaskById(int taskId) {
        try (Connection conn = getConnection();
             PreparedStatement stmtDeleteWorkPackageById = conn.prepareStatement(deleteTaskById)) {
            stmtDeleteWorkPackageById.setInt(1, taskId);
            int affectedRows = stmtDeleteWorkPackageById.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Counts the number of active recover credentials requests.
     *
     * @return the count of active recovery requests, or -1 if an error occurs.
     */
    public static int countTaskDependenciesByTaskId(int task_id) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmtExtractTaskDependenciesByTaskId = conn.prepareStatement(countTaskDependenciesByTaskId)) {
            stmtExtractTaskDependenciesByTaskId.setInt(1, task_id);

            try (ResultSet rsTaskDependenciesByTaskId = stmtExtractTaskDependenciesByTaskId.executeQuery()) {
                if (rsTaskDependenciesByTaskId.next()) {
                    return rsTaskDependenciesByTaskId.getInt(1); // Number of active requests.
                }
                return -1; // There is a problem.
            }
        } catch (SQLException e) {
            return -1; // There is a problem.
        }
    }

    /**
     * Counts the number of active recover credentials requests.
     *
     * @return the count of active recovery requests, or -1 if an error occurs.
     */
    public static int countMilestoneByTaskId(int task_id) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmtExtractMilestonesByTaskId = conn.prepareStatement(countMilestoneByTaskId)) {
            stmtExtractMilestonesByTaskId.setInt(1, task_id);

            try (ResultSet rsMilestonesByTaskId = stmtExtractMilestonesByTaskId.executeQuery()) {
                if (rsMilestonesByTaskId.next()) {
                    return rsMilestonesByTaskId.getInt(1); // Number of active requests.
                }
                return -1; // There is a problem.
            }
        } catch (SQLException e) {
            return -1; // There is a problem.
        }
    }

    public static boolean checkTaskDeadlineValidity(int workPackageId, LocalDate deadline) {
        try (Connection conn = getConnection();
             PreparedStatement stmtWorkPackageTimeRangeById = conn.prepareStatement(queryWorkPackageTimeRangeById)) {
            stmtWorkPackageTimeRangeById.setInt(1, workPackageId);
            try (ResultSet rsWorkPackageTimeRangeById = stmtWorkPackageTimeRangeById.executeQuery()) {
                if (rsWorkPackageTimeRangeById.next()) {
                    LocalDate start_time = rsWorkPackageTimeRangeById.getDate("start_date").toLocalDate();
                    LocalDate end_time = rsWorkPackageTimeRangeById.getDate("end_date").toLocalDate();
                    return !start_time.isAfter(deadline) && !end_time.isBefore(deadline); // start_time <= deadline <= end_time
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    /**
     * Retrieves a list of milestones associated with a specific project.
     *
     * @param projectId The ID of the project.
     * @return A list of milestone titles with their IDs.
     */
    public static List<String> getTasksByProject(int projectId) {
        List<String> tasks = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmtTasksByProjectId = conn.prepareStatement(queryTasksByProjectId)) {
            stmtTasksByProjectId.setInt(1, projectId);
            ResultSet rsTasksByProjectId = stmtTasksByProjectId.executeQuery();

            while (rsTasksByProjectId.next()) {
                tasks.add(rsTasksByProjectId.getInt("id") + " - " + rsTasksByProjectId.getString("title"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tasks;
    }

    /**
     * Retrieves a list of user assignment hours data for a given task.
     * <p>
     * Each row from the database is mapped into a HashMap with:
     * <ul>
     *   <li>Key: a string in the format "id - email"</li>
     *   <li>Value: a string in the format "effort_consumed - effort_hypothetic"</li>
     * </ul>
     * </p>
     *
     * @param taskId the ID of the task to filter the query
     * @return a list of HashMaps, each containing one entry with the user and assignment hours data
     */
    public static List<HashMap<String, String>> getUsersAndAssignmentsHoursByTasks(int taskId) {
        List<HashMap<String, String>> results = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(queryUsersAndAssignmentsHoursByTasks)) {

            // Set the taskId parameter for the SQL query
            stmt.setInt(1, taskId);

            // Execute the query and obtain the result set
            ResultSet rs = stmt.executeQuery();

            // Process each row in the result set
            while (rs.next()) {
                // Retrieve values from the result set
                int id = rs.getInt("id");
                String email = rs.getString("email");
                int effortConsumed = rs.getInt("effort_consumed");
                int effortHypothetic = rs.getInt("effort_hypothetic");

                // Create the key: "id - email"
                String key = id + " - " + email;
                // Create the value: "effortConsumed - effortHypothetic"
                String value = effortConsumed + " - " + effortHypothetic;

                // Create a new HashMap for the current row
                HashMap<String, String> map = new HashMap<>();
                map.put(key, value);

                // Add the map to the results list
                results.add(map);
            }
        } catch (SQLException e) {
            // Log the exception stack trace
            e.printStackTrace();
        }
        return results;
    }

    public static boolean addTaskAssignment(int taskId, int userId, int effortHypothetic) {
        try (Connection conn = getConnection();
             PreparedStatement stmtAddTaskAssignment = conn.prepareStatement(insertTaskAssignments)) {
            stmtAddTaskAssignment.setInt(1, taskId);
            stmtAddTaskAssignment.setInt(2, userId);
            stmtAddTaskAssignment.setInt(3, effortHypothetic);

            int affectedRows = stmtAddTaskAssignment.executeUpdate();

            return affectedRows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String getSingleTaskNameById(int taskId) {
        String results = "";

        try (Connection conn = getConnection();
             PreparedStatement stmtSingleTaskNameById = conn.prepareStatement(querySingleTaskById)) {

            // Set the taskId parameter for the SQL query
            stmtSingleTaskNameById.setInt(1, taskId);

            // Execute the query and obtain the result set
            ResultSet rsSingleTaskNameById = stmtSingleTaskNameById.executeQuery();

            rsSingleTaskNameById.next();

            results = rsSingleTaskNameById.getInt("id") + " - " + rsSingleTaskNameById.getString("title");

        } catch (SQLException e) {
            // Log the exception stack trace
            e.printStackTrace();
        }
        return results;
    }

    public static String getProjectTitleById(int projectId) {
        String results = "";

        try (Connection conn = getConnection();
             PreparedStatement stmtProjectTitleById = conn.prepareStatement(queryProjectTitleById)) {

            // Set the taskId parameter for the SQL query
            stmtProjectTitleById.setInt(1, projectId);

            // Execute the query and obtain the result set
            ResultSet rsProjectTitleById = stmtProjectTitleById.executeQuery();

            rsProjectTitleById.next();

            results = projectId + " - " + rsProjectTitleById.getString("title");

        } catch (SQLException e) {
            // Log the exception stack trace
            e.printStackTrace();
        }
        return results;
    }

    public static boolean addReasearchersToProject(int projectId, int userId) {
        try (Connection conn = getConnection();
             PreparedStatement stmtAddReasearchersToProject = conn.prepareStatement(insertProjectsVisibility)) {
            stmtAddReasearchersToProject.setInt(1, projectId);
            stmtAddReasearchersToProject.setInt(2, userId);

            int affectedRows = stmtAddReasearchersToProject.executeUpdate();

            return affectedRows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
