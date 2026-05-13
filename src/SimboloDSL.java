// Clase auxiliar para representar un símbolo en la tabla
class SimboloDSL {
    String nombre;
    String tipo;
    int linea;
    int columna;

    public SimboloDSL(String nombre, String tipo, int linea, int columna) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.linea = linea;
        this.columna = columna;
    }
}