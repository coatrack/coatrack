package eu.coatrack.admin.service;

/*-
 * #%L
 * coatrack-admin
 * %%
 * Copyright (C) 2013 - 2022 Corizon | Institut für angewandte Systemtechnik Bremen GmbH (ATB)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.coatrack.config.github.GithubEmail;
import lombok.AllArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
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
@AllArgsConstructor
public class OAuthUserDetailsService {

    private static final String GITHUB_API_EMAILS_URL = "https://api.github.com/user/emails";

    private final OAuth2AuthorizedClientService clientService;
    private final RestTemplate restTemplate;

    private OAuth2User getLoggedInUser() {
        return (OAuth2User) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
    }

    public String getLoginNameFromLoggedInUser() {
        return getLoggedInUser().getAttribute("login");
    }

    public String getNameFromLoggedInUser() {
        return getLoggedInUser().getAttribute("name");
    }

    public String getCompanyFromLoggedInUser() {
        return getLoggedInUser().getAttribute("company");
    }

    public String getEmailFromLoggedInUser() throws JsonProcessingException {
        String email = getLoggedInUser().getAttribute("email");
        // In case that the LoggedInUser has their email set as private in the GitHub Profile, then this will not be returned when logging in with the
        // Oauth2 (so the Oauth2 will "respect" the public/private set in the GitHub Profile).
        // Therefore, the only way is to make a specific call to the GitHub API requesting for all the registered emails from the LoggedInUser (using the authentication token to define which user specifically)
        if (email == null || email.isEmpty()) {
            email = getEmailFromLoggedInUserViaGitHubAPI();
        }
        return email;
    }

    private String getEmailFromLoggedInUserViaGitHubAPI() {
        return getEmailAddressesOfLoggedInUserFromGithub()
                .stream()
                .filter(email -> (email.isVerified() && email.isPrimary()))
                .map(GithubEmail::getEmail)
                .findFirst()
                .orElse(null);
    }

    private List<GithubEmail> getEmailAddressesOfLoggedInUserFromGithub() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "token " + getAuthorizationBearerTokenFromLoggedInUser());
        HttpEntity<String> AuthorizationHeaderForGitHubAPI = new HttpEntity(headers);

        ResponseEntity<List<GithubEmail>> emailsListFromGithub = restTemplate.exchange(GITHUB_API_EMAILS_URL, HttpMethod.GET,
                AuthorizationHeaderForGitHubAPI, new ParameterizedTypeReference<List<GithubEmail>>() {});
        return emailsListFromGithub.getBody();
    }

    public String getAuthorizationBearerTokenFromLoggedInUser() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        String clientRegistrationId = oauthToken.getAuthorizedClientRegistrationId();
        OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(clientRegistrationId, oauthToken.getName());
        return client.getAccessToken().getTokenValue();
    }
}
