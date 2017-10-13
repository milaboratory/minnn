package com.milaboratory.mist.parser;

import com.milaboratory.core.Range;
import com.milaboratory.core.sequence.*;
import com.milaboratory.mist.output_converter.MatchedGroup;
import com.milaboratory.mist.pattern.*;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static com.milaboratory.mist.output_converter.GroupUtils.getGroupsFromMatch;
import static com.milaboratory.mist.parser.ParserUtils.parseMultiTargetString;
import static com.milaboratory.mist.util.CommonTestUtils.*;
import static com.milaboratory.mist.util.CommonTestUtils.RandomStringType.*;
import static org.junit.Assert.*;

public class ParserTest {
    private static final Parser strictParser = new Parser(getTestPatternAligner(true));

    @Test
    public void sampleNormalSyntaxQueriesTest1() throws Exception {
        String[] queries = {
            "N{5}ATTAGACAT{4:6}",                                       // 0
            "ATTA + GACA + A & G + T",                                  // 1
            "ATTA + GACA + [A & G] + T",                                // 2
            "[ATTA][(UMI:GACA) >{2}]",                                  // 3
            " [ [ATTA|| GACA] + [ TTTT ||GGGG]]"                        // 4
        };
        String[] targets = {
            "TAATCATCCATTAGACATTTTTTTA",                                // 0
            "AGTAATTACCGGACATGCAT",                                     // 1
            "TATTAGA",                                                  // 2
            "GACAGGGG"                                                  // 3
        };
        Parser parser = new Parser(getTestPatternAligner(-100, 2, 0,
                -1));
        List<Pattern> patterns = Arrays.stream(queries).map(rethrow(parser::parseQuery)).collect(Collectors.toList());
        List<List<Match>> bestMatches = patterns.stream().map(p -> Arrays.stream(targets)
                .map(t -> p.match(new NSequenceWithQuality(t)).getBestMatch(true))
                .collect(Collectors.toList())).collect(Collectors.toList());
        List<List<Range>> matchedRanges = bestMatches.stream().map((List<Match> p) -> p.stream()
                .map(orNull(Match::getRange)).collect(Collectors.toList())).collect(Collectors.toList());

        // first get() is query number, second is target number
        assertEquals(new Range(4, 23), matchedRanges.get(0).get(0));
        assertEquals(new Range(1, 19), matchedRanges.get(1).get(1));
        assertEquals(new Range(4, 20), matchedRanges.get(2).get(1));
        assertEquals(new Range(1, 7), matchedRanges.get(3).get(2));
        assertEquals(new Range(0, 8), matchedRanges.get(4).get(3));
    }

