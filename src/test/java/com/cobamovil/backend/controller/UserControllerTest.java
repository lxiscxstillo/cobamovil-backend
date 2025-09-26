package com.cobamovil.backend.controller;

// ...existing code...
import com.cobamovil.backend.service.UserService;
// ...existing code...
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.security.test.context.support.WithMockUser;

class UserControllerTest {
    @Mock
    private com.cobamovil.backend.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    // Eliminado campo duplicado

    @Mock
    private UserService userService;

    @Mock
    private com.cobamovil.backend.security.JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(userController).build();
    }

    @Test
    @WithMockUser
    void testGetAllUsers() throws Exception {
        mockMvc.perform(get("/api/users")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
