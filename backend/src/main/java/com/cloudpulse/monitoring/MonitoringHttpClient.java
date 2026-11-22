package com.cloudpulse.monitoring;

import com.cloudpulse.exception.SsrfViolationException;
import com.cloudpulse.monitor.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class MonitoringHttpClient {

    private static final Logger log = LoggerFactory.getLogger(MonitoringHttpClient.class);

    private final SsrfProtectionService ssrfProtectionService;
    private final int maxResponseBodyBytes;
    private final int maxTimeoutMs;
    private volatile HttpClient httpClient;

    public MonitoringHttpClient(
            SsrfProtectionService ssrfProtectionService,
            @Value("${app.monitoring.max-response-body-bytes:1048576}") int maxResponseBodyBytes,
            @Value("${app.monitoring.max-timeout-ms:30000}") int maxTimeoutMs) {
        this.ssrfProtectionService = ssrfProtectionService;
        this.maxResponseBodyBytes = maxResponseBodyBytes;
        this.maxTimeoutMs = maxTimeoutMs;
    }

    private HttpClient getHttpClient() {
        HttpClient client = this.httpClient;
        if (client == null) {
            synchronized (this) {
                client = this.httpClient;
                if (client == null) {
                    this.httpClient = client = HttpClient.newBuilder()
                            .version(HttpClient.Version.HTTP_2)
                            .followRedirects(HttpClient.Redirect.NEVER) // Manually handled to validate redirect hops for SSRF
                            .connectTimeout(Duration.ofSeconds(10))
                            .build();
                }
            }
        }
        return client;
    }

    public HttpResponseSnapshot execute(String urlString, HttpMethod method, int timeoutMs,
                                         Map<String, String> customHeaders, String requestBody) {

        int effectiveTimeout = Math.min(timeoutMs > 0 ? timeoutMs : 10000, maxTimeoutMs);
        String currentUrl = urlString;
        int redirectCount = 0;
        final int MAX_REDIRECTS = 3;

        long startTime = System.nanoTime();
        String resolvedIp = "UNKNOWN";

        try {
            while (redirectCount <= MAX_REDIRECTS) {
                // Validate URI at each hop (prevent redirect-based SSRF)
                URI targetUri = ssrfProtectionService.validateUri(currentUrl);

                try {
                    InetAddress addr = InetAddress.getByName(targetUri.getHost());
                    resolvedIp = addr.getHostAddress();
                } catch (Exception ignored) {
                }

                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(targetUri)
                        .timeout(Duration.ofMillis(effectiveTimeout))
                        .header("User-Agent", "CloudPulse-Monitor/1.0 (+https://cloudpulse.io/bot)");

                // Add custom headers
                if (customHeaders != null) {
                    customHeaders.forEach((k, v) -> {
                        // Prevent header injection and skip restricted headers
                        if (k != null && v != null && !k.equalsIgnoreCase("Host") && !k.equalsIgnoreCase("User-Agent")) {
                            requestBuilder.header(k, v);
                        }
                    });
                }

                // Set HTTP method & body
                HttpRequest.BodyPublisher bodyPublisher = (requestBody != null && !requestBody.isBlank())
                        ? HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8)
                        : HttpRequest.BodyPublishers.noBody();

                switch (method) {
                    case GET -> requestBuilder.GET();
                    case POST -> requestBuilder.POST(bodyPublisher);
                    case PUT -> requestBuilder.PUT(bodyPublisher);
                    case DELETE -> requestBuilder.DELETE();
                    case HEAD -> requestBuilder.method("HEAD", HttpRequest.BodyPublishers.noBody());
                    case PATCH -> requestBuilder.method("PATCH", bodyPublisher);
                }

                HttpRequest request = requestBuilder.build();
                HttpResponse<InputStream> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofInputStream());

                int statusCode = response.statusCode();

                // Check for HTTP redirect status codes
                if ((statusCode == 301 || statusCode == 302 || statusCode == 303 || statusCode == 307 || statusCode == 308)
                        && redirectCount < MAX_REDIRECTS) {

                    List<String> locationHeaders = response.headers().allValues("Location");
                    if (!locationHeaders.isEmpty()) {
                        String redirectLocation = locationHeaders.getFirst();
                        URI redirectUri = targetUri.resolve(redirectLocation);
                        currentUrl = redirectUri.toString();
                        redirectCount++;
                        log.debug("Following redirect #{} to {}", redirectCount, currentUrl);
                        continue;
                    }
                }

                long responseTimeMs = (System.nanoTime() - startTime) / 1_000_000;

                // Read bounded response body
                String body = readBoundedBody(response.body());
                Map<String, List<String>> headers = response.headers().map();

                return new HttpResponseSnapshot(
                        true,
                        statusCode,
                        responseTimeMs,
                        headers,
                        body,
                        resolvedIp,
                        null
                );
            }

            long totalTimeMs = (System.nanoTime() - startTime) / 1_000_000;
            return HttpResponseSnapshot.failure("Exceeded maximum redirects limit of " + MAX_REDIRECTS, totalTimeMs, resolvedIp);

        } catch (SsrfViolationException ex) {
            long totalTimeMs = (System.nanoTime() - startTime) / 1_000_000;
            log.warn("Monitoring check blocked by SSRF policy: {}", ex.getMessage());
            return HttpResponseSnapshot.failure("Security Violation (SSRF): " + ex.getMessage(), totalTimeMs, resolvedIp);
        } catch (Exception ex) {
            long totalTimeMs = (System.nanoTime() - startTime) / 1_000_000;
            log.debug("HTTP check failed for {}: {}", currentUrl, ex.getMessage());
            String errorMessage = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            return HttpResponseSnapshot.failure("Connection failed: " + errorMessage, totalTimeMs, resolvedIp);
        }
    }

    private String readBoundedBody(InputStream inputStream) {
        if (inputStream == null) return null;
        try (inputStream) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            int totalRead = 0;
            StringBuilder sb = new StringBuilder();

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                int toAppend = Math.min(bytesRead, maxResponseBodyBytes - totalRead);
                if (toAppend > 0) {
                    sb.append(new String(buffer, 0, toAppend, StandardCharsets.UTF_8));
                    totalRead += toAppend;
                }
                if (totalRead >= maxResponseBodyBytes) {
                    sb.append("\n... [Response body truncated at ").append(maxResponseBodyBytes).append(" bytes]");
                    break;
                }
            }
            return sb.toString();
        } catch (Exception ex) {
            log.warn("Failed reading response stream: {}", ex.getMessage());
            return null;
        }
    }
}
