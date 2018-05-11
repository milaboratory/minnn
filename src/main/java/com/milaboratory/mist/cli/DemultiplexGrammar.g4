grammar DemultiplexGrammar;

demultiplexArguments : (bySample | byBarcode | inputFileName)+ ;
bySample : BY_SAMPLE SPACE fileName ;
byBarcode : BY_BARCODE SPACE barcodeName ;
inputFileName : SPACE fileName ;
fileName : doubleQuotedFileName | singleQuotedFileName | notQuotedFileName ;
doubleQuotedFileName : DOUBLE_QUOTE ('-' | '.' | ',' | '!' | '_' | LETTER | NUMBER | SPACE)+ DOUBLE_QUOTE ;
singleQuotedFileName : SINGLE_QUOTE ('-' | '.' | ',' | '!' | '_' | LETTER | NUMBER | SPACE)+ SINGLE_QUOTE ;
notQuotedFileName : ('-' | '.' | ',' | '!' | '_' | LETTER | NUMBER)+ ;
barcodeName : (LETTER | NUMBER)+ ;

BY_SAMPLE : '--by-sample' ;
BY_BARCODE : '--by-barcode' ;
DOUBLE_QUOTE : '"' ;
SINGLE_QUOTE : '\'' ;
LETTER : [a-zA-Z] ;
NUMBER : [0-9] ;
SPACE : [ ]+ ;
WS : [ \t\n\r]+ -> skip ;
