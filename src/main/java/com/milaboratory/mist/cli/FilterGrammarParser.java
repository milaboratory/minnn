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
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, STRING=6, LEN=7, NUMBER=8, GROUP_NAME=9, 
		WS=10;
	public static final int
		RULE_filter = 0, RULE_or = 1, RULE_or_operand = 2, RULE_and = 3, RULE_and_operand = 4, 
		RULE_pattern = 5, RULE_len = 6;
	public static final String[] ruleNames = {
		"filter", "or", "or_operand", "and", "and_operand", "pattern", "len"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "'|'", "'&'", "'~'", "'('", "')='", null, "'Len'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, null, null, null, null, null, "STRING", "LEN", "NUMBER", "GROUP_NAME", 
		"WS"
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
			setState(18);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,0,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(14);
				or();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(15);
				and();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(16);
				pattern();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(17);
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
		public List<Or_operandContext> or_operand() {
			return getRuleContexts(Or_operandContext.class);
		}
		public Or_operandContext or_operand(int i) {
			return getRuleContext(Or_operandContext.class,i);
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
		enterRule(_localctx, 2, RULE_or);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(20);
			or_operand();
			setState(21);
			match(T__0);
			setState(22);
			or_operand();
			setState(27);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__0) {
				{
				{
				setState(23);
				match(T__0);
				setState(24);
				or_operand();
				}
				}
				setState(29);
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

	public static class Or_operandContext extends ParserRuleContext {
		public AndContext and() {
			return getRuleContext(AndContext.class,0);
		}
		public PatternContext pattern() {
			return getRuleContext(PatternContext.class,0);
		}
		public LenContext len() {
			return getRuleContext(LenContext.class,0);
		}
		public Or_operandContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_or_operand; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).enterOr_operand(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).exitOr_operand(this);
		}
	}

	public final Or_operandContext or_operand() throws RecognitionException {
		Or_operandContext _localctx = new Or_operandContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_or_operand);
		try {
			setState(33);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,2,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(30);
				and();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(31);
				pattern();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(32);
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

	public static class AndContext extends ParserRuleContext {
		public List<And_operandContext> and_operand() {
			return getRuleContexts(And_operandContext.class);
		}
		public And_operandContext and_operand(int i) {
			return getRuleContext(And_operandContext.class,i);
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
		enterRule(_localctx, 6, RULE_and);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(35);
			and_operand();
			setState(36);
			match(T__1);
			setState(37);
			and_operand();
			setState(42);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(38);
				match(T__1);
				setState(39);
				and_operand();
				}
				}
				setState(44);
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

	public static class And_operandContext extends ParserRuleContext {
		public PatternContext pattern() {
			return getRuleContext(PatternContext.class,0);
		}
		public LenContext len() {
			return getRuleContext(LenContext.class,0);
		}
		public And_operandContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_and_operand; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).enterAnd_operand(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof FilterGrammarListener ) ((FilterGrammarListener)listener).exitAnd_operand(this);
		}
	}

	public final And_operandContext and_operand() throws RecognitionException {
		And_operandContext _localctx = new And_operandContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_and_operand);
		try {
			setState(47);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case GROUP_NAME:
				enterOuterAlt(_localctx, 1);
				{
				setState(45);
				pattern();
				}
				break;
			case LEN:
				enterOuterAlt(_localctx, 2);
				{
				setState(46);
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

	public static class PatternContext extends ParserRuleContext {
		public TerminalNode GROUP_NAME() { return getToken(FilterGrammarParser.GROUP_NAME, 0); }
		public TerminalNode STRING() { return getToken(FilterGrammarParser.STRING, 0); }
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
		enterRule(_localctx, 10, RULE_pattern);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(49);
			match(GROUP_NAME);
			setState(50);
			match(T__2);
			setState(51);
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

	public static class LenContext extends ParserRuleContext {
		public TerminalNode LEN() { return getToken(FilterGrammarParser.LEN, 0); }
		public TerminalNode GROUP_NAME() { return getToken(FilterGrammarParser.GROUP_NAME, 0); }
		public TerminalNode NUMBER() { return getToken(FilterGrammarParser.NUMBER, 0); }
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
		enterRule(_localctx, 12, RULE_len);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(53);
			match(LEN);
			setState(54);
			match(T__3);
			setState(55);
			match(GROUP_NAME);
			setState(56);
			match(T__4);
			setState(57);
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\f>\4\2\t\2\4\3\t"+
		"\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\3\2\3\2\3\2\3\2\5\2\25\n\2"+
		"\3\3\3\3\3\3\3\3\3\3\7\3\34\n\3\f\3\16\3\37\13\3\3\4\3\4\3\4\5\4$\n\4"+
		"\3\5\3\5\3\5\3\5\3\5\7\5+\n\5\f\5\16\5.\13\5\3\6\3\6\5\6\62\n\6\3\7\3"+
		"\7\3\7\3\7\3\b\3\b\3\b\3\b\3\b\3\b\3\b\2\2\t\2\4\6\b\n\f\16\2\2\2>\2\24"+
		"\3\2\2\2\4\26\3\2\2\2\6#\3\2\2\2\b%\3\2\2\2\n\61\3\2\2\2\f\63\3\2\2\2"+
		"\16\67\3\2\2\2\20\25\5\4\3\2\21\25\5\b\5\2\22\25\5\f\7\2\23\25\5\16\b"+
		"\2\24\20\3\2\2\2\24\21\3\2\2\2\24\22\3\2\2\2\24\23\3\2\2\2\25\3\3\2\2"+
		"\2\26\27\5\6\4\2\27\30\7\3\2\2\30\35\5\6\4\2\31\32\7\3\2\2\32\34\5\6\4"+
		"\2\33\31\3\2\2\2\34\37\3\2\2\2\35\33\3\2\2\2\35\36\3\2\2\2\36\5\3\2\2"+
		"\2\37\35\3\2\2\2 $\5\b\5\2!$\5\f\7\2\"$\5\16\b\2# \3\2\2\2#!\3\2\2\2#"+
		"\"\3\2\2\2$\7\3\2\2\2%&\5\n\6\2&\'\7\4\2\2\',\5\n\6\2()\7\4\2\2)+\5\n"+
		"\6\2*(\3\2\2\2+.\3\2\2\2,*\3\2\2\2,-\3\2\2\2-\t\3\2\2\2.,\3\2\2\2/\62"+
		"\5\f\7\2\60\62\5\16\b\2\61/\3\2\2\2\61\60\3\2\2\2\62\13\3\2\2\2\63\64"+
		"\7\13\2\2\64\65\7\5\2\2\65\66\7\b\2\2\66\r\3\2\2\2\678\7\t\2\289\7\6\2"+
		"\29:\7\13\2\2:;\7\7\2\2;<\7\n\2\2<\17\3\2\2\2\7\24\35#,\61";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}