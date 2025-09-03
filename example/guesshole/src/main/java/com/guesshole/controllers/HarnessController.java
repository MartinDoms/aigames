package com.guesshole.controllers;

import com.guesshole.repositories.RoundTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/harnesses")
public class HarnessController {

    @Autowired
    public HarnessController() {
    }

    @GetMapping("/round-scoreboard")
    public String videoList() {

        return "pages/harnesses/round-scoreboard.html";
    }

}
