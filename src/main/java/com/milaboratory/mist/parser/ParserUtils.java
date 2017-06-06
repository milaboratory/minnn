package com.milaboratory.mist.parser;

import java.util.*;

import static com.milaboratory.mist.parser.BracketsDetector.getAllBrackets;
import static com.milaboratory.mist.parser.BracketsDetector.getEndByStart;
import static com.milaboratory.mist.parser.BracketsType.*;
import static com.milaboratory.mist.parser.SimplifiedParsers.parseScoreFilter;

final class ParserUtils {
    /**
     * Get position in string right after next semicolon.
     *
     * @param str string to search
     * @param currentPosition current position in str
     * @return position after next semicolon that is after currentPosition
     * @throws ParserException if semicolon not found after currentPosition
     */
    static int getPositionAfterSemicolon(String str, int currentPosition) throws ParserException {
        return 0;
    }

    /**
     * Find areas for fuzzy match patterns in the query. Found areas may contain group edges.
     *
     * @param query query string as it came to the parser
     * @return map of start (inclusive) and end (exclusive) positions for fuzzy match pattern areas
     */
    static HashMap<Integer, Integer> findFuzzyMatchPatterns(String query) throws ParserException {
        return null;
    }

    /**
     * Detect all score thresholds in query string.
     *
     * @param query query string
     * @param format parser format: NORMAL for end users or SIMPLIFIED as toString() output in inner classes
     * @return list of score thresholds
     */
    static ArrayList<ScoreThreshold> getScoreThresholds(String query, ParserFormat format) throws ParserException {
        ArrayList<ScoreThreshold> scoreThresholds = new ArrayList<>();
        switch (format) {
            case NORMAL:
                throw new IllegalStateException("Not yet implemented");
            case SIMPLIFIED:
                final String filterPatternName = "FilterPattern";
                final String scoreFilterName = "ScoreFilter";
                final String simplifiedScoreFilterStart = filterPatternName + "(" + scoreFilterName + "(";
                int minFilterLength = simplifiedScoreFilterStart.length() + 8;
                List<BracketsPair> parentheses = getAllBrackets(PARENTHESES, query);
                for (int currentPosition = 0; currentPosition < query.length() - minFilterLength; currentPosition++)
                    if (query.substring(currentPosition, currentPosition + simplifiedScoreFilterStart.length())
                            .equals(simplifiedScoreFilterStart)) {
                        int startCoordinate = currentPosition + filterPatternName.length();
                        int endCoordinate = getEndByStart(parentheses, startCoordinate);
                        int filterStartCoordinate = startCoordinate + 1;
                        int filterEndCoordinate = getEndByStart(parentheses,
                                currentPosition + simplifiedScoreFilterStart.length() - 1);
                        String scoreFilterSubstring = query.substring(filterStartCoordinate, filterEndCoordinate + 1);
                        int scoreThreshold = parseScoreFilter(scoreFilterSubstring, scoreFilterName + "(").getScoreThreshold();
                        int currentNestedLevel = 0;
                        for (ScoreThreshold currentScoreThreshold : scoreThresholds)
                            if (currentScoreThreshold.contains(startCoordinate, endCoordinate))
                                currentNestedLevel++;
                        scoreThresholds.add(new ScoreThreshold(scoreThreshold, startCoordinate, endCoordinate + 1, currentNestedLevel));
                    }
                return scoreThresholds;
            default:
                throw new IllegalArgumentException("Unknown parser format: " + format);
        }
    }
}
