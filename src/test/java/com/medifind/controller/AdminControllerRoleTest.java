package com.medifind.controller;

import com.medifind.config.SecurityConfig;
import com.medifind.dto.PageResponse;
import com.medifind.dto.user.UserProfileDto;
import com.medifind.security.JwtProvider;
import com.medifind.security.UserDetailsServiceImpl;
import com.medifind.service.AdminService;
import com.medifind.service.InventoryService;
import com.medifind.service.MedicineService;
import com.medifind.service.PharmacyService;
import com.medifind.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class AdminControllerRoleTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private JwtProvider jwtProvider;
    @MockBean private UserDetailsServiceImpl userDetailsService;
    @MockBean private AdminService adminService;
    @MockBean private UserService userService;
    @MockBean private PharmacyService pharmacyService;
    @MockBean private MedicineService medicineService;
    @MockBean private InventoryService inventoryService;

    @Test
    @WithMockUser(roles = "USER")
    void regularUserCannotReadAdminUsers() throws Exception {
        mockMvc.perform(get("/admin/users")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanReadAdminUsers() throws Exception {
        when(userService.getAllUsers(anyInt(), anyInt())).thenReturn(
                PageResponse.<UserProfileDto>builder().content(List.of()).build());

        mockMvc.perform(get("/admin/users")).andExpect(status().isOk());
    }
}
