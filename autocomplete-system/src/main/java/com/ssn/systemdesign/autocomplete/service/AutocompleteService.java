package com.ssn.systemdesign.autocomplete.service;

import com.ssn.systemdesign.autocomplete.model.Suggestion;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AutocompleteService {

    private final TrieService trieService;

    public AutocompleteService(TrieService trieService) {
        this.trieService = trieService;
    }

    @Cacheable(value = "autocomplete", key = "#prefix")
    public List<Suggestion> getSuggestions(String prefix, int k) {
        System.out.println("Cache miss for prefix: '" + prefix + "'. Searching in Trie...");
        return trieService.getTopKSuggestions(prefix.toLowerCase(), k);
    }
}
