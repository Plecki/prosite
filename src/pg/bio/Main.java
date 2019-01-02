package pg.bio;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.stream.Collectors;

public class Main {
    static final String AMINO_ACIDS = "ARNDCEQGHILKMFPSTWYV";

    public static void main(String[] args) {
        List<String> patternsLiteral = Collections.emptyList();
        List<String> sequences = Collections.emptyList();
        try {
            patternsLiteral = Files.readAllLines(Paths.get(".\\src\\pg\\bio\\patterns.txt"), StandardCharsets.UTF_8);
            sequences = Files.readAllLines(Paths.get(".\\src\\pg\\bio\\sequences.txt"), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<Pattern> patterns = makePatterns(patternsLiteral);

        for (String sequence : sequences) {
            boolean patternFound = false;
            for (int i = 0; i < patterns.size(); i++) {
                String patternLiteral = patternsLiteral.get(i);
                Pattern pattern = patterns.get(i);

                SortedSet<PatternResult> foundIndices = pattern.findInSequence(sequence);
                if (foundIndices.size() > 0) {
                    patternFound = true;
                    printSequence(sequence, patternLiteral, foundIndices);
                }
            }

            if (!patternFound) {
                System.out.println("No pattern found in sequence: " + sequence);
            }
        }
    }

    static List<Pattern> makePatterns(List<String> patternsLiteral) {
        List<Pattern> patterns = new ArrayList<>();
        for (String patternLiteral : patternsLiteral) {
            try {
                Pattern pattern = new Pattern(patternLiteral);
                patterns.add(pattern);
                System.out.println(patternLiteral + ": " + pattern);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return patterns;
    }


    static String aminoacidsWithout(String forbiddenAminoacids) {
        List<Character> aminoAcidsChars = AMINO_ACIDS.chars().mapToObj(i -> (char) i).collect(Collectors.toList());
        List<Character> patternChars = forbiddenAminoacids
                .toUpperCase().chars().mapToObj(i -> (char) i).collect(Collectors.toList());
        aminoAcidsChars.removeAll(patternChars);
        return aminoAcidsChars.stream().map(String::valueOf).collect(Collectors.joining());
    }

    static void printSequence(String sequence, String patternLiteral, SortedSet<PatternResult> foundIndices) {
        StringBuilder foundSequences = new StringBuilder();
        for (PatternResult foundIndex : foundIndices) {
            String found = sequence.substring(foundIndex.start, foundIndex.end + 1);
//            String found = sequence.substring(0, foundIndex.start) + ">" +
//                    sequence.substring(foundIndex.start, foundIndex.end + 1) + "<" +
//                    sequence.substring(foundIndex.end + 1);
            foundSequences.append(String.format("\\%d-%d: %s\\ ",
                    foundIndex.start, foundIndex.end, found));
        }
        System.out.println(String.format("sequence: %s, pattern: %s, found: %s",
                sequence, patternLiteral, foundSequences.toString()));
    }


    static class PatternResult implements Comparable {
        int start;
        int end;

        PatternResult(int start, int end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public int compareTo(Object o) {
            PatternResult ob = (PatternResult) o;
            return start != ob.start ? start - ob.start : end - ob.end;
        }
    }
}