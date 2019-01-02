package pg.bio;

import java.util.*;
import java.util.stream.Collectors;

public class Pattern {
    private List<PatternPart> patternParts;

    public Pattern() {
        this.patternParts = new ArrayList<>();
    }

    public Pattern(List<PatternPart> patternParts) {
        this.patternParts = patternParts;
    }

    public Pattern(Pattern pattern) {
        patternParts = new ArrayList<>(pattern.patternParts);
    }

    public Pattern(String patternLiteral) {
        this.patternParts = buildPatternFromLiteral(patternLiteral);
    }

    public SortedSet<PatternResult> findInSequence(String sequence) {
        SortedSet<PatternResult> patternIds = new TreeSet<>();

        for (int fromIndex = 0; fromIndex < sequence.length(); fromIndex++) {
            patternIds.addAll(this.findPatternsFromIndex(sequence, fromIndex));
        }
        return patternIds;
    }

    public List<PatternResult> findPatternsFromIndex(String sequence, int fromIndex) {
        if (patternParts.isEmpty())
            return Collections.singletonList(new PatternResult(fromIndex - 1, fromIndex - 1));

        List<PatternResult> ret = new ArrayList<>();
        String seq = sequence.toUpperCase();
        int patternId = 0;
        int foundIdStart = -1;
        for (int sequenceId = fromIndex; sequenceId < seq.length(); sequenceId++) {
            PatternPart patternPart = patternParts.get(patternId);
            boolean optional = patternPart.isOptional();

            char c = seq.charAt(sequenceId);
            if (patternPart.contains(c)) {  // pattern part found
                if (foundIdStart == -1) {
                    foundIdStart = sequenceId;
                }

                if (optional) {
                    // try finding pattern without the use of optional pattern part
                    List<PatternResult> patternsWithoutOptional = this.subList(patternId + 1)
                            .findPatternsFromIndex(sequence, sequenceId);

                    for (PatternResult patternWoutOpt : patternsWithoutOptional) {
                        patternWoutOpt.start = foundIdStart;
                        ret.add(patternWoutOpt);
                    }
                }

                patternId++;
            } else if (optional) {  // pattern part not found, but it's optional so it's ok
                // next time check the same character with next patternPart
                patternId++;
                sequenceId--;
            } else { // pattern part not found
                return ret;
            }

            if (patternId >= patternParts.size()) { // whole pattern found
                ret.add(new PatternResult(foundIdStart, sequenceId));
                return ret;
            }
        }

        // if the sequence ended, but all remaining patterns are optional
        for (int i = patternId; i < patternParts.size(); i++) {
            PatternPart part = patternParts.get(i);
            if (!part.isOptional())
                return ret;
        }

        ret.add(new PatternResult(foundIdStart, sequence.length() - 1));
        return ret;
    }

    public static List<PatternPart> buildPatternFromLiteral(String patternLiteral) throws IllegalArgumentException {
        String[] patternPartsLiteral = patternLiteral.split("-");
        List<PatternPart> result = new ArrayList<>();

        for (String pattern : patternPartsLiteral) {
            String aminoacids;
            String repeats = "1";

            if (pattern.contains("(") && pattern.endsWith(")")) {
                int bracketId = pattern.indexOf('(');
                aminoacids = pattern.substring(0, bracketId);
                repeats = pattern.substring(bracketId + 1, pattern.length() - 1);
            } else
                aminoacids = pattern;

            aminoacids = aminoacids.toUpperCase();

            try {
                List<PatternPart> patternParts = PatternPart.buildPatternParts(aminoacids, repeats);
                result.addAll(patternParts);
            } catch (Exception e) {
                throw new IllegalArgumentException("Wrong pattern: " + patternLiteral);
            }
        }
        return result;
    }

    public List<PatternPart> getPatternParts() {
        return patternParts;
    }

    public Pattern subList(int fromIndex) {
        return new Pattern(patternParts.subList(fromIndex, patternParts.size()));
    }

    @Override
    public String toString() {
        List<String> parts = patternParts.stream().map(PatternPart::toString).collect(Collectors.toList());
        return String.join("-", parts);
    }


    public static class PatternResult implements Comparable {
        int start;
        int end;

        public PatternResult(int start, int end) {
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
