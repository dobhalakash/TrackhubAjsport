package com.dreamnest.service;

import com.dreamnest.exception.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * Verifies a Facebook Login access token server-side (via the Graph API's
 * debug_token endpoint, using the app secret) before trusting any profile
 * data it returns - this prevents a forged/stolen token for a *different*
 * app from being used to log into DreamNest.
 */
@Service
public class FacebookAuthService {

    private static final Logger log = LoggerFactory.getLogger(FacebookAuthService.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${dreamnest.oauth.facebook.app-id}")
    private String appId;

    @Value("${dreamnest.oauth.facebook.app-secret}")
    private String appSecret;

    public static class FacebookProfile {
        public String id;
        public String email;
        public String name;
    }

    public FacebookProfile verifyAndFetchProfile(String accessToken) {
        if (isUsingDummyCredentials()) {
            throw new UnauthorizedException(
                    "Facebook Sign-In is not configured yet. Set FACEBOOK_APP_ID and FACEBOOK_APP_SECRET to enable it.");
        }

        // Step 1: confirm the token was actually issued for OUR app.
        String debugUrl = UriComponentsBuilder.fromHttpUrl("https://graph.facebook.com/debug_token")
                .queryParam("input_token", accessToken)
                .queryParam("access_token", appId + "|" + appSecret)
                .toUriString();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> debugResponse = restTemplate.getForObject(debugUrl, Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> data = debugResponse != null ? (Map<String, Object>) debugResponse.get("data") : null;
            if (data == null || !appId.equals(String.valueOf(data.get("app_id"))) || !Boolean.TRUE.equals(data.get("is_valid"))) {
                throw new UnauthorizedException("Invalid Facebook sign-in token");
            }
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Facebook token verification failed: {}", e.getMessage());
            throw new UnauthorizedException("Could not verify Facebook sign-in. Please try again.");
        }

        // Step 2: fetch the actual profile now that the token is confirmed valid.
        String profileUrl = UriComponentsBuilder.fromHttpUrl("https://graph.facebook.com/me")
                .queryParam("fields", "id,name,email")
                .queryParam("access_token", accessToken)
                .toUriString();

        @SuppressWarnings("unchecked")
        Map<String, Object> profile = restTemplate.getForObject(profileUrl, Map.class);
        if (profile == null || profile.get("id") == null) {
            throw new UnauthorizedException("Could not fetch Facebook profile");
        }

        FacebookProfile result = new FacebookProfile();
        result.id = String.valueOf(profile.get("id"));
        result.email = (String) profile.get("email");
        result.name = (String) profile.get("name");
        return result;
    }

    public boolean isUsingDummyCredentials() {
        return appId == null || appId.startsWith("DUMMY") || appSecret == null || appSecret.startsWith("dummy");
    }
}
