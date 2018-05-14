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
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, BY_SAMPLE=6, BY_BARCODE=7, LETTER=8, 
		NUMBER=9, SPACE=10, DELIMITER=11, WS=12;
	public static final int
		RULE_demultiplexArguments = 0, RULE_bySample = 1, RULE_byBarcode = 2, 
		RULE_inputFileName = 3, RULE_fileName = 4, RULE_barcodeName = 5;
	public static final String[] ruleNames = {
		"demultiplexArguments", "bySample", "byBarcode", "inputFileName", "fileName", 
		"barcodeName"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "'-'", "'.'", "','", "'!'", "'_'", "'--by-sample'", "'--by-barcode'", 
		null, null, "' '", "'#'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, null, null, null, null, null, "BY_SAMPLE", "BY_BARCODE", "LETTER", 
		"NUMBER", "SPACE", "DELIMITER", "WS"
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
			setState(15); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				setState(15);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,0,_ctx) ) {
				case 1:
					{
					setState(12);
					bySample();
					}
					break;
				case 2:
					{
					setState(13);
					byBarcode();
					}
					break;
				case 3:
					{
					setState(14);
					inputFileName();
					}
					break;
				}
				}
				setState(17); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==DELIMITER );
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
		public List<TerminalNode> DELIMITER() { return getTokens(DemultiplexGrammarParser.DELIMITER); }
		public TerminalNode DELIMITER(int i) {
			return getToken(DemultiplexGrammarParser.DELIMITER, i);
		}
		public TerminalNode BY_SAMPLE() { return getToken(DemultiplexGrammarParser.BY_SAMPLE, 0); }
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
			setState(19);
			match(DELIMITER);
			setState(20);
			match(BY_SAMPLE);
			setState(21);
			match(DELIMITER);
			setState(22);
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
		public List<TerminalNode> DELIMITER() { return getTokens(DemultiplexGrammarParser.DELIMITER); }
		public TerminalNode DELIMITER(int i) {
			return getToken(DemultiplexGrammarParser.DELIMITER, i);
		}
		public TerminalNode BY_BARCODE() { return getToken(DemultiplexGrammarParser.BY_BARCODE, 0); }
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
			setState(24);
			match(DELIMITER);
			setState(25);
			match(BY_BARCODE);
			setState(26);
			match(DELIMITER);
			setState(27);
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
		public TerminalNode DELIMITER() { return getToken(DemultiplexGrammarParser.DELIMITER, 0); }
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
			setState(29);
			match(DELIMITER);
			setState(30);
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
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(33); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(32);
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
				setState(35); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << T__1) | (1L << T__2) | (1L << T__3) | (1L << T__4) | (1L << LETTER) | (1L << NUMBER) | (1L << SPACE))) != 0) );
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
		enterRule(_localctx, 10, RULE_barcodeName);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(38); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(37);
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
				setState(40); 
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\16-\4\2\t\2\4\3\t"+
		"\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\3\2\3\2\3\2\6\2\22\n\2\r\2\16\2\23"+
		"\3\3\3\3\3\3\3\3\3\3\3\4\3\4\3\4\3\4\3\4\3\5\3\5\3\5\3\6\6\6$\n\6\r\6"+
		"\16\6%\3\7\6\7)\n\7\r\7\16\7*\3\7\2\2\b\2\4\6\b\n\f\2\4\4\2\3\7\n\f\3"+
		"\2\n\13\2+\2\21\3\2\2\2\4\25\3\2\2\2\6\32\3\2\2\2\b\37\3\2\2\2\n#\3\2"+
		"\2\2\f(\3\2\2\2\16\22\5\4\3\2\17\22\5\6\4\2\20\22\5\b\5\2\21\16\3\2\2"+
		"\2\21\17\3\2\2\2\21\20\3\2\2\2\22\23\3\2\2\2\23\21\3\2\2\2\23\24\3\2\2"+
		"\2\24\3\3\2\2\2\25\26\7\r\2\2\26\27\7\b\2\2\27\30\7\r\2\2\30\31\5\n\6"+
		"\2\31\5\3\2\2\2\32\33\7\r\2\2\33\34\7\t\2\2\34\35\7\r\2\2\35\36\5\f\7"+
		"\2\36\7\3\2\2\2\37 \7\r\2\2 !\5\n\6\2!\t\3\2\2\2\"$\t\2\2\2#\"\3\2\2\2"+
		"$%\3\2\2\2%#\3\2\2\2%&\3\2\2\2&\13\3\2\2\2\')\t\3\2\2(\'\3\2\2\2)*\3\2"+
		"\2\2*(\3\2\2\2*+\3\2\2\2+\r\3\2\2\2\6\21\23%*";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}