package com.cloudpulse.statuspage;

import com.cloudpulse.exception.GlobalExceptionHandler;
import com.cloudpulse.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class StatusPageControllerTest {

    @Mock
    private StatusPageService statusPageService;

    @InjectMocks
    private StatusPageController statusPageController;

    @InjectMocks
    private PublicStatusController publicStatusController;

    private MockMvc mockMvc;
    private MockMvc publicMockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(statusPageController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        publicMockMvc = MockMvcBuilders.standaloneSetup(publicStatusController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        UserPrincipal principal = new UserPrincipal(
                100L,
                "user@cloudpulse.io",
                "hashed",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testCreateStatusPage_Success() throws Exception {
        StatusPageResponse res = new StatusPageResponse(1L, "Primary APIs", "Production APIs", "sp-prod", false,
                Collections.emptyList(), Instant.now(), Instant.now());
        when(statusPageService.createStatusPage(any(StatusPageRequest.class))).thenReturn(res);

        StatusPageRequest req = new StatusPageRequest("Primary APIs", "Production APIs", null, false, List.of(1L, 2L));

        mockMvc.perform(post("/api/status-pages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.slug").value("sp-prod"))
                .andExpect(jsonPath("$.data.isPublished").value(false));
    }

    @Test
    void testGetUserStatusPages_Success() throws Exception {
        StatusPageResponse res = new StatusPageResponse(1L, "Primary APIs", "Desc", "sp-prod", true,
                Collections.emptyList(), Instant.now(), Instant.now());
        when(statusPageService.getUserStatusPages()).thenReturn(List.of(res));

        mockMvc.perform(get("/api/status-pages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1L));
    }

    @Test
    void testUpdateStatusPage_Success() throws Exception {
        StatusPageResponse res = new StatusPageResponse(1L, "Updated APIs", "New Desc", "sp-prod", true,
                Collections.emptyList(), Instant.now(), Instant.now());
        when(statusPageService.updateStatusPage(eq(1L), any(StatusPageRequest.class))).thenReturn(res);

        StatusPageRequest req = new StatusPageRequest("Updated APIs", "New Desc", null, true, List.of(1L));

        mockMvc.perform(put("/api/status-pages/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated APIs"))
                .andExpect(jsonPath("$.data.isPublished").value(true));
    }

    @Test
    void testDeleteStatusPage_Success() throws Exception {
        mockMvc.perform(delete("/api/status-pages/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(statusPageService).deleteStatusPage(1L);
    }

    @Test
    void testGetPublicStatusPage_CacheControlAndNoAuthRequired() throws Exception {
        PublicStatusPageDto dto = new PublicStatusPageDto(
                "Acme Status", "All systems live", PublicSystemStatus.OPERATIONAL,
                Instant.now(), Collections.emptyList(), Collections.emptyList()
        );
        when(statusPageService.getPublicStatusPage("acme-status")).thenReturn(dto);

        publicMockMvc.perform(get("/api/public/status/acme-status"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, org.hamcrest.Matchers.containsString("must-revalidate")))
                .andExpect(header().string(HttpHeaders.PRAGMA, "no-cache"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Acme Status"))
                .andExpect(jsonPath("$.data.overallStatus").value("OPERATIONAL"));
    }
}
