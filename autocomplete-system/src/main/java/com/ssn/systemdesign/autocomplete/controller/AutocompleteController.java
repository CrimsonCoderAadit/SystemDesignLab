package com.ssn.systemdesign.autocomplete.controller;

import com.ssn.systemdesign.autocomplete.model.Suggestion;
import com.ssn.systemdesign.autocomplete.service.AutocompleteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AutocompleteController {

    private final AutocompleteService autocompleteService;

    public AutocompleteController(AutocompleteService autocompleteService) {
        this.autocompleteService = autocompleteService;
    }

    @GetMapping("/autocomplete")
    public ResponseEntity<List<Suggestion>> getSuggestions(
            @RequestParam String prefix,
            @RequestParam(defaultValue = "5") int k) {
        if (prefix == null || prefix.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        List<Suggestion> suggestions = autocompleteService.getSuggestions(prefix, k);
        return ResponseEntity.ok(suggestions);
    }
}