    @Test
    public void sampleNormalSyntaxQueriesTest2() throws Exception {
        testSample("^<{3}ATTAGACA>>$", "AGA", new Range(0, 3));
        testSample("^<{3}ATTAGACA>$", "AGA", null);
        testSample("^<{3}ATTAGACA>$", "AGAC", new Range(0, 4));
        testSample("^ATTA + GACA", "GATTATTAGACACTA", null);
        testSample("^ATTA + GACA", "ATTATTAGACACTA", new Range(0, 11));
        testSample("GACA$ || ^AT{:2}A", "ATTAGACA", new Range(4, 8));
        testSample("^GACA || ^AT{:2}A", "ATTAGACA", new Range(0, 4));
        testSample("^GACA || AT{:2}A$", "ATTAGACA", null);
        testBadSample("AGTT>>> + [-1.1:<{3}GCAATGC & TTA] + AGC");
        testSample("AGTT>>> + [-1:<{1}GCA{2:2}TGC & T{2:}A] + [0:<{1}AGC]", "TATATTAATCAATGCCCAGCAGC",
                new Range(1, 20));
        testBadSample("^^ATTAGACA");
        testBadSample("ATTAGACA$$");
        testSample("^[^ATTAGACA]", "ATTAGACA", new Range(0, 8));
        testSample("[ATTAGACA$]$", "ATTAGACA", new Range(0, 8));
        testSample("^[ATTAGACA]", "ATTAGACA", new Range(0, 8));
        testSample("[ATTAGACA]$", "ATTAGACA", new Range(0, 8));
        testSample("^[ATTA + GACA]", "ATTAGACA", new Range(0, 8));
        testSample("[ATTA + GACA]$", "ATTAGACA", new Range(0, 8));
        testSample("^[0:[0:ATTA][0:GACA]]", "ATTAGACA", new Range(0, 8));
        testSample("[0:[0:ATTA][0:GACA]]$", "ATTAGACA", new Range(0, 8));
        testSample("^[GACA & ATTA]", "ATTAGACA", new Range(0, 8));
        testSample("[GACA & ATTA]$", "ATTAGACA", new Range(0, 8));
        testSample("^[GACA || ATTA]", "ATTAGACA", new Range(0, 4));
        testSample("[GACA || ATTA]$", "ATTAGACA", new Range(4, 8));
        testSample("^[^ATTAGACA]", "GATTAGACA", null);
        testSample("[ATTAGACA$]$", "ATTAGACAG", null);
        testSample("^[ATTAGACA]", "GATTAGACA", null);
        testSample("[ATTAGACA]$", "ATTAGACAG", null);
        testSample("^[ATTA + GACA]", "GATTAGACA", null);
        testSample("[ATTA + GACA]$", "ATTAGACAG", null);
        testBadSample("^^A{3}");
        testBadSample("A{3}$$");
        testBadSample("^^[A{3}]");
        testBadSample("[A{3}]$$");
        testSample("^[^A{3}]", "TAAA", null);
        testSample("[A{3}$]$", "AAAT", null);
        testBadSample("<[<AAA]");
        testBadSample("<A{3}");
        testSample("[^ATTAGACA]", "ATTAGACAA", new Range(0, 8));
        testSample("[ATTAGACA$]", "GATTAGACA", new Range(1, 9));
        testSample("[0:<{2}^ATTAGACA$>{2}]", "ATTAGACA", new Range(0, 8));
        testSample("[0:^<{2}ATTAGACA>{2}$]", "ATTAGACA", new Range(0, 8));
        testSample("^[0:<{2}ATTAGACA>{2}]$", "ATTAGACA", new Range(0, 8));
        testSample("<{2}(0:^ATTAGACA$)>{2}", "ATTAGACA", new Range(0, 8));
        testSample("<{2}^(0:ATTAGACA)$>{2}", "ATTAGACA", new Range(0, 8));
        testSample("^<{2}(0:ATTAGACA)>{2}$", "ATTAGACA", new Range(0, 8));
        testSample("<{2}(0:^ATTAGACA$)>{2}", "ATTAGACA", new Range(0, 8));
        testBadSample("<{2}[0:^ATTAGACA$]>{2}");
        testBadSample("<{2}^[0:ATTAGACA]$>{2}");
        testBadSample("^<{2}[0:ATTAGACA]>{2}$");
        testBadSample("<{2}[0:^ATTAGACA$]>{2}");
        testSample("[0:<{2}^ATTAGACA]", "ATTAGACA", new Range(0, 8));
        testSample("[0:^<{2}ATTAGACA]", "ATTAGACA", new Range(0, 8));
        testSample("^[0:<{2}ATTAGACA]", "ATTAGACA", new Range(0, 8));
        testBadSample("<{2}[0:^ATTAGACA]");
        testBadSample("<{2}^[0:ATTAGACA]");
        testBadSample("^<{2}[0:ATTAGACA]");
        testBadSample("<{2}[0:^ATTAGACA]");
        testBadSample("[^0:ATTAGACA]");
        testSample("^[ATTAGACA]", "ATTAGACAA", new Range(0, 8));
        testSample("[ATTAGACA]$", "GATTAGACA", new Range(1, 9));
        testSample("[^A{3}]", "AAAT", new Range(0, 3));
        testSample("[A{3}$]", "TAAA", new Range(1, 4));
        testSample("^[A{3}]", "AAAT", new Range(0, 3));
        testSample("[A{3}]$", "TAAA", new Range(1, 4));
        testSample("*", "ATTAGACA", new Range(0, 8));
        testSample("A{*}", "AAAAAAAA", new Range(0, 8));
        testSample("(GROUP:*)", "A", new Range(0, 1));
        testSample("(GROUP:A{*})", "A", new Range(0, 1));
        testSample("[ -1:ATTAGACA]", "ATTAGAC", null);
        testSample("[-1 :ATTA GACA ]", "TTAGACA", null);
        testSample(" [ 0 : [ A T T A ] [ G A C A ] ] ", "ATTAGACA", new Range(0, 8));
        testBadSample("[]");
        testBadSample("[:ATTA]");
        testBadSample("ATTA:GACA");
        testBadSample("(ATTAGACA)");
        testBadSample("");
        testBadSample("A{}");
        testBadSample("(:GACA)");
        testBadSample("(<{}GACA)");
        testBadSample("(<{4:}ATTAGACA)");
        testBadSample("{3}");
        testBadSample(":{3}");
        testBadSample("A{0}");
        testBadSample("A{-8}");
        testBadSample("A{3:2}");
        testBadSample("A*");
        testBadSample("*A");
        testBadSample("**");
        testBadSample("*A*");
        testBadSample("*&*");
        testBadSample("*||*");
        testBadSample("*||A");
        testBadSample("A||*");
        testBadSample("<*");
        testBadSample("A^");
        testSample(" ( TEST1 : [  [  (TEST2:  *  )  ]  ] ) ", "AT", new Range(0, 2));
        testSample("[[<{1}ATTA>+<{1}GACA]>+<AATA$>{1}]&^(X:<GCGC)", "CGCTTACAT", new Range(0, 9));
        testSample("AT><TA", "AA", new Range(0, 2));
        testSample("AT><TA", "ATA", new Range(0, 3));
        testSample("ATTA>><<<GACA", "ATTCA", new Range(0, 5));
        testBadSample("A^$A");
        testBadSample("A+$^A");
        testBadSample("A+^#A");
        testBadSample("^*A");
        testSample("^AT{2}A$", "ATTA", new Range(0, 4));
        testSample("^A{1}TTA{1}$", "ATTA", new Range(0, 4));
        testSample("^<AAT{:2}AA>$", "ATA", new Range(0, 3));
        testBadSample("AAT{2}>");
        testBadSample("AAT{2}$>");
        testBadSample("AAT{2}^>");
        testBadSample("(U MI:AAA)");
        testSample("AT(1:TA + GA)CA", "ATTAGACA", new Range(0, 8));
        testSample("A (1:TT) A || G (1:AC) A", "ATTA", new Range(0, 4));
        testBadSample("AT(1:TA & GA)CA");
        testBadSample("AT(1:TA || GA)CA");
        testBadSample("A(1:TT)A + G(1:AC)A");
        testBadSample("A(1:TT)A & G(1:AC)A");
        testBadSample("[4:N]0");
        testSample("[-7:A]T", "AT", new Range(0, 2));
    }

