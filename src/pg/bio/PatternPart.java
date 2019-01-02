package pg.bio;

import java.util.ArrayList;
import java.util.List;

public class PatternPart {
    private String part;
    private boolean isOptional = false;

    PatternPart(String part) {
        checkAminoacidLetters(part);
        this.part = part;
    }

    PatternPart(PatternPart patternPart, boolean isOptional) {
        this(patternPart.part);
        this.isOptional = isOptional;
    }

    PatternPart(PatternPart patternPart) {
        this.part = patternPart.part;
        this.isOptional = patternPart.isOptional;
    }

    static PatternPart buildPatternPart(String patternPartLiteral) {
        String patternPart;
        if (patternPartLiteral.equals("X")) {
            // x – any amino acid
            patternPart = Main.AMINO_ACIDS;
        } else if (patternPartLiteral.length() == 1) {
            // V – any letter, one letter amino acid code,
            patternPart = patternPartLiteral;
        } else if (patternPartLiteral.startsWith("[") && patternPartLiteral.endsWith("]")) {
            // […] – one amino acid from bracket
            patternPart = patternPartLiteral.substring(1, patternPartLiteral.length() - 1);
        } else if (patternPartLiteral.startsWith("{") && patternPartLiteral.endsWith("}")) {
            // {…} – one amino acid, but not from bracket
            patternPart = Main.aminoacidsWithout(patternPartLiteral.substring(1, patternPartLiteral.length() - 1));
        } else {
            throw new IllegalArgumentException("Wrong pattern");
        }
        return new PatternPart(patternPart);
    }

    static List<PatternPart> repeatPatternPart(PatternPart patternPart, String repeats) {
        List<PatternPart> ret = new ArrayList<>();
        if (repeats.contains(",")) {
            // e(i,j) - repetition of e exactly k times, where k≥i and k≤j
            String[] repeatsSplit = repeats.split(",");
            int min = Integer.parseInt(repeatsSplit[0]);
            int max = Integer.parseInt(repeatsSplit[1]);
            for (int i = 0; i < min; i++) {
                ret.add(new PatternPart(patternPart));
            }
            for (int i = 0; i < max - min; i++) {
                PatternPart optionalPart = new PatternPart(patternPart, true);
                ret.add(optionalPart);
            }
        } else {
            // e(i) – for element e and number i: repetition of e exactly i times
            int value = Integer.parseInt(repeats);
            for (int i = 0; i < value; i++) {
                ret.add(patternPart);
            }
        }
        return ret;
    }

    boolean contains(char aminoacid) {
        return part.indexOf(aminoacid) >= 0;
    }

    boolean checkAminoacidLetters(String potentialAminoacids) {
        for (int i = 0; i < potentialAminoacids.length(); i++) {
            char ch = potentialAminoacids.charAt(i);
            if (Main.AMINO_ACIDS.indexOf(ch) < 0)
                throw new IllegalArgumentException(String.format("Not an aminoacid letter: %s, choose one from: '%s'",
                        Character.toString(ch), Main.AMINO_ACIDS));
        }
        return true;
    }

    boolean isOptional() {
        return isOptional;
    }

    @Override
    public String toString() {
        return (isOptional ? "?" : "") + part;
    }
}
