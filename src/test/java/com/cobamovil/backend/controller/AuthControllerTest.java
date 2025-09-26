package com.cobamovil.backend.controller;

import com.cobamovil.backend.dto.RegisterRequestDTO;
import com.cobamovil.backend.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cobamovil.backend.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.security.test.context.support.WithMockUser;

class AuthControllerTest {
    @Mock
    private com.cobamovil.backend.security.JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthService authService;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(authController).build();
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @WithMockUser
    void testRegisterUser() throws Exception {
        RegisterRequestDTO registerRequest = new RegisterRequestDTO(
            "testuser",
            "test@example.com",
            "TestPass123!"
        );

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());
    }
}
