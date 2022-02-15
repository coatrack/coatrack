package eu.coatrack.admin.service;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class OauthUserAccountManagement {

    private OAuth2User getAttributesFromAuthentication() {
        OAuth2User loggedInUser = (OAuth2User) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
        return loggedInUser;
    }

    public String getLoginNameFromLoggedInUser(){
        return getAttributesFromAuthentication().getAttribute("login");
    }

    public String getNameFromLoggedInUser(){
        return getAttributesFromAuthentication().getAttribute("name");
    }

    public String getCompanyFromLoggedInUser(){
        return getAttributesFromAuthentication().getAttribute("company");
    }

    public String getEmailFromLoggedInUser(){
        return getAttributesFromAuthentication().getAttribute("email");
    }


}
