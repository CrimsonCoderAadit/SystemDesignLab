package com.ssn.systemdesign.autocomplete.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

public class Suggestion implements Serializable, Comparable<Suggestion> {
    private static final long serialVersionUID = 1L;

    private String term;
    private int frequency;

    public Suggestion() {
    }

    public Suggestion(String term, int frequency) {
        this.term = term;
        this.frequency = frequency;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    @Override
    public int compareTo(Suggestion other) {
        // Sort in descending order of frequency
        return Integer.compare(other.frequency, this.frequency);
    }
}
