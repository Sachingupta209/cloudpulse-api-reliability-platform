package com.cloudpulse.notification;

import com.cloudpulse.common.PagedResponse;
import com.cloudpulse.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotificationTenantSecurityTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateUser(Long userId, String email) {
        UserPrincipal principal = new UserPrincipal(
                userId,
                email,
                "hashedPass",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @Test
    @DisplayName("Tenant Isolation: Preferences requests must only fetch data for the authenticated principal")
    void shouldScopePreferencesToAuthenticatedUser() throws Exception {
        authenticateUser(101L, "user101@cloudpulse.io");
        when(notificationService.getOrCreatePreferences(101L)).thenReturn(
                new NotificationPreferenceDto(1L, 101L, true, true, true, true, true, "user101@cloudpulse.io")
        );

        mockMvc.perform(get("/api/notifications/preferences")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(notificationService).getOrCreatePreferences(eq(101L));
    }

    @Test
    @DisplayName("Tenant Isolation: History requests must only query history belonging to authenticated user")
    void shouldScopeHistoryToAuthenticatedUser() throws Exception {
        authenticateUser(202L, "user202@cloudpulse.io");
        when(notificationService.getNotificationHistory(eq(202L), any())).thenReturn(
                PagedResponse.of(new org.springframework.data.domain.PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0))
        );

        mockMvc.perform(get("/api/notifications/history")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(notificationService).getNotificationHistory(eq(202L), any());
    }
}
