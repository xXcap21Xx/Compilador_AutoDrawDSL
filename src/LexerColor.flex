import compilerTools.TextColor;
import java.awt.Color;

%%

%class LexerColor
%unicode
%char
%line
%column

%{
    private TextColor textColor(long start, int size, Color color) {
        return new TextColor((int) start, size, color);
    }
%}

/* Macros */
LineTerminator     = \r|\n|\r\n
InputCharacter     = [^\r\n]
WhiteSpace         = {LineTerminator} | [ \t\f]

TraditionalComment = "/*" [^*]* ~"*/" | "/*" "*"+ "/"
EndOfLineComment   = "//" {InputCharacter}* {LineTerminator}?
Comment            = {TraditionalComment} | {EndOfLineComment}

Identifier         = [a-zA-Z] [a-zA-Z0-9_]*

%%

<YYINITIAL> {
    /* Ignorar espacios */
    {WhiteSpace}       { /* Ignorar */ }

    /* Comentarios (Gris claro) */
    {Comment}          { return textColor(yychar, yylength(), new Color(146, 146, 146)); }

    /* Palabras clave principales (Azul Marino) */
    "TIPO" |
    "ALFABETO" |
    "INICIO" |
    "FINAL" |
    "ESTADOS"          { return textColor(yychar, yylength(), new Color(0, 80, 136)); }

    /* Configuraciones y variables especiales (Verde Esmeralda) */
    "EPSILON" | 
    "FONDO" | 
    "AFD" | 
    "AFN"              { return textColor(yychar, yylength(), new Color(17, 202, 160)); }

    /* Flecha de transición (Naranja brillante) */
    "->"               { return textColor(yychar, yylength(), new Color(255, 100, 0)); }

    /* Delimitadores (Gris oscuro) */
    "[" | "]" | 
    "{" | "}" | 
    "'" | ";" | ","    { return textColor(yychar, yylength(), new Color(100, 100, 100)); }

    /* Colores válidos para FONDO (Ámbar dorado) */
    "blanco" | "negro" | "rojo" | "azul" | "verde" | "amarillo" |
    "naranja" | "gris" | "rosa" | "morado" | "violeta" | "cyan" | "marron"
                       { return textColor(yychar, yylength(), new Color(180, 100, 0)); }

    /* Identificadores normales (Negro) */
    {Identifier}       { return textColor(yychar, yylength(), new Color(0, 0, 0)); }

    /* Errores léxicos no reconocidos (Rojo) */
    .                  { return textColor(yychar, yylength(), new Color(255, 0, 0)); }
}