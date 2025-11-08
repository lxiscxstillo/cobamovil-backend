package com.cobamovil.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {
    @Bean(name = "mailtrapSender")
    public JavaMailSender mailtrapSender() {
        String host = System.getenv("MAILTRAP_HOST");
        String port = System.getenv("MAILTRAP_PORT");
        String username = System.getenv("MAILTRAP_USERNAME");
        String password = System.getenv("MAILTRAP_PASSWORD");
        if (host == null || username == null || password == null) {
            return null; // Not configured
        }
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port != null ? Integer.parseInt(port) : 2525);
        sender.setUsername(username);
        sender.setPassword(password);
        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "false");
        return sender;
    }
}

