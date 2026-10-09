package com.medifind.service;

import com.medifind.dto.PageResponse;
import com.medifind.dto.user.UpdateProfileRequest;
import com.medifind.dto.user.UserProfileDto;
import com.medifind.entity.User;
import com.medifind.enums.Role;
import com.medifind.exception.ResourceNotFoundException;
import com.medifind.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserProfileDto getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        return toDto(user);
    }

    @Transactional(readOnly = true)
    public UserProfileDto getById(Long id) {
        return toDto(userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id)));
    }

    @Transactional
    public UserProfileDto updateProfile(String email, UpdateProfileRequest req) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        if (req.getFirstName() != null) user.setFirstName(req.getFirstName());
        if (req.getLastName() != null) user.setLastName(req.getLastName());
        if (req.getPhone() != null) user.setPhone(req.getPhone());
        if (req.getCity() != null) user.setCity(req.getCity());
        if (req.getState() != null) user.setState(req.getState());
        if (req.getPincode() != null) user.setPincode(req.getPincode());
        if (req.getLatitude() != null) user.setLatitude(req.getLatitude());
        if (req.getLongitude() != null) user.setLongitude(req.getLongitude());
        if (req.getProfileImage() != null) user.setProfileImage(req.getProfileImage());
        return toDto(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public PageResponse<UserProfileDto> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> result = userRepository.findAll(pageable);
        return toPageResponse(result);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserProfileDto> getUsersByRole(Role role, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return toPageResponse(userRepository.findByRole(role, pageable));
    }

    @Transactional
    public void toggleUserEnabled(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        user.setEnabled(!user.getEnabled());
        userRepository.save(user);
    }
    
    @Transactional
    public void setUserEnabled(Long id, boolean enabled) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        user.setEnabled(enabled);
        userRepository.save(user);
    }

    @Transactional
    public UserProfileDto updateById(Long id, UpdateProfileRequest req) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        if (req.getFirstName() != null) user.setFirstName(req.getFirstName());
        if (req.getLastName() != null) user.setLastName(req.getLastName());
        if (req.getPhone() != null) user.setPhone(req.getPhone());
        if (req.getCity() != null) user.setCity(req.getCity());
        if (req.getState() != null) user.setState(req.getState());
        if (req.getPincode() != null) user.setPincode(req.getPincode());
        return toDto(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public static UserProfileDto toDto(User u) {
        return UserProfileDto.builder()
                .id(u.getId())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .email(u.getEmail())
                .phone(u.getPhone())
                .role(u.getRole())
                .city(u.getCity())
                .state(u.getState())
                .pincode(u.getPincode())
                .latitude(u.getLatitude())
                .longitude(u.getLongitude())
                .profileImage(u.getProfileImage())
                .enabled(u.getEnabled())
                .createdAt(u.getCreatedAt())
                .lastLoginAt(u.getLastLoginAt())
                .build();
    }

    private PageResponse<UserProfileDto> toPageResponse(Page<User> page) {
        return PageResponse.<UserProfileDto>builder()
                .content(page.getContent().stream().map(UserService::toDto).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
