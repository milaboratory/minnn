// Generated from DemultiplexGrammar.g4 by ANTLR 4.7
package com.milaboratory.mist.cli;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link DemultiplexGrammarParser}.
 */
public interface DemultiplexGrammarListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link DemultiplexGrammarParser#demultiplexArguments}.
	 * @param ctx the parse tree
	 */
	void enterDemultiplexArguments(DemultiplexGrammarParser.DemultiplexArgumentsContext ctx);
	/**
	 * Exit a parse tree produced by {@link DemultiplexGrammarParser#demultiplexArguments}.
	 * @param ctx the parse tree
	 */
	void exitDemultiplexArguments(DemultiplexGrammarParser.DemultiplexArgumentsContext ctx);
	/**
	 * Enter a parse tree produced by {@link DemultiplexGrammarParser#filter}.
	 * @param ctx the parse tree
	 */
	void enterFilter(DemultiplexGrammarParser.FilterContext ctx);
	/**
	 * Exit a parse tree produced by {@link DemultiplexGrammarParser#filter}.
	 * @param ctx the parse tree
	 */
	void exitFilter(DemultiplexGrammarParser.FilterContext ctx);
	/**
	 * Enter a parse tree produced by {@link DemultiplexGrammarParser#filterInParentheses}.
	 * @param ctx the parse tree
	 */
	void enterFilterInParentheses(DemultiplexGrammarParser.FilterInParenthesesContext ctx);
	/**
	 * Exit a parse tree produced by {@link DemultiplexGrammarParser#filterInParentheses}.
	 * @param ctx the parse tree
	 */
	void exitFilterInParentheses(DemultiplexGrammarParser.FilterInParenthesesContext ctx);
	/**
	 * Enter a parse tree produced by {@link DemultiplexGrammarParser#anySingleFilter}.
	 * @param ctx the parse tree
	 */
	void enterAnySingleFilter(DemultiplexGrammarParser.AnySingleFilterContext ctx);
	/**
	 * Exit a parse tree produced by {@link DemultiplexGrammarParser#anySingleFilter}.
	 * @param ctx the parse tree
	 */
	void exitAnySingleFilter(DemultiplexGrammarParser.AnySingleFilterContext ctx);
	/**
	 * Enter a parse tree produced by {@link DemultiplexGrammarParser#or}.
	 * @param ctx the parse tree
	 */
	void enterOr(DemultiplexGrammarParser.OrContext ctx);
	/**
	 * Exit a parse tree produced by {@link DemultiplexGrammarParser#or}.
	 * @param ctx the parse tree
	 */
	void exitOr(DemultiplexGrammarParser.OrContext ctx);
	/**
	 * Enter a parse tree produced by {@link DemultiplexGrammarParser#orOperand}.
	 * @param ctx the parse tree
	 */
	void enterOrOperand(DemultiplexGrammarParser.OrOperandContext ctx);
	/**
	 * Exit a parse tree produced by {@link DemultiplexGrammarParser#orOperand}.
	 * @param ctx the parse tree
	 */
	void exitOrOperand(DemultiplexGrammarParser.OrOperandContext ctx);
	/**
	 * Enter a parse tree produced by {@link DemultiplexGrammarParser#and}.
	 * @param ctx the parse tree
	 */
	void enterAnd(DemultiplexGrammarParser.AndContext ctx);
	/**
	 * Exit a parse tree produced by {@link DemultiplexGrammarParser#and}.
	 * @param ctx the parse tree
	 */
	void exitAnd(DemultiplexGrammarParser.AndContext ctx);
	/**
	 * Enter a parse tree produced by {@link DemultiplexGrammarParser#andOperand}.
	 * @param ctx the parse tree
	 */
	void enterAndOperand(DemultiplexGrammarParser.AndOperandContext ctx);
	/**
	 * Exit a parse tree produced by {@link DemultiplexGrammarParser#andOperand}.
	 * @param ctx the parse tree
	 */
	void exitAndOperand(DemultiplexGrammarParser.AndOperandContext ctx);
	/**
	 * Enter a parse tree produced by {@link DemultiplexGrammarParser#pattern}.
	 * @param ctx the parse tree
	 */
	void enterPattern(DemultiplexGrammarParser.PatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link DemultiplexGrammarParser#pattern}.
	 * @param ctx the parse tree
	 */
	void exitPattern(DemultiplexGrammarParser.PatternContext ctx);
	/**
	 * Enter a parse tree produced by {@link DemultiplexGrammarParser#minConsensusReads}.
	 * @param ctx the parse tree
	 */
	void enterMinConsensusReads(DemultiplexGrammarParser.MinConsensusReadsContext ctx);
	/**
	 * Exit a parse tree produced by {@link DemultiplexGrammarParser#minConsensusReads}.
	 * @param ctx the parse tree
	 */
	void exitMinConsensusReads(DemultiplexGrammarParser.MinConsensusReadsContext ctx);
	/**
	 * Enter a parse tree produced by {@link DemultiplexGrammarParser#len}.
	 * @param ctx the parse tree
	 */
	void enterLen(DemultiplexGrammarParser.LenContext ctx);
	/**
	 * Exit a parse tree produced by {@link DemultiplexGrammarParser#len}.
	 * @param ctx the parse tree
	 */
	void exitLen(DemultiplexGrammarParser.LenContext ctx);
	/**
	 * Enter a parse tree produced by {@link DemultiplexGrammarParser#patternString}.
	 * @param ctx the parse tree
	 */
	void enterPatternString(DemultiplexGrammarParser.PatternStringContext ctx);
	/**
	 * Exit a parse tree produced by {@link DemultiplexGrammarParser#patternString}.
	 * @param ctx the parse tree
	 */
	void exitPatternString(DemultiplexGrammarParser.PatternStringContext ctx);
	/**
	 * Enter a parse tree produced by {@link DemultiplexGrammarParser#groupName}.
	 * @param ctx the parse tree
	 */
	void enterGroupName(DemultiplexGrammarParser.GroupNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link DemultiplexGrammarParser#groupName}.
	 * @param ctx the parse tree
	 */
	void exitGroupName(DemultiplexGrammarParser.GroupNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link DemultiplexGrammarParser#minConsensusReadsNum}.
	 * @param ctx the parse tree
	 */
	void enterMinConsensusReadsNum(DemultiplexGrammarParser.MinConsensusReadsNumContext ctx);
	/**
	 * Exit a parse tree produced by {@link DemultiplexGrammarParser#minConsensusReadsNum}.
	 * @param ctx the parse tree
	 */
	void exitMinConsensusReadsNum(DemultiplexGrammarParser.MinConsensusReadsNumContext ctx);
	/**
	 * Enter a parse tree produced by {@link DemultiplexGrammarParser#groupLength}.
	 * @param ctx the parse tree
	 */
	void enterGroupLength(DemultiplexGrammarParser.GroupLengthContext ctx);
	/**
	 * Exit a parse tree produced by {@link DemultiplexGrammarParser#groupLength}.
	 * @param ctx the parse tree
	 */
	void exitGroupLength(DemultiplexGrammarParser.GroupLengthContext ctx);
}