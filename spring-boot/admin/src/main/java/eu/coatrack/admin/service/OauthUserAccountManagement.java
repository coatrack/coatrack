package eu.coatrack.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.coatrack.config.github.GithubEmail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class OauthUserAccountManagement {

    private static final String GITHUB_API_EMAIL = "https://api.github.com/user/emails";

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

    public String getEmailFromLoggedInUser() throws JsonProcessingException {
        String email = getAttributesFromAuthentication().getAttribute("email");

        // If the email is defined as private on GitHub (The Oauth2 will retrieve null),
        // so the only way to retrieve is to make request with the Oauth Token
        if (email == null || email.isEmpty()) {

            // Initialize RestTemplate and HTTP Headers
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "token " + getTokenFromLoggedInUser());
            HttpEntity<String> githubRequest = new HttpEntity(headers);

            // Make the request to GitHub for the emails
            ResponseEntity<String> userEmailsRequest = restTemplate.exchange(GITHUB_API_EMAIL, HttpMethod.GET,
                    githubRequest, String.class);

            email = GetPrimaryEmailFromLoggedInUser(userEmailsRequest);
        }
        return email;
    }

    private String GetPrimaryEmailFromLoggedInUser(ResponseEntity<String> userEmailsRequest) throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        List<GithubEmail> emailsList = objectMapper.
                readValue(userEmailsRequest.getBody(),
                        objectMapper
                                .getTypeFactory()
                                .constructCollectionType(List.class, GithubEmail.class));

        for (GithubEmail userEmail : emailsList) {
            if (userEmail.getVerified() == true && userEmail.getPrimary()) {
                return userEmail.getEmail();
            }
        }
        return null;
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
