// Generated from FilterGrammar.g4 by ANTLR 4.7
package com.milaboratory.mist.cli;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link FilterGrammarParser}.
 */
public interface FilterGrammarListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link FilterGrammarParser#filter}.
	 * @param ctx the parse tree
	 */
	void enterFilter(FilterGrammarParser.FilterContext ctx);
	/**
	 * Exit a parse tree produced by {@link FilterGrammarParser#filter}.
	 * @param ctx the parse tree
	 */
	void exitFilter(FilterGrammarParser.FilterContext ctx);
	/**
	 * Enter a parse tree produced by {@link FilterGrammarParser#filterInParentheses}.
	 * @param ctx the parse tree
	 */
	void enterFilterInParentheses(FilterGrammarParser.FilterInParenthesesContext ctx);
	/**
	 * Exit a parse tree produced by {@link FilterGrammarParser#filterInParentheses}.
	 * @param ctx the parse tree
	 */
	void exitFilterInParentheses(FilterGrammarParser.FilterInParenthesesContext ctx);
	/**
	 * Enter a parse tree produced by {@link FilterGrammarParser#anySingleFilter}.
	 * @param ctx the parse tree
	 */
	void enterAnySingleFilter(FilterGrammarParser.AnySingleFilterContext ctx);
	/**
	 * Exit a parse tree produced by {@link FilterGrammarParser#anySingleFilter}.
	 * @param ctx the parse tree
	 */
	void exitAnySingleFilter(FilterGrammarParser.AnySingleFilterContext ctx);
	/**
	 * Enter a parse tree produced by {@link FilterGrammarParser#or}.
	 * @param ctx the parse tree
	 */
	void enterOr(FilterGrammarParser.OrContext ctx);
	/**
	 * Exit a parse tree produced by {@link FilterGrammarParser#or}.
	 * @param ctx the parse tree
	 */
	void exitOr(FilterGrammarParser.OrContext ctx);
	/**
	 * Enter a parse tree produced by {@link FilterGrammarParser#orOperand}.
	 * @param ctx the parse tree
	 */
	void enterOrOperand(FilterGrammarParser.OrOperandContext ctx);
	/**
	 * Exit a parse tree produced by {@link FilterGrammarParser#orOperand}.
	 * @param ctx the parse tree
	 */
	void exitOrOperand(FilterGrammarParser.OrOperandContext ctx);
	/**
	 * Enter a parse tree produced by {@link FilterGrammarParser#and}.
	 * @param ctx the parse tree
	 */
	void enterAnd(FilterGrammarParser.AndContext ctx);
	/**
	 * Exit a parse tree produced by {@link FilterGrammarParser#and}.
	 * @param ctx the parse tree
	 */
	void exitAnd(FilterGrammarParser.AndContext ctx);
	/**
	 * Enter a parse tree produced by {@link FilterGrammarParser#andOperand}.
	 * @param ctx the parse tree
	 */
	void enterAndOperand(FilterGrammarParser.AndOperandContext ctx);
	/**
	 * Exit a parse tree produced by {@link FilterGrammarParser#andOperand}.
	 * @param ctx the parse tree
	 */
	void exitAndOperand(FilterGrammarParser.AndOperandContext ctx);
	/**
	 * Enter a parse tree produced by {@link FilterGrammarParser#pattern}.
	 * @param ctx the parse tree
	 */
	void enterPattern(FilterGrammarParser.PatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link FilterGrammarParser#pattern}.
	 * @param ctx the parse tree
	 */
	void exitPattern(FilterGrammarParser.PatternContext ctx);
	/**
	 * Enter a parse tree produced by {@link FilterGrammarParser#len}.
	 * @param ctx the parse tree
	 */
	void enterLen(FilterGrammarParser.LenContext ctx);
	/**
	 * Exit a parse tree produced by {@link FilterGrammarParser#len}.
	 * @param ctx the parse tree
	 */
	void exitLen(FilterGrammarParser.LenContext ctx);
	/**
	 * Enter a parse tree produced by {@link FilterGrammarParser#patternString}.
	 * @param ctx the parse tree
	 */
	void enterPatternString(FilterGrammarParser.PatternStringContext ctx);
	/**
	 * Exit a parse tree produced by {@link FilterGrammarParser#patternString}.
	 * @param ctx the parse tree
	 */
	void exitPatternString(FilterGrammarParser.PatternStringContext ctx);
	/**
	 * Enter a parse tree produced by {@link FilterGrammarParser#groupName}.
	 * @param ctx the parse tree
	 */
	void enterGroupName(FilterGrammarParser.GroupNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link FilterGrammarParser#groupName}.
	 * @param ctx the parse tree
	 */
	void exitGroupName(FilterGrammarParser.GroupNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link FilterGrammarParser#groupLength}.
	 * @param ctx the parse tree
	 */
	void enterGroupLength(FilterGrammarParser.GroupLengthContext ctx);
	/**
	 * Exit a parse tree produced by {@link FilterGrammarParser#groupLength}.
	 * @param ctx the parse tree
	 */
	void exitGroupLength(FilterGrammarParser.GroupLengthContext ctx);
}