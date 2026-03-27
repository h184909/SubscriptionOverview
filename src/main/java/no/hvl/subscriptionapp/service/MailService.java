package no.hvl.subscriptionapp.service;

import org.springframework.stereotype.Service;

@Service
public class MailService {

    public void send(String to, String subject, String body) {
        System.out.println("=== EMAIL SEND ===");
        System.out.println("TO: " + to);
        System.out.println("SUBJECT: " + subject);
        System.out.println(body);
        System.out.println("==================");
    }
}