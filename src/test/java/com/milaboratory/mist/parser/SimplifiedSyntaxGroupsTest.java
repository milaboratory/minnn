package com.milaboratory.mist.parser;

import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.mist.pattern.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.*;

import static com.milaboratory.mist.parser.ParserFormat.*;
import static com.milaboratory.mist.parser.SimplifiedSyntaxStrings.*;
import static com.milaboratory.mist.util.CommonTestUtils.*;
import static com.milaboratory.mist.util.CommonTestUtils.RandomStringType.*;

public class SimplifiedSyntaxGroupsTest {
    private final PatternAligner patternAligner = getTestPatternAligner();
    private final Parser parser = new Parser(patternAligner);

    // pattern names that are valid as outer objects for group edges; this list doesn't include NOT_OPERATOR_NAME
    private final List<String> validGroupEdgeOuterObjectNames = Arrays.asList(FUZZY_MATCH_PATTERN_NAME,
            REPEAT_PATTERN_NAME, ANY_PATTERN_NAME, AND_PATTERN_NAME, PLUS_PATTERN_NAME, SEQUENCE_PATTERN_NAME,
            OR_PATTERN_NAME, MULTI_PATTERN_NAME, AND_OPERATOR_NAME, OR_OPERATOR_NAME,
            FILTER_PATTERN_NAME, SCORE_FILTER_NAME, STICK_FILTER_NAME, MULTIPLE_READS_FILTER_PATTERN_NAME);
    // object names that are valid as not common and closest common outer objects for edges of one group
    private final List<String> validGroupPartNotCommonObjectNames = Arrays.asList(FUZZY_MATCH_PATTERN_NAME,
            REPEAT_PATTERN_NAME, PLUS_PATTERN_NAME, SEQUENCE_PATTERN_NAME,
            FILTER_PATTERN_NAME, SCORE_FILTER_NAME, STICK_FILTER_NAME);
    // object names that are valid as closed common ancestor of 2 groups with the same name
    private final List<String> validDuplicateGroupsCommonAncestors = Arrays.asList(OR_PATTERN_NAME, OR_OPERATOR_NAME);

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void correctGroupsTest() throws Exception {
        for (int i = 0; i < 50; i++) {
            PatternAligner patternAligner = getRandomPatternAligner();
            ArrayList<FuzzyMatchPattern> fuzzyMatchPatterns = new ArrayList<>();
            ArrayList<RepeatPattern> repeatPatterns = new ArrayList<>();
            ArrayList<OrPattern> orPatterns = new ArrayList<>();
            ArrayList<MultiPattern> multiPatterns = new ArrayList<>();
            ArrayList<OrOperator> orOperators = new ArrayList<>();
            ArrayList<String> groupCheckerQueries = new ArrayList<>();
            for (int j = 0; j < 3; j++) {
                fuzzyMatchPatterns.add(getRandomFuzzyPattern(patternAligner, true));
                repeatPatterns.add(getRandomRepeatPattern(patternAligner, true));
            }
            for (int j = 0; j < 5; j++) {
                orPatterns.add(new OrPattern(patternAligner, fuzzyMatchPatterns.get(rg.nextInt(3)),
                        getRandomSinglePattern(patternAligner, fuzzyMatchPatterns.get(rg.nextInt(3))),
                        repeatPatterns.get(rg.nextInt(3)),
                        getRandomSinglePattern(patternAligner, repeatPatterns.get(rg.nextInt(3)))));
                multiPatterns.add(new MultiPattern(patternAligner, getRandomSinglePattern(patternAligner,
                        rg.nextBoolean() ? fuzzyMatchPatterns.get(rg.nextInt(3)) : repeatPatterns.get(rg.nextInt(3))),
                        getRandomBasicPattern(patternAligner)));
                orOperators.add(new OrOperator(patternAligner, multiPatterns.get(j),
                        new AndOperator(patternAligner, multiPatterns.get(j)),
                        multiPatterns.get(rg.nextInt(multiPatterns.size()))));

                String randomGroupName = getRandomString(rg.nextInt(10) + 1, "", LETTERS_AND_NUMBERS);
                ArrayList<GroupEdgePosition> groupEdgePositions1 = new ArrayList<>();
                ArrayList<GroupEdgePosition> groupEdgePositions2 = new ArrayList<>();
                groupEdgePositions1.add(new GroupEdgePosition(new GroupEdge(randomGroupName, true), 0));
                groupEdgePositions2.add(new GroupEdgePosition(new GroupEdge(randomGroupName, false), 1));
                FuzzyMatchPattern fuzzyMatchPattern = new FuzzyMatchPattern(patternAligner, new NucleotideSequence("A"),
                        groupEdgePositions1);
                RepeatPattern repeatPattern = new RepeatPattern(patternAligner, new NucleotideSequence("A"),
                        1, 2, groupEdgePositions2);
                PlusPattern plusPattern1 = new PlusPattern(patternAligner, getRandomBasicPattern(patternAligner),
                        new FilterPattern(patternAligner, new ScoreFilter(0), fuzzyMatchPattern),
                        getRandomBasicPattern(patternAligner));
                PlusPattern plusPattern2 = new PlusPattern(patternAligner, getRandomBasicPattern(patternAligner),
                        new FilterPattern(patternAligner, new ScoreFilter(-1), repeatPattern),
                        getRandomBasicPattern(patternAligner));

                groupCheckerQueries.add(new SequencePattern(patternAligner, plusPattern1, plusPattern2).toString());
                groupCheckerQueries.add(orPatterns.get(j).toString());
                groupCheckerQueries.add(orOperators.get(j).toString());
            }
            for (String query : groupCheckerQueries)
                parser.parseQuery(query, SIMPLIFIED);
        }
    }

