// Generated from FilterGrammar.g4 by ANTLR 4.7
package com.milaboratory.minnn.cli;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class FilterGrammarParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.7", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		SINGLE_QUOTE=1, STRING=2, MIN_GROUP_QUALITY=3, AVG_GROUP_QUALITY=4, GROUP_N_COUNT=5, 
		GROUP_N_FRACTION=6, MIN_CONSENSUS_READS=7, LEN=8, NO_WILDCARDS=9, FLOAT_NUMBER=10, 
		INT_NUMBER=11, GROUP_NAME=12, OPEN_PARENTHESIS=13, CLOSED_PARENTHESIS=14, 
		EQUALS=15, TILDE=16, AND=17, OR=18, ASTERISK=19, WS=20;
	public static final int
		RULE_filter = 0, RULE_filterInParentheses = 1, RULE_anySingleFilter = 2, 
		RULE_or = 3, RULE_orOperand = 4, RULE_and = 5, RULE_andOperand = 6, RULE_pattern = 7, 
		RULE_simpleFilter = 8, RULE_minGroupQuality = 9, RULE_avgGroupQuality = 10, 
		RULE_groupNCount = 11, RULE_groupNFraction = 12, RULE_len = 13, RULE_noWildcards = 14, 
		RULE_minConsensusReads = 15, RULE_patternString = 16, RULE_minGroupQualityNum = 17, 
		RULE_avgGroupQualityNum = 18, RULE_groupNCountNum = 19, RULE_groupNFractionNum = 20, 
		RULE_groupLength = 21, RULE_minConsensusReadsNum = 22, RULE_groupName = 23, 
		RULE_groupNameOrAll = 24;
	public static final String[] ruleNames = {
		"filter", "filterInParentheses", "anySingleFilter", "or", "orOperand", 
		"and", "andOperand", "pattern", "simpleFilter", "minGroupQuality", "avgGroupQuality", 
		"groupNCount", "groupNFraction", "len", "noWildcards", "minConsensusReads", 
		"patternString", "minGroupQualityNum", "avgGroupQualityNum", "groupNCountNum", 
		"groupNFractionNum", "groupLength", "minConsensusReadsNum", "groupName", 
		"groupNameOrAll"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "'''", null, "'MinGroupQuality'", "'AvgGroupQuality'", "'GroupMaxNCount'", 
		"'GroupMaxNFraction'", "'MinConsensusReads'", "'Len'", "'NoWildcards'", 
		null, null, null, "'('", "')'", "'='", "'~'", "'&'", "'|'", "'*'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "SINGLE_QUOTE", "STRING", "MIN_GROUP_QUALITY", "AVG_GROUP_QUALITY", 
		"GROUP_N_COUNT", "GROUP_N_FRACTION", "MIN_CONSENSUS_READS", "LEN", "NO_WILDCARDS", 
		"FLOAT_NUMBER", "INT_NUMBER", "GROUP_NAME", "OPEN_PARENTHESIS", "CLOSED_PARENTHESIS", 
		"EQUALS", "TILDE", "AND", "OR", "ASTERISK", "WS"
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
	public String getGrammarFileName() { return "FilterGrammar.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public FilterGrammarParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
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
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).enterFilter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).exitFilter(this);
		}
	}

	public final FilterContext filter() throws RecognitionException {
		FilterContext _localctx = new FilterContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_filter);
		try {
			setState(52);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,0,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(50);
				filterInParentheses();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(51);
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
		public TerminalNode OPEN_PARENTHESIS() { return getToken(FilterGrammarParser.OPEN_PARENTHESIS, 0); }
		public AnySingleFilterContext anySingleFilter() {
			return getRuleContext(AnySingleFilterContext.class,0);
		}
		public TerminalNode CLOSED_PARENTHESIS() { return getToken(FilterGrammarParser.CLOSED_PARENTHESIS, 0); }
		public FilterInParenthesesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_filterInParentheses; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).enterFilterInParentheses(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).exitFilterInParentheses(this);
		}
	}

	public final FilterInParenthesesContext filterInParentheses() throws RecognitionException {
		FilterInParenthesesContext _localctx = new FilterInParenthesesContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_filterInParentheses);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(54);
			match(OPEN_PARENTHESIS);
			setState(55);
			anySingleFilter();
			setState(56);
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
		public SimpleFilterContext simpleFilter() {
			return getRuleContext(SimpleFilterContext.class,0);
		}
		public AnySingleFilterContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_anySingleFilter; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).enterAnySingleFilter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).exitAnySingleFilter(this);
		}
	}

	public final AnySingleFilterContext anySingleFilter() throws RecognitionException {
		AnySingleFilterContext _localctx = new AnySingleFilterContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_anySingleFilter);
		try {
			setState(62);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,1,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(58);
				or();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(59);
				and();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(60);
				pattern();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(61);
				simpleFilter();
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
		public List<TerminalNode> OR() { return getTokens(FilterGrammarParser.OR); }
		public TerminalNode OR(int i) {
			return getToken(FilterGrammarParser.OR, i);
		}
		public OrContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_or; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).enterOr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).exitOr(this);
		}
	}

	public final OrContext or() throws RecognitionException {
		OrContext _localctx = new OrContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_or);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(64);
			orOperand();
			setState(65);
			match(OR);
			setState(66);
			orOperand();
			setState(71);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==OR) {
				{
				{
				setState(67);
				match(OR);
				setState(68);
				orOperand();
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

	public static class OrOperandContext extends ParserRuleContext {
		public AndContext and() {
			return getRuleContext(AndContext.class,0);
		}
		public PatternContext pattern() {
			return getRuleContext(PatternContext.class,0);
		}
		public SimpleFilterContext simpleFilter() {
			return getRuleContext(SimpleFilterContext.class,0);
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
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).enterOrOperand(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).exitOrOperand(this);
		}
	}

	public final OrOperandContext orOperand() throws RecognitionException {
		OrOperandContext _localctx = new OrOperandContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_orOperand);
		try {
			setState(78);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,3,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(74);
				and();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(75);
				pattern();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(76);
				simpleFilter();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(77);
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
		public List<TerminalNode> AND() { return getTokens(FilterGrammarParser.AND); }
		public TerminalNode AND(int i) {
			return getToken(FilterGrammarParser.AND, i);
		}
		public AndContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_and; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).enterAnd(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).exitAnd(this);
		}
	}

	public final AndContext and() throws RecognitionException {
		AndContext _localctx = new AndContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_and);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(80);
			andOperand();
			setState(81);
			match(AND);
			setState(82);
			andOperand();
			setState(87);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AND) {
				{
				{
				setState(83);
				match(AND);
				setState(84);
				andOperand();
				}
				}
				setState(89);
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
		public SimpleFilterContext simpleFilter() {
			return getRuleContext(SimpleFilterContext.class,0);
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
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).enterAndOperand(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).exitAndOperand(this);
		}
	}

	public final AndOperandContext andOperand() throws RecognitionException {
		AndOperandContext _localctx = new AndOperandContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_andOperand);
		try {
			setState(93);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case GROUP_NAME:
				enterOuterAlt(_localctx, 1);
				{
				setState(90);
				pattern();
				}
				break;
			case MIN_GROUP_QUALITY:
			case AVG_GROUP_QUALITY:
			case GROUP_N_COUNT:
			case GROUP_N_FRACTION:
			case MIN_CONSENSUS_READS:
			case LEN:
			case NO_WILDCARDS:
				enterOuterAlt(_localctx, 2);
				{
				setState(91);
				simpleFilter();
				}
				break;
			case OPEN_PARENTHESIS:
				enterOuterAlt(_localctx, 3);
				{
				setState(92);
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
		public TerminalNode TILDE() { return getToken(FilterGrammarParser.TILDE, 0); }
		public PatternStringContext patternString() {
			return getRuleContext(PatternStringContext.class,0);
		}
		public PatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).enterPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).exitPattern(this);
		}
	}

	public final PatternContext pattern() throws RecognitionException {
		PatternContext _localctx = new PatternContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_pattern);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(95);
			groupName();
			setState(96);
			match(TILDE);
			setState(97);
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

	public static class SimpleFilterContext extends ParserRuleContext {
		public MinGroupQualityContext minGroupQuality() {
			return getRuleContext(MinGroupQualityContext.class,0);
		}
		public AvgGroupQualityContext avgGroupQuality() {
			return getRuleContext(AvgGroupQualityContext.class,0);
		}
		public GroupNCountContext groupNCount() {
			return getRuleContext(GroupNCountContext.class,0);
		}
		public GroupNFractionContext groupNFraction() {
			return getRuleContext(GroupNFractionContext.class,0);
		}
		public MinConsensusReadsContext minConsensusReads() {
			return getRuleContext(MinConsensusReadsContext.class,0);
		}
		public LenContext len() {
			return getRuleContext(LenContext.class,0);
		}
		public NoWildcardsContext noWildcards() {
			return getRuleContext(NoWildcardsContext.class,0);
		}
		public SimpleFilterContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_simpleFilter; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).enterSimpleFilter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).exitSimpleFilter(this);
		}
	}

	public final SimpleFilterContext simpleFilter() throws RecognitionException {
		SimpleFilterContext _localctx = new SimpleFilterContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_simpleFilter);
		try {
			setState(106);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case MIN_GROUP_QUALITY:
				enterOuterAlt(_localctx, 1);
				{
				setState(99);
				minGroupQuality();
				}
				break;
			case AVG_GROUP_QUALITY:
				enterOuterAlt(_localctx, 2);
				{
				setState(100);
				avgGroupQuality();
				}
				break;
			case GROUP_N_COUNT:
				enterOuterAlt(_localctx, 3);
				{
				setState(101);
				groupNCount();
				}
				break;
			case GROUP_N_FRACTION:
				enterOuterAlt(_localctx, 4);
				{
				setState(102);
				groupNFraction();
				}
				break;
			case MIN_CONSENSUS_READS:
				enterOuterAlt(_localctx, 5);
				{
				setState(103);
				minConsensusReads();
				}
				break;
			case LEN:
				enterOuterAlt(_localctx, 6);
				{
				setState(104);
				len();
				}
				break;
			case NO_WILDCARDS:
				enterOuterAlt(_localctx, 7);
				{
				setState(105);
				noWildcards();
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

	public static class MinGroupQualityContext extends ParserRuleContext {
		public TerminalNode MIN_GROUP_QUALITY() { return getToken(FilterGrammarParser.MIN_GROUP_QUALITY, 0); }
		public TerminalNode OPEN_PARENTHESIS() { return getToken(FilterGrammarParser.OPEN_PARENTHESIS, 0); }
		public GroupNameOrAllContext groupNameOrAll() {
			return getRuleContext(GroupNameOrAllContext.class,0);
		}
		public TerminalNode CLOSED_PARENTHESIS() { return getToken(FilterGrammarParser.CLOSED_PARENTHESIS, 0); }
		public TerminalNode EQUALS() { return getToken(FilterGrammarParser.EQUALS, 0); }
		public MinGroupQualityNumContext minGroupQualityNum() {
			return getRuleContext(MinGroupQualityNumContext.class,0);
		}
		public MinGroupQualityContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_minGroupQuality; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).enterMinGroupQuality(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).exitMinGroupQuality(this);
		}
	}

	public final MinGroupQualityContext minGroupQuality() throws RecognitionException {
		MinGroupQualityContext _localctx = new MinGroupQualityContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_minGroupQuality);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(108);
			match(MIN_GROUP_QUALITY);
			setState(109);
			match(OPEN_PARENTHESIS);
			setState(110);
			groupNameOrAll();
			setState(111);
			match(CLOSED_PARENTHESIS);
			setState(112);
			match(EQUALS);
			setState(113);
			minGroupQualityNum();
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

	public static class AvgGroupQualityContext extends ParserRuleContext {
		public TerminalNode AVG_GROUP_QUALITY() { return getToken(FilterGrammarParser.AVG_GROUP_QUALITY, 0); }
		public TerminalNode OPEN_PARENTHESIS() { return getToken(FilterGrammarParser.OPEN_PARENTHESIS, 0); }
		public GroupNameOrAllContext groupNameOrAll() {
			return getRuleContext(GroupNameOrAllContext.class,0);
		}
		public TerminalNode CLOSED_PARENTHESIS() { return getToken(FilterGrammarParser.CLOSED_PARENTHESIS, 0); }
		public TerminalNode EQUALS() { return getToken(FilterGrammarParser.EQUALS, 0); }
		public AvgGroupQualityNumContext avgGroupQualityNum() {
			return getRuleContext(AvgGroupQualityNumContext.class,0);
		}
		public AvgGroupQualityContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_avgGroupQuality; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).enterAvgGroupQuality(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).exitAvgGroupQuality(this);
		}
	}

	public final AvgGroupQualityContext avgGroupQuality() throws RecognitionException {
		AvgGroupQualityContext _localctx = new AvgGroupQualityContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_avgGroupQuality);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(115);
			match(AVG_GROUP_QUALITY);
			setState(116);
			match(OPEN_PARENTHESIS);
			setState(117);
			groupNameOrAll();
			setState(118);
			match(CLOSED_PARENTHESIS);
			setState(119);
			match(EQUALS);
			setState(120);
			avgGroupQualityNum();
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

	public static class GroupNCountContext extends ParserRuleContext {
		public TerminalNode GROUP_N_COUNT() { return getToken(FilterGrammarParser.GROUP_N_COUNT, 0); }
		public TerminalNode OPEN_PARENTHESIS() { return getToken(FilterGrammarParser.OPEN_PARENTHESIS, 0); }
		public GroupNameOrAllContext groupNameOrAll() {
			return getRuleContext(GroupNameOrAllContext.class,0);
		}
		public TerminalNode CLOSED_PARENTHESIS() { return getToken(FilterGrammarParser.CLOSED_PARENTHESIS, 0); }
		public TerminalNode EQUALS() { return getToken(FilterGrammarParser.EQUALS, 0); }
		public GroupNCountNumContext groupNCountNum() {
			return getRuleContext(GroupNCountNumContext.class,0);
		}
		public GroupNCountContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_groupNCount; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).enterGroupNCount(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).exitGroupNCount(this);
		}
	}

	public final GroupNCountContext groupNCount() throws RecognitionException {
		GroupNCountContext _localctx = new GroupNCountContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_groupNCount);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(122);
			match(GROUP_N_COUNT);
			setState(123);
			match(OPEN_PARENTHESIS);
			setState(124);
			groupNameOrAll();
			setState(125);
			match(CLOSED_PARENTHESIS);
			setState(126);
			match(EQUALS);
			setState(127);
			groupNCountNum();
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

	public static class GroupNFractionContext extends ParserRuleContext {
		public TerminalNode GROUP_N_FRACTION() { return getToken(FilterGrammarParser.GROUP_N_FRACTION, 0); }
		public TerminalNode OPEN_PARENTHESIS() { return getToken(FilterGrammarParser.OPEN_PARENTHESIS, 0); }
		public GroupNameOrAllContext groupNameOrAll() {
			return getRuleContext(GroupNameOrAllContext.class,0);
		}
		public TerminalNode CLOSED_PARENTHESIS() { return getToken(FilterGrammarParser.CLOSED_PARENTHESIS, 0); }
		public TerminalNode EQUALS() { return getToken(FilterGrammarParser.EQUALS, 0); }
		public GroupNFractionNumContext groupNFractionNum() {
			return getRuleContext(GroupNFractionNumContext.class,0);
		}
		public GroupNFractionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_groupNFraction; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).enterGroupNFraction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).exitGroupNFraction(this);
		}
	}

	public final GroupNFractionContext groupNFraction() throws RecognitionException {
		GroupNFractionContext _localctx = new GroupNFractionContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_groupNFraction);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(129);
			match(GROUP_N_FRACTION);
			setState(130);
			match(OPEN_PARENTHESIS);
			setState(131);
			groupNameOrAll();
			setState(132);
			match(CLOSED_PARENTHESIS);
			setState(133);
			match(EQUALS);
			setState(134);
			groupNFractionNum();
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
		public TerminalNode LEN() { return getToken(FilterGrammarParser.LEN, 0); }
		public TerminalNode OPEN_PARENTHESIS() { return getToken(FilterGrammarParser.OPEN_PARENTHESIS, 0); }
		public GroupNameOrAllContext groupNameOrAll() {
			return getRuleContext(GroupNameOrAllContext.class,0);
		}
		public TerminalNode CLOSED_PARENTHESIS() { return getToken(FilterGrammarParser.CLOSED_PARENTHESIS, 0); }
		public TerminalNode EQUALS() { return getToken(FilterGrammarParser.EQUALS, 0); }
		public GroupLengthContext groupLength() {
			return getRuleContext(GroupLengthContext.class,0);
		}
		public LenContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_len; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).enterLen(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).exitLen(this);
		}
	}

	public final LenContext len() throws RecognitionException {
		LenContext _localctx = new LenContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_len);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(136);
			match(LEN);
			setState(137);
			match(OPEN_PARENTHESIS);
			setState(138);
			groupNameOrAll();
			setState(139);
			match(CLOSED_PARENTHESIS);
			setState(140);
			match(EQUALS);
			setState(141);
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

	public static class NoWildcardsContext extends ParserRuleContext {
		public TerminalNode NO_WILDCARDS() { return getToken(FilterGrammarParser.NO_WILDCARDS, 0); }
		public TerminalNode OPEN_PARENTHESIS() { return getToken(FilterGrammarParser.OPEN_PARENTHESIS, 0); }
		public GroupNameOrAllContext groupNameOrAll() {
			return getRuleContext(GroupNameOrAllContext.class,0);
		}
		public TerminalNode CLOSED_PARENTHESIS() { return getToken(FilterGrammarParser.CLOSED_PARENTHESIS, 0); }
		public NoWildcardsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_noWildcards; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).enterNoWildcards(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).exitNoWildcards(this);
		}
	}

	public final NoWildcardsContext noWildcards() throws RecognitionException {
		NoWildcardsContext _localctx = new NoWildcardsContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_noWildcards);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(143);
			match(NO_WILDCARDS);
			setState(144);
			match(OPEN_PARENTHESIS);
			setState(145);
			groupNameOrAll();
			setState(146);
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

	public static class MinConsensusReadsContext extends ParserRuleContext {
		public TerminalNode MIN_CONSENSUS_READS() { return getToken(FilterGrammarParser.MIN_CONSENSUS_READS, 0); }
		public TerminalNode EQUALS() { return getToken(FilterGrammarParser.EQUALS, 0); }
		public MinConsensusReadsNumContext minConsensusReadsNum() {
			return getRuleContext(MinConsensusReadsNumContext.class,0);
		}
		public MinConsensusReadsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_minConsensusReads; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).enterMinConsensusReads(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).exitMinConsensusReads(this);
		}
	}

	public final MinConsensusReadsContext minConsensusReads() throws RecognitionException {
		MinConsensusReadsContext _localctx = new MinConsensusReadsContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_minConsensusReads);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(148);
			match(MIN_CONSENSUS_READS);
			setState(149);
			match(EQUALS);
			setState(150);
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

	public static class PatternStringContext extends ParserRuleContext {
		public TerminalNode STRING() { return getToken(FilterGrammarParser.STRING, 0); }
		public PatternStringContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_patternString; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).enterPatternString(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).exitPatternString(this);
		}
	}

	public final PatternStringContext patternString() throws RecognitionException {
		PatternStringContext _localctx = new PatternStringContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_patternString);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(152);
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

	public static class MinGroupQualityNumContext extends ParserRuleContext {
		public TerminalNode INT_NUMBER() { return getToken(FilterGrammarParser.INT_NUMBER, 0); }
		public MinGroupQualityNumContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_minGroupQualityNum; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).enterMinGroupQualityNum(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).exitMinGroupQualityNum(this);
		}
	}

	public final MinGroupQualityNumContext minGroupQualityNum() throws RecognitionException {
		MinGroupQualityNumContext _localctx = new MinGroupQualityNumContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_minGroupQualityNum);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(154);
			match(INT_NUMBER);
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

	public static class AvgGroupQualityNumContext extends ParserRuleContext {
		public TerminalNode INT_NUMBER() { return getToken(FilterGrammarParser.INT_NUMBER, 0); }
		public AvgGroupQualityNumContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_avgGroupQualityNum; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).enterAvgGroupQualityNum(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).exitAvgGroupQualityNum(this);
		}
	}

	public final AvgGroupQualityNumContext avgGroupQualityNum() throws RecognitionException {
		AvgGroupQualityNumContext _localctx = new AvgGroupQualityNumContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_avgGroupQualityNum);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(156);
			match(INT_NUMBER);
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

	public static class GroupNCountNumContext extends ParserRuleContext {
		public TerminalNode INT_NUMBER() { return getToken(FilterGrammarParser.INT_NUMBER, 0); }
		public GroupNCountNumContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_groupNCountNum; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).enterGroupNCountNum(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).exitGroupNCountNum(this);
		}
	}

	public final GroupNCountNumContext groupNCountNum() throws RecognitionException {
		GroupNCountNumContext _localctx = new GroupNCountNumContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_groupNCountNum);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(158);
			match(INT_NUMBER);
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

	public static class GroupNFractionNumContext extends ParserRuleContext {
		public TerminalNode FLOAT_NUMBER() { return getToken(FilterGrammarParser.FLOAT_NUMBER, 0); }
		public TerminalNode INT_NUMBER() { return getToken(FilterGrammarParser.INT_NUMBER, 0); }
		public GroupNFractionNumContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_groupNFractionNum; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).enterGroupNFractionNum(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).exitGroupNFractionNum(this);
		}
	}

	public final GroupNFractionNumContext groupNFractionNum() throws RecognitionException {
		GroupNFractionNumContext _localctx = new GroupNFractionNumContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_groupNFractionNum);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(160);
			_la = _input.LA(1);
			if ( !(_la==FLOAT_NUMBER || _la==INT_NUMBER) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
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

	public static class GroupLengthContext extends ParserRuleContext {
		public TerminalNode INT_NUMBER() { return getToken(FilterGrammarParser.INT_NUMBER, 0); }
		public GroupLengthContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_groupLength; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).enterGroupLength(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).exitGroupLength(this);
		}
	}

	public final GroupLengthContext groupLength() throws RecognitionException {
		GroupLengthContext _localctx = new GroupLengthContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_groupLength);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(162);
			match(INT_NUMBER);
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
		public TerminalNode INT_NUMBER() { return getToken(FilterGrammarParser.INT_NUMBER, 0); }
		public MinConsensusReadsNumContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_minConsensusReadsNum; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).enterMinConsensusReadsNum(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).exitMinConsensusReadsNum(this);
		}
	}

	public final MinConsensusReadsNumContext minConsensusReadsNum() throws RecognitionException {
		MinConsensusReadsNumContext _localctx = new MinConsensusReadsNumContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_minConsensusReadsNum);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(164);
			match(INT_NUMBER);
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
		public TerminalNode GROUP_NAME() { return getToken(FilterGrammarParser.GROUP_NAME, 0); }
		public GroupNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_groupName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).enterGroupName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).exitGroupName(this);
		}
	}

	public final GroupNameContext groupName() throws RecognitionException {
		GroupNameContext _localctx = new GroupNameContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_groupName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(166);
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

	public static class GroupNameOrAllContext extends ParserRuleContext {
		public TerminalNode GROUP_NAME() { return getToken(FilterGrammarParser.GROUP_NAME, 0); }
		public TerminalNode ASTERISK() { return getToken(FilterGrammarParser.ASTERISK, 0); }
		public GroupNameOrAllContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_groupNameOrAll; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).enterGroupNameOrAll(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).exitGroupNameOrAll(this);
		}
	}

	public final GroupNameOrAllContext groupNameOrAll() throws RecognitionException {
		GroupNameOrAllContext _localctx = new GroupNameOrAllContext(_ctx, getState());
		enterRule(_localctx, 48, RULE_groupNameOrAll);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(168);
			_la = _input.LA(1);
			if ( !(_la==GROUP_NAME || _la==ASTERISK) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
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

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\26\u00ad\4\2\t\2"+
		"\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\3\2\3\2\5\2\67\n\2\3\3\3\3\3\3\3\3\3\4\3\4\3\4\3\4\5\4A\n\4"+
		"\3\5\3\5\3\5\3\5\3\5\7\5H\n\5\f\5\16\5K\13\5\3\6\3\6\3\6\3\6\5\6Q\n\6"+
		"\3\7\3\7\3\7\3\7\3\7\7\7X\n\7\f\7\16\7[\13\7\3\b\3\b\3\b\5\b`\n\b\3\t"+
		"\3\t\3\t\3\t\3\n\3\n\3\n\3\n\3\n\3\n\3\n\5\nm\n\n\3\13\3\13\3\13\3\13"+
		"\3\13\3\13\3\13\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\r\3\r\3\r\3\r\3\r\3\r\3"+
		"\r\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\17\3\17\3\17\3\17\3\17\3\17\3"+
		"\17\3\20\3\20\3\20\3\20\3\20\3\21\3\21\3\21\3\21\3\22\3\22\3\23\3\23\3"+
		"\24\3\24\3\25\3\25\3\26\3\26\3\27\3\27\3\30\3\30\3\31\3\31\3\32\3\32\3"+
		"\32\2\2\33\2\4\6\b\n\f\16\20\22\24\26\30\32\34\36 \"$&(*,.\60\62\2\4\3"+
		"\2\f\r\4\2\16\16\25\25\2\u00a4\2\66\3\2\2\2\48\3\2\2\2\6@\3\2\2\2\bB\3"+
		"\2\2\2\nP\3\2\2\2\fR\3\2\2\2\16_\3\2\2\2\20a\3\2\2\2\22l\3\2\2\2\24n\3"+
		"\2\2\2\26u\3\2\2\2\30|\3\2\2\2\32\u0083\3\2\2\2\34\u008a\3\2\2\2\36\u0091"+
		"\3\2\2\2 \u0096\3\2\2\2\"\u009a\3\2\2\2$\u009c\3\2\2\2&\u009e\3\2\2\2"+
		"(\u00a0\3\2\2\2*\u00a2\3\2\2\2,\u00a4\3\2\2\2.\u00a6\3\2\2\2\60\u00a8"+
		"\3\2\2\2\62\u00aa\3\2\2\2\64\67\5\4\3\2\65\67\5\6\4\2\66\64\3\2\2\2\66"+
		"\65\3\2\2\2\67\3\3\2\2\289\7\17\2\29:\5\6\4\2:;\7\20\2\2;\5\3\2\2\2<A"+
		"\5\b\5\2=A\5\f\7\2>A\5\20\t\2?A\5\22\n\2@<\3\2\2\2@=\3\2\2\2@>\3\2\2\2"+
		"@?\3\2\2\2A\7\3\2\2\2BC\5\n\6\2CD\7\24\2\2DI\5\n\6\2EF\7\24\2\2FH\5\n"+
		"\6\2GE\3\2\2\2HK\3\2\2\2IG\3\2\2\2IJ\3\2\2\2J\t\3\2\2\2KI\3\2\2\2LQ\5"+
		"\f\7\2MQ\5\20\t\2NQ\5\22\n\2OQ\5\4\3\2PL\3\2\2\2PM\3\2\2\2PN\3\2\2\2P"+
		"O\3\2\2\2Q\13\3\2\2\2RS\5\16\b\2ST\7\23\2\2TY\5\16\b\2UV\7\23\2\2VX\5"+
		"\16\b\2WU\3\2\2\2X[\3\2\2\2YW\3\2\2\2YZ\3\2\2\2Z\r\3\2\2\2[Y\3\2\2\2\\"+
		"`\5\20\t\2]`\5\22\n\2^`\5\4\3\2_\\\3\2\2\2_]\3\2\2\2_^\3\2\2\2`\17\3\2"+
		"\2\2ab\5\60\31\2bc\7\22\2\2cd\5\"\22\2d\21\3\2\2\2em\5\24\13\2fm\5\26"+
		"\f\2gm\5\30\r\2hm\5\32\16\2im\5 \21\2jm\5\34\17\2km\5\36\20\2le\3\2\2"+
		"\2lf\3\2\2\2lg\3\2\2\2lh\3\2\2\2li\3\2\2\2lj\3\2\2\2lk\3\2\2\2m\23\3\2"+
		"\2\2no\7\5\2\2op\7\17\2\2pq\5\62\32\2qr\7\20\2\2rs\7\21\2\2st\5$\23\2"+
		"t\25\3\2\2\2uv\7\6\2\2vw\7\17\2\2wx\5\62\32\2xy\7\20\2\2yz\7\21\2\2z{"+
		"\5&\24\2{\27\3\2\2\2|}\7\7\2\2}~\7\17\2\2~\177\5\62\32\2\177\u0080\7\20"+
		"\2\2\u0080\u0081\7\21\2\2\u0081\u0082\5(\25\2\u0082\31\3\2\2\2\u0083\u0084"+
		"\7\b\2\2\u0084\u0085\7\17\2\2\u0085\u0086\5\62\32\2\u0086\u0087\7\20\2"+
		"\2\u0087\u0088\7\21\2\2\u0088\u0089\5*\26\2\u0089\33\3\2\2\2\u008a\u008b"+
		"\7\n\2\2\u008b\u008c\7\17\2\2\u008c\u008d\5\62\32\2\u008d\u008e\7\20\2"+
		"\2\u008e\u008f\7\21\2\2\u008f\u0090\5,\27\2\u0090\35\3\2\2\2\u0091\u0092"+
		"\7\13\2\2\u0092\u0093\7\17\2\2\u0093\u0094\5\62\32\2\u0094\u0095\7\20"+
		"\2\2\u0095\37\3\2\2\2\u0096\u0097\7\t\2\2\u0097\u0098\7\21\2\2\u0098\u0099"+
		"\5.\30\2\u0099!\3\2\2\2\u009a\u009b\7\4\2\2\u009b#\3\2\2\2\u009c\u009d"+
		"\7\r\2\2\u009d%\3\2\2\2\u009e\u009f\7\r\2\2\u009f\'\3\2\2\2\u00a0\u00a1"+
		"\7\r\2\2\u00a1)\3\2\2\2\u00a2\u00a3\t\2\2\2\u00a3+\3\2\2\2\u00a4\u00a5"+
		"\7\r\2\2\u00a5-\3\2\2\2\u00a6\u00a7\7\r\2\2\u00a7/\3\2\2\2\u00a8\u00a9"+
		"\7\16\2\2\u00a9\61\3\2\2\2\u00aa\u00ab\t\3\2\2\u00ab\63\3\2\2\2\t\66@"+
		"IPY_l";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}