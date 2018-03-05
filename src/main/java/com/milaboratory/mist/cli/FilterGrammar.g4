grammar FilterGrammar;

filter : filterInParentheses | anySingleFilter ;
filterInParentheses : OPEN_PARENTHESIS anySingleFilter CLOSED_PARENTHESIS ;
anySingleFilter : or | and | pattern | len ;
or : orOperand OR orOperand (OR orOperand)* ;
orOperand : and | pattern | len | filterInParentheses ;
and : andOperand AND andOperand (AND andOperand)* ;
andOperand : pattern | len | filterInParentheses ;
pattern : groupName TILDE patternString ;
len : LEN OPEN_PARENTHESIS groupName CLOSED_PARENTHESIS EQUALS groupLength ;
patternString : STRING ;
groupName : GROUP_NAME ;
groupLength : NUMBER ;

SINGLE_QUOTE : '\'' ;
STRING : SINGLE_QUOTE .*? SINGLE_QUOTE ;
LEN : 'Len' ;
NUMBER : [0-9]+ ;
GROUP_NAME : [0-9a-zA-Z]+ ;
OPEN_PARENTHESIS : '(' ;
CLOSED_PARENTHESIS : ')' ;
EQUALS : '=' ;
TILDE : '~' ;
AND : '&' ;
OR : '|' ;
WS : [ \t\n\r]+ -> skip ;
