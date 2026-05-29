import compilerTools.Token;
import java_cup.runtime.Symbol;

%%

%class Lexer
%type Symbol
%cup
%line
%column

%state EXPECT_COLOR

%{
    /**
     * Crea un Token interno y lo envuelve en un Symbol de CUP.
     * symCode debe coincidir exactamente con el nombre del terminal en sym.java.
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

/* Comentarios */
TraditionalComment = "/*" [^*]* ~"*/" | "/*" "*"+ "/"
EndOfLineComment   = "//" {InputCharacter}* {LineTerminator}?
Comment            = {TraditionalComment} | {EndOfLineComment}

/* Identificadores */
Identifier         = [a-zA-Z] [a-zA-Z0-9_]*

%%

/* ============================================================
   Estado normal
   ============================================================ */
<YYINITIAL> {
    {WhiteSpace}       { /* Ignorar */ }
    {Comment}          { /* Ignorar */ }

    /* Palabras Reservadas principales */
    "TIPO"             { return token(yytext(), "TIPO",     yyline, yycolumn, sym.TIPO);     }
    "ALFABETO"         { return token(yytext(), "ALFABETO", yyline, yycolumn, sym.ALFABETO); }
    "INICIO"           { return token(yytext(), "INICIO",   yyline, yycolumn, sym.INICIO);   }
    "FINAL"            { return token(yytext(), "FINAL",    yyline, yycolumn, sym.FINAL);    }
    "ESTADO"           { return token(yytext(), "ESTADO",   yyline, yycolumn, sym.ESTADO);   }

    /* Configuraciones especiales */
    "EPSILON"          { return token(yytext(), "EPSILON", yyline, yycolumn, sym.EPSILON); }
    "AFD"              { return token(yytext(), "AFD",     yyline, yycolumn, sym.AFD);     }
    "AFN"              { return token(yytext(), "AFN",     yyline, yycolumn, sym.AFN);     }

    /* FONDO: entra al estado de validación de color */
    "FONDO"            { yybegin(EXPECT_COLOR); return token(yytext(), "FONDO", yyline, yycolumn, sym.FONDO); }

    /* Operadores y Delimitadores */
    "->"               { return token(yytext(), "FLECHA",        yyline, yycolumn, sym.FLECHA);        }
    "["                { return token(yytext(), "CORCHETE_IZQ",  yyline, yycolumn, sym.CORCHETE_IZQ);  }
    "]"                { return token(yytext(), "CORCHETE_DER",  yyline, yycolumn, sym.CORCHETE_DER);  }
    "{"                { return token(yytext(), "LLAVE_IZQ",     yyline, yycolumn, sym.LLAVE_IZQ);     }
    "}"                { return token(yytext(), "LLAVE_DER",     yyline, yycolumn, sym.LLAVE_DER);     }
    "'"                { return token(yytext(), "COMILLA_SIMPLE",yyline, yycolumn, sym.COMILLA_SIMPLE);}
    ";"                { return token(yytext(), "PUNTO_Y_COMA",  yyline, yycolumn, sym.PUNTO_Y_COMA);  }
    ","                { return token(yytext(), "COMA",          yyline, yycolumn, sym.COMA);          }

    /* Identificadores generales (nombres de estados, símbolos, etc.) */
    {Identifier}       { return token(yytext(), "IDENTIFICADOR", yyline, yycolumn, sym.IDENTIFICADOR); }

    /* Cualquier carácter no reconocido: error léxico */
    .                  { return token(yytext(), "ERROR_LEXICO", yyline, yycolumn, sym.ERROR); }
}

/* ============================================================
   Estado EXPECT_COLOR: se entra después de leer FONDO.
   Solo se acepta un identificador que sea un color válido.
   Cualquier otra palabra es un error léxico de color inválido.
   ============================================================ */
<EXPECT_COLOR> {
    {WhiteSpace}       { /* Ignorar */ }
    {Comment}          { /* Ignorar */ }

    /* Colores válidos: vuelven al estado normal */
    "blanco" | "negro" | "rojo"  | "azul"    | "verde"   | "amarillo" |
    "naranja" | "gris" | "rosa"  | "morado"  | "violeta" | "cyan"     | "marron"
                       { yybegin(YYINITIAL); return token(yytext(), "COLOR", yyline, yycolumn, sym.COLOR); }

    /* Identificador que no es un color válido: LexError 002 */
    {Identifier}       { yybegin(YYINITIAL); return token(yytext(), "ERROR_COLOR", yyline, yycolumn, sym.ERROR); }

    /* ';' sin color previo: volver al estado normal para que el parser lo procese */
    ";"                { yybegin(YYINITIAL); return token(yytext(), "PUNTO_Y_COMA", yyline, yycolumn, sym.PUNTO_Y_COMA); }

    /* Cualquier otro carácter: error léxico genérico */
    .                  { yybegin(YYINITIAL); return token(yytext(), "ERROR_LEXICO", yyline, yycolumn, sym.ERROR); }
}
