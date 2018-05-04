// Generated from FilterGrammar.g4 by ANTLR 4.7
package com.milaboratory.mist.cli;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class FilterGrammarLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.7", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		SINGLE_QUOTE=1, STRING=2, MIN_CONSENSUS_READS=3, LEN=4, NUMBER=5, GROUP_NAME=6, 
		OPEN_PARENTHESIS=7, CLOSED_PARENTHESIS=8, EQUALS=9, TILDE=10, AND=11, 
		OR=12, WS=13;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"SINGLE_QUOTE", "STRING", "MIN_CONSENSUS_READS", "LEN", "NUMBER", "GROUP_NAME", 
		"OPEN_PARENTHESIS", "CLOSED_PARENTHESIS", "EQUALS", "TILDE", "AND", "OR", 
		"WS"
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


	public FilterGrammarLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "FilterGrammar.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\17[\b\1\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\4\16\t\16\3\2\3\2\3\3\3\3\7\3\"\n\3\f\3\16\3%\13\3"+
		"\3\3\3\3\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3"+
		"\4\3\4\3\4\3\5\3\5\3\5\3\5\3\6\6\6@\n\6\r\6\16\6A\3\7\6\7E\n\7\r\7\16"+
		"\7F\3\b\3\b\3\t\3\t\3\n\3\n\3\13\3\13\3\f\3\f\3\r\3\r\3\16\6\16V\n\16"+
		"\r\16\16\16W\3\16\3\16\3#\2\17\3\3\5\4\7\5\t\6\13\7\r\b\17\t\21\n\23\13"+
		"\25\f\27\r\31\16\33\17\3\2\5\3\2\62;\5\2\62;C\\c|\5\2\13\f\17\17\"\"\2"+
		"^\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2"+
		"\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2"+
		"\2\31\3\2\2\2\2\33\3\2\2\2\3\35\3\2\2\2\5\37\3\2\2\2\7(\3\2\2\2\t:\3\2"+
		"\2\2\13?\3\2\2\2\rD\3\2\2\2\17H\3\2\2\2\21J\3\2\2\2\23L\3\2\2\2\25N\3"+
		"\2\2\2\27P\3\2\2\2\31R\3\2\2\2\33U\3\2\2\2\35\36\7)\2\2\36\4\3\2\2\2\37"+
		"#\5\3\2\2 \"\13\2\2\2! \3\2\2\2\"%\3\2\2\2#$\3\2\2\2#!\3\2\2\2$&\3\2\2"+
		"\2%#\3\2\2\2&\'\5\3\2\2\'\6\3\2\2\2()\7O\2\2)*\7k\2\2*+\7p\2\2+,\7E\2"+
		"\2,-\7q\2\2-.\7p\2\2./\7u\2\2/\60\7g\2\2\60\61\7p\2\2\61\62\7u\2\2\62"+
		"\63\7w\2\2\63\64\7u\2\2\64\65\7T\2\2\65\66\7g\2\2\66\67\7c\2\2\678\7f"+
		"\2\289\7u\2\29\b\3\2\2\2:;\7N\2\2;<\7g\2\2<=\7p\2\2=\n\3\2\2\2>@\t\2\2"+
		"\2?>\3\2\2\2@A\3\2\2\2A?\3\2\2\2AB\3\2\2\2B\f\3\2\2\2CE\t\3\2\2DC\3\2"+
		"\2\2EF\3\2\2\2FD\3\2\2\2FG\3\2\2\2G\16\3\2\2\2HI\7*\2\2I\20\3\2\2\2JK"+
		"\7+\2\2K\22\3\2\2\2LM\7?\2\2M\24\3\2\2\2NO\7\u0080\2\2O\26\3\2\2\2PQ\7"+
		"(\2\2Q\30\3\2\2\2RS\7~\2\2S\32\3\2\2\2TV\t\4\2\2UT\3\2\2\2VW\3\2\2\2W"+
		"U\3\2\2\2WX\3\2\2\2XY\3\2\2\2YZ\b\16\2\2Z\34\3\2\2\2\7\2#AFW\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}