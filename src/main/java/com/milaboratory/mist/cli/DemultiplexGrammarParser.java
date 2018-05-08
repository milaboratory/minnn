// Generated from DemultiplexGrammar.g4 by ANTLR 4.7
package com.milaboratory.mist.cli;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class DemultiplexGrammarParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.7", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		SINGLE_QUOTE=1, STRING=2, MIN_CONSENSUS_READS=3, LEN=4, NUMBER=5, GROUP_NAME=6, 
		OPEN_PARENTHESIS=7, CLOSED_PARENTHESIS=8, EQUALS=9, TILDE=10, AND=11, 
		OR=12, WS=13;
	public static final int
		RULE_demultiplexArguments = 0, RULE_filter = 1, RULE_filterInParentheses = 2, 
		RULE_anySingleFilter = 3, RULE_or = 4, RULE_orOperand = 5, RULE_and = 6, 
		RULE_andOperand = 7, RULE_pattern = 8, RULE_minConsensusReads = 9, RULE_len = 10, 
		RULE_patternString = 11, RULE_groupName = 12, RULE_minConsensusReadsNum = 13, 
		RULE_groupLength = 14;
	public static final String[] ruleNames = {
		"demultiplexArguments", "filter", "filterInParentheses", "anySingleFilter", 
		"or", "orOperand", "and", "andOperand", "pattern", "minConsensusReads", 
		"len", "patternString", "groupName", "minConsensusReadsNum", "groupLength"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "'''", null, "'MinConsensusReads'", "'Len'", null, null, "'('", 
		"')'", "'='", "'~'", "'&'", "'|'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "SINGLE_QUOTE", "STRING", "MIN_CONSENSUS_READS", "LEN", "NUMBER", 
		"GROUP_NAME", "OPEN_PARENTHESIS", "CLOSED_PARENTHESIS", "EQUALS", "TILDE", 
		"AND", "OR", "WS"
	};
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "DemultiplexGrammar.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public DemultiplexGrammarParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}
	public static class DemultiplexArgumentsContext extends ParserRuleContext {
		public FilterContext filter() {
			return getRuleContext(FilterContext.class,0);
		}
		public DemultiplexArgumentsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_demultiplexArguments; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DemultiplexGrammarListener ) ((DemultiplexGrammarListener)listener).enterDemultiplexArguments(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DemultiplexGrammarListener ) ((DemultiplexGrammarListener)listener).exitDemultiplexArguments(this);
		}
	}

	public final DemultiplexArgumentsContext demultiplexArguments() throws RecognitionException {
		DemultiplexArgumentsContext _localctx = new DemultiplexArgumentsContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_demultiplexArguments);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(30);
			filter();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FilterContext extends ParserRuleContext {
		public FilterInParenthesesContext filterInParentheses() {
			return getRuleContext(FilterInParenthesesContext.class,0);
		}
		public AnySingleFilterContext anySingleFilter() {
			return getRuleContext(AnySingleFilterContext.class,0);
		}
		public FilterContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_filter; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DemultiplexGrammarListener ) ((DemultiplexGrammarListener)listener).enterFilter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DemultiplexGrammarListener ) ((DemultiplexGrammarListener)listener).exitFilter(this);
		}
	}

	public final FilterContext filter() throws RecognitionException {
		FilterContext _localctx = new FilterContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_filter);
		try {
			setState(34);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,0,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(32);
				filterInParentheses();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(33);
				anySingleFilter();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FilterInParenthesesContext extends ParserRuleContext {
		public TerminalNode OPEN_PARENTHESIS() { return getToken(DemultiplexGrammarParser.OPEN_PARENTHESIS, 0); }
		public AnySingleFilterContext anySingleFilter() {
			return getRuleContext(AnySingleFilterContext.class,0);
		}
		public TerminalNode CLOSED_PARENTHESIS() { return getToken(DemultiplexGrammarParser.CLOSED_PARENTHESIS, 0); }
		public FilterInParenthesesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_filterInParentheses; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DemultiplexGrammarListener ) ((DemultiplexGrammarListener)listener).enterFilterInParentheses(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DemultiplexGrammarListener ) ((DemultiplexGrammarListener)listener).exitFilterInParentheses(this);
		}
	}

	public final FilterInParenthesesContext filterInParentheses() throws RecognitionException {
		FilterInParenthesesContext _localctx = new FilterInParenthesesContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_filterInParentheses);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(36);
			match(OPEN_PARENTHESIS);
			setState(37);
			anySingleFilter();
			setState(38);
			match(CLOSED_PARENTHESIS);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AnySingleFilterContext extends ParserRuleContext {
		public OrContext or() {
			return getRuleContext(OrContext.class,0);
		}
		public AndContext and() {
			return getRuleContext(AndContext.class,0);
		}
		public PatternContext pattern() {
			return getRuleContext(PatternContext.class,0);
		}
		public MinConsensusReadsContext minConsensusReads() {
			return getRuleContext(MinConsensusReadsContext.class,0);
		}
		public LenContext len() {
			return getRuleContext(LenContext.class,0);
		}
		public AnySingleFilterContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_anySingleFilter; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DemultiplexGrammarListener ) ((DemultiplexGrammarListener)listener).enterAnySingleFilter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DemultiplexGrammarListener ) ((DemultiplexGrammarListener)listener).exitAnySingleFilter(this);
		}
	}

	public final AnySingleFilterContext anySingleFilter() throws RecognitionException {
		AnySingleFilterContext _localctx = new AnySingleFilterContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_anySingleFilter);
		try {
			setState(45);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,1,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(40);
				or();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(41);
				and();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(42);
				pattern();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(43);
				minConsensusReads();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(44);
				len();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class OrContext extends ParserRuleContext {
		public List<OrOperandContext> orOperand() {
			return getRuleContexts(OrOperandContext.class);
		}
		public OrOperandContext orOperand(int i) {
			return getRuleContext(OrOperandContext.class,i);
		}
		public List<TerminalNode> OR() { return getTokens(DemultiplexGrammarParser.OR); }
		public TerminalNode OR(int i) {
			return getToken(DemultiplexGrammarParser.OR, i);
		}
		public OrContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_or; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DemultiplexGrammarListener ) ((DemultiplexGrammarListener)listener).enterOr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DemultiplexGrammarListener ) ((DemultiplexGrammarListener)listener).exitOr(this);
		}
	}

	public final OrContext or() throws RecognitionException {
		OrContext _localctx = new OrContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_or);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(47);
			orOperand();
			setState(48);
			match(OR);
			setState(49);
			orOperand();
			setState(54);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==OR) {
				{
				{
				setState(50);
				match(OR);
				setState(51);
				orOperand();
				}
				}
				setState(56);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class OrOperandContext extends ParserRuleContext {
		public AndContext and() {
			return getRuleContext(AndContext.class,0);
		}
		public PatternContext pattern() {
			return getRuleContext(PatternContext.class,0);
		}
		public MinConsensusReadsContext minConsensusReads() {
			return getRuleContext(MinConsensusReadsContext.class,0);
		}
		public LenContext len() {
			return getRuleContext(LenContext.class,0);
		}
		public FilterInParenthesesContext filterInParentheses() {
			return getRuleContext(FilterInParenthesesContext.class,0);
		}
		public OrOperandContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_orOperand; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DemultiplexGrammarListener ) ((DemultiplexGrammarListener)listener).enterOrOperand(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DemultiplexGrammarListener ) ((DemultiplexGrammarListener)listener).exitOrOperand(this);
		}
	}

	public final OrOperandContext orOperand() throws RecognitionException {
		OrOperandContext _localctx = new OrOperandContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_orOperand);
		try {
			setState(62);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,3,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(57);
				and();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(58);
				pattern();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(59);
				minConsensusReads();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(60);
				len();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(61);
				filterInParentheses();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AndContext extends ParserRuleContext {
		public List<AndOperandContext> andOperand() {
			return getRuleContexts(AndOperandContext.class);
		}
		public AndOperandContext andOperand(int i) {
			return getRuleContext(AndOperandContext.class,i);
		}
		public List<TerminalNode> AND() { return getTokens(DemultiplexGrammarParser.AND); }
		public TerminalNode AND(int i) {
			return getToken(DemultiplexGrammarParser.AND, i);
		}
		public AndContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_and; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DemultiplexGrammarListener ) ((DemultiplexGrammarListener)listener).enterAnd(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DemultiplexGrammarListener ) ((DemultiplexGrammarListener)listener).exitAnd(this);
		}
	}

	public final AndContext and() throws RecognitionException {
		AndContext _localctx = new AndContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_and);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(64);
			andOperand();
			setState(65);
			match(AND);
			setState(66);
			andOperand();
			setState(71);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AND) {
				{
				{
				setState(67);
				match(AND);
				setState(68);
				andOperand();
				}
				}
				setState(73);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AndOperandContext extends ParserRuleContext {
		public PatternContext pattern() {
			return getRuleContext(PatternContext.class,0);
		}
		public MinConsensusReadsContext minConsensusReads() {
			return getRuleContext(MinConsensusReadsContext.class,0);
		}
		public LenContext len() {
			return getRuleContext(LenContext.class,0);
		}
		public FilterInParenthesesContext filterInParentheses() {
			return getRuleContext(FilterInParenthesesContext.class,0);
		}
		public AndOperandContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_andOperand; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DemultiplexGrammarListener ) ((DemultiplexGrammarListener)listener).enterAndOperand(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DemultiplexGrammarListener ) ((DemultiplexGrammarListener)listener).exitAndOperand(this);
		}
	}

	public final AndOperandContext andOperand() throws RecognitionException {
		AndOperandContext _localctx = new AndOperandContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_andOperand);
		try {
			setState(78);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case GROUP_NAME:
				enterOuterAlt(_localctx, 1);
				{
				setState(74);
				pattern();
				}
				break;
			case MIN_CONSENSUS_READS:
				enterOuterAlt(_localctx, 2);
				{
				setState(75);
				minConsensusReads();
				}
				break;
			case LEN:
				enterOuterAlt(_localctx, 3);
				{
				setState(76);
				len();
				}
				break;
			case OPEN_PARENTHESIS:
				enterOuterAlt(_localctx, 4);
				{
				setState(77);
				filterInParentheses();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PatternContext extends ParserRuleContext {
		public GroupNameContext groupName() {
			return getRuleContext(GroupNameContext.class,0);
		}
		public TerminalNode TILDE() { return getToken(DemultiplexGrammarParser.TILDE, 0); }
		public PatternStringContext patternString() {
			return getRuleContext(PatternStringContext.class,0);
		}
		public PatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DemultiplexGrammarListener ) ((DemultiplexGrammarListener)listener).enterPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DemultiplexGrammarListener ) ((DemultiplexGrammarListener)listener).exitPattern(this);
		}
	}

	public final PatternContext pattern() throws RecognitionException {
		PatternContext _localctx = new PatternContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_pattern);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(80);
			groupName();
			setState(81);
			match(TILDE);
			setState(82);
			patternString();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class MinConsensusReadsContext extends ParserRuleContext {
		public TerminalNode MIN_CONSENSUS_READS() { return getToken(DemultiplexGrammarParser.MIN_CONSENSUS_READS, 0); }
		public TerminalNode EQUALS() { return getToken(DemultiplexGrammarParser.EQUALS, 0); }
		public MinConsensusReadsNumContext minConsensusReadsNum() {
			return getRuleContext(MinConsensusReadsNumContext.class,0);
		}
		public MinConsensusReadsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_minConsensusReads; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DemultiplexGrammarListener ) ((DemultiplexGrammarListener)listener).enterMinConsensusReads(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DemultiplexGrammarListener ) ((DemultiplexGrammarListener)listener).exitMinConsensusReads(this);
		}
	}

	public final MinConsensusReadsContext minConsensusReads() throws RecognitionException {
		MinConsensusReadsContext _localctx = new MinConsensusReadsContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_minConsensusReads);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(84);
			match(MIN_CONSENSUS_READS);
			setState(85);
			match(EQUALS);
			setState(86);
			minConsensusReadsNum();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class LenContext extends ParserRuleContext {
		public TerminalNode LEN() { return getToken(DemultiplexGrammarParser.LEN, 0); }
		public TerminalNode OPEN_PARENTHESIS() { return getToken(DemultiplexGrammarParser.OPEN_PARENTHESIS, 0); }
		public GroupNameContext groupName() {
			return getRuleContext(GroupNameContext.class,0);
		}
		public TerminalNode CLOSED_PARENTHESIS() { return getToken(DemultiplexGrammarParser.CLOSED_PARENTHESIS, 0); }
		public TerminalNode EQUALS() { return getToken(DemultiplexGrammarParser.EQUALS, 0); }
		public GroupLengthContext groupLength() {
			return getRuleContext(GroupLengthContext.class,0);
		}
		public LenContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_len; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DemultiplexGrammarListener ) ((DemultiplexGrammarListener)listener).enterLen(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DemultiplexGrammarListener ) ((DemultiplexGrammarListener)listener).exitLen(this);
		}
	}

	public final LenContext len() throws RecognitionException {
		LenContext _localctx = new LenContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_len);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(88);
			match(LEN);
			setState(89);
			match(OPEN_PARENTHESIS);
			setState(90);
			groupName();
			setState(91);
			match(CLOSED_PARENTHESIS);
			setState(92);
			match(EQUALS);
			setState(93);
			groupLength();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PatternStringContext extends ParserRuleContext {
		public TerminalNode STRING() { return getToken(DemultiplexGrammarParser.STRING, 0); }
		public PatternStringContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_patternString; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DemultiplexGrammarListener ) ((DemultiplexGrammarListener)listener).enterPatternString(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DemultiplexGrammarListener ) ((DemultiplexGrammarListener)listener).exitPatternString(this);
		}
	}

	public final PatternStringContext patternString() throws RecognitionException {
		PatternStringContext _localctx = new PatternStringContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_patternString);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(95);
			match(STRING);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class GroupNameContext extends ParserRuleContext {
		public TerminalNode GROUP_NAME() { return getToken(DemultiplexGrammarParser.GROUP_NAME, 0); }
		public GroupNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_groupName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DemultiplexGrammarListener ) ((DemultiplexGrammarListener)listener).enterGroupName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DemultiplexGrammarListener ) ((DemultiplexGrammarListener)listener).exitGroupName(this);
		}
	}

	public final GroupNameContext groupName() throws RecognitionException {
		GroupNameContext _localctx = new GroupNameContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_groupName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(97);
			match(GROUP_NAME);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class MinConsensusReadsNumContext extends ParserRuleContext {
		public TerminalNode NUMBER() { return getToken(DemultiplexGrammarParser.NUMBER, 0); }
		public MinConsensusReadsNumContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_minConsensusReadsNum; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DemultiplexGrammarListener ) ((DemultiplexGrammarListener)listener).enterMinConsensusReadsNum(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DemultiplexGrammarListener ) ((DemultiplexGrammarListener)listener).exitMinConsensusReadsNum(this);
		}
	}

	public final MinConsensusReadsNumContext minConsensusReadsNum() throws RecognitionException {
		MinConsensusReadsNumContext _localctx = new MinConsensusReadsNumContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_minConsensusReadsNum);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(99);
			match(NUMBER);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class GroupLengthContext extends ParserRuleContext {
		public TerminalNode NUMBER() { return getToken(DemultiplexGrammarParser.NUMBER, 0); }
		public GroupLengthContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_groupLength; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DemultiplexGrammarListener ) ((DemultiplexGrammarListener)listener).enterGroupLength(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DemultiplexGrammarListener ) ((DemultiplexGrammarListener)listener).exitGroupLength(this);
		}
	}

	public final GroupLengthContext groupLength() throws RecognitionException {
		GroupLengthContext _localctx = new GroupLengthContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_groupLength);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(101);
			match(NUMBER);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\17j\4\2\t\2\4\3\t"+
		"\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t\13\4"+
		"\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\3\2\3\2\3\3\3\3\5\3%\n\3"+
		"\3\4\3\4\3\4\3\4\3\5\3\5\3\5\3\5\3\5\5\5\60\n\5\3\6\3\6\3\6\3\6\3\6\7"+
		"\6\67\n\6\f\6\16\6:\13\6\3\7\3\7\3\7\3\7\3\7\5\7A\n\7\3\b\3\b\3\b\3\b"+
		"\3\b\7\bH\n\b\f\b\16\bK\13\b\3\t\3\t\3\t\3\t\5\tQ\n\t\3\n\3\n\3\n\3\n"+
		"\3\13\3\13\3\13\3\13\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\r\3\r\3\16\3\16\3\17"+
		"\3\17\3\20\3\20\3\20\2\2\21\2\4\6\b\n\f\16\20\22\24\26\30\32\34\36\2\2"+
		"\2h\2 \3\2\2\2\4$\3\2\2\2\6&\3\2\2\2\b/\3\2\2\2\n\61\3\2\2\2\f@\3\2\2"+
		"\2\16B\3\2\2\2\20P\3\2\2\2\22R\3\2\2\2\24V\3\2\2\2\26Z\3\2\2\2\30a\3\2"+
		"\2\2\32c\3\2\2\2\34e\3\2\2\2\36g\3\2\2\2 !\5\4\3\2!\3\3\2\2\2\"%\5\6\4"+
		"\2#%\5\b\5\2$\"\3\2\2\2$#\3\2\2\2%\5\3\2\2\2&\'\7\t\2\2\'(\5\b\5\2()\7"+
		"\n\2\2)\7\3\2\2\2*\60\5\n\6\2+\60\5\16\b\2,\60\5\22\n\2-\60\5\24\13\2"+
		".\60\5\26\f\2/*\3\2\2\2/+\3\2\2\2/,\3\2\2\2/-\3\2\2\2/.\3\2\2\2\60\t\3"+
		"\2\2\2\61\62\5\f\7\2\62\63\7\16\2\2\638\5\f\7\2\64\65\7\16\2\2\65\67\5"+
		"\f\7\2\66\64\3\2\2\2\67:\3\2\2\28\66\3\2\2\289\3\2\2\29\13\3\2\2\2:8\3"+
		"\2\2\2;A\5\16\b\2<A\5\22\n\2=A\5\24\13\2>A\5\26\f\2?A\5\6\4\2@;\3\2\2"+
		"\2@<\3\2\2\2@=\3\2\2\2@>\3\2\2\2@?\3\2\2\2A\r\3\2\2\2BC\5\20\t\2CD\7\r"+
		"\2\2DI\5\20\t\2EF\7\r\2\2FH\5\20\t\2GE\3\2\2\2HK\3\2\2\2IG\3\2\2\2IJ\3"+
		"\2\2\2J\17\3\2\2\2KI\3\2\2\2LQ\5\22\n\2MQ\5\24\13\2NQ\5\26\f\2OQ\5\6\4"+
		"\2PL\3\2\2\2PM\3\2\2\2PN\3\2\2\2PO\3\2\2\2Q\21\3\2\2\2RS\5\32\16\2ST\7"+
		"\f\2\2TU\5\30\r\2U\23\3\2\2\2VW\7\5\2\2WX\7\13\2\2XY\5\34\17\2Y\25\3\2"+
		"\2\2Z[\7\6\2\2[\\\7\t\2\2\\]\5\32\16\2]^\7\n\2\2^_\7\13\2\2_`\5\36\20"+
		"\2`\27\3\2\2\2ab\7\4\2\2b\31\3\2\2\2cd\7\b\2\2d\33\3\2\2\2ef\7\7\2\2f"+
		"\35\3\2\2\2gh\7\7\2\2h\37\3\2\2\2\b$/8@IP";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}