package com.dreamnest.service.impl;

import com.dreamnest.dto.request.ChangePasswordRequest;
import com.dreamnest.dto.request.UpdateProfileRequest;
import com.dreamnest.dto.response.UserResponse;
import com.dreamnest.entity.User;
import com.dreamnest.exception.BadRequestException;
import com.dreamnest.exception.ResourceNotFoundException;
import com.dreamnest.mapper.UserMapper;
import com.dreamnest.repository.UserRepository;
import com.dreamnest.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link UserService}.
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserResponse getCurrentUser(Long userId) {
        User user = findUser(userId);
        return UserMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUser(userId);
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        if (request.getMobileNumber() != null) {
            user.setMobileNumber(request.getMobileNumber());
        }
        user = userRepository.save(user);
        return UserMapper.toResponse(user);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUser(userId);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }
}
