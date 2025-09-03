package com.guesshole.controllers;

import com.guesshole.entities.RoundTemplate;
import com.guesshole.repositories.RoundTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * REST controller for round templates.
 * Provides endpoints for the admin map view.
 */
@RestController
@RequestMapping("/admin/api/round-templates")
public class RoundTemplateRestController {

    private final RoundTemplateRepository roundTemplateRepository;

    @Autowired
    public RoundTemplateRestController(RoundTemplateRepository roundTemplateRepository) {
        this.roundTemplateRepository = roundTemplateRepository;
    }

    /**
     * Get all round templates for the map view.
     *
     * @return A Flux of all RoundTemplate entities
     */
    @GetMapping("/all")
    public Flux<RoundTemplate> getAllTemplates() {
        return roundTemplateRepository.findAll();
    }
}