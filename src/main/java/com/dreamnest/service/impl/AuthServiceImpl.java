package com.dreamnest.service.impl;

import com.dreamnest.dto.request.LoginRequest;
import com.dreamnest.dto.request.RefreshTokenRequest;
import com.dreamnest.dto.request.RegisterBusinessRequest;
import com.dreamnest.dto.request.RegisterCustomerRequest;
import com.dreamnest.dto.request.ResendOtpRequest;
import com.dreamnest.dto.request.SocialLoginRequest;
import com.dreamnest.dto.request.VerifyOtpRequest;
import com.dreamnest.service.FacebookAuthService;
import com.dreamnest.service.JwksTokenVerifier;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import com.dreamnest.dto.response.AuthResponse;
import com.dreamnest.entity.BusinessProfile;
import com.dreamnest.entity.Cart;
import com.dreamnest.entity.Role;
import com.dreamnest.entity.User;
import com.dreamnest.enums.RoleName;
import com.dreamnest.exception.BadRequestException;
import com.dreamnest.exception.DuplicateResourceException;
import com.dreamnest.exception.ResourceNotFoundException;
import com.dreamnest.exception.UnauthorizedException;
import com.dreamnest.mapper.UserMapper;
import com.dreamnest.repository.BusinessProfileRepository;
import com.dreamnest.repository.CartRepository;
import com.dreamnest.repository.RoleRepository;
import com.dreamnest.repository.UserRepository;
import com.dreamnest.security.JwtUtil;
import com.dreamnest.security.UserPrincipal;
import com.dreamnest.service.AuthService;
import com.dreamnest.service.OtpService;
import com.dreamnest.enums.VerificationChannel;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link AuthService} handling registration, login and token refresh.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final OtpService otpService;
    private final JwksTokenVerifier jwksTokenVerifier;
    private final FacebookAuthService facebookAuthService;

    @Value("${dreamnest.oauth.google.client-id}")
    private String googleClientId;

    @Value("${dreamnest.oauth.apple.client-id}")
    private String appleClientId;

    public AuthServiceImpl(UserRepository userRepository,
                            RoleRepository roleRepository,
                            BusinessProfileRepository businessProfileRepository,
                            CartRepository cartRepository,
                            PasswordEncoder passwordEncoder,
                            AuthenticationManager authenticationManager,
                            JwtUtil jwtUtil,
                            OtpService otpService,
                            JwksTokenVerifier jwksTokenVerifier,
                            FacebookAuthService facebookAuthService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.businessProfileRepository = businessProfileRepository;
        this.cartRepository = cartRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.otpService = otpService;
        this.jwksTokenVerifier = jwksTokenVerifier;
        this.facebookAuthService = facebookAuthService;
    }

    @Override
    @Transactional
    public AuthResponse registerCustomer(RegisterCustomerRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Password and confirm password do not match");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("An account with this email already exists");
        }

        Role customerRole = roleRepository.findByName(RoleName.CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", RoleName.CUSTOMER));

        User user = new User(
                request.getFirstName(),
                request.getLastName(),
                request.getEmail(),
                request.getMobileNumber(),
                passwordEncoder.encode(request.getPassword()),
                customerRole
        );
        user = userRepository.save(user);

        // Create an empty cart for the new customer
        cartRepository.save(new Cart(user));

        // Best-effort: send email + SMS verification codes. Registration
        // still succeeds even if delivery fails (e.g. dummy mail/SMS
        // credentials in dev) - see EmailService / ConsoleSmsServiceImpl.
        otpService.sendEmailVerification(user);
        otpService.sendMobileVerification(user);

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse registerBusiness(RegisterBusinessRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("An account with this email already exists");
        }

        Role businessRole = roleRepository.findByName(RoleName.BUSINESS_ADMIN)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", RoleName.BUSINESS_ADMIN));

        User user = new User(
                request.getOwnerName(),
                "",
                request.getEmail(),
                request.getMobile(),
                passwordEncoder.encode(request.getPassword()),
                businessRole
        );
        user = userRepository.save(user);

        BusinessProfile profile = new BusinessProfile(user, request.getBusinessName(), request.getOwnerName(), request.getGstNumber());
        businessProfileRepository.save(profile);

        otpService.sendEmailVerification(user);
        otpService.sendMobileVerification(user);

        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userPrincipal.getId()));

        if (!user.isEnabled()) {
            throw new UnauthorizedException("Your account has been disabled. Please contact support.");
        }

        String accessToken = jwtUtil.generateAccessToken(userPrincipal);
        String refreshToken = jwtUtil.generateRefreshToken(userPrincipal);

        return new AuthResponse(accessToken, refreshToken, UserMapper.toResponse(user));
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String token = request.getRefreshToken();

        if (!jwtUtil.validateToken(token) || !"REFRESH".equals(jwtUtil.getTokenType(token))) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        String email = jwtUtil.getEmailFromToken(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        UserPrincipal userPrincipal = UserPrincipal.create(user);
        String newAccessToken = jwtUtil.generateAccessToken(userPrincipal);
        String newRefreshToken = jwtUtil.generateRefreshToken(userPrincipal);

        return new AuthResponse(newAccessToken, newRefreshToken, UserMapper.toResponse(user));
    }

    @Override
    @Transactional
    public void verifyEmail(VerifyOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));
        if (user.isEmailVerified()) {
            return;
        }
        otpService.verify(user, VerificationChannel.EMAIL, request.getCode());
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void verifyMobile(VerifyOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));
        if (user.isMobileVerified()) {
            return;
        }
        otpService.verify(user, VerificationChannel.MOBILE, request.getCode());
        user.setMobileVerified(true);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void resendOtp(ResendOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        if ("EMAIL".equals(request.getChannel())) {
            if (user.isEmailVerified()) {
                throw new BadRequestException("Email is already verified");
            }
            otpService.sendEmailVerification(user);
        } else {
            if (user.isMobileVerified()) {
                throw new BadRequestException("Mobile number is already verified");
            }
            if (user.getMobileNumber() == null || user.getMobileNumber().isBlank()) {
                throw new BadRequestException("No mobile number is associated with this account");
            }
            otpService.sendMobileVerification(user);
        }
    }

    @Override
    @Transactional
    public AuthResponse socialLogin(SocialLoginRequest request) {
        SocialProfile profile = switch (request.getProvider()) {
            case "GOOGLE" -> verifyGoogle(request.getToken());
            case "FACEBOOK" -> verifyFacebook(request.getToken());
            case "APPLE" -> verifyApple(request.getToken(), request.getFirstName(), request.getLastName());
            default -> throw new BadRequestException("Unsupported sign-in provider");
        };

        if (profile.email == null || profile.email.isBlank()) {
            throw new BadRequestException(
                    "Your " + request.getProvider() + " account did not share an email address. " +
                            "Please use email/password sign-up instead, or allow email sharing and try again.");
        }

        User user = userRepository.findByEmail(profile.email).orElse(null);

        if (user == null) {
            Role customerRole = roleRepository.findByName(RoleName.CUSTOMER)
                    .orElseThrow(() -> new ResourceNotFoundException("Role", "name", RoleName.CUSTOMER));

            user = new User(
                    profile.firstName != null ? profile.firstName : "SK",
                    profile.lastName != null ? profile.lastName : "Sports User",
                    profile.email,
                    null,
                    // Social accounts never log in with a password - this random hash
                    // is unreachable, it just satisfies the non-null DB constraint and
                    // lets the normal password-login path fail safely instead of crashing.
                    passwordEncoder.encode(java.util.UUID.randomUUID().toString()),
                    customerRole
            );
            user.setAuthProvider(request.getProvider());
            user.setProviderId(profile.providerId);
            // The provider already verified this email address as part of its own
            // sign-in flow, so we can trust it without sending our own OTP.
            user.setEmailVerified(true);
            user = userRepository.save(user);
            cartRepository.save(new Cart(user));
        } else if (!user.isEnabled()) {
            throw new UnauthorizedException("Your account has been disabled. Please contact support.");
        }

        return buildAuthResponse(user);
    }

    private SocialProfile verifyGoogle(String idToken) {
        if (googleClientId == null || googleClientId.startsWith("DUMMY")) {
            throw new UnauthorizedException(
                    "Google Sign-In is not configured yet. Set GOOGLE_CLIENT_ID to enable it.");
        }
        Claims claims = jwksTokenVerifier.verify(idToken, "https://www.googleapis.com/oauth2/v3/certs",
                "https://accounts.google.com", googleClientId);

        SocialProfile profile = new SocialProfile();
        profile.providerId = claims.getSubject();
        profile.email = claims.get("email", String.class);
        profile.firstName = claims.get("given_name", String.class);
        profile.lastName = claims.get("family_name", String.class);
        return profile;
    }

    private SocialProfile verifyApple(String identityToken, String firstName, String lastName) {
        if (appleClientId == null || appleClientId.contains("dummy")) {
            throw new UnauthorizedException(
                    "Apple Sign-In is not configured yet. Set APPLE_CLIENT_ID to enable it.");
        }
        Claims claims = jwksTokenVerifier.verify(identityToken, "https://appleid.apple.com/auth/keys",
                "https://appleid.apple.com", appleClientId);

        SocialProfile profile = new SocialProfile();
        profile.providerId = claims.getSubject();
        profile.email = claims.get("email", String.class);
        // Apple only ever sends the user's name once, in the client-side
        // authorization payload on first sign-up - not inside the token itself.
        profile.firstName = firstName;
        profile.lastName = lastName;
        return profile;
    }

    private SocialProfile verifyFacebook(String accessToken) {
        FacebookAuthService.FacebookProfile fb = facebookAuthService.verifyAndFetchProfile(accessToken);
        SocialProfile profile = new SocialProfile();
        profile.providerId = fb.id;
        profile.email = fb.email;
        if (fb.name != null) {
            String[] parts = fb.name.trim().split("\\s+", 2);
            profile.firstName = parts[0];
            profile.lastName = parts.length > 1 ? parts[1] : null;
        }
        return profile;
    }

    private static class SocialProfile {
        String providerId;
        String email;
        String firstName;
        String lastName;
    }

    private AuthResponse buildAuthResponse(User user) {
        UserPrincipal userPrincipal = UserPrincipal.create(user);
        String accessToken = jwtUtil.generateAccessToken(userPrincipal);
        String refreshToken = jwtUtil.generateRefreshToken(userPrincipal);
        return new AuthResponse(accessToken, refreshToken, UserMapper.toResponse(user));
    }
}
