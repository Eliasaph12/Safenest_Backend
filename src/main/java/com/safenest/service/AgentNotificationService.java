package com.safenest.service;

import com.safenest.model.UserAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class AgentNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(AgentNotificationService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String frontendUrl;
    private final boolean mailConfigured;

    public AgentNotificationService(
        JavaMailSender mailSender,
        @Value("${spring.mail.username:}") String fromAddress,
        @Value("${FRONTEND_URL:http://localhost:5173}") String frontendUrl,
        @Value("${MAIL_HOST:}") String mailHost
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.frontendUrl = frontendUrl;
        this.mailConfigured = mailHost != null && !mailHost.isBlank();
    }

    public void notifyAgentOfIncomingChat(UserAccount victim, UserAccount agent, String messageText) {
        if (!mailConfigured) {
            logger.info("Mail notification skipped because SMTP is not configured. Victim={}, Agent={}", victim.getEmail(), agent.getEmail());
            return;
        }

        try {
            SimpleMailMessage email = new SimpleMailMessage();
            if (fromAddress != null && !fromAddress.isBlank()) {
                email.setFrom(fromAddress);
            }
            email.setTo(agent.getEmail());
            email.setSubject("SafeNest support chat: new message from " + victim.getName());
            email.setText(buildMessageBody(victim, agent, messageText));
            mailSender.send(email);
            logger.info("Support chat notification email sent to {}", agent.getEmail());
        } catch (Exception exception) {
            logger.warn("Failed to send support chat email to {}: {}", agent.getEmail(), exception.getMessage());
        }
    }

    private String buildMessageBody(UserAccount victim, UserAccount agent, String messageText) {
        return String.join("\n\n",
            "Hello " + agent.getName() + ",",
            "A victim user has sent you a new message in SafeNest support chat.",
            "Victim: " + victim.getName() + " (" + victim.getEmail() + ")",
            "Your role: " + agent.getRole(),
            "Latest message:\n" + messageText,
            "Please log in to continue the conversation:\n" + frontendUrl,
            "SafeNest notification"
        );
    }
}
