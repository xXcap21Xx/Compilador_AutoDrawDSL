package compilerTools;

/**
 * Clase para representar una producción en la gramática formal
 * @author MiStErX
 */
public class Production {
    private String leftSide;
    private String rightSide;
    private int productionNumber;

    /**
     * Constructor de Production
     * @param leftSide Lado izquierdo (no terminal)
     * @param rightSide Lado derecho (derivación)
     */
    public Production(String leftSide, String rightSide) {
        this.leftSide = leftSide;
        this.rightSide = rightSide;
        this.productionNumber = 0;
    }

    /**
     * Constructor con número de producción
     * @param leftSide Lado izquierdo
     * @param rightSide Lado derecho
     * @param productionNumber Número de la producción
     */
    public Production(String leftSide, String rightSide, int productionNumber) {
        this.leftSide = leftSide;
        this.rightSide = rightSide;
        this.productionNumber = productionNumber;
    }

    /**
     * Obtiene el lado izquierdo
     * @return Lado izquierdo (no terminal)
     */
    public String getLeftSide() {
        return leftSide;
    }

    /**
     * Establece el lado izquierdo
     * @param leftSide Lado izquierdo
     */
    public void setLeftSide(String leftSide) {
        this.leftSide = leftSide;
    }

    /**
     * Obtiene el lado derecho
     * @return Lado derecho (derivación)
     */
    public String getRightSide() {
        return rightSide;
    }

    /**
     * Establece el lado derecho
     * @param rightSide Lado derecho
     */
    public void setRightSide(String rightSide) {
        this.rightSide = rightSide;
    }

    /**
     * Obtiene el número de producción
     * @return Número de producción
     */
    public int getProductionNumber() {
        return productionNumber;
    }

    /**
     * Establece el número de producción
     * @param productionNumber Número de producción
     */
    public void setProductionNumber(int productionNumber) {
        this.productionNumber = productionNumber;
    }

    /**
     * Representación en cadena de la producción
     * @return Cadena con formato "leftSide -> rightSide"
     */
    @Override
    public String toString() {
        return String.format("%s -> %s", leftSide, rightSide);
    }
}
