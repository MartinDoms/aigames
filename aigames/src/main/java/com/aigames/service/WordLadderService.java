package com.aigames.service;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class WordLadderService {

    // Basic word list - in a real application, this would come from a file or database
    private static final Set<String> VALID_WORDS = Set.of(
        "cat", "bat", "mat", "hat", "rat", "sat", "fat", "pat", "vat",
        "cap", "map", "tap", "rap", "sap", "gap", "lap", "nap", "zap",
        "car", "bar", "tar", "jar", "war", "far", "par", 
        "can", "ban", "man", "fan", "pan", "van", "ran", "tan",
        "dog", "log", "fog", "bog", "hog", "jog", "cog",
        "dig", "big", "fig", "jig", "pig", "rig", "wig",
        "day", "bay", "hay", "jay", "lay", "may", "pay", "ray", "say", "way",
        "den", "hen", "men", "pen", "ten", "yen", "zen",
        "get", "bet", "jet", "let", "met", "net", "pet", "set", "vet", "wet", "yet",
        "got", "hot", "lot", "not", "pot", "rot", "tot",
        "gun", "bun", "fun", "nun", "run", "sun",
        "hit", "bit", "fit", "kit", "lit", "pit", "sit", "wit",
        "hop", "bop", "cop", "mop", "pop", "sop", "top",
        "how", "bow", "cow", "low", "now", "row", "sow", "tow", "wow",
        "ice", "ace", "die", "lie", "pie", "tie", "vie",
        "job", "bob", "cob", "fob", "gob", "lob", "mob", "rob", "sob"
    );

    public boolean isValidWord(String word) {
        return VALID_WORDS.contains(word.toLowerCase());
    }

    public boolean isOneLetterDifferent(String word1, String word2) {
        if (word1.length() != word2.length()) {
            return false;
        }
        
        int differences = 0;
        for (int i = 0; i < word1.length(); i++) {
            if (word1.charAt(i) != word2.charAt(i)) {
                differences++;
                if (differences > 1) {
                    return false;
                }
            }
        }
        return differences == 1;
    }

    public List<String> findShortestPath(String startWord, String endWord) {
        if (!isValidWord(startWord) || !isValidWord(endWord)) {
            return Collections.emptyList();
        }
        
        if (startWord.equals(endWord)) {
            return List.of(startWord);
        }

        Queue<List<String>> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        
        queue.offer(List.of(startWord));
        visited.add(startWord);
        
        while (!queue.isEmpty()) {
            List<String> path = queue.poll();
            String currentWord = path.get(path.size() - 1);
            
            for (String word : VALID_WORDS) {
                if (!visited.contains(word) && isOneLetterDifferent(currentWord, word)) {
                    List<String> newPath = new ArrayList<>(path);
                    newPath.add(word);
                    
                    if (word.equals(endWord)) {
                        return newPath;
                    }
                    
                    queue.offer(newPath);
                    visited.add(word);
                }
            }
        }
        
        return Collections.emptyList();
    }

    public boolean isValidMove(String fromWord, String toWord) {
        return isValidWord(toWord) && isOneLetterDifferent(fromWord, toWord);
    }

    public Set<String> getPossibleNextWords(String currentWord) {
        return VALID_WORDS.stream()
            .filter(word -> isOneLetterDifferent(currentWord, word))
            .collect(HashSet::new, HashSet::add, HashSet::addAll);
    }

    // Generate a random word pair for daily challenges
    public WordPair generateRandomWordPair() {
        List<String> wordList = new ArrayList<>(VALID_WORDS);
        Random random = new Random();
        
        String startWord = wordList.get(random.nextInt(wordList.size()));
        String endWord;
        
        // Try to find a word that has a solution
        int attempts = 0;
        do {
            endWord = wordList.get(random.nextInt(wordList.size()));
            attempts++;
        } while (startWord.equals(endWord) && attempts < 100);
        
        // Verify there's a solution
        List<String> solution = findShortestPath(startWord, endWord);
        if (solution.isEmpty()) {
            // Fallback to a known working pair
            return new WordPair("cat", "dog");
        }
        
        return new WordPair(startWord, endWord);
    }

    public record WordPair(String startWord, String endWord) {}
}