package com.medifind.controller;

import com.medifind.dto.ApiResponse;
import com.medifind.dto.notification.NotificationDto;
import com.medifind.entity.Notification;
import com.medifind.entity.User;
import com.medifind.exception.ResourceNotFoundException;
import com.medifind.repository.NotificationRepository;
import com.medifind.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    private Long getUserId(UserDetails userDetails) {
        if (userDetails == null) throw new AccessDeniedException("Sign in to view notifications.");
        return userRepository.findByEmail(userDetails.getUsername())
                .map(User::getId)
            .orElseThrow(() -> new AccessDeniedException("Authenticated user could not be found."));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationDto>>> getAllNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        Page<Notification> notifs = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(notifs.getContent().stream().map(this::toDto).toList()));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        long count = notificationRepository.countByUserIdAndReadFalse(userId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("count", count, "unreadCount", count)));
    }

    @PatchMapping("/{id}/read")
    @Transactional
    public ResponseEntity<ApiResponse<String>> markAsRead(
            @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        Notification notification = notificationRepository.findByIdAndUserId(id, getUserId(userDetails))
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        notification.setRead(true);
        notificationRepository.save(notification);
        return ResponseEntity.ok(ApiResponse.success("Marked as read", "SUCCESS"));
    }

    @PatchMapping("/read-all")
    @Transactional
    public ResponseEntity<ApiResponse<String>> markAllAsRead(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        notificationRepository.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success("All marked as read", "SUCCESS"));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<String>> deleteNotification(
            @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        if (notificationRepository.findByIdAndUserId(id, userId).isEmpty()) {
            throw new ResourceNotFoundException("Notification", id);
        }
        notificationRepository.deleteByIdAndUserId(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Notification deleted", "SUCCESS"));
    }

    @DeleteMapping
    @Transactional
    public ResponseEntity<ApiResponse<String>> deleteAllNotifications(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        notificationRepository.deleteByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("All notifications deleted", "SUCCESS"));
    }

    private NotificationDto toDto(Notification notification) {
        return NotificationDto.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .read(notification.getRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
