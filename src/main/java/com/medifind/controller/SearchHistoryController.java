package com.medifind.controller;

import com.medifind.dto.ApiResponse;
import com.medifind.entity.SearchHistory;
import com.medifind.entity.User;
import com.medifind.repository.SearchHistoryRepository;
import com.medifind.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/search-history")
@RequiredArgsConstructor
public class SearchHistoryController {

    private final SearchHistoryRepository searchHistoryRepository;
    private final UserRepository userRepository;

    private Long getUserId(UserDetails userDetails) {
        if (userDetails == null) return 1L;
        return userRepository.findByEmail(userDetails.getUsername())
                .map(User::getId)
                .orElse(1L);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SearchHistory>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        List<SearchHistory> history = searchHistoryRepository.findByUserIdOrderBySearchedAtDesc(userId, PageRequest.of(page, size)).getContent();
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<ApiResponse<String>> save(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        User user = userRepository.findById(userId).orElseThrow();
        String query = body.getOrDefault("query", "").toString();
        if (!query.isBlank()) {
            searchHistoryRepository.save(SearchHistory.builder()
                    .user(user)
                    .searchQuery(query)
                    .build());
        }
        return ResponseEntity.ok(ApiResponse.success("Search saved", "SUCCESS"));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<String>> deleteOne(@PathVariable Long id) {
        searchHistoryRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Deleted", "SUCCESS"));
    }

    @DeleteMapping
    @Transactional
    public ResponseEntity<ApiResponse<String>> clearAll(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        searchHistoryRepository.deleteByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("Search history cleared", "SUCCESS"));
    }
}
