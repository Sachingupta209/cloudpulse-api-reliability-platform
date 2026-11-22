package com.cloudpulse.notification;

import com.cloudpulse.common.PagedResponse;
import com.cloudpulse.exception.GlobalExceptionHandler;
import com.cloudpulse.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        UserPrincipal principal = new UserPrincipal(
                101L,
                "dev@cloudpulse.io",
                "hashedPass",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/notifications/preferences should return preferences with HTTP 200")
    void shouldReturnNotificationPreferences() throws Exception {
        NotificationPreferenceDto prefDto = new NotificationPreferenceDto(
                1L, 101L, true, true, true, true, true, "dev@cloudpulse.io"
        );
        when(notificationService.getOrCreatePreferences(101L)).thenReturn(prefDto);

        mockMvc.perform(get("/api/notifications/preferences")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.emailEnabled").value(true))
                .andExpect(jsonPath("$.data.notificationEmail").value("dev@cloudpulse.io"));
    }

    @Test
    @DisplayName("PUT /api/notifications/preferences should update and return preferences with HTTP 200")
    void shouldUpdateNotificationPreferences() throws Exception {
        NotificationPreferenceUpdateRequest request = new NotificationPreferenceUpdateRequest(
                true, true, false, true, false, "ops@cloudpulse.io"
        );
        NotificationPreferenceDto updatedDto = new NotificationPreferenceDto(
                1L, 101L, true, true, false, true, false, "ops@cloudpulse.io"
        );
        when(notificationService.updatePreferences(eq(101L), any(NotificationPreferenceUpdateRequest.class)))
                .thenReturn(updatedDto);

        mockMvc.perform(put("/api/notifications/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.notificationEmail").value("ops@cloudpulse.io"))
                .andExpect(jsonPath("$.data.notifyOnIncidentResolved").value(false));
    }

    @Test
    @DisplayName("GET /api/notifications/history should return paginated history with HTTP 200")
    void shouldReturnNotificationHistory() throws Exception {
        NotificationHistoryDto item = new NotificationHistoryDto(
                55L, 10L, "Auth Service", "https://auth.example.com", 2L,
                "Downtime detected for 'Auth Service'", NotificationType.INCIDENT_OPENED,
                "dev@cloudpulse.io", "[CloudPulse Incident] Downtime detected for 'Auth Service'",
                NotificationStatus.SENT, null, 1, Instant.now(), Instant.now()
        );
        PagedResponse<NotificationHistoryDto> pagedResponse = PagedResponse.of(
                new org.springframework.data.domain.PageImpl<>(List.of(item), PageRequest.of(0, 20), 1)
        );
        when(notificationService.getNotificationHistory(eq(101L), any())).thenReturn(pagedResponse);

        mockMvc.perform(get("/api/notifications/history")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].monitorName").value("Auth Service"))
                .andExpect(jsonPath("$.data.content[0].notificationType").value("INCIDENT_OPENED"))
                .andExpect(jsonPath("$.data.content[0].status").value("SENT"));
    }

    @Test
    @DisplayName("POST /api/notifications/test should dispatch test notification and return HTTP 200")
    void shouldDispatchTestNotification() throws Exception {
        NotificationHistoryDto testResult = new NotificationHistoryDto(
                99L, null, null, null, null, null,
                NotificationType.TEST, "dev@cloudpulse.io",
                "[CloudPulse] Test Alert Notification", NotificationStatus.SENT, null, 1, Instant.now(), Instant.now()
        );
        when(notificationService.sendTestNotification(101L)).thenReturn(testResult);

        mockMvc.perform(post("/api/notifications/test")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.notificationType").value("TEST"))
                .andExpect(jsonPath("$.data.status").value("SENT"));
    }
}
