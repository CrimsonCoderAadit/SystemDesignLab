package com.ssn.systemdesign.autocomplete.service;

import com.ssn.systemdesign.autocomplete.model.Suggestion;
import com.ssn.systemdesign.autocomplete.model.TrieNode;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class TrieService {

    private final TrieNode root;

    public TrieService() {
        this.root = new TrieNode();
    }

    @PostConstruct
    public void init() {
        loadDataset();
    }

    private void loadDataset() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new ClassPathResource("dataset.csv").getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    insert(parts[0].trim(), Integer.parseInt(parts[1].trim()));
                }
            }
            System.out.println("Dataset loaded into Trie successfully.");
        } catch (Exception e) {
            System.err.println("Failed to load dataset: " + e.getMessage());
        }
    }

    public void insert(String word, int frequency) {
        TrieNode current = root;
        for (char ch : word.toCharArray()) {
            current.children.putIfAbsent(ch, new TrieNode());
            current = current.children.get(ch);
        }
        current.isEndOfWord = true;
        current.frequency = frequency;
    }

    public List<Suggestion> getTopKSuggestions(String prefix, int k) {
        List<Suggestion> results = new ArrayList<>();
        TrieNode node = searchPrefixNode(prefix);

        if (node == null) {
            return results;
        }

        findAllWords(node, prefix, results);
        Collections.sort(results); // Sorts descending by frequency due to compareTo

        return results.size() > k ? new ArrayList<>(results.subList(0, k)) : results;
    }

    private TrieNode searchPrefixNode(String prefix) {
        TrieNode current = root;
        for (char ch : prefix.toCharArray()) {
            if (!current.children.containsKey(ch)) {
                return null;
            }
            current = current.children.get(ch);
        }
        return current;
    }

    private void findAllWords(TrieNode node, String currentPrefix, List<Suggestion> results) {
        if (node.isEndOfWord) {
            results.add(new Suggestion(currentPrefix, node.frequency));
        }
        for (char ch : node.children.keySet()) {
            findAllWords(node.children.get(ch), currentPrefix + ch, results);
        }
    }
}
