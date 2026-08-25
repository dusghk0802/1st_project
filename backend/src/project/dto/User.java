package project.dto;

import java.sql.Timestamp;

public class User {
    private long userId;
    private String loginId;
    private String passwordHash;
    private String nickname;
    private String email;
    private String role;
    private String status;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public User() {}

    public User(String loginId, String passwordHash, String nickname, String email, String role) {
        this.loginId = loginId;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.email = email;
        this.role = role;
    }

    // Getter & Setter
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    public String getLoginId() { return loginId; }
    public void setLoginId(String loginId) { this.loginId = loginId; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}