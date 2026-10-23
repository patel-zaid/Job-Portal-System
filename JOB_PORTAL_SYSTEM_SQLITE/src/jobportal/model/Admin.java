package jobportal.model;

/**
 * The administrator of the portal.
 * The class is FINAL: no other class is allowed to extend Admin.
 */
public final class Admin extends User {

    public Admin() {
        super();
    }

    public Admin(int userId, String fullName, String email, String passwordHash, String phone, String status) {
        super(userId, fullName, email, passwordHash, phone, status);
    }

    @Override
    public String getRole() {
        return ROLE_ADMIN;
    }

    @Override
    public String getDashboardTitle() {
        return "Admin Dashboard";
    }
}