    @Test
    public void groupEdgeWithoutPairTest() throws Exception {
        exception.expect(ParserException.class);
        parser.parseQuery("FuzzyMatchPattern(A, -1, -1, [GroupEdgePosition(GroupEdge('1', true), 0)])", SIMPLIFIED);
    }

    @Test
    public void groupEdgesWrongOrderTest() throws Exception {
        exception.expect(ParserException.class);
        parser.parseQuery("PlusPattern([FuzzyMatchPattern(GAAGCA, -1, -1, [GroupEdgePosition(" +
                "GroupEdge('UMI', false), 2)]), FuzzyMatchPattern(ATTAGACA, -1, -1, [GroupEdgePosition(" +
                "GroupEdge('UMI', true), 0)])])", SIMPLIFIED);
    }

    @Test
    public void groupEdgePositionsWrongOrderTest() throws Exception {
        exception.expect(ParserException.class);
        parser.parseQuery("FuzzyMatchPattern(GAAGCA, -1, -1, [GroupEdgePosition(" +
                "GroupEdge('UMI', true), 2), GroupEdgePosition(GroupEdge('UMI', false), 0)])", SIMPLIFIED);
    }

    @Test
    public void groupInvalidOuterObjectTest() throws Exception {
        exception.expect(ParserException.class);
        parser.parseQuery("NotOperator(MultiPattern([FuzzyMatchPattern(GAAGCA, -1, -1, [GroupEdgePosition(" +
                "GroupEdge('UMI', true), 2), GroupEdgePosition(GroupEdge('UMI', false), 4)])]))", SIMPLIFIED);
    }

    @Test
    public void groupPartInvalidNonCommonObjectTest() throws Exception {
        String randomGroupName = getRandomString(rg.nextInt(10) + 1, "", LETTERS_AND_NUMBERS);
        ArrayList<GroupEdgePosition> groupEdgePositions1 = new ArrayList<>();
        ArrayList<GroupEdgePosition> groupEdgePositions2 = new ArrayList<>();
        groupEdgePositions1.add(new GroupEdgePosition(new GroupEdge(randomGroupName, true), 0));
        groupEdgePositions2.add(new GroupEdgePosition(new GroupEdge(randomGroupName, false), 1));
        FuzzyMatchPattern fuzzyMatchPattern1 = new FuzzyMatchPattern(getRandomPatternAligner(),
                new NucleotideSequence("A"), groupEdgePositions1);
        FuzzyMatchPattern fuzzyMatchPattern2 = new FuzzyMatchPattern(getRandomPatternAligner(),
                new NucleotideSequence("A"), groupEdgePositions2);
        Pattern invalidPattern = getRandomPatternNotInList(validGroupPartNotCommonObjectNames,
                fuzzyMatchPattern1, fuzzyMatchPattern2);
        exception.expect(ParserException.class);
        parser.parseQuery(invalidPattern.toString(), SIMPLIFIED);
    }

    @Test
    public void groupsWithSameNameTest() throws Exception {
        repeatAndExpectExceptionEveryTime(100, ParserException.class, () -> {
            SinglePattern basicPattern = getRandomBasicPattern(true);
            ArrayList<String> excludePatterns = new ArrayList<>(validDuplicateGroupsCommonAncestors);
            excludePatterns.addAll(Arrays.asList(FUZZY_MATCH_PATTERN_NAME, REPEAT_PATTERN_NAME, FILTER_PATTERN_NAME,
                    MULTIPLE_READS_FILTER_PATTERN_NAME));
            Pattern invalidPattern = getRandomPatternNotInList(excludePatterns, getRandomBasicPattern());
            String invalidPatternString = invalidPattern.toString().split("\\(")[0] + "([" + basicPattern.toString()
                    + ", " + basicPattern.toString() + "])";
            // this ParseQuery must always throw ParserException
            parser.parseQuery(invalidPatternString, SIMPLIFIED);
            return null;
        });
    }

    private Pattern getRandomPatternNotInList(List<String> list, SinglePattern... basicPatterns) {
        ArrayList<String> croppedList = new ArrayList<>(validGroupEdgeOuterObjectNames);
        croppedList.removeAll(list);
        Pattern resultPattern = null;
        String resultPatternName = "";
        do {
            try {
                resultPattern = rg.nextBoolean() ? getRandomSinglePattern(basicPatterns)
                        : getRandomMultiReadPattern(singleToMultiPatterns(basicPatterns));
            } catch (IllegalStateException e) {
                continue;
            }
            resultPatternName = resultPattern.toString().split("\\(")[0];
        } while (!croppedList.contains(resultPatternName));
        return resultPattern;
    }
}
