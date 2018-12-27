package pg.bio;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Main {
    private static final String AMINO_ACIDS = "ARNDCEQGHILKMFPSTWYV";

    public static void main(String[] args) {
        List<String> patternsLiteral = Collections.emptyList();
        List<String> sequences = Collections.emptyList();
        try {
            patternsLiteral = Files.readAllLines(Paths.get(".\\src\\pg\\bio\\patterns.txt"), StandardCharsets.UTF_8);
            sequences = Files.readAllLines(Paths.get(".\\src\\pg\\bio\\sequences.txt"), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<List<String>> patterns = makePatterns(patternsLiteral);

        for (String sequence : sequences) {
            boolean patternFound = false;
            for (int i = 0; i < patterns.size(); i++) {
                String patternLiteral = patternsLiteral.get(i);
                List<String> pattern = patterns.get(i);

                List<int[]> foundIndices = findPatternInSequence(sequence, pattern);
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

    private static List<List<String>> makePatterns(List<String> patternsLiteral) {
        List<List<String>> patterns = new ArrayList<>();
        for (String patternLiteral : patternsLiteral) {
            try {
                List<String> pattern = buildPatternFromLiteral(patternLiteral);
                patterns.add(pattern);
//                System.out.println(patternLiteral + ": " + pattern);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return patterns;
    }

    private static List<String> buildPatternFromLiteral(String patternLiteral) throws IllegalArgumentException {
        String[] patterns = patternLiteral.split("-");
        List<String> result = new ArrayList<>();

        for (String pattern : patterns) {
            String aminoacids;
            String repeats = "1";

            if (pattern.contains("(") && pattern.endsWith(")")) {
                int bracketId = pattern.indexOf('(');
                aminoacids = pattern.substring(0, bracketId);
                repeats = pattern.substring(bracketId + 1, pattern.length() - 1);
            } else
                aminoacids = pattern;

            aminoacids = aminoacids.toUpperCase();

            String patternPart;
            if (aminoacids.equals("X")) {
                // x – any amino acid
                patternPart = AMINO_ACIDS;
            } else if (aminoacids.length() == 1) {
                // V – any letter, one letter amino acid code,
                patternPart = aminoacids;
            } else if (aminoacids.startsWith("[") && aminoacids.endsWith("]")) {
                // […] – one amino acid from bracket
                patternPart = aminoacids.substring(1, aminoacids.length() - 1);
            } else if (aminoacids.startsWith("{") && aminoacids.endsWith("}")) {
                // {…} – one amino acid, but not from bracket
                patternPart = listSubtraction(aminoacids);
            } else {
                throw new IllegalArgumentException("Wrong pattern: " + patternLiteral);
            }

            if (checkAminoacids(patternPart))
                addPatternPartNTimes(result, patternPart, repeats);
        }
        return result;
    }

    private static boolean checkAminoacids(String potentialAminoacids) {
        for (int i = 0; i < potentialAminoacids.length(); i++) {
            char ch = potentialAminoacids.charAt(i);
            if (AMINO_ACIDS.indexOf(ch) < 0)
                throw new IllegalArgumentException(String.format("Not an aminoacid letter: %s, choose one from: '%s'",
                        Character.toString(ch), AMINO_ACIDS));
        }
        return true;
    }

    private static List<int[]> findPatternInSequence(String sequence, List<String> pattern) {
        List<int[]> patternIds = new ArrayList<>();
        int fromIndex = 0;
        while (true) {
            int[] patternId = findFirstPattern(sequence, pattern, fromIndex);
            if (patternId[0] == -1)
                break;
            patternIds.add(patternId);
            fromIndex = patternId[0] + 1;
        }
        return patternIds;
    }

    private static int[] findFirstPattern(String sequence, List<String> pattern, int fromIndex) {
        String seq = sequence.toUpperCase();
        int patternId = 0;
        int foundIdStart = -1;
        int foundIdEnd = -1;
        for (int i = fromIndex; i < seq.length(); i++) {
            String patternPart = pattern.get(patternId);
            boolean optional = isOptional(patternPart);

            char c = seq.charAt(i);
            if (patternPart.indexOf(c) >= 0) {
                if (patternId == 0) {
                    foundIdStart = i;
                }
                foundIdEnd = i;
                patternId++;
            } else if (optional) {
                // next time check the same character with next patternPart
                patternId++;
                i--;
            } else {
                if (patternId > 0) {
                    int nextStartingId = foundIdStart == -1 ? fromIndex + 1 : foundIdStart + 1;
                    return findFirstPattern(sequence, pattern, nextStartingId);
                }
            }

            if (patternId >= pattern.size()) {
                return new int[]{foundIdStart, foundIdEnd};
            }
        }

//        // if the sequence ended, but all remaining patterns are optional
        for (int i = patternId; i < pattern.size(); i++) {
            String patterPart = pattern.get(i);
            if (!isOptional(patterPart))
                return new int[]{-1, -1};
        }

        return new int[]{foundIdStart, sequence.length() - 1};
    }

    private static void addPatternPartNTimes(List<String> result, String sequence, String range) {
        if (range.contains(",")) {
            // e(i,j) - repetition of e exactly k times, where k≥i and k≤j
            String[] rangeSplitted = range.split(",");
            int min = Integer.parseInt(rangeSplitted[0]);
            int max = Integer.parseInt(rangeSplitted[1]);
            for (int i = 0; i < min; i++) {
                result.add(sequence);
            }
            for (int i = 0; i < max - min; i++) {
                result.add("?" + sequence);
            }
        } else {
            // e(i) – for element e and number i: repetition of e exactly i times
            int value = Integer.parseInt(range);
            for (int i = 0; i < value; i++) {
                result.add(sequence);
            }
        }
    }

    private static String listSubtraction(String p) {
        List<Character> aminoAcidsChars = AMINO_ACIDS.chars().mapToObj(i -> (char) i).collect(Collectors.toList());
        List<Character> patternChars = p.substring(1, p.length() - 1).toUpperCase().chars().mapToObj(i -> (char) i).collect(Collectors.toList());
        aminoAcidsChars.removeAll(patternChars);
        return aminoAcidsChars.stream().map(String::valueOf).collect(Collectors.joining());
    }

    private static boolean isOptional(String patternPart) {
        return patternPart.contains("?");
    }

    private static void printSequence(String sequence, String patternLiteral, List<int[]> foundIndices) {
        StringBuilder foundSequences = new StringBuilder();
        for (int[] foundIndex : foundIndices) {
            String found = sequence.substring(foundIndex[0], foundIndex[1] + 1);
//            String found = sequence.substring(0, foundIndex[0]) + ">" +
//                    sequence.substring(foundIndex[0], foundIndex[1] + 1) + "<" +
//                    sequence.substring(foundIndex[1] + 1);
            foundSequences.append(String.format("\\%d-%d: %s\\ ",
                    foundIndex[0], foundIndex[1], found));
        }
        System.out.println(String.format("sequence: %s, pattern: %s, found: %s",
                sequence, patternLiteral, foundSequences.toString()));
    }
}