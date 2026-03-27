package no.hvl.subscriptionapp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final JavaMailSender sender;

    @Value("${app.mail.from:no-reply@subscriptionoverview.com}")
    private String from;

    public MailService(JavaMailSender sender) {
        this.sender = sender;
    }

    public void send(String to, String subject, String body) {
        SimpleMailMessage m = new SimpleMailMessage();
        m.setFrom(from);
        m.setTo(to);
        m.setSubject(subject);
        m.setText(body);
        sender.send(m);
    }
}