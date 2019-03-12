// Generated from FilterGrammar.g4 by ANTLR 4.7
package com.milaboratory.minnn.cli;
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
		SINGLE_QUOTE=1, STRING=2, MIN_GROUP_QUALITY=3, AVG_GROUP_QUALITY=4, GROUP_N_COUNT=5, 
		GROUP_N_FRACTION=6, MIN_CONSENSUS_READS=7, LEN=8, FLOAT_NUMBER=9, INT_NUMBER=10, 
		GROUP_NAME=11, OPEN_PARENTHESIS=12, CLOSED_PARENTHESIS=13, EQUALS=14, 
		TILDE=15, AND=16, OR=17, ASTERISK=18, WS=19;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"SINGLE_QUOTE", "STRING", "MIN_GROUP_QUALITY", "AVG_GROUP_QUALITY", "GROUP_N_COUNT", 
		"GROUP_N_FRACTION", "MIN_CONSENSUS_READS", "LEN", "FLOAT_NUMBER", "INT_NUMBER", 
		"GROUP_NAME", "OPEN_PARENTHESIS", "CLOSED_PARENTHESIS", "EQUALS", "TILDE", 
		"AND", "OR", "ASTERISK", "WS", "DIGIT"
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\25\u00c2\b\1\4\2"+
		"\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4"+
		"\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
		"\t\22\4\23\t\23\4\24\t\24\4\25\t\25\3\2\3\2\3\3\3\3\7\3\60\n\3\f\3\16"+
		"\3\63\13\3\3\3\3\3\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4"+
		"\3\4\3\4\3\4\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3"+
		"\5\3\5\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\7"+
		"\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3"+
		"\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b"+
		"\3\t\3\t\3\t\3\t\3\n\6\n\u008f\n\n\r\n\16\n\u0090\3\n\3\n\7\n\u0095\n"+
		"\n\f\n\16\n\u0098\13\n\3\n\3\n\6\n\u009c\n\n\r\n\16\n\u009d\5\n\u00a0"+
		"\n\n\3\13\6\13\u00a3\n\13\r\13\16\13\u00a4\3\f\6\f\u00a8\n\f\r\f\16\f"+
		"\u00a9\3\r\3\r\3\16\3\16\3\17\3\17\3\20\3\20\3\21\3\21\3\22\3\22\3\23"+
		"\3\23\3\24\6\24\u00bb\n\24\r\24\16\24\u00bc\3\24\3\24\3\25\3\25\3\61\2"+
		"\26\3\3\5\4\7\5\t\6\13\7\r\b\17\t\21\n\23\13\25\f\27\r\31\16\33\17\35"+
		"\20\37\21!\22#\23%\24\'\25)\2\3\2\5\5\2\62;C\\c|\5\2\13\f\17\17\"\"\3"+
		"\2\62;\2\u00c8\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3"+
		"\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2"+
		"\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3\2\2\2\2\35\3\2\2\2\2\37\3\2\2\2\2!\3"+
		"\2\2\2\2#\3\2\2\2\2%\3\2\2\2\2\'\3\2\2\2\3+\3\2\2\2\5-\3\2\2\2\7\66\3"+
		"\2\2\2\tF\3\2\2\2\13V\3\2\2\2\re\3\2\2\2\17w\3\2\2\2\21\u0089\3\2\2\2"+
		"\23\u009f\3\2\2\2\25\u00a2\3\2\2\2\27\u00a7\3\2\2\2\31\u00ab\3\2\2\2\33"+
		"\u00ad\3\2\2\2\35\u00af\3\2\2\2\37\u00b1\3\2\2\2!\u00b3\3\2\2\2#\u00b5"+
		"\3\2\2\2%\u00b7\3\2\2\2\'\u00ba\3\2\2\2)\u00c0\3\2\2\2+,\7)\2\2,\4\3\2"+
		"\2\2-\61\5\3\2\2.\60\13\2\2\2/.\3\2\2\2\60\63\3\2\2\2\61\62\3\2\2\2\61"+
		"/\3\2\2\2\62\64\3\2\2\2\63\61\3\2\2\2\64\65\5\3\2\2\65\6\3\2\2\2\66\67"+
		"\7O\2\2\678\7k\2\289\7p\2\29:\7I\2\2:;\7t\2\2;<\7q\2\2<=\7w\2\2=>\7r\2"+
		"\2>?\7S\2\2?@\7w\2\2@A\7c\2\2AB\7n\2\2BC\7k\2\2CD\7v\2\2DE\7{\2\2E\b\3"+
		"\2\2\2FG\7C\2\2GH\7x\2\2HI\7i\2\2IJ\7I\2\2JK\7t\2\2KL\7q\2\2LM\7w\2\2"+
		"MN\7r\2\2NO\7S\2\2OP\7w\2\2PQ\7c\2\2QR\7n\2\2RS\7k\2\2ST\7v\2\2TU\7{\2"+
		"\2U\n\3\2\2\2VW\7I\2\2WX\7t\2\2XY\7q\2\2YZ\7w\2\2Z[\7r\2\2[\\\7O\2\2\\"+
		"]\7c\2\2]^\7z\2\2^_\7P\2\2_`\7E\2\2`a\7q\2\2ab\7w\2\2bc\7p\2\2cd\7v\2"+
		"\2d\f\3\2\2\2ef\7I\2\2fg\7t\2\2gh\7q\2\2hi\7w\2\2ij\7r\2\2jk\7O\2\2kl"+
		"\7c\2\2lm\7z\2\2mn\7P\2\2no\7H\2\2op\7t\2\2pq\7c\2\2qr\7e\2\2rs\7v\2\2"+
		"st\7k\2\2tu\7q\2\2uv\7p\2\2v\16\3\2\2\2wx\7O\2\2xy\7k\2\2yz\7p\2\2z{\7"+
		"E\2\2{|\7q\2\2|}\7p\2\2}~\7u\2\2~\177\7g\2\2\177\u0080\7p\2\2\u0080\u0081"+
		"\7u\2\2\u0081\u0082\7w\2\2\u0082\u0083\7u\2\2\u0083\u0084\7T\2\2\u0084"+
		"\u0085\7g\2\2\u0085\u0086\7c\2\2\u0086\u0087\7f\2\2\u0087\u0088\7u\2\2"+
		"\u0088\20\3\2\2\2\u0089\u008a\7N\2\2\u008a\u008b\7g\2\2\u008b\u008c\7"+
		"p\2\2\u008c\22\3\2\2\2\u008d\u008f\5)\25\2\u008e\u008d\3\2\2\2\u008f\u0090"+
		"\3\2\2\2\u0090\u008e\3\2\2\2\u0090\u0091\3\2\2\2\u0091\u0092\3\2\2\2\u0092"+
		"\u0096\7\60\2\2\u0093\u0095\5)\25\2\u0094\u0093\3\2\2\2\u0095\u0098\3"+
		"\2\2\2\u0096\u0094\3\2\2\2\u0096\u0097\3\2\2\2\u0097\u00a0\3\2\2\2\u0098"+
		"\u0096\3\2\2\2\u0099\u009b\7\60\2\2\u009a\u009c\5)\25\2\u009b\u009a\3"+
		"\2\2\2\u009c\u009d\3\2\2\2\u009d\u009b\3\2\2\2\u009d\u009e\3\2\2\2\u009e"+
		"\u00a0\3\2\2\2\u009f\u008e\3\2\2\2\u009f\u0099\3\2\2\2\u00a0\24\3\2\2"+
		"\2\u00a1\u00a3\5)\25\2\u00a2\u00a1\3\2\2\2\u00a3\u00a4\3\2\2\2\u00a4\u00a2"+
		"\3\2\2\2\u00a4\u00a5\3\2\2\2\u00a5\26\3\2\2\2\u00a6\u00a8\t\2\2\2\u00a7"+
		"\u00a6\3\2\2\2\u00a8\u00a9\3\2\2\2\u00a9\u00a7\3\2\2\2\u00a9\u00aa\3\2"+
		"\2\2\u00aa\30\3\2\2\2\u00ab\u00ac\7*\2\2\u00ac\32\3\2\2\2\u00ad\u00ae"+
		"\7+\2\2\u00ae\34\3\2\2\2\u00af\u00b0\7?\2\2\u00b0\36\3\2\2\2\u00b1\u00b2"+
		"\7\u0080\2\2\u00b2 \3\2\2\2\u00b3\u00b4\7(\2\2\u00b4\"\3\2\2\2\u00b5\u00b6"+
		"\7~\2\2\u00b6$\3\2\2\2\u00b7\u00b8\7,\2\2\u00b8&\3\2\2\2\u00b9\u00bb\t"+
		"\3\2\2\u00ba\u00b9\3\2\2\2\u00bb\u00bc\3\2\2\2\u00bc\u00ba\3\2\2\2\u00bc"+
		"\u00bd\3\2\2\2\u00bd\u00be\3\2\2\2\u00be\u00bf\b\24\2\2\u00bf(\3\2\2\2"+
		"\u00c0\u00c1\t\4\2\2\u00c1*\3\2\2\2\13\2\61\u0090\u0096\u009d\u009f\u00a4"+
		"\u00a9\u00bc\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}