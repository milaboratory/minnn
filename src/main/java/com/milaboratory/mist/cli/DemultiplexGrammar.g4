grammar DemultiplexGrammar;

demultiplexArguments : demultiplexArgument+ ;
demultiplexArgument: bySample | byBarcode| inputFileName ;
bySample : DELIMITER BY_SAMPLE DELIMITER fileName ;
byBarcode : DELIMITER BY_BARCODE DELIMITER barcodeName ;
inputFileName : DELIMITER fileName ;
fileName : ('-' | '.' | ',' | '!' | '_' | '/' | LETTER | NUMBER | SPACE)+ ;
barcodeName : (LETTER | NUMBER)+ ;

BY_SAMPLE : '--by-sample' ;
BY_BARCODE : '--by-barcode' ;
LETTER : [a-zA-Z] ;
NUMBER : [0-9] ;
SPACE : ' ' ;
DELIMITER : '#' ;
WS : [\t\n\r]+ -> skip ;
