package com.example.campuscrush.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.campuscrush.entity.user.User;

public class CampusUserDetails implements UserDetails {

    private final User user;

    public CampusUserDetails(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override public String getPassword() { return null; }
    @Override public String getUsername() { return user.getPublicId().toString(); }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return !user.isShadowBanned(); }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
