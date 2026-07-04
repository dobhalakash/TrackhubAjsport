package com.dreamnest.service;

import com.dreamnest.exception.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Verifies RS256-signed identity tokens (ID tokens) issued by Google or
 * Apple Sign-In, by fetching the provider's public JSON Web Key Set (JWKS)
 * and checking the token's signature, issuer, audience, and expiry.
 *
 * <p>This is the same verification approach both providers' own server-side
 * SDKs perform - we just do it directly with the JJWT library already in
 * this project, rather than pulling in Google's/Apple's full client
 * libraries for a single check.</p>
 */
@Service
public class JwksTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(JwksTokenVerifier.class);

    private final RestTemplate restTemplate = new RestTemplate();

    // Simple in-memory cache of fetched JWKS keyed by the provider's JWKS URL,
    // since these rotate infrequently and re-fetching on every login would be wasteful.
    private final Map<String, Map<String, PublicKey>> jwksCache = new ConcurrentHashMap<>();

    /**
     * Verifies a JWT against the given JWKS endpoint and expected issuer/audience.
     *
     * @param token       the raw JWT (e.g. Google's id_token or Apple's identityToken)
     * @param jwksUrl     the provider's JWKS endpoint
     * @param expectedIss the expected "iss" claim
     * @param expectedAud the expected "aud" claim (your OAuth client id)
     * @return the verified claims
     * @throws UnauthorizedException if the token is invalid, expired, or doesn't match
     */
    @SuppressWarnings("unchecked")
    public Claims verify(String token, String jwksUrl, String expectedIss, String expectedAud) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKeyResolver(new SigningKeyResolverAdapter() {
                        @Override
                        public java.security.Key resolveSigningKey(JwsHeader header, Claims claims) {
                            return resolveKey(jwksUrl, header.getKeyId());
                        }
                    })
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            if (!expectedIss.equals(claims.getIssuer())) {
                throw new UnauthorizedException("Token issuer does not match");
            }
            Object aud = claims.get("aud");
            boolean audMatches = expectedAud.equals(aud)
                    || (aud instanceof List<?> auds && ((List<Object>) auds).contains(expectedAud));
            if (!audMatches) {
                throw new UnauthorizedException("Token was not issued for this app");
            }
            return claims;
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Social sign-in token verification failed: {}", e.getMessage());
            throw new UnauthorizedException("Could not verify sign-in token. Please try again.");
        }
    }

    private PublicKey resolveKey(String jwksUrl, String kid) {
        Map<String, PublicKey> keys = jwksCache.computeIfAbsent(jwksUrl, this::fetchJwks);
        PublicKey key = keys.get(kid);
        if (key == null) {
            // Key rotated since we last cached - refresh once and retry.
            keys = fetchJwks(jwksUrl);
            jwksCache.put(jwksUrl, keys);
            key = keys.get(kid);
        }
        if (key == null) {
            throw new UnauthorizedException("Unrecognized signing key");
        }
        return key;
    }

    @SuppressWarnings("unchecked")
    private Map<String, PublicKey> fetchJwks(String jwksUrl) {
        Map<String, Object> response = restTemplate.getForObject(jwksUrl, Map.class);
        Map<String, PublicKey> result = new ConcurrentHashMap<>();
        if (response == null || !(response.get("keys") instanceof List<?> keys)) {
            return result;
        }
        for (Object k : keys) {
            Map<String, Object> jwk = (Map<String, Object>) k;
            if (!"RSA".equals(jwk.get("kty"))) {
                continue;
            }
            String kid = (String) jwk.get("kid");
            String n = (String) jwk.get("n");
            String e = (String) jwk.get("e");
            try {
                BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(n));
                BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(e));
                PublicKey publicKey = KeyFactory.getInstance("RSA")
                        .generatePublic(new RSAPublicKeySpec(modulus, exponent));
                result.put(kid, publicKey);
            } catch (Exception ex) {
                log.warn("Could not parse JWK {}: {}", kid, ex.getMessage());
            }
        }
        return result;
    }
}
