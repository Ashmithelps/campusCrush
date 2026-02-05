package com.example.campuscrush.security.util;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import com.example.campuscrush.entity.user.User;
import com.example.campuscrush.security.CampusUserDetails;

public class SecurityUtils {

    public static User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !(auth.getPrincipal() instanceof CampusUserDetails)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        return ((CampusUserDetails) auth.getPrincipal()).getUser();
    }
}