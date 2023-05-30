package com.braunSebs.rpaetc.vendors.uiPath.config;

import java.time.Instant;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * The UiPathAuthenticator class handles the authentication process for UiPath
 * API
 * requests. It stores the access token and refreshes it if it's expired.
 */
@Component
public class UiPathAuthenticator {

    private static final Logger LOGGER = LoggerFactory.getLogger(UiPathAuthenticator.class);

    @Autowired
    private RestTemplate uiPathDirectoryRestClient;

    @Value("${bridge.uipath.app-id}")
    private String clientId;

    @Value("${bridge.uipath.secret-app-key}")
    private String clientSecret;

    @Value("${bridge.uipath.cloud.url}")
    private String cloudUrl;

    @Value("${bridge.uipath.cloud.org}")
    private String cloudOrg;

    @Value("${bridge.uipath.cloud.tenant}")
    private String cloudTenant;

    @Value("${bridge.uipath.cloud.access-token-url}")
    
    private String accessTokenUrl;

    private HttpHeaders headers = new HttpHeaders();

    private String accessToken;

    private Instant tokenExpiration;

    /**
     * Returns the HttpHeaders containing the authentication information.
     *
     * @return HttpHeaders instance with authentication data.
     */
    public HttpHeaders getHeaders() {
        return headers;
    }

    /**
     * Authenticates the client and stores the access token in headers.
     *
     * @throws HttpClientErrorException if an error occurs during authentication.
     */
    public void authenticate() throws HttpClientErrorException {
        LOGGER.debug("Starting client authentication process");

        HttpHeaders headersAuth = new HttpHeaders();
        headersAuth.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("scope", "OR.Default");

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headersAuth);

        try {
            ResponseEntity<String> responseEntity = uiPathDirectoryRestClient.exchange(
                    accessTokenUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class);

            JSONObject responseBody = new JSONObject(responseEntity.getBody());
            accessToken = responseBody.getString("access_token");
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            LOGGER.info("Client authenticated successfully");

            int expiresIn = responseBody.getInt("expires_in");
            tokenExpiration = Instant.now().plusSeconds(expiresIn - 60);

        } catch (HttpClientErrorException e) {
            LOGGER.error("Authentication failed with status code {}: {}", e.getRawStatusCode(), e.getMessage());
            throw e;
        }
    }

    /**
     * Checks if the access token is expired.
     *
     * @return true if the access token is expired, false otherwise.
     */
    public boolean isAccessTokenExpired() {
        // Check if tokenExpiration is null or if the current time is after the token
        // expiration
        return tokenExpiration == null || Instant.now().isAfter(tokenExpiration);
    }

    /**
     * Refreshes the access token if it is expired.
     *
     * This method is synchronized to prevent race conditions in multi-threaded
     * environments.
     */
    public synchronized void refreshTokenIfExpired() {
        // Check if the access token is expired
        if (isAccessTokenExpired()) {
            LOGGER.debug("Access token is expired, starting refresh process");
            // If it is expired, authenticate again to get a new access token
            authenticate();
            LOGGER.info("Access token successfully refreshed");
        }
    }

    /**
     * Returns the current access token.
     *
     * @return the current access token
     */
    public String getAccessToken() {
        return accessToken;
    }

}