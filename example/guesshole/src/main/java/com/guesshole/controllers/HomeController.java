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


@Controller
@RequestMapping("/")
public class HomeController {

    private static final Logger log = LoggerFactory.getLogger(HomeController.class);

    @Autowired
    private JavaMailSender emailSender;

    @Autowired
    public HomeController() {
    }

    @GetMapping("/")
    public String home() {
        return "pages/home";
    }

    @GetMapping("/tos")
    public String tos() {
        return "pages/tos";
    }

    @GetMapping("/privacy")
    public String privacy() {
        return "pages/privacy";
    }

}