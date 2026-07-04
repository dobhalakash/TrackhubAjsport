package com.dreamnest.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Best-effort, IP-based geolocation for "where was this order placed from"
 * shown to admins. This is NOT GPS/precise location - it never asks the
 * customer for permission and never tracks them in real time; it's a single
 * lookup of the city/region implied by their IP address at checkout, the
 * same level of detail almost every e-commerce fraud/analytics dashboard
 * shows.
 *
 * <p>Uses ip-api.com's free tier (no API key, ~45 requests/minute limit) -
 * fine for moderate order volume. For higher volume or higher accuracy,
 * swap in a paid provider (ipapi.co, MaxMind, ipinfo.io) by replacing the
 * URL/response parsing below; the calling code in OrderServiceImpl doesn't
 * need to change.</p>
 */
@Service
public class IpGeolocationService {

    private static final Logger log = LoggerFactory.getLogger(IpGeolocationService.class);
    private final RestTemplate restTemplate = new RestTemplate();

    public static class Location {
        public String city;
        public String region;
        public String country;
    }

    /** Looks up city/region/country for an IP. Returns null (never throws) if the IP is local/private or the lookup fails. */
    public Location lookup(String ip) {
        if (ip == null || ip.isBlank() || isPrivateOrLocal(ip)) {
            return null;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(
                    "http://ip-api.com/json/" + ip + "?fields=status,country,regionName,city", Map.class);

            if (response == null || !"success".equals(response.get("status"))) {
                return null;
            }
            Location location = new Location();
            location.city = (String) response.get("city");
            location.region = (String) response.get("regionName");
            location.country = (String) response.get("country");
            return location;
        } catch (Exception e) {
            log.warn("IP geolocation lookup failed for {}: {}", ip, e.getMessage());
            return null;
        }
    }

    private boolean isPrivateOrLocal(String ip) {
        return ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1") || ip.equals("::1")
                || ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.16.")
                || ip.startsWith("172.17.") || ip.startsWith("172.18.") || ip.startsWith("172.19.")
                || ip.startsWith("172.2") || ip.startsWith("172.3");
    }
}
