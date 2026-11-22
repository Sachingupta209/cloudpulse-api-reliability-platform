package com.cloudpulse.statuspage;

import com.cloudpulse.exception.BadRequestException;
import com.cloudpulse.exception.ResourceNotFoundException;
import com.cloudpulse.monitor.Monitor;
import com.cloudpulse.monitor.MonitorRepository;
import com.cloudpulse.security.UserPrincipal;
import com.cloudpulse.user.Role;
import com.cloudpulse.user.User;
import com.cloudpulse.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatusPageSecurityTest {

    @Mock
    private StatusPageRepository statusPageRepository;
    @Mock
    private MonitorRepository monitorRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StatusPageService statusPageService;

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        UserPrincipal principal = new UserPrincipal(101L, "userA@cloudpulse.io", "hashA", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        userA = new User("userA@cloudpulse.io", "hashA", "User", "A", Role.ROLE_USER);
        ReflectionTestUtils.setField(userA, "id", 101L);

        userB = new User("userB@cloudpulse.io", "hashB", "User", "B", Role.ROLE_USER);
        ReflectionTestUtils.setField(userB, "id", 202L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testTenantIsolation_CannotReadOtherUserStatusPage() {
        // user A tries to access status page belonging to user B
        when(statusPageRepository.findByIdAndUserId(999L, 101L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> statusPageService.getStatusPageById(999L));
    }

    @Test
    void testTenantIsolation_CannotUpdateOtherUserStatusPage() {
        when(statusPageRepository.findByIdAndUserId(999L, 101L)).thenReturn(Optional.empty());

        StatusPageRequest req = new StatusPageRequest("Hacked", null, null, true, null);
        assertThrows(ResourceNotFoundException.class, () -> statusPageService.updateStatusPage(999L, req));
    }

    @Test
    void testTenantIsolation_CannotDeleteOtherUserStatusPage() {
        when(statusPageRepository.findByIdAndUserId(999L, 101L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> statusPageService.deleteStatusPage(999L));
    }

    @Test
    void testOwnership_CannotPublishOtherUserMonitor() {
        when(userRepository.findById(101L)).thenReturn(Optional.of(userA));

        Monitor foreignMonitor = new Monitor(userB, "Secret Core API", "https://internal-bank.corp/api", null, 60, 5000, 200);
        ReflectionTestUtils.setField(foreignMonitor, "id", 555L);

        when(monitorRepository.findAllById(List.of(555L))).thenReturn(List.of(foreignMonitor));

        StatusPageRequest req = new StatusPageRequest("Leaked Status", null, null, true, List.of(555L));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> statusPageService.createStatusPage(req));
        assertTrue(ex.getMessage().contains("do not belong to the authenticated user"));
    }

    @Test
    void testDataLeakPrevention_PublicDtoHasZeroSensitiveFields() {
        // Assert that PublicMonitorDto does NOT have URL, headers, or request body fields
        PublicMonitorDto dto = new PublicMonitorDto("Safe Display Name", "OPERATIONAL", 99.9, 99.8, 99.7, 120.0);
        assertNotNull(dto.getName());
        assertEquals("Safe Display Name", dto.getName());

        // Reflection verify: no getUrl, getHeaders, getRequestBody, getExpectedStatusCode methods exist on PublicMonitorDto
        assertThrows(NoSuchMethodException.class, () -> PublicMonitorDto.class.getMethod("getUrl"));
        assertThrows(NoSuchMethodException.class, () -> PublicMonitorDto.class.getMethod("getHeadersJson"));
        assertThrows(NoSuchMethodException.class, () -> PublicMonitorDto.class.getMethod("getRequestBody"));
        assertThrows(NoSuchMethodException.class, () -> PublicMonitorDto.class.getMethod("getValidationRules"));
        assertThrows(NoSuchMethodException.class, () -> PublicMonitorDto.class.getMethod("getId"));
    }
}
