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
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, BY_SAMPLE=6, BY_BARCODE=7, DOUBLE_QUOTE=8, 
		SINGLE_QUOTE=9, LETTER=10, NUMBER=11, SPACE=12, WS=13;
	public static final int
		RULE_demultiplexArguments = 0, RULE_bySample = 1, RULE_byBarcode = 2, 
		RULE_inputFileName = 3, RULE_fileName = 4, RULE_doubleQuotedFileName = 5, 
		RULE_singleQuotedFileName = 6, RULE_notQuotedFileName = 7, RULE_barcodeName = 8;
	public static final String[] ruleNames = {
		"demultiplexArguments", "bySample", "byBarcode", "inputFileName", "fileName", 
		"doubleQuotedFileName", "singleQuotedFileName", "notQuotedFileName", "barcodeName"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "'-'", "'.'", "','", "'!'", "'_'", "'--by-sample'", "'--by-barcode'", 
		"'\"'", "'''"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, null, null, null, null, null, "BY_SAMPLE", "BY_BARCODE", "DOUBLE_QUOTE", 
		"SINGLE_QUOTE", "LETTER", "NUMBER", "SPACE", "WS"
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
		public List<BySampleContext> bySample() {
			return getRuleContexts(BySampleContext.class);
		}
		public BySampleContext bySample(int i) {
			return getRuleContext(BySampleContext.class,i);
		}
		public List<ByBarcodeContext> byBarcode() {
			return getRuleContexts(ByBarcodeContext.class);
		}
		public ByBarcodeContext byBarcode(int i) {
			return getRuleContext(ByBarcodeContext.class,i);
		}
		public List<InputFileNameContext> inputFileName() {
			return getRuleContexts(InputFileNameContext.class);
		}
		public InputFileNameContext inputFileName(int i) {
			return getRuleContext(InputFileNameContext.class,i);
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
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(21); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				setState(21);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case BY_SAMPLE:
					{
					setState(18);
					bySample();
					}
					break;
				case BY_BARCODE:
					{
					setState(19);
					byBarcode();
					}
					break;
				case SPACE:
					{
					setState(20);
					inputFileName();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				setState(23); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << BY_SAMPLE) | (1L << BY_BARCODE) | (1L << SPACE))) != 0) );
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

	public static class BySampleContext extends ParserRuleContext {
		public TerminalNode BY_SAMPLE() { return getToken(DemultiplexGrammarParser.BY_SAMPLE, 0); }
		public TerminalNode SPACE() { return getToken(DemultiplexGrammarParser.SPACE, 0); }
		public FileNameContext fileName() {
			return getRuleContext(FileNameContext.class,0);
		}
		public BySampleContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_bySample; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DemultiplexGrammarListener ) ((DemultiplexGrammarListener)listener).enterBySample(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DemultiplexGrammarListener ) ((DemultiplexGrammarListener)listener).exitBySample(this);
		}
	}

	public final BySampleContext bySample() throws RecognitionException {
		BySampleContext _localctx = new BySampleContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_bySample);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(25);
			match(BY_SAMPLE);
			setState(26);
			match(SPACE);
			setState(27);
			fileName();
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

	public static class ByBarcodeContext extends ParserRuleContext {
		public TerminalNode BY_BARCODE() { return getToken(DemultiplexGrammarParser.BY_BARCODE, 0); }
		public TerminalNode SPACE() { return getToken(DemultiplexGrammarParser.SPACE, 0); }
		public BarcodeNameContext barcodeName() {
			return getRuleContext(BarcodeNameContext.class,0);
		}
		public ByBarcodeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_byBarcode; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DemultiplexGrammarListener ) ((DemultiplexGrammarListener)listener).enterByBarcode(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DemultiplexGrammarListener ) ((DemultiplexGrammarListener)listener).exitByBarcode(this);
		}
	}

	public final ByBarcodeContext byBarcode() throws RecognitionException {
		ByBarcodeContext _localctx = new ByBarcodeContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_byBarcode);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(29);
			match(BY_BARCODE);
			setState(30);
			match(SPACE);
			setState(31);
			barcodeName();
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

	public static class InputFileNameContext extends ParserRuleContext {
		public TerminalNode SPACE() { return getToken(DemultiplexGrammarParser.SPACE, 0); }
		public FileNameContext fileName() {
			return getRuleContext(FileNameContext.class,0);
		}
		public InputFileNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_inputFileName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DemultiplexGrammarListener ) ((DemultiplexGrammarListener)listener).enterInputFileName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DemultiplexGrammarListener ) ((DemultiplexGrammarListener)listener).exitInputFileName(this);
		}
	}

	public final InputFileNameContext inputFileName() throws RecognitionException {
		InputFileNameContext _localctx = new InputFileNameContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_inputFileName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(33);
			match(SPACE);
			setState(34);
			fileName();
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

	public static class FileNameContext extends ParserRuleContext {
		public DoubleQuotedFileNameContext doubleQuotedFileName() {
			return getRuleContext(DoubleQuotedFileNameContext.class,0);
		}
		public SingleQuotedFileNameContext singleQuotedFileName() {
			return getRuleContext(SingleQuotedFileNameContext.class,0);
		}
		public NotQuotedFileNameContext notQuotedFileName() {
			return getRuleContext(NotQuotedFileNameContext.class,0);
		}
		public FileNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fileName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DemultiplexGrammarListener ) ((DemultiplexGrammarListener)listener).enterFileName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DemultiplexGrammarListener ) ((DemultiplexGrammarListener)listener).exitFileName(this);
		}
	}

	public final FileNameContext fileName() throws RecognitionException {
		FileNameContext _localctx = new FileNameContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_fileName);
		try {
			setState(39);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case DOUBLE_QUOTE:
				enterOuterAlt(_localctx, 1);
				{
				setState(36);
				doubleQuotedFileName();
				}
				break;
			case SINGLE_QUOTE:
				enterOuterAlt(_localctx, 2);
				{
				setState(37);
				singleQuotedFileName();
				}
				break;
			case T__0:
			case T__1:
			case T__2:
			case T__3:
			case T__4:
			case LETTER:
			case NUMBER:
				enterOuterAlt(_localctx, 3);
				{
				setState(38);
				notQuotedFileName();
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

	public static class DoubleQuotedFileNameContext extends ParserRuleContext {
		public List<TerminalNode> DOUBLE_QUOTE() { return getTokens(DemultiplexGrammarParser.DOUBLE_QUOTE); }
		public TerminalNode DOUBLE_QUOTE(int i) {
			return getToken(DemultiplexGrammarParser.DOUBLE_QUOTE, i);
		}
		public List<TerminalNode> LETTER() { return getTokens(DemultiplexGrammarParser.LETTER); }
		public TerminalNode LETTER(int i) {
			return getToken(DemultiplexGrammarParser.LETTER, i);
		}
		public List<TerminalNode> NUMBER() { return getTokens(DemultiplexGrammarParser.NUMBER); }
		public TerminalNode NUMBER(int i) {
			return getToken(DemultiplexGrammarParser.NUMBER, i);
		}
		public List<TerminalNode> SPACE() { return getTokens(DemultiplexGrammarParser.SPACE); }
		public TerminalNode SPACE(int i) {
			return getToken(DemultiplexGrammarParser.SPACE, i);
		}
		public DoubleQuotedFileNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_doubleQuotedFileName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DemultiplexGrammarListener ) ((DemultiplexGrammarListener)listener).enterDoubleQuotedFileName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DemultiplexGrammarListener ) ((DemultiplexGrammarListener)listener).exitDoubleQuotedFileName(this);
		}
	}

	public final DoubleQuotedFileNameContext doubleQuotedFileName() throws RecognitionException {
		DoubleQuotedFileNameContext _localctx = new DoubleQuotedFileNameContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_doubleQuotedFileName);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(41);
			match(DOUBLE_QUOTE);
			setState(43); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(42);
				_la = _input.LA(1);
				if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << T__1) | (1L << T__2) | (1L << T__3) | (1L << T__4) | (1L << LETTER) | (1L << NUMBER) | (1L << SPACE))) != 0)) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				}
				setState(45); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << T__1) | (1L << T__2) | (1L << T__3) | (1L << T__4) | (1L << LETTER) | (1L << NUMBER) | (1L << SPACE))) != 0) );
			setState(47);
			match(DOUBLE_QUOTE);
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

	public static class SingleQuotedFileNameContext extends ParserRuleContext {
		public List<TerminalNode> SINGLE_QUOTE() { return getTokens(DemultiplexGrammarParser.SINGLE_QUOTE); }
		public TerminalNode SINGLE_QUOTE(int i) {
			return getToken(DemultiplexGrammarParser.SINGLE_QUOTE, i);
		}
		public List<TerminalNode> LETTER() { return getTokens(DemultiplexGrammarParser.LETTER); }
		public TerminalNode LETTER(int i) {
			return getToken(DemultiplexGrammarParser.LETTER, i);
		}
		public List<TerminalNode> NUMBER() { return getTokens(DemultiplexGrammarParser.NUMBER); }
		public TerminalNode NUMBER(int i) {
			return getToken(DemultiplexGrammarParser.NUMBER, i);
		}
		public List<TerminalNode> SPACE() { return getTokens(DemultiplexGrammarParser.SPACE); }
		public TerminalNode SPACE(int i) {
			return getToken(DemultiplexGrammarParser.SPACE, i);
		}
		public SingleQuotedFileNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_singleQuotedFileName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DemultiplexGrammarListener ) ((DemultiplexGrammarListener)listener).enterSingleQuotedFileName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DemultiplexGrammarListener ) ((DemultiplexGrammarListener)listener).exitSingleQuotedFileName(this);
		}
	}

	public final SingleQuotedFileNameContext singleQuotedFileName() throws RecognitionException {
		SingleQuotedFileNameContext _localctx = new SingleQuotedFileNameContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_singleQuotedFileName);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(49);
			match(SINGLE_QUOTE);
			setState(51); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(50);
				_la = _input.LA(1);
				if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << T__1) | (1L << T__2) | (1L << T__3) | (1L << T__4) | (1L << LETTER) | (1L << NUMBER) | (1L << SPACE))) != 0)) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				}
				setState(53); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << T__1) | (1L << T__2) | (1L << T__3) | (1L << T__4) | (1L << LETTER) | (1L << NUMBER) | (1L << SPACE))) != 0) );
			setState(55);
			match(SINGLE_QUOTE);
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

	public static class NotQuotedFileNameContext extends ParserRuleContext {
		public List<TerminalNode> LETTER() { return getTokens(DemultiplexGrammarParser.LETTER); }
		public TerminalNode LETTER(int i) {
			return getToken(DemultiplexGrammarParser.LETTER, i);
		}
		public List<TerminalNode> NUMBER() { return getTokens(DemultiplexGrammarParser.NUMBER); }
		public TerminalNode NUMBER(int i) {
			return getToken(DemultiplexGrammarParser.NUMBER, i);
		}
		public NotQuotedFileNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_notQuotedFileName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DemultiplexGrammarListener ) ((DemultiplexGrammarListener)listener).enterNotQuotedFileName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DemultiplexGrammarListener ) ((DemultiplexGrammarListener)listener).exitNotQuotedFileName(this);
		}
	}

	public final NotQuotedFileNameContext notQuotedFileName() throws RecognitionException {
		NotQuotedFileNameContext _localctx = new NotQuotedFileNameContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_notQuotedFileName);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(58); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(57);
				_la = _input.LA(1);
				if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << T__1) | (1L << T__2) | (1L << T__3) | (1L << T__4) | (1L << LETTER) | (1L << NUMBER))) != 0)) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				}
				setState(60); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << T__1) | (1L << T__2) | (1L << T__3) | (1L << T__4) | (1L << LETTER) | (1L << NUMBER))) != 0) );
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

	public static class BarcodeNameContext extends ParserRuleContext {
		public List<TerminalNode> LETTER() { return getTokens(DemultiplexGrammarParser.LETTER); }
		public TerminalNode LETTER(int i) {
			return getToken(DemultiplexGrammarParser.LETTER, i);
		}
		public List<TerminalNode> NUMBER() { return getTokens(DemultiplexGrammarParser.NUMBER); }
		public TerminalNode NUMBER(int i) {
			return getToken(DemultiplexGrammarParser.NUMBER, i);
		}
		public BarcodeNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_barcodeName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DemultiplexGrammarListener ) ((DemultiplexGrammarListener)listener).enterBarcodeName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DemultiplexGrammarListener ) ((DemultiplexGrammarListener)listener).exitBarcodeName(this);
		}
	}

	public final BarcodeNameContext barcodeName() throws RecognitionException {
		BarcodeNameContext _localctx = new BarcodeNameContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_barcodeName);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(63); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(62);
				_la = _input.LA(1);
				if ( !(_la==LETTER || _la==NUMBER) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				}
				setState(65); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==LETTER || _la==NUMBER );
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\17F\4\2\t\2\4\3\t"+
		"\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\3\2\3\2\3\2"+
		"\6\2\30\n\2\r\2\16\2\31\3\3\3\3\3\3\3\3\3\4\3\4\3\4\3\4\3\5\3\5\3\5\3"+
		"\6\3\6\3\6\5\6*\n\6\3\7\3\7\6\7.\n\7\r\7\16\7/\3\7\3\7\3\b\3\b\6\b\66"+
		"\n\b\r\b\16\b\67\3\b\3\b\3\t\6\t=\n\t\r\t\16\t>\3\n\6\nB\n\n\r\n\16\n"+
		"C\3\n\2\2\13\2\4\6\b\n\f\16\20\22\2\5\4\2\3\7\f\16\4\2\3\7\f\r\3\2\f\r"+
		"\2E\2\27\3\2\2\2\4\33\3\2\2\2\6\37\3\2\2\2\b#\3\2\2\2\n)\3\2\2\2\f+\3"+
		"\2\2\2\16\63\3\2\2\2\20<\3\2\2\2\22A\3\2\2\2\24\30\5\4\3\2\25\30\5\6\4"+
		"\2\26\30\5\b\5\2\27\24\3\2\2\2\27\25\3\2\2\2\27\26\3\2\2\2\30\31\3\2\2"+
		"\2\31\27\3\2\2\2\31\32\3\2\2\2\32\3\3\2\2\2\33\34\7\b\2\2\34\35\7\16\2"+
		"\2\35\36\5\n\6\2\36\5\3\2\2\2\37 \7\t\2\2 !\7\16\2\2!\"\5\22\n\2\"\7\3"+
		"\2\2\2#$\7\16\2\2$%\5\n\6\2%\t\3\2\2\2&*\5\f\7\2\'*\5\16\b\2(*\5\20\t"+
		"\2)&\3\2\2\2)\'\3\2\2\2)(\3\2\2\2*\13\3\2\2\2+-\7\n\2\2,.\t\2\2\2-,\3"+
		"\2\2\2./\3\2\2\2/-\3\2\2\2/\60\3\2\2\2\60\61\3\2\2\2\61\62\7\n\2\2\62"+
		"\r\3\2\2\2\63\65\7\13\2\2\64\66\t\2\2\2\65\64\3\2\2\2\66\67\3\2\2\2\67"+
		"\65\3\2\2\2\678\3\2\2\289\3\2\2\29:\7\13\2\2:\17\3\2\2\2;=\t\3\2\2<;\3"+
		"\2\2\2=>\3\2\2\2><\3\2\2\2>?\3\2\2\2?\21\3\2\2\2@B\t\4\2\2A@\3\2\2\2B"+
		"C\3\2\2\2CA\3\2\2\2CD\3\2\2\2D\23\3\2\2\2\t\27\31)/\67>C";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}