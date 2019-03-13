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
		GROUP_N_FRACTION=6, MIN_CONSENSUS_READS=7, LEN=8, FLOAT_NUMBER=9, INT_NUMBER=10, 
		GROUP_NAME=11, OPEN_PARENTHESIS=12, CLOSED_PARENTHESIS=13, EQUALS=14, 
		TILDE=15, AND=16, OR=17, ASTERISK=18, WS=19;
	public static final int
		RULE_filter = 0, RULE_filterInParentheses = 1, RULE_anySingleFilter = 2, 
		RULE_or = 3, RULE_orOperand = 4, RULE_and = 5, RULE_andOperand = 6, RULE_pattern = 7, 
		RULE_simpleFilter = 8, RULE_minGroupQuality = 9, RULE_avgGroupQuality = 10, 
		RULE_groupNCount = 11, RULE_groupNFraction = 12, RULE_len = 13, RULE_minConsensusReads = 14, 
		RULE_patternString = 15, RULE_minGroupQualityNum = 16, RULE_avgGroupQualityNum = 17, 
		RULE_groupNCountNum = 18, RULE_groupNFractionNum = 19, RULE_groupLength = 20, 
		RULE_minConsensusReadsNum = 21, RULE_groupName = 22, RULE_groupNameOrAll = 23;
	public static final String[] ruleNames = {
		"filter", "filterInParentheses", "anySingleFilter", "or", "orOperand", 
		"and", "andOperand", "pattern", "simpleFilter", "minGroupQuality", "avgGroupQuality", 
		"groupNCount", "groupNFraction", "len", "minConsensusReads", "patternString", 
		"minGroupQualityNum", "avgGroupQualityNum", "groupNCountNum", "groupNFractionNum", 
		"groupLength", "minConsensusReadsNum", "groupName", "groupNameOrAll"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "'''", null, "'MinGroupQuality'", "'AvgGroupQuality'", "'GroupMaxNCount'", 
		"'GroupMaxNFraction'", "'MinConsensusReads'", "'Len'", null, null, null, 
		"'('", "')'", "'='", "'~'", "'&'", "'|'", "'*'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "SINGLE_QUOTE", "STRING", "MIN_GROUP_QUALITY", "AVG_GROUP_QUALITY", 
		"GROUP_N_COUNT", "GROUP_N_FRACTION", "MIN_CONSENSUS_READS", "LEN", "FLOAT_NUMBER", 
		"INT_NUMBER", "GROUP_NAME", "OPEN_PARENTHESIS", "CLOSED_PARENTHESIS", 
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
			setState(50);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,0,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(48);
				filterInParentheses();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(49);
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
			setState(52);
			match(OPEN_PARENTHESIS);
			setState(53);
			anySingleFilter();
			setState(54);
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
			setState(60);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,1,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(56);
				or();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(57);
				and();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(58);
				pattern();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(59);
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
			setState(62);
			orOperand();
			setState(63);
			match(OR);
			setState(64);
			orOperand();
			setState(69);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==OR) {
				{
				{
				setState(65);
				match(OR);
				setState(66);
				orOperand();
				}
				}
				setState(71);
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
			setState(76);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,3,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(72);
				and();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(73);
				pattern();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(74);
				simpleFilter();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(75);
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
			setState(78);
			andOperand();
			setState(79);
			match(AND);
			setState(80);
			andOperand();
			setState(85);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AND) {
				{
				{
				setState(81);
				match(AND);
				setState(82);
				andOperand();
				}
				}
				setState(87);
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
			setState(91);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case GROUP_NAME:
				enterOuterAlt(_localctx, 1);
				{
				setState(88);
				pattern();
				}
				break;
			case MIN_GROUP_QUALITY:
			case AVG_GROUP_QUALITY:
			case GROUP_N_COUNT:
			case GROUP_N_FRACTION:
			case MIN_CONSENSUS_READS:
			case LEN:
				enterOuterAlt(_localctx, 2);
				{
				setState(89);
				simpleFilter();
				}
				break;
			case OPEN_PARENTHESIS:
				enterOuterAlt(_localctx, 3);
				{
				setState(90);
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
			setState(93);
			groupName();
			setState(94);
			match(TILDE);
			setState(95);
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
			setState(103);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case MIN_GROUP_QUALITY:
				enterOuterAlt(_localctx, 1);
				{
				setState(97);
				minGroupQuality();
				}
				break;
			case AVG_GROUP_QUALITY:
				enterOuterAlt(_localctx, 2);
				{
				setState(98);
				avgGroupQuality();
				}
				break;
			case GROUP_N_COUNT:
				enterOuterAlt(_localctx, 3);
				{
				setState(99);
				groupNCount();
				}
				break;
			case GROUP_N_FRACTION:
				enterOuterAlt(_localctx, 4);
				{
				setState(100);
				groupNFraction();
				}
				break;
			case MIN_CONSENSUS_READS:
				enterOuterAlt(_localctx, 5);
				{
				setState(101);
				minConsensusReads();
				}
				break;
			case LEN:
				enterOuterAlt(_localctx, 6);
				{
				setState(102);
				len();
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
			setState(105);
			match(MIN_GROUP_QUALITY);
			setState(106);
			match(OPEN_PARENTHESIS);
			setState(107);
			groupNameOrAll();
			setState(108);
			match(CLOSED_PARENTHESIS);
			setState(109);
			match(EQUALS);
			setState(110);
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
			setState(112);
			match(AVG_GROUP_QUALITY);
			setState(113);
			match(OPEN_PARENTHESIS);
			setState(114);
			groupNameOrAll();
			setState(115);
			match(CLOSED_PARENTHESIS);
			setState(116);
			match(EQUALS);
			setState(117);
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
			setState(119);
			match(GROUP_N_COUNT);
			setState(120);
			match(OPEN_PARENTHESIS);
			setState(121);
			groupNameOrAll();
			setState(122);
			match(CLOSED_PARENTHESIS);
			setState(123);
			match(EQUALS);
			setState(124);
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
			setState(126);
			match(GROUP_N_FRACTION);
			setState(127);
			match(OPEN_PARENTHESIS);
			setState(128);
			groupNameOrAll();
			setState(129);
			match(CLOSED_PARENTHESIS);
			setState(130);
			match(EQUALS);
			setState(131);
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
			setState(133);
			match(LEN);
			setState(134);
			match(OPEN_PARENTHESIS);
			setState(135);
			groupNameOrAll();
			setState(136);
			match(CLOSED_PARENTHESIS);
			setState(137);
			match(EQUALS);
			setState(138);
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
		enterRule(_localctx, 28, RULE_minConsensusReads);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(140);
			match(MIN_CONSENSUS_READS);
			setState(141);
			match(EQUALS);
			setState(142);
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
		enterRule(_localctx, 30, RULE_patternString);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(144);
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
		enterRule(_localctx, 32, RULE_minGroupQualityNum);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(146);
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
		enterRule(_localctx, 34, RULE_avgGroupQualityNum);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(148);
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
		enterRule(_localctx, 36, RULE_groupNCountNum);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(150);
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
		enterRule(_localctx, 38, RULE_groupNFractionNum);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(152);
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
		enterRule(_localctx, 40, RULE_groupLength);
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
		enterRule(_localctx, 42, RULE_minConsensusReadsNum);
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
		enterRule(_localctx, 44, RULE_groupName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(158);
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
		enterRule(_localctx, 46, RULE_groupNameOrAll);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(160);
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\25\u00a5\4\2\t\2"+
		"\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\3\2\3\2\5\2\65\n\2\3\3\3\3\3\3\3\3\3\4\3\4\3\4\3\4\5\4?\n\4\3\5\3\5\3"+
		"\5\3\5\3\5\7\5F\n\5\f\5\16\5I\13\5\3\6\3\6\3\6\3\6\5\6O\n\6\3\7\3\7\3"+
		"\7\3\7\3\7\7\7V\n\7\f\7\16\7Y\13\7\3\b\3\b\3\b\5\b^\n\b\3\t\3\t\3\t\3"+
		"\t\3\n\3\n\3\n\3\n\3\n\3\n\5\nj\n\n\3\13\3\13\3\13\3\13\3\13\3\13\3\13"+
		"\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\16\3\16\3\16"+
		"\3\16\3\16\3\16\3\16\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\20\3\20\3\20"+
		"\3\20\3\21\3\21\3\22\3\22\3\23\3\23\3\24\3\24\3\25\3\25\3\26\3\26\3\27"+
		"\3\27\3\30\3\30\3\31\3\31\3\31\2\2\32\2\4\6\b\n\f\16\20\22\24\26\30\32"+
		"\34\36 \"$&(*,.\60\2\4\3\2\13\f\4\2\r\r\24\24\2\u009c\2\64\3\2\2\2\4\66"+
		"\3\2\2\2\6>\3\2\2\2\b@\3\2\2\2\nN\3\2\2\2\fP\3\2\2\2\16]\3\2\2\2\20_\3"+
		"\2\2\2\22i\3\2\2\2\24k\3\2\2\2\26r\3\2\2\2\30y\3\2\2\2\32\u0080\3\2\2"+
		"\2\34\u0087\3\2\2\2\36\u008e\3\2\2\2 \u0092\3\2\2\2\"\u0094\3\2\2\2$\u0096"+
		"\3\2\2\2&\u0098\3\2\2\2(\u009a\3\2\2\2*\u009c\3\2\2\2,\u009e\3\2\2\2."+
		"\u00a0\3\2\2\2\60\u00a2\3\2\2\2\62\65\5\4\3\2\63\65\5\6\4\2\64\62\3\2"+
		"\2\2\64\63\3\2\2\2\65\3\3\2\2\2\66\67\7\16\2\2\678\5\6\4\289\7\17\2\2"+
		"9\5\3\2\2\2:?\5\b\5\2;?\5\f\7\2<?\5\20\t\2=?\5\22\n\2>:\3\2\2\2>;\3\2"+
		"\2\2><\3\2\2\2>=\3\2\2\2?\7\3\2\2\2@A\5\n\6\2AB\7\23\2\2BG\5\n\6\2CD\7"+
		"\23\2\2DF\5\n\6\2EC\3\2\2\2FI\3\2\2\2GE\3\2\2\2GH\3\2\2\2H\t\3\2\2\2I"+
		"G\3\2\2\2JO\5\f\7\2KO\5\20\t\2LO\5\22\n\2MO\5\4\3\2NJ\3\2\2\2NK\3\2\2"+
		"\2NL\3\2\2\2NM\3\2\2\2O\13\3\2\2\2PQ\5\16\b\2QR\7\22\2\2RW\5\16\b\2ST"+
		"\7\22\2\2TV\5\16\b\2US\3\2\2\2VY\3\2\2\2WU\3\2\2\2WX\3\2\2\2X\r\3\2\2"+
		"\2YW\3\2\2\2Z^\5\20\t\2[^\5\22\n\2\\^\5\4\3\2]Z\3\2\2\2][\3\2\2\2]\\\3"+
		"\2\2\2^\17\3\2\2\2_`\5.\30\2`a\7\21\2\2ab\5 \21\2b\21\3\2\2\2cj\5\24\13"+
		"\2dj\5\26\f\2ej\5\30\r\2fj\5\32\16\2gj\5\36\20\2hj\5\34\17\2ic\3\2\2\2"+
		"id\3\2\2\2ie\3\2\2\2if\3\2\2\2ig\3\2\2\2ih\3\2\2\2j\23\3\2\2\2kl\7\5\2"+
		"\2lm\7\16\2\2mn\5\60\31\2no\7\17\2\2op\7\20\2\2pq\5\"\22\2q\25\3\2\2\2"+
		"rs\7\6\2\2st\7\16\2\2tu\5\60\31\2uv\7\17\2\2vw\7\20\2\2wx\5$\23\2x\27"+
		"\3\2\2\2yz\7\7\2\2z{\7\16\2\2{|\5\60\31\2|}\7\17\2\2}~\7\20\2\2~\177\5"+
		"&\24\2\177\31\3\2\2\2\u0080\u0081\7\b\2\2\u0081\u0082\7\16\2\2\u0082\u0083"+
		"\5\60\31\2\u0083\u0084\7\17\2\2\u0084\u0085\7\20\2\2\u0085\u0086\5(\25"+
		"\2\u0086\33\3\2\2\2\u0087\u0088\7\n\2\2\u0088\u0089\7\16\2\2\u0089\u008a"+
		"\5\60\31\2\u008a\u008b\7\17\2\2\u008b\u008c\7\20\2\2\u008c\u008d\5*\26"+
		"\2\u008d\35\3\2\2\2\u008e\u008f\7\t\2\2\u008f\u0090\7\20\2\2\u0090\u0091"+
		"\5,\27\2\u0091\37\3\2\2\2\u0092\u0093\7\4\2\2\u0093!\3\2\2\2\u0094\u0095"+
		"\7\f\2\2\u0095#\3\2\2\2\u0096\u0097\7\f\2\2\u0097%\3\2\2\2\u0098\u0099"+
		"\7\f\2\2\u0099\'\3\2\2\2\u009a\u009b\t\2\2\2\u009b)\3\2\2\2\u009c\u009d"+
		"\7\f\2\2\u009d+\3\2\2\2\u009e\u009f\7\f\2\2\u009f-\3\2\2\2\u00a0\u00a1"+
		"\7\r\2\2\u00a1/\3\2\2\2\u00a2\u00a3\t\3\2\2\u00a3\61\3\2\2\2\t\64>GNW"+
		"]i";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}