package com.guesshole.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller for the add round template page.
 */
@Controller
@RequestMapping("/admin/round-templates")
public class AdminRoundTemplatePageController {

    /**
     * Display the add round template page.
     *
     * @return The name of the Thymeleaf template to render
     */
    @GetMapping("/new")
    public String showAddRoundTemplatePage() {
        return "pages/admin/add-round-template";
    }
}