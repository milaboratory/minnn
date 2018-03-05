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
		SINGLE_QUOTE=1, STRING=2, LEN=3, NUMBER=4, GROUP_NAME=5, OPEN_PARENTHESIS=6, 
		CLOSED_PARENTHESIS=7, EQUALS=8, TILDE=9, AND=10, OR=11, WS=12;
	public static final int
		RULE_filter = 0, RULE_filterInParentheses = 1, RULE_anySingleFilter = 2, 
		RULE_or = 3, RULE_orOperand = 4, RULE_and = 5, RULE_andOperand = 6, RULE_pattern = 7, 
		RULE_len = 8, RULE_patternString = 9, RULE_groupName = 10, RULE_groupLength = 11;
	public static final String[] ruleNames = {
		"filter", "filterInParentheses", "anySingleFilter", "or", "orOperand", 
		"and", "andOperand", "pattern", "len", "patternString", "groupName", "groupLength"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "'''", null, "'Len'", null, null, "'('", "')'", "'='", "'~'", "'&'", 
		"'|'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "SINGLE_QUOTE", "STRING", "LEN", "NUMBER", "GROUP_NAME", "OPEN_PARENTHESIS", 
		"CLOSED_PARENTHESIS", "EQUALS", "TILDE", "AND", "OR", "WS"
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
			setState(26);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,0,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(24);
				filterInParentheses();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(25);
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
			setState(28);
			match(OPEN_PARENTHESIS);
			setState(29);
			anySingleFilter();
			setState(30);
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
			setState(36);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,1,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(32);
				or();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(33);
				and();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(34);
				pattern();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(35);
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
			setState(38);
			orOperand();
			setState(39);
			match(OR);
			setState(40);
			orOperand();
			setState(45);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==OR) {
				{
				{
				setState(41);
				match(OR);
				setState(42);
				orOperand();
				}
				}
				setState(47);
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
			setState(52);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,3,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(48);
				and();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(49);
				pattern();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(50);
				len();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(51);
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
			setState(54);
			andOperand();
			setState(55);
			match(AND);
			setState(56);
			andOperand();
			setState(61);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AND) {
				{
				{
				setState(57);
				match(AND);
				setState(58);
				andOperand();
				}
				}
				setState(63);
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
			setState(67);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case GROUP_NAME:
				enterOuterAlt(_localctx, 1);
				{
				setState(64);
				pattern();
				}
				break;
			case LEN:
				enterOuterAlt(_localctx, 2);
				{
				setState(65);
				len();
				}
				break;
			case OPEN_PARENTHESIS:
				enterOuterAlt(_localctx, 3);
				{
				setState(66);
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
			setState(69);
			groupName();
			setState(70);
			match(TILDE);
			setState(71);
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
		enterRule(_localctx, 16, RULE_len);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(73);
			match(LEN);
			setState(74);
			match(OPEN_PARENTHESIS);
			setState(75);
			groupName();
			setState(76);
			match(CLOSED_PARENTHESIS);
			setState(77);
			match(EQUALS);
			setState(78);
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
		enterRule(_localctx, 18, RULE_patternString);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(80);
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
		enterRule(_localctx, 20, RULE_groupName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(82);
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
		enterRule(_localctx, 22, RULE_groupLength);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(84);
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\16Y\4\2\t\2\4\3\t"+
		"\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t\13\4"+
		"\f\t\f\4\r\t\r\3\2\3\2\5\2\35\n\2\3\3\3\3\3\3\3\3\3\4\3\4\3\4\3\4\5\4"+
		"\'\n\4\3\5\3\5\3\5\3\5\3\5\7\5.\n\5\f\5\16\5\61\13\5\3\6\3\6\3\6\3\6\5"+
		"\6\67\n\6\3\7\3\7\3\7\3\7\3\7\7\7>\n\7\f\7\16\7A\13\7\3\b\3\b\3\b\5\b"+
		"F\n\b\3\t\3\t\3\t\3\t\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\13\3\13\3\f\3\f\3"+
		"\r\3\r\3\r\2\2\16\2\4\6\b\n\f\16\20\22\24\26\30\2\2\2W\2\34\3\2\2\2\4"+
		"\36\3\2\2\2\6&\3\2\2\2\b(\3\2\2\2\n\66\3\2\2\2\f8\3\2\2\2\16E\3\2\2\2"+
		"\20G\3\2\2\2\22K\3\2\2\2\24R\3\2\2\2\26T\3\2\2\2\30V\3\2\2\2\32\35\5\4"+
		"\3\2\33\35\5\6\4\2\34\32\3\2\2\2\34\33\3\2\2\2\35\3\3\2\2\2\36\37\7\b"+
		"\2\2\37 \5\6\4\2 !\7\t\2\2!\5\3\2\2\2\"\'\5\b\5\2#\'\5\f\7\2$\'\5\20\t"+
		"\2%\'\5\22\n\2&\"\3\2\2\2&#\3\2\2\2&$\3\2\2\2&%\3\2\2\2\'\7\3\2\2\2()"+
		"\5\n\6\2)*\7\r\2\2*/\5\n\6\2+,\7\r\2\2,.\5\n\6\2-+\3\2\2\2.\61\3\2\2\2"+
		"/-\3\2\2\2/\60\3\2\2\2\60\t\3\2\2\2\61/\3\2\2\2\62\67\5\f\7\2\63\67\5"+
		"\20\t\2\64\67\5\22\n\2\65\67\5\4\3\2\66\62\3\2\2\2\66\63\3\2\2\2\66\64"+
		"\3\2\2\2\66\65\3\2\2\2\67\13\3\2\2\289\5\16\b\29:\7\f\2\2:?\5\16\b\2;"+
		"<\7\f\2\2<>\5\16\b\2=;\3\2\2\2>A\3\2\2\2?=\3\2\2\2?@\3\2\2\2@\r\3\2\2"+
		"\2A?\3\2\2\2BF\5\20\t\2CF\5\22\n\2DF\5\4\3\2EB\3\2\2\2EC\3\2\2\2ED\3\2"+
		"\2\2F\17\3\2\2\2GH\5\26\f\2HI\7\13\2\2IJ\5\24\13\2J\21\3\2\2\2KL\7\5\2"+
		"\2LM\7\b\2\2MN\5\26\f\2NO\7\t\2\2OP\7\n\2\2PQ\5\30\r\2Q\23\3\2\2\2RS\7"+
		"\4\2\2S\25\3\2\2\2TU\7\7\2\2U\27\3\2\2\2VW\7\6\2\2W\31\3\2\2\2\b\34&/"+
		"\66?E";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}