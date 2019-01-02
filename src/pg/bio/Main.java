package pg.bio;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Main {
    public static final String AMINO_ACIDS = "ARNDCEQGHILKMFPSTWYV";
    public static String patternsFile = ".\\src\\pg\\bio\\patterns.txt";
    public static final String sequencesFile = ".\\src\\pg\\bio\\sequences.txt";

    public static void main(String[] args) {
        List<String> patternsLiteral = Collections.emptyList();
        List<String> sequences = Collections.emptyList();
        try {
            patternsLiteral = readFile(patternsFile);
            sequences = readFile(sequencesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<Pattern> patterns = makePatterns(patternsLiteral);

        for (String sequence : sequences) {
            Map<Pattern, SortedSet<Pattern.PatternResult>> patternsInSequence =
                    findPatternsInSequence(sequence, patternsLiteral, patterns);

            if(patternsInSequence.isEmpty())
                System.out.println("No pattern found in sequence: " + sequence);
        }
    }

    public static Map<Pattern, SortedSet<Pattern.PatternResult>>
    findPatternsInSequence(String sequence, List<String> patternsLiteral, List<Pattern> patterns) {
        Map<Pattern, SortedSet<Pattern.PatternResult>> ret = new HashMap<>();

        for (int i = 0; i < patterns.size(); i++) {
            String patternLiteral = patternsLiteral.get(i);
            Pattern pattern = patterns.get(i);

            SortedSet<Pattern.PatternResult> foundIndices = pattern.findInSequence(sequence);
            if (!foundIndices.isEmpty()) {
                ret.put(pattern, foundIndices);
                printSequence(sequence, patternLiteral, foundIndices);
            }
        }

        return ret;
    }

    public static List<String> readFile(String path) throws IOException {
        return Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8);
    }

    public static List<Pattern> makePatterns(List<String> patternsLiteral) {
        List<Pattern> patterns = new ArrayList<>();
        for (String patternLiteral : patternsLiteral) {
            Pattern pattern = new Pattern(patternLiteral);
            patterns.add(pattern);
            System.out.println(patternLiteral + ": " + pattern);
        }
        return patterns;
    }


    public static String aminoacidsWithout(String forbiddenAminoacids) {
        List<Character> aminoAcidsChars = AMINO_ACIDS.chars().mapToObj(i -> (char) i).collect(Collectors.toList());
        List<Character> patternChars = forbiddenAminoacids
                .toUpperCase().chars().mapToObj(i -> (char) i).collect(Collectors.toList());
        aminoAcidsChars.removeAll(patternChars);
        return aminoAcidsChars.stream().map(String::valueOf).collect(Collectors.joining());
    }

    public static void printSequence(String sequence, String patternLiteral, SortedSet<Pattern.PatternResult> foundIndices) {
        StringBuilder foundSequences = new StringBuilder();
        for (Pattern.PatternResult foundIndex : foundIndices) {
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


}