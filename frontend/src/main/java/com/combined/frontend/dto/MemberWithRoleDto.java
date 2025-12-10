package com.combined.frontend.dto;

import com.combined.backend.model.Membership;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MemberWithRoleDto {
    private Long userId;
    private String firstName;
    private String lastName;
    private String nickname;
    private String email;
    private Membership.Role role;

    public MemberWithRoleDto() {}

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Membership.Role getRole() {
        return role;
    }

    public void setRole(Membership.Role role) {
        this.role = role;
    }
}
