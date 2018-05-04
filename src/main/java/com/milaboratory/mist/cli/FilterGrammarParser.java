// Generated from FilterGrammar.g4 by ANTLR 4.7
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
public class FilterGrammarParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.7", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		SINGLE_QUOTE=1, STRING=2, MIN_CONSENSUS_READS=3, LEN=4, NUMBER=5, GROUP_NAME=6, 
		OPEN_PARENTHESIS=7, CLOSED_PARENTHESIS=8, EQUALS=9, TILDE=10, AND=11, 
		OR=12, WS=13;
	public static final int
		RULE_filter = 0, RULE_filterInParentheses = 1, RULE_anySingleFilter = 2, 
		RULE_or = 3, RULE_orOperand = 4, RULE_and = 5, RULE_andOperand = 6, RULE_pattern = 7, 
		RULE_minConsensusReads = 8, RULE_len = 9, RULE_patternString = 10, RULE_groupName = 11, 
		RULE_minConsensusReadsNum = 12, RULE_groupLength = 13;
	public static final String[] ruleNames = {
		"filter", "filterInParentheses", "anySingleFilter", "or", "orOperand", 
		"and", "andOperand", "pattern", "minConsensusReads", "len", "patternString", 
		"groupName", "minConsensusReadsNum", "groupLength"
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
			setState(30);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,0,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(28);
				filterInParentheses();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(29);
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
			setState(32);
			match(OPEN_PARENTHESIS);
			setState(33);
			anySingleFilter();
			setState(34);
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
			setState(41);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,1,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(36);
				or();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(37);
				and();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(38);
				pattern();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(39);
				minConsensusReads();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(40);
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
			setState(43);
			orOperand();
			setState(44);
			match(OR);
			setState(45);
			orOperand();
			setState(50);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==OR) {
				{
				{
				setState(46);
				match(OR);
				setState(47);
				orOperand();
				}
				}
				setState(52);
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
			setState(58);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,3,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(53);
				and();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(54);
				pattern();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(55);
				minConsensusReads();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(56);
				len();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(57);
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
			setState(60);
			andOperand();
			setState(61);
			match(AND);
			setState(62);
			andOperand();
			setState(67);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AND) {
				{
				{
				setState(63);
				match(AND);
				setState(64);
				andOperand();
				}
				}
				setState(69);
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
			setState(74);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case GROUP_NAME:
				enterOuterAlt(_localctx, 1);
				{
				setState(70);
				pattern();
				}
				break;
			case MIN_CONSENSUS_READS:
				enterOuterAlt(_localctx, 2);
				{
				setState(71);
				minConsensusReads();
				}
				break;
			case LEN:
				enterOuterAlt(_localctx, 3);
				{
				setState(72);
				len();
				}
				break;
			case OPEN_PARENTHESIS:
				enterOuterAlt(_localctx, 4);
				{
				setState(73);
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
			setState(76);
			groupName();
			setState(77);
			match(TILDE);
			setState(78);
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
		enterRule(_localctx, 16, RULE_minConsensusReads);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(80);
			match(MIN_CONSENSUS_READS);
			setState(81);
			match(EQUALS);
			setState(82);
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
		public TerminalNode LEN() { return getToken(FilterGrammarParser.LEN, 0); }
		public TerminalNode OPEN_PARENTHESIS() { return getToken(FilterGrammarParser.OPEN_PARENTHESIS, 0); }
		public GroupNameContext groupName() {
			return getRuleContext(GroupNameContext.class,0);
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
		enterRule(_localctx, 18, RULE_len);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(84);
			match(LEN);
			setState(85);
			match(OPEN_PARENTHESIS);
			setState(86);
			groupName();
			setState(87);
			match(CLOSED_PARENTHESIS);
			setState(88);
			match(EQUALS);
			setState(89);
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
		enterRule(_localctx, 20, RULE_patternString);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(91);
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
		enterRule(_localctx, 22, RULE_groupName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(93);
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
		public TerminalNode NUMBER() { return getToken(FilterGrammarParser.NUMBER, 0); }
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
		enterRule(_localctx, 24, RULE_minConsensusReadsNum);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(95);
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
		public TerminalNode NUMBER() { return getToken(FilterGrammarParser.NUMBER, 0); }
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
		enterRule(_localctx, 26, RULE_groupLength);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(97);
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\17f\4\2\t\2\4\3\t"+
		"\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t\13\4"+
		"\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\3\2\3\2\5\2!\n\2\3\3\3\3\3\3\3\3\3"+
		"\4\3\4\3\4\3\4\3\4\5\4,\n\4\3\5\3\5\3\5\3\5\3\5\7\5\63\n\5\f\5\16\5\66"+
		"\13\5\3\6\3\6\3\6\3\6\3\6\5\6=\n\6\3\7\3\7\3\7\3\7\3\7\7\7D\n\7\f\7\16"+
		"\7G\13\7\3\b\3\b\3\b\3\b\5\bM\n\b\3\t\3\t\3\t\3\t\3\n\3\n\3\n\3\n\3\13"+
		"\3\13\3\13\3\13\3\13\3\13\3\13\3\f\3\f\3\r\3\r\3\16\3\16\3\17\3\17\3\17"+
		"\2\2\20\2\4\6\b\n\f\16\20\22\24\26\30\32\34\2\2\2e\2 \3\2\2\2\4\"\3\2"+
		"\2\2\6+\3\2\2\2\b-\3\2\2\2\n<\3\2\2\2\f>\3\2\2\2\16L\3\2\2\2\20N\3\2\2"+
		"\2\22R\3\2\2\2\24V\3\2\2\2\26]\3\2\2\2\30_\3\2\2\2\32a\3\2\2\2\34c\3\2"+
		"\2\2\36!\5\4\3\2\37!\5\6\4\2 \36\3\2\2\2 \37\3\2\2\2!\3\3\2\2\2\"#\7\t"+
		"\2\2#$\5\6\4\2$%\7\n\2\2%\5\3\2\2\2&,\5\b\5\2\',\5\f\7\2(,\5\20\t\2),"+
		"\5\22\n\2*,\5\24\13\2+&\3\2\2\2+\'\3\2\2\2+(\3\2\2\2+)\3\2\2\2+*\3\2\2"+
		"\2,\7\3\2\2\2-.\5\n\6\2./\7\16\2\2/\64\5\n\6\2\60\61\7\16\2\2\61\63\5"+
		"\n\6\2\62\60\3\2\2\2\63\66\3\2\2\2\64\62\3\2\2\2\64\65\3\2\2\2\65\t\3"+
		"\2\2\2\66\64\3\2\2\2\67=\5\f\7\28=\5\20\t\29=\5\22\n\2:=\5\24\13\2;=\5"+
		"\4\3\2<\67\3\2\2\2<8\3\2\2\2<9\3\2\2\2<:\3\2\2\2<;\3\2\2\2=\13\3\2\2\2"+
		">?\5\16\b\2?@\7\r\2\2@E\5\16\b\2AB\7\r\2\2BD\5\16\b\2CA\3\2\2\2DG\3\2"+
		"\2\2EC\3\2\2\2EF\3\2\2\2F\r\3\2\2\2GE\3\2\2\2HM\5\20\t\2IM\5\22\n\2JM"+
		"\5\24\13\2KM\5\4\3\2LH\3\2\2\2LI\3\2\2\2LJ\3\2\2\2LK\3\2\2\2M\17\3\2\2"+
		"\2NO\5\30\r\2OP\7\f\2\2PQ\5\26\f\2Q\21\3\2\2\2RS\7\5\2\2ST\7\13\2\2TU"+
		"\5\32\16\2U\23\3\2\2\2VW\7\6\2\2WX\7\t\2\2XY\5\30\r\2YZ\7\n\2\2Z[\7\13"+
		"\2\2[\\\5\34\17\2\\\25\3\2\2\2]^\7\4\2\2^\27\3\2\2\2_`\7\b\2\2`\31\3\2"+
		"\2\2ab\7\7\2\2b\33\3\2\2\2cd\7\7\2\2d\35\3\2\2\2\b +\64<EL";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}