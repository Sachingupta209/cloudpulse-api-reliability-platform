package com.cloudpulse.notification;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String fromEmail;
    private final String fromName;

    public EmailNotificationService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${app.notifications.from-email:alerts@cloudpulse.io}") String fromEmail,
            @Value("${app.notifications.from-name:CloudPulse Alerts}") String fromName) {
        this.mailSenderProvider = mailSenderProvider;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
    }

    public record EmailSendResult(boolean success, String errorMessage) {}

    public EmailSendResult sendSingleEmail(String recipient, String subject, String htmlBody, String textBody) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("JavaMailSender bean is not available. Skipping email to {}", recipient);
            return new EmailSendResult(false, "Mail sender is not configured");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(new InternetAddress(fromEmail, fromName));
            helper.setTo(recipient);
            helper.setSubject(subject);

            if (textBody != null && !textBody.isBlank()) {
                helper.setText(textBody, htmlBody);
            } else {
                helper.setText(htmlBody, true);
            }

            mailSender.send(message);
            log.info("Email successfully dispatched to {} with subject '{}'", recipient, subject);
            return new EmailSendResult(true, null);
        } catch (Exception ex) {
            String sanitizedError = NotificationSanitizer.sanitizeErrorMessage(ex);
            log.warn("Failed to dispatch email to {}: {}", recipient, sanitizedError);
            return new EmailSendResult(false, sanitizedError);
        }
    }

    public String buildHtmlEmail(
            String title,
            String badgeText,
            String badgeColor,
            String monitorName,
            String monitorUrl,
            String timestampUtc,
            String details,
            String actionUrl) {

        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>%s</title>
                </head>
                <body style="margin: 0; padding: 0; background-color: #0f172a; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #f8fafc;">
                    <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color: #0f172a; padding: 40px 20px;">
                        <tr>
                            <td align="center">
                                <table width="600" border="0" cellspacing="0" cellpadding="0" style="max-width: 600px; width: 100%%; background-color: #1e293b; border-radius: 12px; border: 1px solid #334155; overflow: hidden; box-shadow: 0 10px 25px rgba(0, 0, 0, 0.5);">
                                    <tr>
                                        <td style="padding: 28px 32px; border-bottom: 1px solid #334155;">
                                            <table width="100%%" border="0" cellspacing="0" cellpadding="0">
                                                <tr>
                                                    <td>
                                                        <span style="font-size: 22px; font-weight: 700; color: #38bdf8; letter-spacing: -0.5px;">Cloud<span style="color: #60a5fa;">Pulse</span></span>
                                                        <span style="font-size: 13px; color: #94a3b8; margin-left: 8px;">Alerting</span>
                                                    </td>
                                                    <td align="right">
                                                        <span style="display: inline-block; padding: 5px 12px; border-radius: 9999px; font-size: 12px; font-weight: 700; background-color: %s; color: #ffffff; text-transform: uppercase; letter-spacing: 0.5px;">
                                                            %s
                                                        </span>
                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding: 32px;">
                                            <h1 style="margin: 0 0 16px 0; font-size: 20px; font-weight: 600; color: #f8fafc; line-height: 1.4;">
                                                %s
                                            </h1>
                                            <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color: #0f172a; border-radius: 8px; border: 1px solid #334155; margin-bottom: 24px;">
                                                <tr>
                                                    <td style="padding: 16px 20px; border-bottom: 1px solid #1e293b; color: #94a3b8; font-size: 13px; width: 120px;">Target Monitor</td>
                                                    <td style="padding: 16px 20px; border-bottom: 1px solid #1e293b; color: #f8fafc; font-size: 14px; font-weight: 600;">%s</td>
                                                </tr>
                                                <tr>
                                                    <td style="padding: 16px 20px; border-bottom: 1px solid #1e293b; color: #94a3b8; font-size: 13px;">Endpoint URL</td>
                                                    <td style="padding: 16px 20px; border-bottom: 1px solid #1e293b; color: #38bdf8; font-size: 13px; font-family: monospace;">%s</td>
                                                </tr>
                                                <tr>
                                                    <td style="padding: 16px 20px; color: #94a3b8; font-size: 13px;">Timestamp (UTC)</td>
                                                    <td style="padding: 16px 20px; color: #e2e8f0; font-size: 13px;">%s</td>
                                                </tr>
                                            </table>
                                            <div style="background-color: #0f172a; border-radius: 8px; border-left: 4px solid %s; padding: 16px 20px; margin-bottom: 28px;">
                                                <div style="font-size: 12px; font-weight: 600; color: #94a3b8; text-transform: uppercase; margin-bottom: 6px; letter-spacing: 0.5px;">Details</div>
                                                <div style="font-size: 14px; color: #e2e8f0; line-height: 1.5; white-space: pre-wrap;">%s</div>
                                            </div>
                                            <table width="100%%" border="0" cellspacing="0" cellpadding="0">
                                                <tr>
                                                    <td align="center">
                                                        <a href="%s" style="display: inline-block; padding: 12px 28px; background-color: #0284c7; color: #ffffff; text-decoration: none; border-radius: 8px; font-size: 14px; font-weight: 600; box-shadow: 0 4px 12px rgba(2, 132, 199, 0.3);">
                                                            View in CloudPulse Dashboard &rarr;
                                                        </a>
                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding: 20px 32px; background-color: #0f172a; border-top: 1px solid #334155; text-align: center; color: #64748b; font-size: 12px;">
                                            This is an automated notification from CloudPulse API Reliability Platform.<br>
                                            You can manage alert preferences in your CloudPulse Settings.
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """,
                title,
                badgeColor,
                badgeText,
                title,
                monitorName != null ? monitorName : "N/A",
                monitorUrl != null ? monitorUrl : "N/A",
                timestampUtc,
                badgeColor,
                details,
                actionUrl != null ? actionUrl : "http://localhost:5174"
        );
    }
}
