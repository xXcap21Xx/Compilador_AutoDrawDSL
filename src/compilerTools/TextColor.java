package compilerTools;

import java.awt.Color;

/**
 * Clase para representar información de colores en el editor de texto
 * @author MiStErX
 */
public class TextColor {
    private int startPosition;
    private int length;
    private Color color;

    /**
     * Constructor de TextColor
     * @param startPosition Posición inicial del texto a colorear
     * @param length Longitud del texto a colorear
     * @param color Color a aplicar
     */
    public TextColor(int startPosition, int length, Color color) {
        this.startPosition = startPosition;
        this.length = length;
        this.color = color;
    }

    /**
     * Obtiene la posición inicial
     * @return Posición inicial
     */
    public int getStartPosition() {
        return startPosition;
    }

    /**
     * Establece la posición inicial
     * @param startPosition Posición inicial
     */
    public void setStartPosition(int startPosition) {
        this.startPosition = startPosition;
    }

    /**
     * Obtiene la longitud del texto a colorear
     * @return Longitud
     */
    public int getLength() {
        return length;
    }

    /**
     * Establece la longitud del texto a colorear
     * @param length Longitud
     */
    public void setLength(int length) {
        this.length = length;
    }

    /**
     * Obtiene el color
     * @return Color
     */
    public Color getColor() {
        return color;
    }

    /**
     * Establece el color
     * @param color Color
     */
    public void setColor(Color color) {
        this.color = color;
    }

    /**
     * Representación en cadena
     * @return Cadena descriptiva
     */
    @Override
    public String toString() {
        return String.format("TextColor [pos=%d, len=%d, color=%s]", startPosition, length, color);
    }
}
