package com.guesshole.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller for the admin map view.
 */
@Controller
@RequestMapping("/admin")
public class AdminMapController {

    /**
     * Display the round templates map page.
     *
     * @return The template name for the map view
     */
    @GetMapping("/map")
    public String showMapPage() {
        return "pages/admin/map";
    }
}