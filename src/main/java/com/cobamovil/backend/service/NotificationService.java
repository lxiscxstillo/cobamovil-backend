package com.cobamovil.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final String accountSid = System.getenv("TWILIO_ACCOUNT_SID");
    private final String authToken = System.getenv("TWILIO_AUTH_TOKEN");
    private final String fromWhatsApp = System.getenv("TWILIO_WHATSAPP_FROM"); // e.g., +1415...
    private final boolean enabled;
    private final RestTemplate http = new RestTemplate();

    private final JavaMailSender mailtrapSender;

    public NotificationService(@org.springframework.beans.factory.annotation.Autowired(required = false) JavaMailSender mailtrapSender) {
        this.mailtrapSender = mailtrapSender;
        this.enabled = accountSid != null && authToken != null && fromWhatsApp != null;
        if (enabled) {
            log.info("Twilio REST configured for WhatsApp from {}", fromWhatsApp);
        } else {
            log.warn("Twilio env vars missing. WhatsApp notifications disabled.");
        }
    }

    public void sendWhatsApp(String toE164, String body) {
        if (!enabled) { log.debug("Twilio disabled, skipping WhatsApp message to {}", toE164); return; }
        try {
            String url = String.format("https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json", accountSid);
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(accountSid, authToken);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("To", "whatsapp:" + toE164);
            form.add("From", "whatsapp:" + fromWhatsApp);
            form.add("Body", body);
            HttpEntity<MultiValueMap<String, String>> req = new HttpEntity<>(form, headers);
            http.postForEntity(url, req, String.class);
            log.info("WhatsApp message sent to {}", toE164);
        } catch (Exception ex) {
            log.error("Failed to send WhatsApp message: {}", ex.getMessage());
        }
    }

    /**
     * High-level notification entrypoint used across the app.
     * channel: "WHATSAPP" | "EMAIL" | "INTERNAL" (no-op)
     */
    public void notifyBookingEvent(com.cobamovil.backend.entity.User user, String event, String channel) {
        try {
            switch (channel == null ? "" : channel.toUpperCase()) {
                case "WHATSAPP" -> {
                    String to = (user != null && user.getPhone() != null && !user.getPhone().isBlank())
                            ? user.getPhone()
                            : System.getenv("TWILIO_TEST_TO");
                    if (to == null || to.isBlank()) {
                        log.warn("No phone or TWILIO_TEST_TO set; skipping WhatsApp notification for event {}", event);
                        return;
                    }
                    String body = switch (event) {
                        case "BOOKING_CREATED" -> "Tu reserva fue recibida y est\u00E1 pendiente de aprobaci\u00F3n.";
                        case "BOOKING_RESCHEDULED" -> "Tu reserva fue reprogramada.";
                        case "BOOKING_CANCELED" -> "Tu reserva fue cancelada.";
                        case "BOOKING_APPROVED" -> "\u00A1Tu reserva fue aprobada!";
                        case "BOOKING_REJECTED" -> "Lo sentimos, a\u00FAn no llegamos a tu zona.";
                        case "BOOKING_ON_ROUTE" -> "Estamos en camino.";
                        case "BOOKING_COMPLETED" -> "Servicio completado. \u00A1Gracias!";
                        default -> "Actualizaci\u00F3n de tu reserva.";
                    };
                    sendWhatsApp(to, body);
                }
                case "EMAIL" -> {
                    sendEmail(
                            user != null ? user.getEmail() : null,
                            subjectFor(event),
                            htmlFor(event)
                    );
                }
                default -> {
                    // INTERNAL or unknown: no-op
                    log.debug("INTERNAL notification for event {}", event);
                }
            }
        } catch (Exception ex) {
            log.error("Notification dispatch failed: {}", ex.getMessage());
        }
    }

    private String subjectFor(String event) {
        return switch (event) {
            case "BOOKING_CREATED" -> "Reserva recibida";
            case "BOOKING_APPROVED" -> "Reserva aprobada";
            case "BOOKING_REJECTED" -> "Reserva rechazada";
            case "BOOKING_ON_ROUTE" -> "Estamos en camino";
            case "BOOKING_COMPLETED" -> "Servicio completado";
            case "BOOKING_RESCHEDULED" -> "Reserva reprogramada";
            case "BOOKING_CANCELED" -> "Reserva cancelada";
            default -> "Actualizaci\u00F3n de tu reserva";
        };
    }

    private String htmlFor(String event) {
        String body = switch (event) {
            case "BOOKING_CREATED" -> "Tu reserva fue recibida y est\u00E1 pendiente de aprobaci\u00F3n.";
            case "BOOKING_APPROVED" -> "\u00A1Tu reserva fue aprobada!";
            case "BOOKING_REJECTED" -> "Lo sentimos, a\u00FAn no llegamos a tu zona.";
            case "BOOKING_ON_ROUTE" -> "Estamos en camino.";
            case "BOOKING_COMPLETED" -> "Servicio completado. \u00A1Gracias!";
            case "BOOKING_RESCHEDULED" -> "Tu reserva fue reprogramada.";
            case "BOOKING_CANCELED" -> "Tu reserva fue cancelada.";
            default -> "Actualizaci\u00F3n de tu reserva.";
        };
        return "<p>" + body + "</p>";
    }

    private void sendEmail(String to, String subject, String html) {
        if (to == null || to.isBlank()) { log.warn("Recipient email missing; skipping email."); return; }
        // 1) Try Mailtrap via SMTP (if configured)
        try {
            String host = System.getenv("MAILTRAP_HOST");
            if (mailtrapSender != null && host != null) {
                var mime = mailtrapSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mime, "UTF-8");
                String from = System.getenv().getOrDefault("MAILTRAP_FROM_EMAIL", "Coba M\u00F3vil <no-reply@cobamovil.test>");
                helper.setFrom(from);
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(html, true);
                mailtrapSender.send(mime);
                log.info("Email sent to {} via Mailtrap SMTP", to);
                return;
            }
        } catch (Exception ex) {
            log.error("Failed to send email via Mailtrap SMTP: {}", ex.getMessage());
        }

        // 2) Fallback to Resend API if available
        String apiKey = System.getenv("RESEND_API_KEY");
        if (apiKey == null || apiKey.isBlank()) { log.warn("No mail provider configured (Mailtrap/Resend)"); return; }
        try {
            String url = "https://api.resend.com/emails";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            String from = System.getenv().getOrDefault("RESEND_FROM_EMAIL", "Coba M\u00F3vil <notifications@resend.dev>");
            String payload = String.format("{\"from\":\"%s\",\"to\":[\"%s\"],\"subject\":\"%s\",\"html\":\"%s\"}",
                    from.replace("\"","'"), to.replace("\"","'"), subject.replace("\"","'"), html.replace("\"","'"));
            HttpEntity<String> req = new HttpEntity<>(payload, headers);
            http.postForEntity(url, req, String.class);
            log.info("Email sent to {} via Resend", to);
        } catch (Exception ex) {
            log.error("Failed to send email via Resend: {}", ex.getMessage());
        }
    }
}

