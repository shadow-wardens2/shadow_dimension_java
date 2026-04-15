package Entities.User;

public class User {

    private int id;
    private String email;
    private String username;
    private String password;
    private String fullName;
    private String phone;
    private String country;
    private String city;
    private String bio;
    private String roles;
    private int isActive;
    private int isLocked;
    private int isVerified;
    private String lastPresence;

    public User() {
    }

    public User(int id, String email, String username, String password, String fullName,
                String phone, String country, String city, String bio) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.phone = phone;
        this.country = country;
        this.city = city;
        this.bio = bio;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public int getIsActive() {
        return isActive;
    }

    public void setIsActive(int isActive) {
        this.isActive = isActive;
    }

    public int getIsLocked() {
        return isLocked;
    }

    public void setIsLocked(int isLocked) {
        this.isLocked = isLocked;
    }

    public int getIsVerified() {
        return isVerified;
    }

    public void setIsVerified(int isVerified) {
        this.isVerified = isVerified;
    }

    public String getLastPresence() {
        return lastPresence;
    }

    public void setLastPresence(String lastPresence) {
        this.lastPresence = lastPresence;
    }

    public String getUserIdentity() {
        String u = (username == null || username.isBlank()) ? "-" : username;
        String e = (email == null || email.isBlank()) ? "-" : email;
        return u + "\n" + e;
    }

    public String getRank() {
        String role = extractPrimaryRole();
        if (role.startsWith("ROLE_")) {
            return role.substring(5);
        }
        return role;
    }

    public String getStatus() {
        if (isLocked == 1) {
            return "LOCKED";
        }
        return isActive == 1 ? "ACTIVE" : "INACTIVE";
    }

    public String extractPrimaryRole() {
        if (roles == null || roles.isBlank()) {
            return "ROLE_USER";
        }

        String cleaned = roles.replace("[", "")
                .replace("]", "")
                .replace("\"", "")
                .trim();

        if (cleaned.contains(",")) {
            return cleaned.split(",")[0].trim();
        }

        return cleaned.isBlank() ? "ROLE_USER" : cleaned;
    }

    public boolean isAdmin() {
        return extractPrimaryRole().equals("ROLE_ADMIN") || (roles != null && roles.contains("ROLE_ADMIN"));
    }
}
