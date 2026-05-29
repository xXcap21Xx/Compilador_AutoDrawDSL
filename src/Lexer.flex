import compilerTools.Token;
import java_cup.runtime.Symbol;

%%

%class Lexer
%implements java_cup.runtime.Scanner
%type Symbol
%cup
%line
%column

%{
    /**
     * Crea un Token interno (para tu AST o para mostrar) y lo envuelve en un Symbol de CUP.
     * symCode debe coincidir exactamente con el nombre del terminal en sym.java (generado por CUP).
     */
    private Symbol token(String lexeme, String lexicalComp, int line, int column, int symCode) {
        Token t = new Token(lexeme, lexicalComp, line + 1, column + 1);
        return new Symbol(symCode, t);
    }
%}

/* ----------------- Macros / Definiciones ----------------- */
LineTerminator     = \r|\n|\r\n
InputCharacter     = [^\r\n]
WhiteSpace         = {LineTerminator} | [ \t\f]

/* comments */
TraditionalComment = "/*" [^*]* ~"*/" | "/*" "*"+ "/"
EndOfLineComment   = "//" {InputCharacter}* {LineTerminator}?
Comment            = {TraditionalComment} | {EndOfLineComment}

/* Identifiers */
Identifier         = [a-zA-Z] [a-zA-Z0-9_]*

%%

<YYINITIAL> {
    /* Ignorar espacios y comentarios */
    {WhiteSpace}       { /* Ignorar */ }
    {Comment}          { /* Ignorar */ }

    /* Palabras Reservadas principales */
    "TIPO"             { return token(yytext(), "TIPO", yyline, yycolumn, sym.TIPO); }
    "ALFABETO"         { return token(yytext(), "ALFABETO", yyline, yycolumn, sym.ALFABETO); }
    "INICIO"           { return token(yytext(), "INICIO", yyline, yycolumn, sym.INICIO); }
    "FINAL"            { return token(yytext(), "FINAL", yyline, yycolumn, sym.FINAL); }
    "ESTADO"           { return token(yytext(), "ESTADO", yyline, yycolumn, sym.ESTADO); }

    /* Configuraciones Especiales */
    "EPSILON"          { return token(yytext(), "EPSILON", yyline, yycolumn, sym.EPSILON); }
    "FONDO"            { return token(yytext(), "FONDO", yyline, yycolumn, sym.FONDO); }
    "AFD"              { return token(yytext(), "AFD", yyline, yycolumn, sym.AFD); }
    "AFN"              { return token(yytext(), "AFN", yyline, yycolumn, sym.AFN); }

    /* Operadores y Delimitadores */
    "->"               { return token(yytext(), "FLECHA", yyline, yycolumn, sym.FLECHA); }
    "["                { return token(yytext(), "CORCHETE_IZQ", yyline, yycolumn, sym.CORCHETE_IZQ); }
    "]"                { return token(yytext(), "CORCHETE_DER", yyline, yycolumn, sym.CORCHETE_DER); }
    "{"                { return token(yytext(), "LLAVE_IZQ", yyline, yycolumn, sym.LLAVE_IZQ); }
    "}"                { return token(yytext(), "LLAVE_DER", yyline, yycolumn, sym.LLAVE_DER); }
    "'"                { return token(yytext(), "COMILLA_SIMPLE", yyline, yycolumn, sym.COMILLA_SIMPLE); }
    ";"                { return token(yytext(), "PUNTO_Y_COMA", yyline, yycolumn, sym.PUNTO_Y_COMA); }
    ","                { return token(yytext(), "COMA", yyline, yycolumn, sym.COMA); }

    /* Colores válidos para FONDO (deben ir ANTES que el Identifier genérico) */
    "blanco"           { return token(yytext(), "COLOR", yyline, yycolumn, sym.COLOR); }
    "negro"            { return token(yytext(), "COLOR", yyline, yycolumn, sym.COLOR); }
    "rojo"             { return token(yytext(), "COLOR", yyline, yycolumn, sym.COLOR); }
    "azul"             { return token(yytext(), "COLOR", yyline, yycolumn, sym.COLOR); }
    "verde"            { return token(yytext(), "COLOR", yyline, yycolumn, sym.COLOR); }
    "amarillo"         { return token(yytext(), "COLOR", yyline, yycolumn, sym.COLOR); }
    "naranja"          { return token(yytext(), "COLOR", yyline, yycolumn, sym.COLOR); }
    "gris"             { return token(yytext(), "COLOR", yyline, yycolumn, sym.COLOR); }
    "rosa"             { return token(yytext(), "COLOR", yyline, yycolumn, sym.COLOR); }
    "morado"           { return token(yytext(), "COLOR", yyline, yycolumn, sym.COLOR); }
    "violeta"          { return token(yytext(), "COLOR", yyline, yycolumn, sym.COLOR); }
    "cyan"             { return token(yytext(), "COLOR", yyline, yycolumn, sym.COLOR); }
    "marron"           { return token(yytext(), "COLOR", yyline, yycolumn, sym.COLOR); }

    /* Identificadores (nombres de estados o variables) */
    {Identifier}       { return token(yytext(), "IDENTIFICADOR", yyline, yycolumn, sym.IDENTIFICADOR); }

    /* Manejo de errores léxicos (Cualquier caracter no reconocido) */
    .                  { return token(yytext(), "ERROR_LEXICO", yyline, yycolumn, sym.error); }
}