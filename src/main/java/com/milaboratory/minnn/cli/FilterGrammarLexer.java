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
		GROUP_N_FRACTION=6, MIN_CONSENSUS_READS=7, LEN=8, NO_WILDCARDS=9, FLOAT_NUMBER=10, 
		INT_NUMBER=11, GROUP_NAME=12, OPEN_PARENTHESIS=13, CLOSED_PARENTHESIS=14, 
		EQUALS=15, TILDE=16, AND=17, OR=18, ASTERISK=19, WS=20;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"SINGLE_QUOTE", "STRING", "MIN_GROUP_QUALITY", "AVG_GROUP_QUALITY", "GROUP_N_COUNT", 
		"GROUP_N_FRACTION", "MIN_CONSENSUS_READS", "LEN", "NO_WILDCARDS", "FLOAT_NUMBER", 
		"INT_NUMBER", "GROUP_NAME", "OPEN_PARENTHESIS", "CLOSED_PARENTHESIS", 
		"EQUALS", "TILDE", "AND", "OR", "ASTERISK", "WS", "DIGIT"
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\26\u00d0\b\1\4\2"+
		"\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4"+
		"\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
		"\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\3\2\3\2\3\3\3\3\7\3\62\n"+
		"\3\f\3\16\3\65\13\3\3\3\3\3\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3"+
		"\4\3\4\3\4\3\4\3\4\3\4\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5"+
		"\3\5\3\5\3\5\3\5\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3"+
		"\6\3\6\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7"+
		"\3\7\3\7\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3"+
		"\b\3\b\3\b\3\t\3\t\3\t\3\t\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n"+
		"\3\n\3\13\6\13\u009d\n\13\r\13\16\13\u009e\3\13\3\13\7\13\u00a3\n\13\f"+
		"\13\16\13\u00a6\13\13\3\13\3\13\6\13\u00aa\n\13\r\13\16\13\u00ab\5\13"+
		"\u00ae\n\13\3\f\6\f\u00b1\n\f\r\f\16\f\u00b2\3\r\6\r\u00b6\n\r\r\r\16"+
		"\r\u00b7\3\16\3\16\3\17\3\17\3\20\3\20\3\21\3\21\3\22\3\22\3\23\3\23\3"+
		"\24\3\24\3\25\6\25\u00c9\n\25\r\25\16\25\u00ca\3\25\3\25\3\26\3\26\3\63"+
		"\2\27\3\3\5\4\7\5\t\6\13\7\r\b\17\t\21\n\23\13\25\f\27\r\31\16\33\17\35"+
		"\20\37\21!\22#\23%\24\'\25)\26+\2\3\2\5\5\2\62;C\\c|\5\2\13\f\17\17\""+
		"\"\3\2\62;\2\u00d6\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13"+
		"\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2"+
		"\2\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3\2\2\2\2\35\3\2\2\2\2\37\3\2\2\2\2"+
		"!\3\2\2\2\2#\3\2\2\2\2%\3\2\2\2\2\'\3\2\2\2\2)\3\2\2\2\3-\3\2\2\2\5/\3"+
		"\2\2\2\78\3\2\2\2\tH\3\2\2\2\13X\3\2\2\2\rg\3\2\2\2\17y\3\2\2\2\21\u008b"+
		"\3\2\2\2\23\u008f\3\2\2\2\25\u00ad\3\2\2\2\27\u00b0\3\2\2\2\31\u00b5\3"+
		"\2\2\2\33\u00b9\3\2\2\2\35\u00bb\3\2\2\2\37\u00bd\3\2\2\2!\u00bf\3\2\2"+
		"\2#\u00c1\3\2\2\2%\u00c3\3\2\2\2\'\u00c5\3\2\2\2)\u00c8\3\2\2\2+\u00ce"+
		"\3\2\2\2-.\7)\2\2.\4\3\2\2\2/\63\5\3\2\2\60\62\13\2\2\2\61\60\3\2\2\2"+
		"\62\65\3\2\2\2\63\64\3\2\2\2\63\61\3\2\2\2\64\66\3\2\2\2\65\63\3\2\2\2"+
		"\66\67\5\3\2\2\67\6\3\2\2\289\7O\2\29:\7k\2\2:;\7p\2\2;<\7I\2\2<=\7t\2"+
		"\2=>\7q\2\2>?\7w\2\2?@\7r\2\2@A\7S\2\2AB\7w\2\2BC\7c\2\2CD\7n\2\2DE\7"+
		"k\2\2EF\7v\2\2FG\7{\2\2G\b\3\2\2\2HI\7C\2\2IJ\7x\2\2JK\7i\2\2KL\7I\2\2"+
		"LM\7t\2\2MN\7q\2\2NO\7w\2\2OP\7r\2\2PQ\7S\2\2QR\7w\2\2RS\7c\2\2ST\7n\2"+
		"\2TU\7k\2\2UV\7v\2\2VW\7{\2\2W\n\3\2\2\2XY\7I\2\2YZ\7t\2\2Z[\7q\2\2[\\"+
		"\7w\2\2\\]\7r\2\2]^\7O\2\2^_\7c\2\2_`\7z\2\2`a\7P\2\2ab\7E\2\2bc\7q\2"+
		"\2cd\7w\2\2de\7p\2\2ef\7v\2\2f\f\3\2\2\2gh\7I\2\2hi\7t\2\2ij\7q\2\2jk"+
		"\7w\2\2kl\7r\2\2lm\7O\2\2mn\7c\2\2no\7z\2\2op\7P\2\2pq\7H\2\2qr\7t\2\2"+
		"rs\7c\2\2st\7e\2\2tu\7v\2\2uv\7k\2\2vw\7q\2\2wx\7p\2\2x\16\3\2\2\2yz\7"+
		"O\2\2z{\7k\2\2{|\7p\2\2|}\7E\2\2}~\7q\2\2~\177\7p\2\2\177\u0080\7u\2\2"+
		"\u0080\u0081\7g\2\2\u0081\u0082\7p\2\2\u0082\u0083\7u\2\2\u0083\u0084"+
		"\7w\2\2\u0084\u0085\7u\2\2\u0085\u0086\7T\2\2\u0086\u0087\7g\2\2\u0087"+
		"\u0088\7c\2\2\u0088\u0089\7f\2\2\u0089\u008a\7u\2\2\u008a\20\3\2\2\2\u008b"+
		"\u008c\7N\2\2\u008c\u008d\7g\2\2\u008d\u008e\7p\2\2\u008e\22\3\2\2\2\u008f"+
		"\u0090\7P\2\2\u0090\u0091\7q\2\2\u0091\u0092\7Y\2\2\u0092\u0093\7k\2\2"+
		"\u0093\u0094\7n\2\2\u0094\u0095\7f\2\2\u0095\u0096\7e\2\2\u0096\u0097"+
		"\7c\2\2\u0097\u0098\7t\2\2\u0098\u0099\7f\2\2\u0099\u009a\7u\2\2\u009a"+
		"\24\3\2\2\2\u009b\u009d\5+\26\2\u009c\u009b\3\2\2\2\u009d\u009e\3\2\2"+
		"\2\u009e\u009c\3\2\2\2\u009e\u009f\3\2\2\2\u009f\u00a0\3\2\2\2\u00a0\u00a4"+
		"\7\60\2\2\u00a1\u00a3\5+\26\2\u00a2\u00a1\3\2\2\2\u00a3\u00a6\3\2\2\2"+
		"\u00a4\u00a2\3\2\2\2\u00a4\u00a5\3\2\2\2\u00a5\u00ae\3\2\2\2\u00a6\u00a4"+
		"\3\2\2\2\u00a7\u00a9\7\60\2\2\u00a8\u00aa\5+\26\2\u00a9\u00a8\3\2\2\2"+
		"\u00aa\u00ab\3\2\2\2\u00ab\u00a9\3\2\2\2\u00ab\u00ac\3\2\2\2\u00ac\u00ae"+
		"\3\2\2\2\u00ad\u009c\3\2\2\2\u00ad\u00a7\3\2\2\2\u00ae\26\3\2\2\2\u00af"+
		"\u00b1\5+\26\2\u00b0\u00af\3\2\2\2\u00b1\u00b2\3\2\2\2\u00b2\u00b0\3\2"+
		"\2\2\u00b2\u00b3\3\2\2\2\u00b3\30\3\2\2\2\u00b4\u00b6\t\2\2\2\u00b5\u00b4"+
		"\3\2\2\2\u00b6\u00b7\3\2\2\2\u00b7\u00b5\3\2\2\2\u00b7\u00b8\3\2\2\2\u00b8"+
		"\32\3\2\2\2\u00b9\u00ba\7*\2\2\u00ba\34\3\2\2\2\u00bb\u00bc\7+\2\2\u00bc"+
		"\36\3\2\2\2\u00bd\u00be\7?\2\2\u00be \3\2\2\2\u00bf\u00c0\7\u0080\2\2"+
		"\u00c0\"\3\2\2\2\u00c1\u00c2\7(\2\2\u00c2$\3\2\2\2\u00c3\u00c4\7~\2\2"+
		"\u00c4&\3\2\2\2\u00c5\u00c6\7,\2\2\u00c6(\3\2\2\2\u00c7\u00c9\t\3\2\2"+
		"\u00c8\u00c7\3\2\2\2\u00c9\u00ca\3\2\2\2\u00ca\u00c8\3\2\2\2\u00ca\u00cb"+
		"\3\2\2\2\u00cb\u00cc\3\2\2\2\u00cc\u00cd\b\25\2\2\u00cd*\3\2\2\2\u00ce"+
		"\u00cf\t\4\2\2\u00cf,\3\2\2\2\13\2\63\u009e\u00a4\u00ab\u00ad\u00b2\u00b7"+
		"\u00ca\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}