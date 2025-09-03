package com.guesshole.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


@Controller
@RequestMapping("/contact")
public class ContactController {

    private static final Logger log = LoggerFactory.getLogger(HomeController.class);

    @Autowired
    private JavaMailSender emailSender;

    @Autowired
    public ContactController() {
    }


    @GetMapping()
    public String contact() {
        return "pages/contact";
    }

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<String> createContact(ServerWebExchange exchange, Model model) {
        return exchange.getFormData()
                .map(formData -> {
                    try {
                        String name = formData.getFirst("name");
                        String email = formData.getFirst("email");
                        String subject = formData.getFirst("subject");
                        String message = formData.getFirst("message");

                        if (name == null || email == null || subject == null || message == null) {
                            throw new IllegalArgumentException("Missing required form fields");
                        }

                        SimpleMailMessage mailMessage = new SimpleMailMessage();
                        mailMessage.setTo("support@guesshole.com");
                        // Add this line to set the from address
                        mailMessage.setFrom(email);
                        mailMessage.setSubject("Contact Form: " + subject);
                        mailMessage.setText("Name: " + name + "\nEmail: " + email + "\n\n" + message);

                        emailSender.send(mailMessage);
                        model.addAttribute("success", true);
                    } catch (Exception e) {
                        log.error("Failed to send email", e);
                        model.addAttribute("error", true);
                    }

                    return "pages/contact";
                });
    }
}