/*
 * Copyright (c) 2016-2019, MiLaboratory LLC
 * All Rights Reserved
 *
 * Permission to use, copy, modify and distribute any part of this program for
 * educational, research and non-profit purposes, by non-profit institutions
 * only, without fee, and without a written agreement is hereby granted,
 * provided that the above copyright notice, this paragraph and the following
 * three paragraphs appear in all copies.
 *
 * Those desiring to incorporate this work into commercial products or use for
 * commercial purposes should contact MiLaboratory LLC, which owns exclusive
 * rights for distribution of this program for commercial purposes, using the
 * following email address: licensing@milaboratory.com.
 *
 * IN NO EVENT SHALL THE INVENTORS BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT,
 * SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS,
 * ARISING OUT OF THE USE OF THIS SOFTWARE, EVEN IF THE INVENTORS HAS BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * THE SOFTWARE PROVIDED HEREIN IS ON AN "AS IS" BASIS, AND THE INVENTORS HAS
 * NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR
 * MODIFICATIONS. THE INVENTORS MAKES NO REPRESENTATIONS AND EXTENDS NO
 * WARRANTIES OF ANY KIND, EITHER IMPLIED OR EXPRESS, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY OR FITNESS FOR A
 * PARTICULAR PURPOSE, OR THAT THE USE OF THE SOFTWARE WILL NOT INFRINGE ANY
 * PATENT, TRADEMARK OR OTHER RIGHTS.
 */
package com.milaboratory.minnn.parser;

import com.milaboratory.minnn.pattern.Pattern;

import java.util.stream.IntStream;

public final class Parser {
    public final static int BUILTIN_READ_GROUPS_NUM = 127;
    private final ParserConfiguration conf;

    public Parser(ParserConfiguration conf) {
        // ParserConfiguration is mutable and requires initialization; copying the object to keep the original intact
        this.conf = new ParserConfiguration(conf);
    }

    public Pattern parseQuery(String query) throws ParserException {
        return parseQuery(query, ParserFormat.NORMAL);
    }

    /**
     * Main parser function that transforms query string to Pattern object. It will throw ParserException if something
     * is wrong in the query.
     *
     * @param query     query string
     * @param format    parser format: NORMAL for end users or SIMPLIFIED as toString() output in inner classes
     * @return          Pattern object for specified query string
     */
    public Pattern parseQuery(String query, ParserFormat format) throws ParserException {
        if (query.equals("")) throw new ParserException("Query is empty!");
        TokenizedString tokenizedString = new TokenizedString(query);
        Tokenizer tokenizer;
        if (format == ParserFormat.NORMAL) {
            conf.init(defaultGroupsOverride(query, false));
            tokenizer = new NormalTokenizer(conf);
        } else {
            conf.init(defaultGroupsOverride(query, true));
            tokenizer = new SimplifiedTokenizer(conf);
        }
        tokenizer.tokenize(tokenizedString);
        return tokenizedString.getFinalPattern();
    }

    /**
     * Detect whether there is default group override in the pattern.
     *
     * @param query             pattern query
     * @param simplifiedSyntax  true if it is simplified syntax, otherwise false
     * @return                  true if there is default group override
     */
    private static boolean defaultGroupsOverride(String query, boolean simplifiedSyntax) {
        String strippedQuery = query.replaceAll("\\s+", "");
        return IntStream.rangeClosed(1, BUILTIN_READ_GROUPS_NUM).mapToObj(i -> "R" + i).anyMatch(groupName -> {
            if (simplifiedSyntax)
                return strippedQuery.contains("GroupEdge('" + groupName + "'");
            else
                return strippedQuery.contains("(" + groupName + ":");
        });
    }
}
