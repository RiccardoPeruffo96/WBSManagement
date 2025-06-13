package it.univr.wbsmanagement.models;

import it.univr.wbsmanagement.database.DatabaseManager;

/**
 * Represents a user in the time tracking system.
 *
 * <p>This class encapsulates basic user attributes such as email, role name, and role id.
 * It provides constructors that allow creating a user by specifying either the role name or role id.
 * When only one of these is provided, the corresponding value is retrieved from the DatabaseManager.</p>
 *
 * <p>Note: The set_role methods are placeholders and currently always return true.</p>
 */
public class User {
    /**
     * The id of the user.
     */
    private final int user_id;

    /**
     * The email address of the user.
     */
    private final String email;

    /**
     * The role name of the user.
     */
    private String role;

    /**
     * The role id of the user.
     */
    private int role_id;

    /**
     * Constructs a new User with the specified email and role name.
     *
     * <p>The role id is determined by querying the DatabaseManager with the provided role name.</p>
     *
     * @param email the email address of the user.
     * @param role the role name of the user.
     */
    public User(int user_id, String email, String role) {
        this.user_id = user_id;
        this.email = email;
        this.role = role;
        this.role_id = DatabaseManager.getRoleId(role);
    }

    /**
     * Constructs a new User with the specified email and role id.
     *
     * <p>The role name is determined by querying the DatabaseManager with the provided role id.</p>
     *
     * @param email the email address of the user.
     * @param role_id the role id of the user.
     */
    public User(int user_id, String email, int role_id) {
        this.user_id = user_id;
        this.email = email;
        this.role_id = role_id;
        this.role = DatabaseManager.getRoleName(role_id);
    }

    /**
     * Constructs a new User with the specified email, role name, and role id.
     *
     * @param email the email address of the user.
     * @param role the role name of the user.
     * @param role_id the role id of the user.
     */
    public User(int user_id, String email, String role, int role_id) {
        this.user_id = user_id;
        this.email = email;
        this.role = role;
        this.role_id = role_id;
    }

    /**
     * Returns the id of the user.
     *
     * @return the user's id.
     */
    public int getUserId(){
        return user_id;
    }

    /**
     * Returns the email address of the user.
     *
     * @return the user's email.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Returns the role name of the user.
     *
     * @return the user's role name.
     */
    public String getRole() {
        return role;
    }

    /**
     * Returns the role id of the user.
     *
     * @return the user's role id.
     */
    public int getRole_id() {
        return role_id;
    }

    /**
     * Sets the user's role by specifying a role name.
     *
     * <p>This method is a placeholder and currently always returns true.
     * Future implementations should update both the role name and role id accordingly.</p>
     *
     * @param role the new role name.
     * @return true if the operation was successful.
     */
    public boolean set_role(String role) {
        return this.set_role(role, -1);
    }

    /**
     * Sets the user's role by specifying a role id.
     *
     * <p>This method is a placeholder and currently always returns true.
     * Future implementations should update both the role id and role name accordingly.</p>
     *
     * @param role_id the new role id.
     * @return true if the operation was successful.
     */
    public boolean set_role(int role_id) {
        return this.set_role("", role_id);
    }

    /**
     * Sets the user's role by specifying both a role name and a role id.
     *
     * <p>This method is a placeholder and currently always returns true.
     * Future implementations may include validations to ensure the role name and role id match.</p>
     *
     * @param role the new role name.
     * @param role_id the new role id.
     * @return true if the operation was successful.
     */
    public boolean set_role(String role, int role_id) {
        try {

            this.role = role;
            this.role_id = role_id;

            if(this.role.isEmpty()){
                this.role = DatabaseManager.getRoleName(this.role_id);
            }

            if(this.role_id == -1) {
                this.role_id = DatabaseManager.getRoleId(this.role);
            }

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}