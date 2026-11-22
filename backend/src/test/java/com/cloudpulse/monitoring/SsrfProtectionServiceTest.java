package com.cloudpulse.monitoring;

import com.cloudpulse.exception.SsrfViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetAddress;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SsrfProtectionServiceTest {

    private SsrfProtectionService ssrfService;

    @BeforeEach
    void setUp() {
        ssrfService = new SsrfProtectionService(true, false);
    }

    @Test
    @DisplayName("Should accept valid public HTTPS URL")
    void shouldAcceptValidPublicUrl() {
        URI uri = ssrfService.validateUri("https://example.com/api/health");
        assertThat(uri).isNotNull();
        assertThat(uri.getHost()).isEqualTo("example.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ftp://example.com",
            "file:///etc/passwd",
            "gopher://example.com",
            "dict://example.com"
    })
    @DisplayName("Should reject unsupported protocols")
    void shouldRejectUnsupportedProtocols(String url) {
        assertThatThrownBy(() -> ssrfService.validateUri(url))
                .isInstanceOf(SsrfViolationException.class)
                .hasMessageContaining("Forbidden URL scheme");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://admin:secret@example.com/api",
            "https://user:pass@example.com"
    })
    @DisplayName("Should reject URLs containing embedded credentials")
    void shouldRejectEmbeddedCredentials(String url) {
        assertThatThrownBy(() -> ssrfService.validateUri(url))
                .isInstanceOf(SsrfViolationException.class)
                .hasMessageContaining("user authentication credentials");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://example.com:22",
            "http://example.com:25",
            "http://example.com:3306",
            "http://example.com:5432",
            "http://example.com:6379"
    })
    @DisplayName("Should reject connections to non-whitelisted ports")
    void shouldRejectDisallowedPorts(String url) {
        assertThatThrownBy(() -> ssrfService.validateUri(url))
                .isInstanceOf(SsrfViolationException.class)
                .hasMessageContaining("port");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://localhost:8080/api",
            "http://127.0.0.1:8080/metrics",
            "http://169.254.169.254/latest/meta-data/",
            "http://metadata.google.internal/computeMetadata/v1/"
    })
    @DisplayName("Should block loopback and cloud metadata hostnames")
    void shouldBlockLoopbackAndCloudMetadata(String url) {
        assertThatThrownBy(() -> ssrfService.validateUri(url))
                .isInstanceOf(SsrfViolationException.class);
    }

    @Test
    @DisplayName("Should validate destination IP addresses against RFC 1918 private ranges")
    void shouldBlockPrivateIpsDirectly() throws Exception {
        // 10.0.0.1
        InetAddress ip10 = InetAddress.getByName("10.0.0.1");
        assertThatThrownBy(() -> ssrfService.validateAddress(ip10))
                .isInstanceOf(SsrfViolationException.class)
                .hasMessageContaining("private RFC1918");

        // 192.168.1.1
        InetAddress ip192 = InetAddress.getByName("192.168.1.1");
        assertThatThrownBy(() -> ssrfService.validateAddress(ip192))
                .isInstanceOf(SsrfViolationException.class)
                .hasMessageContaining("private RFC1918");

        // 172.16.0.1
        InetAddress ip172 = InetAddress.getByName("172.16.0.1");
        assertThatThrownBy(() -> ssrfService.validateAddress(ip172))
                .isInstanceOf(SsrfViolationException.class)
                .hasMessageContaining("private RFC1918");

        // 127.0.0.1
        InetAddress loopback = InetAddress.getByName("127.0.0.1");
        assertThatThrownBy(() -> ssrfService.validateAddress(loopback))
                .isInstanceOf(SsrfViolationException.class)
                .hasMessageContaining("loopback");

        // AWS/GCP Metadata 169.254.169.254
        InetAddress linkLocal = InetAddress.getByName("169.254.169.254");
        assertThatThrownBy(() -> ssrfService.validateAddress(linkLocal))
                .isInstanceOf(SsrfViolationException.class);
    }
}
