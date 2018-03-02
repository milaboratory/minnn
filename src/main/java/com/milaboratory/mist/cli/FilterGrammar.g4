grammar FilterGrammar;

filter : or | and | pattern | len ;
or : or_operand '|' or_operand ('|' or_operand)* ;
or_operand : and | pattern | len ;
and : and_operand '&' and_operand ('&' and_operand)* ;
and_operand : pattern | len ;
pattern : GROUP_NAME '~' STRING ;
len : LEN '(' GROUP_NAME ')=' NUMBER ;

STRING : '"' ('""'|~'"')* '"' ;
LEN : 'Len' ;
NUMBER : [0-9a-zA-Z]+ ;
GROUP_NAME : [0-9a-zA-Z]+ ;
WS : [ \t\n\r]+ -> skip ;
