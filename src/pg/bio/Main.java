package pg.bio;

import com.sun.javaws.exceptions.InvalidArgumentException;

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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return patterns;
    }

    private static List<String> buildPatternFromLiteral(String patternLiteral) throws InvalidArgumentException {
        String[] patterns = patternLiteral.split("-");
        List<String> result = new ArrayList<>();

        for (String p : patterns) {
            if (p.equals("x")) {
                // x – any amino acid
                result.add(AMINO_ACIDS);
            } else if (p.length() == 1 && AMINO_ACIDS.contains(p.toUpperCase())) {
                // V – any letter, one letter amino acid code,
                result.add(p.toUpperCase());
            } else if (p.startsWith("[") && p.endsWith("]")) {
                // […] – one amino acid from bracket
                result.add(p.substring(1, p.length() - 1).toUpperCase());
            } else if (p.startsWith("{") && p.endsWith("}")) {
                // {…} – one amino acid, but not from bracket
                String subtractionResult = listSubtraction(p);
                result.add(subtractionResult);
            } else if (p.contains("(") && p.endsWith(")")) {
                String firstSign = p.substring(0, 1).toUpperCase();
                if (firstSign.equals("X")) {
                    //x(i) or x(i,j)
                    String range = p.substring(2, p.length() - 1);
                    repeat(range, AMINO_ACIDS, result);
                } else if (AMINO_ACIDS.contains(firstSign)) {
                    //e(i) or e(i,j)
                    String range = p.substring(2, p.length() - 1);
                    repeat(range, firstSign, result);
                } else if (firstSign.equals("[")) {
                    // [V](i) or [V](i,j)
                    int bracketId = p.indexOf('(');
                    String sequence = p.substring(1, bracketId - 1);
                    String range = p.substring(bracketId + 1, p.length() - 1);
                    repeat(range, sequence, result);
                } else if (firstSign.equals("{")) {
                    // {V}(i) or {V}(i,j)
                    int bracketId = p.indexOf('(');
                    String sequence = listSubtraction(p.substring(0, bracketId));
                    String range = p.substring(bracketId + 1, p.length() - 1);
                    repeat(range, sequence, result);
                } else {
                    throw new IllegalArgumentException("Wrong pattern: " + patternLiteral);
                }
            } else {
                throw new IllegalArgumentException("Wrong pattern: " + patternLiteral);
            }
        }
        return result;
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

    private static void repeat(String range, String sequence, List<String> result) {
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
//                        String found = sequence.substring(0, foundIndex[0]) + ">" +
//                                sequence.substring(foundIndex[0], foundIndex[1] + 1) + "<" +
//                                sequence.substring(foundIndex[1]+1);
            foundSequences.append(String.format("\\%d-%d: %s\\ ",
                    foundIndex[0], foundIndex[1], found));
        }
        System.out.println(String.format("sequence: %s, pattern: %s, found: %s",
                sequence, patternLiteral, foundSequences.toString()));
    }
}