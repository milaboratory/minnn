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
	 * Enter a parse tree produced by {@link FilterGrammarParser#or_operand}.
	 * @param ctx the parse tree
	 */
	void enterOr_operand(FilterGrammarParser.Or_operandContext ctx);
	/**
	 * Exit a parse tree produced by {@link FilterGrammarParser#or_operand}.
	 * @param ctx the parse tree
	 */
	void exitOr_operand(FilterGrammarParser.Or_operandContext ctx);
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
	 * Enter a parse tree produced by {@link FilterGrammarParser#and_operand}.
	 * @param ctx the parse tree
	 */
	void enterAnd_operand(FilterGrammarParser.And_operandContext ctx);
	/**
	 * Exit a parse tree produced by {@link FilterGrammarParser#and_operand}.
	 * @param ctx the parse tree
	 */
	void exitAnd_operand(FilterGrammarParser.And_operandContext ctx);
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
}