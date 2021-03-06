package tests;

import org.junit.Assert;
import pg.bio.Main;
import pg.bio.Pattern;
import pg.bio.PatternPart;

import java.io.IOException;
import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;

import static pg.bio.Main.printSequence;
import static pg.bio.Main.readFile;

public class PrositeTest {
    String regexPattern(pg.bio.Pattern pattern) {
        StringBuilder regex = new StringBuilder();
        List<PatternPart> patternParts = pattern.getPatternParts();
        for (PatternPart patternPart : patternParts) {
            String part = patternPart.getPart();
            if(part.length() == 1)
                regex.append(part);
            else
                regex.append("[").append(part).append("]");

            if (patternPart.isOptional())
                regex.append("?");
        }

        return regex.toString();
    }

    @org.junit.Test
    public void testRegexPattern() throws IOException {
        List<String> patternsLiteral = readFile(Main.patternsFile);
        List<pg.bio.Pattern> patterns = Main.makePatterns(patternsLiteral);
        List<String> patternsRegex = new ArrayList<>();
        List<String> sequences = readFile(Main.sequencesFile);
        for (pg.bio.Pattern pattern : patterns) {
            String regexPattern = regexPattern(pattern);
            patternsRegex.add(regexPattern);
//            System.out.println(regexPattern);
        }

        for (String sequence : sequences) {
            Map<pg.bio.Pattern, SortedSet<pg.bio.Pattern.PatternResult>> ret = new HashMap<>();

            for (int i = 0; i < patterns.size(); i++) {
                SortedSet<Pattern.PatternResult> foundIndices = new TreeSet<>();

                SortedSet<Pattern.PatternResult> myFind = patterns.get(i).findInSequence(sequence);

                java.util.regex.Pattern pat = java.util.regex.Pattern.compile(patternsRegex.get(i));
                Matcher matcher = pat.matcher(sequence);
                while (matcher.find()) {
                    MatchResult matchResult = matcher.toMatchResult();
                    Pattern.PatternResult res = new Pattern.PatternResult(matchResult.start(), matchResult.end() - 1);
                    foundIndices.add(res);

                    Assert.assertTrue(myFind.contains(res));
                }

                if (!foundIndices.isEmpty()) {
                    ret.put(patterns.get(i), foundIndices);
                    printSequence(sequence, patternsLiteral.get(i), foundIndices);
                }
            }

            if(ret.isEmpty())
                System.out.println("No pattern found in sequence: " + sequence);
        }
    }


}
