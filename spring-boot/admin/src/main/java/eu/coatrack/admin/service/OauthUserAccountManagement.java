package eu.coatrack.admin.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class OauthUserAccountManagement {

    @Autowired
    OAuth2AuthorizedClientService clientService;

    private OAuth2User getAttributesFromAuthentication() {
        OAuth2User loggedInUser = (OAuth2User) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
        return loggedInUser;
    }

    public String getLoginNameFromLoggedInUser() {
        return getAttributesFromAuthentication().getAttribute("login");
    }

    public String getNameFromLoggedInUser() {
        return getAttributesFromAuthentication().getAttribute("name");
    }

    public String getCompanyFromLoggedInUser() {
        return getAttributesFromAuthentication().getAttribute("company");
    }

    public String getEmailFromLoggedInUser() {
        return getAttributesFromAuthentication().getAttribute("email");
    }

    public String getTokenFromLoggedInUser() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        String clientRegistrationId = oauthToken.getAuthorizedClientRegistrationId();
        OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(clientRegistrationId,
                oauthToken.getName());
        return client.getAccessToken().getTokenValue();
    }
}
