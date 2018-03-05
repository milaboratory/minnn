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
		SINGLE_QUOTE=1, STRING=2, LEN=3, NUMBER=4, GROUP_NAME=5, OPEN_PARENTHESIS=6, 
		CLOSED_PARENTHESIS=7, EQUALS=8, TILDE=9, AND=10, OR=11, WS=12;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"SINGLE_QUOTE", "STRING", "LEN", "NUMBER", "GROUP_NAME", "OPEN_PARENTHESIS", 
		"CLOSED_PARENTHESIS", "EQUALS", "TILDE", "AND", "OR", "WS"
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\16G\b\1\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\3\2\3\2\3\3\3\3\7\3 \n\3\f\3\16\3#\13\3\3\3\3\3\3"+
		"\4\3\4\3\4\3\4\3\5\6\5,\n\5\r\5\16\5-\3\6\6\6\61\n\6\r\6\16\6\62\3\7\3"+
		"\7\3\b\3\b\3\t\3\t\3\n\3\n\3\13\3\13\3\f\3\f\3\r\6\rB\n\r\r\r\16\rC\3"+
		"\r\3\r\3!\2\16\3\3\5\4\7\5\t\6\13\7\r\b\17\t\21\n\23\13\25\f\27\r\31\16"+
		"\3\2\5\3\2\62;\5\2\62;C\\c|\5\2\13\f\17\17\"\"\2J\2\3\3\2\2\2\2\5\3\2"+
		"\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21"+
		"\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\3\33\3\2"+
		"\2\2\5\35\3\2\2\2\7&\3\2\2\2\t+\3\2\2\2\13\60\3\2\2\2\r\64\3\2\2\2\17"+
		"\66\3\2\2\2\218\3\2\2\2\23:\3\2\2\2\25<\3\2\2\2\27>\3\2\2\2\31A\3\2\2"+
		"\2\33\34\7)\2\2\34\4\3\2\2\2\35!\5\3\2\2\36 \13\2\2\2\37\36\3\2\2\2 #"+
		"\3\2\2\2!\"\3\2\2\2!\37\3\2\2\2\"$\3\2\2\2#!\3\2\2\2$%\5\3\2\2%\6\3\2"+
		"\2\2&\'\7N\2\2\'(\7g\2\2()\7p\2\2)\b\3\2\2\2*,\t\2\2\2+*\3\2\2\2,-\3\2"+
		"\2\2-+\3\2\2\2-.\3\2\2\2.\n\3\2\2\2/\61\t\3\2\2\60/\3\2\2\2\61\62\3\2"+
		"\2\2\62\60\3\2\2\2\62\63\3\2\2\2\63\f\3\2\2\2\64\65\7*\2\2\65\16\3\2\2"+
		"\2\66\67\7+\2\2\67\20\3\2\2\289\7?\2\29\22\3\2\2\2:;\7\u0080\2\2;\24\3"+
		"\2\2\2<=\7(\2\2=\26\3\2\2\2>?\7~\2\2?\30\3\2\2\2@B\t\4\2\2A@\3\2\2\2B"+
		"C\3\2\2\2CA\3\2\2\2CD\3\2\2\2DE\3\2\2\2EF\b\r\2\2F\32\3\2\2\2\7\2!-\62"+
		"C\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}