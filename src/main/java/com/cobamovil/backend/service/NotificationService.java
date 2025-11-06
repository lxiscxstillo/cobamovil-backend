package com.cobamovil.backend.service;

import com.cobamovil.backend.entity.NotificationLog;
import com.cobamovil.backend.entity.User;
import com.cobamovil.backend.repository.NotificationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationLogRepository notificationLogRepository;

    @Value("${app.whatsapp.number:}")
    private String whatsappNumber;

    public NotificationService(NotificationLogRepository notificationLogRepository) {
        this.notificationLogRepository = notificationLogRepository;
    }

    public void notifyBookingEvent(User user, String event, String preferredChannel) {
        // Stub: log and persist a record. Integrations can be added behind adapters.
        String channel = preferredChannel != null ? preferredChannel : "INTERNAL";
        String destination = switch (channel) {
            case "WHATSAPP" -> whatsappNumber;
            case "EMAIL" -> user.getEmail();
            default -> user.getUsername();
        };
        log.info("Notify {} via {} to {}", event, channel, destination);
        NotificationLog nl = new NotificationLog();
        nl.setUser(user);
        nl.setChannel(channel);
        nl.setEvent(event);
        nl.setDestination(destination);
        nl.setStatus("SENT");
        notificationLogRepository.save(nl);
    }
}

