package compilerTools;

/**
 * Clase para representar un error en la compilación del DSL
 * @author MiStErX
 */
public class ErrorLSSL {
    private int code;
    private String description;
    private Token token;

    /**
     * Constructor de ErrorLSSL
     * @param code Código del error
     * @param description Descripción del error
     * @param token Token asociado al error
     */
    public ErrorLSSL(int code, String description, Token token) {
        this.code = code;
        this.description = description;
        this.token = token;
    }

    /**
     * Obtiene el código del error
     * @return Código del error
     */
    public int getCode() {
        return code;
    }

    /**
     * Establece el código del error
     * @param code Código del error
     */
    public void setCode(int code) {
        this.code = code;
    }

    /**
     * Obtiene la descripción del error
     * @return Descripción del error
     */
    public String getDescription() {
        return description;
    }

    /**
     * Establece la descripción del error
     * @param description Descripción del error
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Obtiene el token asociado al error
     * @return Token del error
     */
    public Token getToken() {
        return token;
    }

    /**
     * Establece el token asociado al error
     * @param token Token del error
     */
    public void setToken(Token token) {
        this.token = token;
    }

    /**
     * Obtiene el número de línea del token del error
     * @return Número de línea
     */
    public int getLine() {
        return token != null ? token.getLine() : -1;
    }

    /**
     * Obtiene el número de columna del token del error
     * @return Número de columna
     */
    public int getColumn() {
        return token != null ? token.getColumn() : -1;
    }

    /**
     * Representación en cadena del error
     * @return Cadena con información del error
     */
    @Override
    public String toString() {
        if (token != null) {
            return String.format("%s [Línea: %d, Columna: %d]", description, token.getLine(), token.getColumn());
        }
        return description;
    }
}
