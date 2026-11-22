package com.cloudpulse.monitoring;

import com.cloudpulse.exception.SsrfViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.*;
import java.util.Arrays;
import java.util.Set;

@Service
public class SsrfProtectionService {

    private static final Logger log = LoggerFactory.getLogger(SsrfProtectionService.class);

    private final boolean ssrfProtectionEnabled;
    private final boolean allowPrivateIps;

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");
    private static final Set<Integer> ALLOWED_PORTS = Set.of(80, 443, 8080, 8443, 3000, 5000, 8000, 8081, 9000);

    // Well-known cloud metadata hostnames and addresses
    private static final Set<String> BLOCKED_HOSTNAMES = Set.of(
            "metadata.google.internal",
            "metadata.google.internal.",
            "169.254.169.254",
            "instance-data",
            "localhost"
    );

    public SsrfProtectionService(
            @Value("${app.ssrf.enabled:true}") boolean ssrfProtectionEnabled,
            @Value("${app.ssrf.allow-private-ips:false}") boolean allowPrivateIps) {
        this.ssrfProtectionEnabled = ssrfProtectionEnabled;
        this.allowPrivateIps = allowPrivateIps;
    }

    /**
     * Validates a target URI for SSRF vulnerabilities before attempting connection.
     */
    public URI validateUri(String urlString) {
        if (urlString == null || urlString.isBlank()) {
            throw new SsrfViolationException("URL cannot be empty");
        }

        URI uri;
        try {
            uri = URI.create(urlString.trim());
        } catch (IllegalArgumentException ex) {
            throw new SsrfViolationException("Invalid URL syntax: " + ex.getMessage());
        }

        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
            throw new SsrfViolationException("Forbidden URL scheme: " + scheme + ". Only HTTP and HTTPS are permitted.");
        }

        if (uri.getUserInfo() != null) {
            throw new SsrfViolationException("URLs containing user authentication credentials are not permitted.");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new SsrfViolationException("URL host is missing or invalid");
        }

        int port = uri.getPort();
        if (port != -1 && !ALLOWED_PORTS.contains(port)) {
            throw new SsrfViolationException("Connection to port " + port + " is not permitted by security policy.");
        }

        if (!ssrfProtectionEnabled || allowPrivateIps) {
            return uri;
        }

        String lowerHost = host.toLowerCase();
        if (BLOCKED_HOSTNAMES.contains(lowerHost)) {
            throw new SsrfViolationException("Access to host '" + host + "' is blocked by SSRF policy.");
        }

        // Resolve DNS and validate every returned IP
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            if (addresses == null || addresses.length == 0) {
                throw new SsrfViolationException("Host could not be resolved: " + host);
            }

            for (InetAddress address : addresses) {
                validateAddress(address);
            }
        } catch (UnknownHostException ex) {
            throw new SsrfViolationException("Unknown or unresolvable host: " + host);
        }

        return uri;
    }

    /**
     * Validates an exact IP address at connection time to guard against DNS rebinding.
     */
    public void validateAddress(InetAddress address) {
        if (!ssrfProtectionEnabled || allowPrivateIps) {
            return;
        }

        // Check Loopback (127.0.0.0/8, ::1)
        if (address.isLoopbackAddress()) {
            throw new SsrfViolationException("Access to loopback IP address is forbidden: " + address.getHostAddress());
        }

        // Check Any Local (0.0.0.0, ::)
        if (address.isAnyLocalAddress()) {
            throw new SsrfViolationException("Access to wildcard/local IP address is forbidden: " + address.getHostAddress());
        }

        // Check Link Local (169.254.0.0/16, fe80::/10)
        if (address.isLinkLocalAddress()) {
            throw new SsrfViolationException("Access to link-local IP address is forbidden: " + address.getHostAddress());
        }

        // Check Site Local / RFC 1918 Private Ranges (10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16)
        if (address.isSiteLocalAddress()) {
            throw new SsrfViolationException("Access to private RFC1918 IP address is forbidden: " + address.getHostAddress());
        }

        // Check Multicast
        if (address.isMulticastAddress()) {
            throw new SsrfViolationException("Access to multicast IP address is forbidden: " + address.getHostAddress());
        }

        byte[] bytes = address.getAddress();

        // Check IPv4 specific ranges
        if (bytes.length == 4) {
            int b0 = bytes[0] & 0xFF;
            int b1 = bytes[1] & 0xFF;

            // 169.254.x.x (AWS/GCP/Azure Cloud Metadata)
            if (b0 == 169 && b1 == 254) {
                throw new SsrfViolationException("Access to cloud metadata IP address is forbidden: " + address.getHostAddress());
            }

            // 100.64.0.0/10 (Carrier Grade NAT & Alibaba Cloud Metadata)
            if (b0 == 100 && (b1 >= 64 && b1 <= 127)) {
                throw new SsrfViolationException("Access to CGNAT/cloud metadata IP address is forbidden: " + address.getHostAddress());
            }

            // 0.0.0.0/8
            if (b0 == 0) {
                throw new SsrfViolationException("Access to broadcast/local network is forbidden: " + address.getHostAddress());
            }
        }

        // Check IPv6 specific ranges & IPv4-mapped IPv6
        if (bytes.length == 16) {
            // Check for IPv4-mapped IPv6 (::ffff:x.x.x.x)
            if (isIPv4MappedIPv6(bytes)) {
                byte[] ipv4Bytes = Arrays.copyOfRange(bytes, 12, 16);
                try {
                    InetAddress ipv4 = InetAddress.getByAddress(ipv4Bytes);
                    validateAddress(ipv4);
                } catch (UnknownHostException ignored) {
                }
            }

            // Unique Local Address (fc00::/7)
            int firstByte = bytes[0] & 0xFF;
            if ((firstByte & 0xFE) == 0xFC) {
                throw new SsrfViolationException("Access to IPv6 unique local address is forbidden: " + address.getHostAddress());
            }
        }
    }

    private boolean isIPv4MappedIPv6(byte[] bytes) {
        for (int i = 0; i < 10; i++) {
            if (bytes[i] != 0) return false;
        }
        return (bytes[10] & 0xFF) == 0xFF && (bytes[11] & 0xFF) == 0xFF;
    }
}