    @Test
    public void sampleNormalSyntaxQueriesTest3() throws Exception {
        testMultiSample("ATTA \\ GACA", "ATTA GACA", true);
        testMultiSample("AAA || TTT \\ GGG || CCC", "TTT GGG", true);
        testMultiSample("[AAA \\ TTT] || [GGG \\ CCC]", "GGG CCC", true);
        testMultiSample("~AAA", "A", true);
        testMultiSample("AAA + T || ~CCC", "AAACCT", true);
        testMultiSample("~AAA", "AAA", false);
        testMultiSample("~[~AAA]", "AAA", true);
        testBadSample("~~AAA");
        testMultiSample("A + A && C + C", "ACAC", true);
        testMultiSample("GC \\ AA && TT \\ C & ATG", "GCTTGC ATGAAC", true);
        testBadSample("~ATTA \\ ~GACA || ~GC \\ ~[AT + AT] && ~[TTT \\ GCC] && TA & A \\ <<AGACA");
        testMultiSample("~[ATTA \\ GACA] || ~[GC \\ [AT + AT]] && ~[TTT \\ GCC] && TA & A \\ <<AGACA",
                "ATTA GACA", true);
        testMultiSample("[AT || AC \\ GT || GC] || [CT || CC \\ TT || TC]",
                "ATGTCTTT ACGCCCTC", true);
        testMultiSample("<<AT(1:TA)TT \\ G & G\\A && [[TG\\C\\*]||[TC{3}ACC>{2}\\<^TG\\(2:*)]]",
                "TATTCCCA GG A", true);
        testMultiSample("<<AT(1:TA)TT \\ G & G\\A && [[TG\\C\\*]||[TC{3}ACC>{2}\\^TG\\(2:*)]]",
                "TATTCCCA GG A", false);
        testMultiSample("*\\A", "T A", true);
        testMultiSample("*\\* && *\\* && [*\\*] || ~[*\\*]", "A A", true);
        testBadSample("[TT\\AA]||[*||GC\\CG]");
        testMultiSample("[A(1:TT)A \\ G(2:AC)A] || [(1:A) \\ (2:T)]", "A GACA", false);
        testBadSample("[A(1:TT)A \\ G(2:AC)A] && [(1:A) \\ (2:T)]");
        testBadSample("A(1:TT)A \\ G(1:AC)A");
        testBadSample("T~A&&");
        testBadSample("T~[A&&A]");
        testMultiSample("[[^[A]\\T]&&[G$\\C]]", "AG TC", true);
        testMultiSample("[[A\\^T]&&[G\\[C]$]]", "AG TC", true);
        testBadSample("[^[A\\T]&&[G\\C]]");
        testBadSample("[[A\\T]&&[G\\C]$]");
        testBadSample("^[[A\\T]&&[G\\C]]");
        testBadSample("[[A\\T]&&[G\\C]]$");
    }

