/*
 * Copyright (c) 2016-2018, MiLaboratory LLC
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
package com.milaboratory.mist.parser;

import com.milaboratory.mist.pattern.Pattern;
import com.milaboratory.mist.pattern.PatternAligner;

public final class Parser {
    private final PatternAligner patternAligner;

    public Parser(PatternAligner patternAligner) {
        this.patternAligner = patternAligner;
    }

    public Pattern parseQuery(String query) throws ParserException {
        return parseQuery(query, ParserFormat.NORMAL);
    }

    /**
     * Main parser function that transforms query string to Pattern object. It will throw ParserException if something
     * is wrong in the query.
     *
     * @param query query string
     * @param format parser format: NORMAL for end users or SIMPLIFIED as toString() output in inner classes
     * @return Pattern object for specified query string
     */
    public Pattern parseQuery(String query, ParserFormat format) throws ParserException {
        if (query.equals("")) throw new ParserException("Query is empty!");
        TokenizedString tokenizedString = new TokenizedString(query);
        Tokenizer tokenizer = (format == ParserFormat.NORMAL) ? new NormalTokenizer(patternAligner)
                : new SimplifiedTokenizer(patternAligner);
        tokenizer.tokenize(tokenizedString);
        return tokenizedString.getFinalPattern();
    }
}
