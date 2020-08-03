// Generated from DemultiplexGrammar.g4 by ANTLR 4.7
package com.milaboratory.minnn.cli;
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
	 * Enter a parse tree produced by {@link DemultiplexGrammarParser#demultiplexArgument}.
	 * @param ctx the parse tree
	 */
	void enterDemultiplexArgument(DemultiplexGrammarParser.DemultiplexArgumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link DemultiplexGrammarParser#demultiplexArgument}.
	 * @param ctx the parse tree
	 */
	void exitDemultiplexArgument(DemultiplexGrammarParser.DemultiplexArgumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link DemultiplexGrammarParser#bySample}.
	 * @param ctx the parse tree
	 */
	void enterBySample(DemultiplexGrammarParser.BySampleContext ctx);
	/**
	 * Exit a parse tree produced by {@link DemultiplexGrammarParser#bySample}.
	 * @param ctx the parse tree
	 */
	void exitBySample(DemultiplexGrammarParser.BySampleContext ctx);
	/**
	 * Enter a parse tree produced by {@link DemultiplexGrammarParser#byBarcode}.
	 * @param ctx the parse tree
	 */
	void enterByBarcode(DemultiplexGrammarParser.ByBarcodeContext ctx);
	/**
	 * Exit a parse tree produced by {@link DemultiplexGrammarParser#byBarcode}.
	 * @param ctx the parse tree
	 */
	void exitByBarcode(DemultiplexGrammarParser.ByBarcodeContext ctx);
	/**
	 * Enter a parse tree produced by {@link DemultiplexGrammarParser#inputFileName}.
	 * @param ctx the parse tree
	 */
	void enterInputFileName(DemultiplexGrammarParser.InputFileNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link DemultiplexGrammarParser#inputFileName}.
	 * @param ctx the parse tree
	 */
	void exitInputFileName(DemultiplexGrammarParser.InputFileNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link DemultiplexGrammarParser#fileName}.
	 * @param ctx the parse tree
	 */
	void enterFileName(DemultiplexGrammarParser.FileNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link DemultiplexGrammarParser#fileName}.
	 * @param ctx the parse tree
	 */
	void exitFileName(DemultiplexGrammarParser.FileNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link DemultiplexGrammarParser#barcodeName}.
	 * @param ctx the parse tree
	 */
	void enterBarcodeName(DemultiplexGrammarParser.BarcodeNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link DemultiplexGrammarParser#barcodeName}.
	 * @param ctx the parse tree
	 */
	void exitBarcodeName(DemultiplexGrammarParser.BarcodeNameContext ctx);
}