    @Test
    public void testMatchedGroups() throws Exception {
        ArrayList<MatchedGroup> testGroups1 = getGroupsFromSample(
                "(0:[0:AT(1:T{3}A)])+(2:[<<(3:GAT+C)+T]) || (4:AAA)", "GGAAAATTTTAGATCTATG");
        assertGroupRange(testGroups1, "0", new Range(5, 11));
        assertGroupRange(testGroups1, "1", new Range(7, 11));
        assertGroupRange(testGroups1, "2", new Range(11, 16));
        assertGroupRange(testGroups1, "3", new Range(11, 15));
        assertEquals(0, testGroups1.stream().filter(g -> g.getGroupName().equals("4")).count());

        ArrayList<MatchedGroup> testGroups2 = getGroupsFromSample(
                "(1:[[^[0:[<AA+T]+G]TG>]]<<ATAT(2:G+G+TG+[(3:T+GA>{1})]C)C)$", "AATTGTGTTATGAGATGATAGCC");
        assertGroupRange(testGroups2, "1", new Range(0, 23));
        assertGroupRange(testGroups2, "2", new Range(11, 22));
        assertGroupRange(testGroups2, "3", new Range(18, 21));

        ArrayList<MatchedGroup> testGroups3 = getGroupsFromSample(
                "(1:(2:(3:(4:(5:(6:(7:(8:(9:(10:A))))))))))", "A");
        for (int i = 1; i <= 10; i++)
            assertGroupRange(testGroups3, Integer.toString(i), new Range(0, 1));

        ArrayList<MatchedGroup> testGroups4 = getGroupsFromSample(
                "^<ATTA ( UMI: GACA ) [ ATT + ( G1: GCC ) + TTA ] || ^<AGC(UMI:GC) ATTGAGCC(G2:TTG)GG$",
                "TTAGACAATTATTGTTCTTCGCCGCCTTAT");
        assertGroupRange(testGroups4, "UMI", new Range(3, 7));
        assertGroupRange(testGroups4, "G1", new Range(20, 23));
        assertEquals(0, testGroups4.stream().filter(g -> g.getGroupName().equals("G2")).count());
    }

    @Test
    public void fuzzingTest() throws Exception {
        for (int i = 0; i < 100000; i++) {
            int stringLength = rg.nextInt(30) + 1;
            String randomString = rg.nextBoolean()
                    ? getRandomString(stringLength, "", QUERY_CHARACTERS) : rg.nextBoolean()
                    ? getRandomString(stringLength, "", UNICODE)
                    : getRandomString(stringLength, "", LIMITED);
            try {
                strictParser.parseQuery(randomString);
            } catch (Exception e) {
                if (!e.getClass().equals(ParserException.class)) {
                    System.out.println(randomString + "\n" + e);
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Test
    public void bestMatchTest() throws Exception {
        String query = "AGTT >>>+ [-1:<{1} GCA{2:2}TGC & T{2 :}A] +[0: <{1}AGC] &AGC& [[[[[T ] ] ] ] ] ";
        String target = "TATATTAATCAATGCCCAGCAGC";
        Pattern pattern = strictParser.parseQuery(query);
        assertEquals(target, bestToString(pattern.match(new NSequenceWithQuality(target))));
    }

    private static void testSample(String query, String target, Range expectedRange) throws Exception {
        Pattern pattern = strictParser.parseQuery(query);
        Match bestMatch = pattern.match(new NSequenceWithQuality(target)).getBestMatch(true);
        Range matchedRange = (bestMatch == null) ? null : bestMatch.getRange();
        assertEquals(expectedRange, matchedRange);
    }

    private static void testMultiSample(String query, String multiTarget, boolean mustMatch) throws Exception {
        Pattern pattern = strictParser.parseQuery(query);
        MatchingResult results = pattern.match(parseMultiTargetString(multiTarget));
        assertEquals(mustMatch, results.getBestMatch(true) != null);
    }

    private static void testBadSample(String query) throws Exception {
        assertException(ParserException.class, () -> {
            strictParser.parseQuery(query);
            return null;
        });
    }

    private static ArrayList<MatchedGroup> getGroupsFromSample(String query, String target) throws Exception {
        Pattern pattern = strictParser.parseQuery(query);
        Match bestMatch = pattern.match(new NSequenceWithQuality(target)).getBestMatch(true);
        return getGroupsFromMatch(bestMatch);
    }

    private static void assertGroupRange(List<MatchedGroup> groups, String groupName, Range expectedRange) {
        Range groupRange = groups.stream().filter(g -> g.getGroupName().equals(groupName)).findFirst()
                .orElseThrow(IllegalArgumentException::new).getRange();
        assertEquals(expectedRange, groupRange);
    }
}
