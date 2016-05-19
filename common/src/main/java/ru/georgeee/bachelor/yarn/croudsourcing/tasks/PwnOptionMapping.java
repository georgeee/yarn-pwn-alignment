package ru.georgeee.bachelor.yarn.croudsourcing.tasks;

import java.util.Collection;
import java.util.List;

public class PwnOptionMapping {
    public Pwn pwn;
    public List<Option> options;

    public static class Pwn {
        String gloss;
        Collection<String> examples;
        Collection<String> words;
        List<String> images;
    }

    public static class Option {
        Collection<String> words;
        int id;
    }
}
