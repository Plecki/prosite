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

    private static List<String> build_pattern(String pattern) {
        String[] patterns = pattern.split("-");
        List<String> result = new ArrayList<>();

        for (String p: patterns) {
            if (p.equals("x")) {
                // x – any amino acid
                result.add(AMINO_ACIDS);
            } else if (p.length() == 1 && AMINO_ACIDS.contains(p.toUpperCase())) {
                // V – any letter, one letter amino acid code,
                result.add(p.toUpperCase());
            } else if (p.startsWith("[") && p.endsWith("]")) {
                // […] – one amino acid from bracket
                result.add(p.substring(1, p.length()-1).toUpperCase());
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
                    String[] patternSplitted = p.split("\\(");
                    String sequence = patternSplitted[0].substring(1, patternSplitted.length - 1);
                    String range = patternSplitted[1].substring(0, patternSplitted.length - 1);
                    repeat(range, sequence, result);
                } else if (firstSign.equals("{")) {
                    // {V}(i) or {V}(i,j)
                    String[] patternSplitted = p.split("\\(");
                    String sequence = listSubtraction(patternSplitted[0]);
                    String range = patternSplitted[1].substring(0, patternSplitted.length - 1);
                    repeat(range, sequence, result);
                }
            } else {
                return null;
            }
        }
        return result;
    }

    private static int find_pattern(String fragment, List<String> patterns) {
        String fr = fragment.toUpperCase();
        int index = 0;
        int found = -1;
        boolean optional = false;
        for (int i = 0; i < fr.length() ; i++) {
            char c = fr.charAt(i);
            String pattern = patterns.get(index);
            if (pattern.contains("?")) {
                pattern = pattern.substring(1);
                optional = true;
            }
            if (pattern.contains(Character.toString(c))) {
                if (index == 0) {
                    found = i;
                }
                if (index+1 < patterns.size()) {
                    index++;
                } else {
                    return found;
                }
            } else if (index != 0){
                if (optional) {
                    while(true) {
                        if (index + 1 < patterns.size()) {
                            String p = patterns.get(++index);
                            if (!p.contains("?")) {
                                optional = false;
                                break;
                            }
                        } else {
                            return found;
                        }
                    }
                } else {
                    index = 0;
                    found = -1;
                }
            }
        }
        return -1;
    }

    public static void main(String[] args) {
        List<String> patterns = Collections.emptyList();
        List<String> sequences = Collections.emptyList();
        try {
            patterns = Files.readAllLines(Paths.get(".\\src\\pg\\bio\\patterns.txt"), StandardCharsets.UTF_8);
            sequences = Files.readAllLines(Paths.get(".\\src\\pg\\bio\\sequences.txt"), StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        for (String sequence: sequences) {
            for (String pattern: patterns) {
                List<String> p = build_pattern(pattern);
                if (p != null) {
                    int foundIndex = find_pattern(sequence, p);
                    System.out.println("sequence: " + sequence + ", pattern: " + pattern + ", found: " + foundIndex);
                } else {
                    System.out.println("wrong pattern: " + pattern);
                }
            }
        }
    }
}