package compilerTools;

import java.util.ArrayList;
import java.util.List;

/**
 * Clase para representar una gramática formal del compilador
 * @author MiStErX
 */
public class Grammar {
    private String name;
    private List<Production> productions;
    private String startSymbol;

    /**
     * Constructor de Grammar
     */
    public Grammar() {
        this.productions = new ArrayList<>();
        this.name = "Default Grammar";
        this.startSymbol = "Program";
    }

    /**
     * Constructor con nombre
     * @param name Nombre de la gramática
     */
    public Grammar(String name) {
        this();
        this.name = name;
    }

    /**
     * Obtiene el nombre de la gramática
     * @return Nombre
     */
    public String getName() {
        return name;
    }

    /**
     * Establece el nombre de la gramática
     * @param name Nombre
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Obtiene el símbolo inicial
     * @return Símbolo inicial
     */
    public String getStartSymbol() {
        return startSymbol;
    }

    /**
     * Establece el símbolo inicial
     * @param startSymbol Símbolo inicial
     */
    public void setStartSymbol(String startSymbol) {
        this.startSymbol = startSymbol;
    }

    /**
     * Obtiene todas las producciones
     * @return Lista de producciones
     */
    public List<Production> getProductions() {
        return productions;
    }

    /**
     * Agrega una producción a la gramática
     * @param production Producción a agregar
     */
    public void addProduction(Production production) {
        productions.add(production);
    }

    /**
     * Obtiene el número de producciones
     * @return Cantidad de producciones
     */
    public int getProductionCount() {
        return productions.size();
    }

    /**
     * Representación en cadena
     * @return Cadena descriptiva
     */
    @Override
    public String toString() {
        return String.format("Grammar [name=%s, startSymbol=%s, productions=%d]", name, startSymbol, productions.size());
    }
}
