
import com.formdev.flatlaf.FlatIntelliJLaf;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import compilerTools.ASTNode;
import compilerTools.Directory;
import compilerTools.Functions;
import compilerTools.Grammar;
import compilerTools.Production;
import compilerTools.TextColor;
import compilerTools.Token;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import javax.swing.Timer;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JLabel;
import java.util.Collections;
import java.util.Comparator;
import compilerTools.ErrorLSSL;
import java.io.ByteArrayInputStream;
import java.io.Reader;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author MiStErX
 */
public class Compilador extends javax.swing.JFrame {

    private String title;
    private Directory Directorio;
    private ArrayList<Token> tokens;
    private ArrayList<ErrorLSSL> errors;
    private ArrayList<TextColor> textsColor;
    private Timer timerKeyReleased;
    private ArrayList<Production> identProd;
    private HashMap<String, String> identificadores;
    private boolean codeHasBeenCompiled = false;
    private java.util.List<SimboloDSL> listaSimbolos = new java.util.ArrayList<>();
    private java.util.List<SimboloDSL> listaSimbolosGlobal = new java.util.ArrayList<>();
    private Object compilerTools;

    /**
     * Creates new form Compilador
     */
    public Compilador() {
        initComponents();
        init();

    }

    private void init() {
        title = "AutoDrawDSL";
        setLocationRelativeTo(null);
        setTitle(title);
        Directorio = new Directory(this, panel_Codigo, title, ".draw");
        addWindowListener(new WindowAdapter() {// Cuando presiona la "X" de la esquina superior derecha
            @Override
            public void windowClosing(WindowEvent e) {
                Directorio.Exit();
                System.exit(0);
            }
        });
        Functions.setLineNumberOnJTextComponent(panel_Codigo, jScrollPane1);
        timerKeyReleased = new Timer((int) (1000 * 0.3), (ActionEvent e) -> {
            timerKeyReleased.stop();
            colorAnalysis();
        });
        Functions.insertAsteriskInName(this, panel_Codigo, () -> {
            timerKeyReleased.restart();
        });
        tokens = new ArrayList<>();
        errors = new ArrayList<>();
        textsColor = new ArrayList<>();
        identProd = new ArrayList<>();
        identificadores = new HashMap<>();
        Functions.setAutocompleterJTextComponent(new String[]{"color", "numero", "este", "oeste", "sur", "norte", "pintar"}, panel_Codigo, () -> { //Corregir para proyecto
            timerKeyReleased.restart();
        });
        panel_Codigo.setBackground(Color.WHITE);

        // ── Barra de menú con opciones de análisis ──
        JMenuBar menuBar = new JMenuBar();
        JMenu menuAnalisis = new JMenu("Análisis");
        menuAnalisis.setFont(new java.awt.Font("Consolas", java.awt.Font.PLAIN, 13));

        JMenuItem itemTabla = new JMenuItem("Tabla de Transiciones δ");
        itemTabla.setFont(new java.awt.Font("Consolas", java.awt.Font.PLAIN, 12));
        itemTabla.addActionListener(e -> mostrarTablaTransiciones());

        JMenuItem itemSimular = new JMenuItem("Simular Cadena...");
        itemSimular.setFont(new java.awt.Font("Consolas", java.awt.Font.PLAIN, 12));
        itemSimular.addActionListener(e -> mostrarSimulacionCadena());

        menuAnalisis.add(itemTabla);
        menuAnalisis.addSeparator();
        menuAnalisis.add(itemSimular);
        menuBar.add(menuAnalisis);
        setJMenuBar(menuBar);
    }

    private void colorAnalysis() {
        /* Limpiar el arreglo de colores */
        textsColor.clear();
        /* Extraer rangos de colores */
        LexerColor lexerColor;
        try {
            File codigo = new File("color.encrypter");
            FileOutputStream output = new FileOutputStream(codigo);
            byte[] bytesText = panel_Codigo.getText().getBytes();
            output.write(bytesText);
            BufferedReader entrada = new BufferedReader(new InputStreamReader(new FileInputStream(codigo), "UTF8"));
            lexerColor = new LexerColor(entrada);
            while (true) {
                TextColor textColor = lexerColor.yylex();
                if (textColor == null) {
                    break;
                }
                textsColor.add(textColor);
            }
        } catch (FileNotFoundException ex) {
            System.out.println("El archivo no pudo ser encontrado... " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("Error al escribir en el archivo... " + ex.getMessage());
        }
        Functions.colorTextPane(textsColor, panel_Codigo, Color.WHITE);
    }

    private void getASTAsString(ASTNode node, String prefix, StringBuilder sb) {
        if (node == null) {
            return;
        }

        // Agregar el nodo actual al StringBuilder
        sb.append(prefix);
        sb.append(node.label != null ? node.label : "Node");
        if (node.value != null && !node.value.isEmpty()) {
            sb.append(": ").append(node.value);
        }
        sb.append("\n");

        // Recorrer hijos
        if (node.children != null) {
            for (int i = 0; i < node.children.size(); i++) {
                ASTNode child = node.children.get(i);
                // Lógica para dibujar las líneas del árbol
                boolean isLast = (i == node.children.size() - 1);
                String newPrefix = prefix + (isLast ? "    " : "│   ");
                String childPrefix = prefix + (isLast ? "└── " : "├── ");

                // Llamada recursiva
                // Pero para los hijos de los hijos pasamos newPrefix
                getASTAsString(child, prefix + (isLast ? "    " : "│   "), sb);

                // NOTA: Para simplificarlo visualmente, a veces es mejor hacerlo así:
                // getASTAsString(child, prefix + "    ", sb);
            }
        }
    }

    private void mostrarVentanaErrores() {
        if (errors.isEmpty()) {
            JOptionPane.showMessageDialog(this, "¡Felicidades! No hay errores en el código.", "Compilación Exitosa", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        javax.swing.JDialog ventanaErrores = new javax.swing.JDialog(this, "Tabla de Errores Detectados", true);
        ventanaErrores.setSize(1050, 420);
        ventanaErrores.setLocationRelativeTo(this);

        String[] columnas = {"Código", "Tipo", "Línea", "Col.", "Descripción", "Sugerencia de Corrección"};
        DefaultTableModel modelo = new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        javax.swing.JTable tabla = new javax.swing.JTable(modelo);

        // Regex para extraer el código entre corchetes: [LexError 001], [SinError 010], [SemError 001], etc.
        Pattern codigoPattern = Pattern.compile("\\[((?:LexError|SinError|SemError)\\s+\\d+)\\]");

        for (ErrorLSSL error : errors) {
            String desc = error.getDescription();

            // Extraer código con regex
            Matcher mCode = codigoPattern.matcher(desc);
            String codigoStr = mCode.find() ? mCode.group(1) : "Desconocido";

            // Determinar categoría
            String tipo = codigoStr.startsWith("LexError") ? "Léxico"
                        : codigoStr.startsWith("SinError") ? "Sintáctico"
                        : codigoStr.startsWith("SemError") ? "Semántico"
                        : "General";

            // Separar descripción de sugerencia por el delimitador " | ✏ "
            String descripcion = desc;
            String sugerencia  = "";
            int sep = desc.indexOf(" | ✏ ");
            if (sep >= 0) {
                descripcion = desc.substring(0, sep);
                sugerencia  = desc.substring(sep + 5); // salta " | ✏ "
            }
            // Quitar el código del inicio de la descripción (ya está en la columna Código)
            descripcion = descripcion.replaceFirst("\\[(?:LexError|SinError|SemError)\\s+\\d+\\]\\s*", "").trim();

            Object[] fila = {
                codigoStr,
                tipo,
                error.getLine()   > 0 ? error.getLine()   : "-",
                error.getColumn() > 0 ? error.getColumn() : "-",
                descripcion,
                sugerencia
            };
            modelo.addRow(fila);
        }

        // Anchos de columna
        tabla.setRowHeight(28);
        tabla.setFont(new java.awt.Font("Consolas", java.awt.Font.PLAIN, 12));
        tabla.getTableHeader().setFont(new java.awt.Font("Consolas", java.awt.Font.BOLD, 12));
        tabla.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(115); // Código
        tabla.getColumnModel().getColumn(1).setPreferredWidth(80);  // Tipo
        tabla.getColumnModel().getColumn(2).setPreferredWidth(50);  // Línea
        tabla.getColumnModel().getColumn(3).setPreferredWidth(40);  // Col.
        tabla.getColumnModel().getColumn(4).setPreferredWidth(350); // Descripción
        tabla.getColumnModel().getColumn(5).setPreferredWidth(390); // Sugerencia

        // Colorear filas según tipo de error
        tabla.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(
                    javax.swing.JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                if (!sel) {
                    String tipo = (String) t.getValueAt(row, 1);
                    if ("Léxico".equals(tipo))      setBackground(new java.awt.Color(255, 230, 230));
                    else if ("Sintáctico".equals(tipo)) setBackground(new java.awt.Color(255, 245, 210));
                    else if ("Semántico".equals(tipo))  setBackground(new java.awt.Color(230, 240, 255));
                    else setBackground(java.awt.Color.WHITE);
                    setForeground(java.awt.Color.BLACK);
                }
                return this;
            }
        });

        // Resumen en la parte superior
        long numLex = errors.stream().filter(e -> e.getDescription().contains("LexError")).count();
        long numSin = errors.stream().filter(e -> e.getDescription().contains("SinError")).count();
        long numSem = errors.stream().filter(e -> e.getDescription().contains("SemError")).count();
        String resumen = String.format("Total: %d error(es)  |  🔴 Léxicos: %d  |  🟡 Sintácticos: %d  |  🔵 Semánticos: %d",
                errors.size(), numLex, numSin, numSem);
        javax.swing.JLabel lblResumen = new javax.swing.JLabel(resumen);
        lblResumen.setFont(new java.awt.Font("Consolas", java.awt.Font.BOLD, 12));
        lblResumen.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 10, 6, 10));

        ventanaErrores.setLayout(new java.awt.BorderLayout());
        ventanaErrores.add(lblResumen, java.awt.BorderLayout.NORTH);
        ventanaErrores.add(new javax.swing.JScrollPane(tabla), java.awt.BorderLayout.CENTER);
        ventanaErrores.setVisible(true);
    }

    private void mostrarVentanaSimbolos() {
        // 1. Verificamos la lista correcta
        if (listaSimbolosGlobal == null || listaSimbolosGlobal.isEmpty()) {
            JOptionPane.showMessageDialog(this, "La tabla de símbolos está vacía.\nAsegúrate de compilar código que tenga declaraciones (ej. ESTADO q0; o TIPO AFD;)", "Tabla Vacía", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Mensaje de prueba para la consola de NetBeans
        System.out.println("Abriendo ventana. Total de símbolos a dibujar: " + listaSimbolosGlobal.size());

        javax.swing.JDialog ventana = new javax.swing.JDialog(this, "Tabla de Símbolos", true);
        ventana.setSize(600, 400);
        ventana.setLocationRelativeTo(this);

        String[] columnas = {"Nombre / Identificador", "Categoría / Tipo", "Línea", "Columna"};
        DefaultTableModel modelo = new DefaultTableModel(columnas, 0);
        javax.swing.JTable tabla = new javax.swing.JTable(modelo);

        // Llenar la tabla
        for (SimboloDSL sym : listaSimbolosGlobal) {
            Object[] fila = {sym.nombre, sym.tipo, sym.linea, sym.columna};
            modelo.addRow(fila);
        }

        tabla.setRowHeight(25);
        ventana.add(new javax.swing.JScrollPane(tabla));
        ventana.setVisible(true);
    }

    private void clearFields() {
        Functions.clearDataInTable(tbl_Token);
        panel_Salida.setText("");
        tokens.clear();
        errors.clear();
        identProd.clear();
        identificadores.clear();
        codeHasBeenCompiled = false;
        panel_Codigo.getHighlighter().removeAllHighlights();
    }

    private void compile() {
        clearFields();
        lexicalAnalysis();
        fieldTableTokens();
        syntacticAnalysis();
        semanticAnalysis();
        printConsole();
        codeHasBeenCompiled = true;
    }

    private void lexicalAnalysis() {
        // Extraer tokens
        tokens.clear();
        Lexer lexer;
        try {
            // 1) Guardar texto en archivo temporal
            File codigo = new File("code.encrypter");
            try (FileOutputStream output = new FileOutputStream(codigo)) {
                byte[] bytesText = panel_Codigo.getText()
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8);
                output.write(bytesText);
            }

            // 2) Abrir reader en UTF-8 y crear el Lexer
            try (BufferedReader entrada = new BufferedReader(
                    new InputStreamReader(new FileInputStream(codigo), java.nio.charset.StandardCharsets.UTF_8))) {
                lexer = new Lexer(entrada);

                // 3) Leer símbolos hasta EOF
                while (true) {
                    java_cup.runtime.Symbol symbol = lexer.next_token();
                    if (symbol == null) {
                        break;
                    }
                    // Aquí “sym” es la clase; “symbol” es la variable
                    if (symbol.sym == sym.EOF) {
                        break;
                    }
                    Token token = (Token) symbol.value;
                    tokens.add(token);
                }
            }
        } catch (FileNotFoundException ex) {
            System.err.println("El archivo no pudo ser encontrado: " + ex.getMessage());
        } catch (IOException ex) {
            System.err.println("Error de E/S con el archivo: " + ex.getMessage());
        }
    }

    private void fieldTableTokens() {
        tokens.forEach(token -> {
            Object[] data = new Object[]{token.getLexicalComp(), token.getLexeme(), "[" + token.getLine() + "," + token.getColumn() + "]"};
            Functions.addRowDataInTable(tbl_Token, data);
        });
    }

    private void syntacticAnalysis() {
        errors.clear(); // Limpia errores anteriores
        try {
            String code = panel_Codigo.getText();
            Reader reader = new java.io.StringReader(code);
            Lexer lexer = new Lexer(reader);

            Parser parser = new Parser(lexer);
            java_cup.runtime.Symbol result = parser.parse();

            errors.addAll(parser.errors);

            // Mostrar errores o éxito
            if (!errors.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append("Errores sintácticos detectados:\n");
                for (compilerTools.ErrorLSSL err : errors) {
                    sb.append(err.toString()).append("\n");
                }
                panel_Salida.setText(sb.toString());
            } else {
                panel_Salida.setText("Compilación sintáctica exitosa. No se detectaron errores.");

                // Mostrar el árbol de derivación en consola
                ASTNode root = (ASTNode) result.value;
                printAST(root, "");
            }
        } catch (Exception ex) {
            panel_Salida.setText("Error durante el análisis sintáctico: " + ex.getMessage());
        }
    }

    private void semanticAnalysis() {
        semanticAnalysis(panel_Codigo.getText());
    }

    private void semanticAnalysis(String input) {
        if (input == null || input.trim().isEmpty()) {
            return;
        }

        Set<String> alphabetSymbols = new HashSet<>();
        Set<String> declaredStates = new HashSet<>();
        Set<String> finalStates = new HashSet<>();
        Set<String> initialStates = new HashSet<>();
        boolean hasAutomatonType = false;
        boolean hasEpsilon = false;

        for (SimboloDSL symbol : listaSimbolosGlobal) {
            if (symbol.tipo == null) {
                continue;
            }
            switch (symbol.tipo) {
                case "Alphabet_Symbol":
                    alphabetSymbols.add(symbol.nombre.replace("'", ""));
                    break;
                case "Epsilon_Symbol":
                    hasEpsilon = true;
                    alphabetSymbols.add("EPSILON");
                    break;
                case "State_Declared":
                case "Final_State":
                case "Initial_State":
                    declaredStates.add(symbol.nombre);
                    break;
            }
            if (symbol.tipo.equals("Tipo_Automata_AFD") || symbol.tipo.equals("Tipo_Automata_AFN")) {
                hasAutomatonType = true;
            }
            if (symbol.tipo.equals("Final_State")) {
                finalStates.add(symbol.nombre);
            }
            if (symbol.tipo.equals("Initial_State")) {
                initialStates.add(symbol.nombre);
            }
        }

        if (!hasAutomatonType) {
            errors.add(new ErrorLSSL(1, "[SemError 001] No se declaró el tipo de autómata. | ✏ Agrega al inicio del programa: TIPO AFD;  ó  TIPO AFN;", null));
        }
        if (initialStates.isEmpty()) {
            errors.add(new ErrorLSSL(1, "[SemError 002] No se declaró el estado inicial. | ✏ Agrega: INICIO q0;  (debe ir después de ALFABETO)", null));
        }
        if (finalStates.isEmpty()) {
            errors.add(new ErrorLSSL(1, "[SemError 003] No se declaró ningún estado final. | ✏ Agrega al final del programa: FINAL q2;  ó  FINAL q1, q2;", null));
        }
        if (alphabetSymbols.isEmpty()) {
            errors.add(new ErrorLSSL(1, "[SemError 004] No se declaró el alfabeto. | ✏ Agrega: ALFABETO { 'a', 'b' };  (después de TIPO)", null));
        }

        Pattern transitionPattern = Pattern.compile("([A-Za-z0-9_]+)\\s*->\\s*([A-Za-z0-9_]+)\\s*\\[\\s*([^\\]]+)\\s*\\]\\s*;");
        Matcher matcher = transitionPattern.matcher(input);
        int transitionCount = 0;

        while (matcher.find()) {
            transitionCount++;
            String origin = matcher.group(1);
            String destination = matcher.group(2);
            String symbolText = matcher.group(3).trim();

            if (!declaredStates.contains(origin)) {
                errors.add(new ErrorLSSL(1, "[SemError 005] El estado origen '" + origin + "' no fue declarado. | ✏ Agrega: ESTADO " + origin + ";  (o INICIO " + origin + "; si es el estado inicial)", null));
            }
            if (!declaredStates.contains(destination)) {
                errors.add(new ErrorLSSL(1, "[SemError 006] El estado destino '" + destination + "' no fue declarado. | ✏ Agrega: ESTADO " + destination + ";  (antes de las transiciones)", null));
            }

            String[] parts = symbolText.split(",");
            for (String part : parts) {
                String tokenValue = part.trim();
                if (tokenValue.startsWith("'") && tokenValue.endsWith("'")) {
                    tokenValue = tokenValue.substring(1, tokenValue.length() - 1);
                }
                if (tokenValue.equals("EPSILON") && !hasEpsilon) {
                    errors.add(new ErrorLSSL(1, "[SemError 007] Se usa EPSILON en una transición pero no está en el alfabeto. | ✏ Agrega EPSILON al alfabeto: ALFABETO { ..., EPSILON };  (solo válido en AFN)", null));
                } else if (!tokenValue.equals("EPSILON") && !alphabetSymbols.contains(tokenValue)) {
                    errors.add(new ErrorLSSL(1, "[SemError 008] El símbolo '" + tokenValue + "' se usa en una transición pero no pertenece al alfabeto. | ✏ Agrégalo al alfabeto: ALFABETO { ..., '" + tokenValue + "' };", null));
                }
            }
        }

        if (transitionCount == 0) {
            errors.add(new ErrorLSSL(1, "[SemError 009] No se encontró ninguna transición en el programa. | ✏ Agrega al menos una: q0 -> q1 ['a'];", null));
        }
    }

    private void printAST(ASTNode node, String indent) {
        if (node == null) {
            return;
        }
        String display = node.label + (node.value != null && !node.value.isEmpty() ? ": " + node.value : "");
        System.out.println(indent + display);
        for (ASTNode child : node.children) {
            printAST(child, indent + "  ");
        }
    }

    private void printConsole() {
        int sizeErrors = errors.size();
        if (sizeErrors > 0) {
            Functions.sortErrorsByLineAndColumn(errors);
            String strErrors = "\n";
            for (ErrorLSSL error : errors) {
                String strError = String.valueOf(error);
                strErrors += strError + "\n";
            }
            panel_Salida.setText("Compilación Terminada...\n" + strErrors + "\nLa compilación terminó con errores");
        } else {
            panel_Salida.setText("Compilación Terminada...");
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane2 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        panel_Principal = new javax.swing.JPanel();
        panel_botones = new javax.swing.JPanel();
        btn_Nuevo = new javax.swing.JButton();
        btn_Abrir = new javax.swing.JButton();
        btn_GuardarC = new javax.swing.JButton();
        btn_Guardar = new javax.swing.JButton();
        panel_botones_exec_comp = new javax.swing.JPanel();
        btn_Compilar = new javax.swing.JButton();
        btn_Ejecutar = new javax.swing.JButton();
        btn_VerArbol = new javax.swing.JButton();
        btn_Errores = new javax.swing.JButton();
        btn_Simbolos = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        panel_Codigo = new javax.swing.JTextPane();
        jScrollPane3 = new javax.swing.JScrollPane();
        panel_Salida = new javax.swing.JTextPane();
        jScrollPane4 = new javax.swing.JScrollPane();
        tbl_Token = new javax.swing.JTable();

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane2.setViewportView(jTextArea1);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.LINE_AXIS));

        btn_Nuevo.setFont(new java.awt.Font("Consolas", 1, 14)); // NOI18N
        btn_Nuevo.setText("Nuevo");
        btn_Nuevo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_NuevoActionPerformed(evt);
            }
        });

        btn_Abrir.setFont(new java.awt.Font("Consolas", 1, 14)); // NOI18N
        btn_Abrir.setText("Abrir");
        btn_Abrir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_AbrirActionPerformed(evt);
            }
        });

        btn_GuardarC.setFont(new java.awt.Font("Consolas", 1, 14)); // NOI18N
        btn_GuardarC.setText("Guardar Como");
        btn_GuardarC.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_GuardarCActionPerformed(evt);
            }
        });

        btn_Guardar.setFont(new java.awt.Font("Consolas", 1, 14)); // NOI18N
        btn_Guardar.setText("Guardar");
        btn_Guardar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_GuardarActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panel_botonesLayout = new javax.swing.GroupLayout(panel_botones);
        panel_botones.setLayout(panel_botonesLayout);
        panel_botonesLayout.setHorizontalGroup(
            panel_botonesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panel_botonesLayout.createSequentialGroup()
                .addGap(9, 9, 9)
                .addComponent(btn_Nuevo)
                .addGap(18, 18, 18)
                .addComponent(btn_Abrir)
                .addGap(18, 18, 18)
                .addComponent(btn_Guardar)
                .addGap(18, 18, 18)
                .addComponent(btn_GuardarC)
                .addContainerGap(9, Short.MAX_VALUE))
        );
        panel_botonesLayout.setVerticalGroup(
            panel_botonesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panel_botonesLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(panel_botonesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn_Nuevo)
                    .addComponent(btn_Abrir)
                    .addComponent(btn_Guardar)
                    .addComponent(btn_GuardarC))
                .addContainerGap())
        );

        btn_Compilar.setFont(new java.awt.Font("Consolas", 1, 14)); // NOI18N
        btn_Compilar.setText("Compilar");
        btn_Compilar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_CompilarActionPerformed(evt);
            }
        });

        btn_Ejecutar.setFont(new java.awt.Font("Consolas", 1, 14)); // NOI18N
        btn_Ejecutar.setText("Ejecutar");
        btn_Ejecutar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_EjecutarActionPerformed(evt);
            }
        });

        btn_VerArbol.setFont(new java.awt.Font("Consolas", 1, 14)); // NOI18N
        btn_VerArbol.setText("Arbol de Derivacion");
        btn_VerArbol.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_VerArbolActionPerformed(evt);
            }
        });

        btn_Errores.setFont(new java.awt.Font("Consolas", 1, 14)); // NOI18N
        btn_Errores.setText("Tabla de Errores");
        btn_Errores.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_ErroresActionPerformed(evt);
            }
        });

        btn_Simbolos.setFont(new java.awt.Font("Consolas", 1, 14)); // NOI18N
        btn_Simbolos.setText("Tabla de Simbolos");
        btn_Simbolos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_SimbolosActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panel_botones_exec_compLayout = new javax.swing.GroupLayout(panel_botones_exec_comp);
        panel_botones_exec_comp.setLayout(panel_botones_exec_compLayout);
        panel_botones_exec_compLayout.setHorizontalGroup(
            panel_botones_exec_compLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panel_botones_exec_compLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btn_VerArbol)
                .addGap(18, 18, 18)
                .addComponent(btn_Compilar)
                .addGap(18, 18, 18)
                .addComponent(btn_Ejecutar)
                .addGap(18, 18, 18)
                .addComponent(btn_Errores)
                .addGap(18, 18, 18)
                .addComponent(btn_Simbolos)
                .addGap(0, 6, Short.MAX_VALUE))
        );
        panel_botones_exec_compLayout.setVerticalGroup(
            panel_botones_exec_compLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panel_botones_exec_compLayout.createSequentialGroup()
                .addContainerGap(7, Short.MAX_VALUE)
                .addGroup(panel_botones_exec_compLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn_Ejecutar)
                    .addComponent(btn_Compilar)
                    .addComponent(btn_VerArbol)
                    .addComponent(btn_Errores)
                    .addComponent(btn_Simbolos))
                .addContainerGap())
        );

        panel_Codigo.setBackground(new java.awt.Color(255, 255, 255));
        panel_Codigo.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jScrollPane1.setViewportView(panel_Codigo);

        panel_Salida.setBackground(new java.awt.Color(255, 255, 255));
        panel_Salida.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jScrollPane3.setViewportView(panel_Salida);

        tbl_Token.setBackground(new java.awt.Color(255, 255, 255));
        tbl_Token.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null}
            },
            new String [] {
                "Componente Lexico", "Lexema", "[Línea, Columna]"
            }
        ));
        jScrollPane4.setViewportView(tbl_Token);

        javax.swing.GroupLayout panel_PrincipalLayout = new javax.swing.GroupLayout(panel_Principal);
        panel_Principal.setLayout(panel_PrincipalLayout);
        panel_PrincipalLayout.setHorizontalGroup(
            panel_PrincipalLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panel_PrincipalLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(panel_PrincipalLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panel_PrincipalLayout.createSequentialGroup()
                        .addComponent(panel_botones, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(147, 147, 147)
                        .addComponent(panel_botones_exec_comp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(panel_PrincipalLayout.createSequentialGroup()
                        .addGroup(panel_PrincipalLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jScrollPane3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 905, Short.MAX_VALUE)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING))
                        .addGap(18, 18, 18)
                        .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)))
                .addContainerGap(20, Short.MAX_VALUE))
        );
        panel_PrincipalLayout.setVerticalGroup(
            panel_PrincipalLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panel_PrincipalLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panel_PrincipalLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(panel_botones_exec_comp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(panel_botones, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(panel_PrincipalLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panel_PrincipalLayout.createSequentialGroup()
                        .addComponent(jScrollPane1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 210, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 535, Short.MAX_VALUE))
                .addContainerGap())
        );

        getContentPane().add(panel_Principal);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btn_NuevoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_NuevoActionPerformed
        Directorio.New();
        clearFields();
    }//GEN-LAST:event_btn_NuevoActionPerformed

    private void btn_AbrirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_AbrirActionPerformed
        if (Directorio.Open()) {
            colorAnalysis();
            clearFields();
        }
    }//GEN-LAST:event_btn_AbrirActionPerformed

    private void btn_GuardarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_GuardarActionPerformed
        if (Directorio.Save()) {
            clearFields();
        }
    }//GEN-LAST:event_btn_GuardarActionPerformed

    private void btn_GuardarCActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_GuardarCActionPerformed
        if (Directorio.SaveAs()) {
            clearFields();
        }
    }//GEN-LAST:event_btn_GuardarCActionPerformed

    private void btn_CompilarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_CompilarActionPerformed
// 1. Limpiamos campos, tablas y consola
        Functions.clearDataInTable(tbl_Token);
        panel_Salida.setText("");
        errors.clear();
        tokens.clear();

        // Limpiamos la tabla de símbolos de compilaciones anteriores
        if (listaSimbolos != null) {
            listaSimbolos.clear();
        } else {
            listaSimbolos = new java.util.ArrayList<>();
        }
        if (listaSimbolosGlobal != null) {
            listaSimbolosGlobal.clear();
        } else {
            listaSimbolosGlobal = new java.util.ArrayList<>();
        }

        codeHasBeenCompiled = false;

        String input = panel_Codigo.getText();
        if (input.trim().isEmpty()) {
            panel_Salida.setForeground(Color.BLACK);
            panel_Salida.setText("No hay código para compilar.");
            return;
        }

        try {
            // ==========================================
            // PASO 1: ANÁLISIS LÉXICO (Llenar la tabla)
            // ==========================================
            Lexer lexerTabla = new Lexer(new java.io.StringReader(input));
            while (true) {
                java_cup.runtime.Symbol symbol = lexerTabla.next_token();
                if (symbol == null || symbol.sym == sym.EOF) {
                    break;
                }

                // Agregamos el token a la lista y a la tabla visual
                if (symbol.value instanceof Token) {
                    Token token = (Token) symbol.value;
                    tokens.add(token);

                    Object[] data = new Object[]{
                        token.getLexicalComp(),
                        token.getLexeme(),
                        "[" + token.getLine() + ", " + token.getColumn() + "]"
                    };
                    Functions.addRowDataInTable(tbl_Token, data);

                    // Si el token es un error léxico no reconocido (configurado en tu .flex)
                    if (token.getLexicalComp().equals("ERROR_LEXICO")) {
                        errors.add(new ErrorLSSL(1, "[LexError 001] Error Léxico: Carácter no reconocido", token));
                    }
                }
            }

            // ==========================================
            // PASO 2: ANÁLISIS SINTÁCTICO
            // ==========================================
            Lexer lexerParser = new Lexer(new java.io.StringReader(input));
            Parser parser = new Parser(lexerParser);

            try {
                parser.parse();
            } catch (Exception ex) {
                // ... ignoramos el error grave aquí para poder leer los errores recuperados
            }

            // Recolectamos los errores sintácticos
            if (parser.errors != null && !parser.errors.isEmpty()) {
                errors.addAll(parser.errors);
            }

            // ========================================================
            // 🌟 ESTA ES LA PARTE QUE LLENA LA TABLA DE SÍMBOLOS
            // ========================================================
            if (parser.symbols != null && !parser.symbols.isEmpty()) {
                listaSimbolosGlobal.addAll(parser.symbols);
                System.out.println("Símbolos extraídos del parser: " + parser.symbols.size());
            } else {
                System.out.println("El parser no guardó ningún símbolo en esta ejecución.");
            }

            // ========================================================
            // 🌟 VALIDACIÓN SEMÁNTICA BÁSICA
            // ========================================================
            if (parser.errors == null || parser.errors.isEmpty()) {
                semanticAnalysis(input);
            }

        } catch (Exception ex) {
            System.out.println("Error general en la compilación: " + ex.getMessage());
        }

        // ==========================================
        // PASO 3: MOSTRAR RESULTADOS EN LA CONSOLA
        // ==========================================
        if (errors.isEmpty()) {
            // Si no hay errores, mensaje de éxito
            panel_Salida.setForeground(new Color(0, 150, 0)); // Verde
            panel_Salida.setText("✅ Compilación exitosa.\nNo se encontraron errores léxicos ni sintácticos.");
        } else {
            panel_Salida.setForeground(Color.RED);
            StringBuilder consola = new StringBuilder();
            String codigoCompleto = panel_Codigo.getText();
            String[] lineasDeCodigo = codigoCompleto.split("\\r?\\n");

            // Conteo por categoría
            long numLex = errors.stream().filter(e -> e.getDescription().contains("LexError")).count();
            long numSin = errors.stream().filter(e -> e.getDescription().contains("SinError")).count();
            long numSem = errors.stream().filter(e -> e.getDescription().contains("SemError")).count();

            consola.append("❌ Compilación con errores — ")
                   .append(errors.size()).append(" error(es) encontrado(s)\n");
            if (numLex > 0) consola.append("   🔴 Léxicos    : ").append(numLex).append("\n");
            if (numSin > 0) consola.append("   🟡 Sintácticos: ").append(numSin).append("\n");
            if (numSem > 0) consola.append("   🔵 Semánticos : ").append(numSem).append("\n");
            consola.append("\n");

            for (ErrorLSSL error : errors) {
                String desc = error.getDescription();
                int numLinea = error.getLine();

                // Categoría
                String categoria = desc.contains("LexError") ? "LÉXICO"
                                 : desc.contains("SinError") ? "SINTÁCTICO"
                                 : desc.contains("SemError") ? "SEMÁNTICO" : "ERROR";

                // Separar descripción de sugerencia
                String descripcion = desc;
                String sugerencia  = null;
                int sepIdx = desc.indexOf(" | ✏ ");
                if (sepIdx >= 0) {
                    descripcion = desc.substring(0, sepIdx);
                    sugerencia  = desc.substring(sepIdx + 5);
                }

                // Línea del código fuente
                String fragmento = "";
                if (numLinea > 0 && numLinea <= lineasDeCodigo.length) {
                    fragmento = lineasDeCodigo[numLinea - 1].trim();
                }

                // Formato en consola
                consola.append("┌─ [").append(categoria).append("]");
                if (numLinea > 0) consola.append("  Línea ").append(numLinea)
                                         .append(", Col. ").append(error.getColumn());
                consola.append("\n");
                consola.append("│  ").append(descripcion).append("\n");
                if (!fragmento.isEmpty()) {
                    consola.append("│  ► ").append(fragmento).append("\n");
                }
                if (sugerencia != null && !sugerencia.isEmpty()) {
                    consola.append("│  ✏ ").append(sugerencia).append("\n");
                }
                consola.append("└─────────────────────────────────────────────────\n\n");
            }
            panel_Salida.setText(consola.toString());
        }

        codeHasBeenCompiled = true;
        resaltarErroresEnEditor();
    }//GEN-LAST:event_btn_CompilarActionPerformed

    private void btn_EjecutarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_EjecutarActionPerformed
        btn_Compilar.doClick();
        if (codeHasBeenCompiled) {
            if (!errors.isEmpty()) {
                JOptionPane.showMessageDialog(null, "No se puede ejecutar el código ya que se encontró uno o más errores");
            }
        } else {

        }
    }//GEN-LAST:event_btn_EjecutarActionPerformed

    private void btn_ErroresActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_ErroresActionPerformed
        // TODO add your handling code here:
        mostrarVentanaErrores();
    }//GEN-LAST:event_btn_ErroresActionPerformed

    private void btn_SimbolosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_SimbolosActionPerformed
        // TODO add your handling code here:
        mostrarVentanaSimbolos();
    }//GEN-LAST:event_btn_SimbolosActionPerformed

    private void btn_VerArbolActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            String code = panel_Codigo.getText();
            Reader reader = new java.io.StringReader(code);
            Lexer lexer = new Lexer(reader);
            Parser parser = new Parser(lexer);
            java_cup.runtime.Symbol result = parser.parse();

            errors.clear();
            errors.addAll(parser.errors);

            if (!errors.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Corrige los errores sintácticos antes de ver el árbol.");
                return;
            }

            // Aquí va la validación:
            if (!(result.value instanceof ASTNode)) {
                JOptionPane.showMessageDialog(this, "No se pudo generar el árbol de derivación (resultado inesperado).");
                return;
            }
            ASTNode root = (ASTNode) result.value;
            new VentanaArbolGrafico(root).setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al generar el árbol: " + ex.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // PUNTO 5 — Subrayado inline de errores en el editor
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Resalta con fondo rojo suave todas las líneas que tengan errores
     * en el editor (panel_Codigo). Se llama después de compilar.
     */
    private void resaltarErroresEnEditor() {
        javax.swing.text.Highlighter hl = panel_Codigo.getHighlighter();
        hl.removeAllHighlights();
        if (errors.isEmpty()) return;

        String text = panel_Codigo.getText();
        // Calcular posición de inicio de cada línea
        String[] lines = text.split("\n", -1);
        int[] lineStart = new int[lines.length + 1];
        lineStart[0] = 0;
        for (int i = 0; i < lines.length; i++) {
            lineStart[i + 1] = lineStart[i] + lines[i].length() + 1; // +1 por \n
        }

        javax.swing.text.Highlighter.HighlightPainter painter =
            new javax.swing.text.DefaultHighlighter.DefaultHighlightPainter(new Color(255, 200, 200));

        for (ErrorLSSL error : errors) {
            int lineNum = error.getLine();
            if (lineNum > 0 && lineNum <= lines.length) {
                try {
                    int start = lineStart[lineNum - 1];
                    int end   = lineStart[lineNum] - 1; // antes del \n
                    if (end < start) end = start;
                    hl.addHighlight(start, end, painter);
                } catch (javax.swing.text.BadLocationException ex) {
                    // sin acción — la línea puede haber cambiado
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // PUNTO 2 — Tabla de transiciones δ
    // ══════════════════════════════════════════════════════════════════════

    /** Muestra la tabla de transiciones δ del autómata compilado. */
    private void mostrarTablaTransiciones() {
        if (!codeHasBeenCompiled) {
            JOptionPane.showMessageDialog(this, "Primero debes compilar el código.", "Sin compilar", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!errors.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El código tiene errores. Corrígelos antes de ver la tabla δ.", "Errores detectados", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // ── Extraer datos del autómata ──────────────────────────────────────
        String input = panel_Codigo.getText();
        java.util.List<String> alphabet    = new java.util.ArrayList<>();
        java.util.List<String> stateOrder  = new java.util.ArrayList<>();
        String tipoAutomata  = "AFD";
        String estadoInicial = null;
        java.util.Set<String> estadosFinales = new java.util.LinkedHashSet<>();

        for (SimboloDSL s : listaSimbolosGlobal) {
            if (s.tipo == null) continue;
            switch (s.tipo) {
                case "Tipo_Automata_AFN": tipoAutomata = "AFN"; break;
                case "Alphabet_Symbol":
                    String sym = s.nombre.replace("'", "");
                    if (!alphabet.contains(sym)) alphabet.add(sym);
                    break;
                case "Epsilon_Symbol":
                    if (!alphabet.contains("ε")) alphabet.add("ε");
                    break;
                case "Initial_State":
                    if (!stateOrder.contains(s.nombre)) stateOrder.add(0, s.nombre); // inicial primero
                    estadoInicial = s.nombre;
                    break;
                case "State_Declared":
                    if (!stateOrder.contains(s.nombre)) stateOrder.add(s.nombre);
                    break;
                case "Final_State":
                    if (!stateOrder.contains(s.nombre)) stateOrder.add(s.nombre);
                    estadosFinales.add(s.nombre);
                    break;
            }
        }

        if (stateOrder.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No se encontraron estados declarados.", "Tabla vacía", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // ── Construir mapa delta ────────────────────────────────────────────
        java.util.Map<String, java.util.Map<String, java.util.List<String>>> delta = new java.util.LinkedHashMap<>();
        for (String st : stateOrder) delta.put(st, new java.util.LinkedHashMap<>());

        Pattern transPat = Pattern.compile(
            "([A-Za-z0-9_]+)\\s*->\\s*([A-Za-z0-9_]+)\\s*\\[\\s*([^\\]]+)\\s*\\]\\s*;");
        Matcher m = transPat.matcher(input);
        while (m.find()) {
            String origin = m.group(1);
            String dest   = m.group(2);
            for (String rawSym : m.group(3).split(",")) {
                String sym = rawSym.trim().replace("'", "");
                if (sym.equalsIgnoreCase("EPSILON")) sym = "ε";
                delta.computeIfAbsent(origin, k -> new java.util.LinkedHashMap<>())
                     .computeIfAbsent(sym,    k -> new java.util.ArrayList<>())
                     .add(dest);
            }
        }

        // ── Construir modelo de tabla ───────────────────────────────────────
        String[] colHeaders = new String[alphabet.size() + 1];
        colHeaders[0] = "Estado";
        for (int i = 0; i < alphabet.size(); i++) colHeaders[i + 1] = alphabet.get(i);

        DefaultTableModel model = new DefaultTableModel(colHeaders, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        final String estadoInicialFinal = estadoInicial;
        for (String state : stateOrder) {
            Object[] row = new Object[alphabet.size() + 1];
            boolean isInit  = state.equals(estadoInicialFinal);
            boolean isFinal = estadosFinales.contains(state);
            String stateLabel = (isInit && isFinal) ? "→* " + state
                              : isInit               ? "→ "  + state
                              : isFinal              ? "* "  + state
                              :                        "    " + state;
            row[0] = stateLabel;
            for (int i = 0; i < alphabet.size(); i++) {
                java.util.List<String> targets = delta.getOrDefault(state, Collections.emptyMap())
                                                      .get(alphabet.get(i));
                if (targets == null || targets.isEmpty()) row[i + 1] = "—";
                else if (targets.size() == 1)             row[i + 1] = targets.get(0);
                else                                      row[i + 1] = "{" + String.join(", ", targets) + "}";
            }
            model.addRow(row);
        }

        // ── Tabla con renderer ──────────────────────────────────────────────
        javax.swing.JTable table = new javax.swing.JTable(model);
        table.setRowHeight(28);
        table.setFont(new java.awt.Font("Consolas", java.awt.Font.PLAIN, 12));
        table.getTableHeader().setFont(new java.awt.Font("Consolas", java.awt.Font.BOLD, 12));
        table.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);

        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(
                    javax.swing.JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                if (!sel) {
                    String sv = (String) t.getValueAt(row, 0);
                    if (sv != null && sv.contains("→") && sv.contains("*")) setBackground(new Color(200, 240, 200));
                    else if (sv != null && sv.contains("→"))                 setBackground(new Color(255, 255, 190));
                    else if (sv != null && sv.contains("*"))                 setBackground(new Color(200, 220, 255));
                    else setBackground(Color.WHITE);
                    setForeground(Color.BLACK);
                    setHorizontalAlignment(col == 0 ? LEFT : CENTER);
                }
                return this;
            }
        });

        // ── Diálogo ─────────────────────────────────────────────────────────
        javax.swing.JDialog dialog = new javax.swing.JDialog(this, "Tabla de Transiciones δ — " + tipoAutomata, true);
        dialog.setSize(660, 360);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new java.awt.BorderLayout());

        JPanel headerPnl = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        headerPnl.setBackground(new Color(41, 98, 155));
        JLabel headerLbl = new JLabel("  Función de Transición δ — " + tipoAutomata
            + "      →  inicial      *  final      →*  ambos");
        headerLbl.setForeground(Color.WHITE);
        headerLbl.setFont(new java.awt.Font("Consolas", java.awt.Font.BOLD, 12));
        headerPnl.add(headerLbl);

        JPanel legendPnl = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 5));
        legendPnl.setBackground(new Color(250, 250, 250));
        legendPnl.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(200, 200, 200)));
        legendPnl.add(deltaChip(new Color(255, 255, 190), "→  Estado inicial"));
        legendPnl.add(deltaChip(new Color(200, 220, 255), "*  Estado final"));
        legendPnl.add(deltaChip(new Color(200, 240, 200), "→*  Inicial y final"));

        dialog.add(headerPnl,  java.awt.BorderLayout.NORTH);
        dialog.add(new javax.swing.JScrollPane(table), java.awt.BorderLayout.CENTER);
        dialog.add(legendPnl,  java.awt.BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    /** Chip coloreado pequeño para la leyenda de la tabla δ. */
    private JPanel deltaChip(Color color, String text) {
        JPanel chip = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 4, 0));
        chip.setBackground(new Color(250, 250, 250));
        JPanel sq = new JPanel();
        sq.setPreferredSize(new java.awt.Dimension(13, 13));
        sq.setBackground(color);
        sq.setBorder(BorderFactory.createLineBorder(color.darker(), 1));
        JLabel lbl = new JLabel(text);
        lbl.setFont(new java.awt.Font("Consolas", java.awt.Font.PLAIN, 11));
        chip.add(sq);
        chip.add(lbl);
        return chip;
    }

    // ══════════════════════════════════════════════════════════════════════
    // PUNTO 3 — Simulación de cadenas
    // ══════════════════════════════════════════════════════════════════════

    /** Solicita una cadena y simula su recorrido en el autómata compilado. */
    private void mostrarSimulacionCadena() {
        if (!codeHasBeenCompiled) {
            JOptionPane.showMessageDialog(this, "Primero debes compilar el código.", "Sin compilar", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!errors.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El código tiene errores. Corrígelos antes de simular.", "Errores detectados", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String cadena = JOptionPane.showInputDialog(this,
            "Ingresa la cadena a simular:\n(deja vacío para la cadena ε vacía)",
            "Simulación de Cadena", JOptionPane.PLAIN_MESSAGE);
        if (cadena == null) return;  // usuario canceló

        // ── Extraer datos ───────────────────────────────────────────────────
        String input         = panel_Codigo.getText();
        String tipoAutomata  = "AFD";
        String estadoInicial = null;
        java.util.Set<String> estadosFinales = new java.util.LinkedHashSet<>();

        for (SimboloDSL s : listaSimbolosGlobal) {
            if (s.tipo == null) continue;
            switch (s.tipo) {
                case "Tipo_Automata_AFN": tipoAutomata = "AFN"; break;
                case "Initial_State":  estadoInicial = s.nombre; break;
                case "Final_State":    estadosFinales.add(s.nombre); break;
            }
        }

        if (estadoInicial == null) {
            JOptionPane.showMessageDialog(this, "No se pudo determinar el estado inicial.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // ── Construir delta ─────────────────────────────────────────────────
        java.util.Map<String, java.util.Map<String, java.util.List<String>>> delta = new java.util.HashMap<>();
        Pattern transPat = Pattern.compile(
            "([A-Za-z0-9_]+)\\s*->\\s*([A-Za-z0-9_]+)\\s*\\[\\s*([^\\]]+)\\s*\\]\\s*;");
        Matcher m = transPat.matcher(input);
        while (m.find()) {
            String origin = m.group(1);
            String dest   = m.group(2);
            for (String rawSym : m.group(3).split(",")) {
                String sym = rawSym.trim().replace("'", "");
                if (sym.equalsIgnoreCase("EPSILON")) sym = "ε";
                delta.computeIfAbsent(origin, k -> new java.util.HashMap<>())
                     .computeIfAbsent(sym,    k -> new java.util.ArrayList<>())
                     .add(dest);
            }
        }

        // ── Simular ─────────────────────────────────────────────────────────
        StringBuilder log = new StringBuilder();
        boolean accepted;
        if ("AFN".equals(tipoAutomata)) {
            accepted = simulateAFN(cadena, estadoInicial, estadosFinales, delta, log);
        } else {
            accepted = simulateAFD(cadena, estadoInicial, estadosFinales, delta, log);
        }

        // ── Mostrar resultado ────────────────────────────────────────────────
        javax.swing.JDialog dialog = new javax.swing.JDialog(this, "Simulación de Cadena — " + tipoAutomata, true);
        dialog.setSize(640, 420);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new java.awt.BorderLayout());

        JPanel headerPnl = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 12, 8));
        headerPnl.setBackground(accepted ? new Color(0, 120, 50) : new Color(180, 0, 0));
        JLabel headerLbl = new JLabel(accepted
            ? "  ✅  Cadena ACEPTADA:  \"" + cadena + "\""
            : "  ❌  Cadena RECHAZADA:  \"" + cadena + "\"");
        headerLbl.setForeground(Color.WHITE);
        headerLbl.setFont(new java.awt.Font("Consolas", java.awt.Font.BOLD, 14));
        headerPnl.add(headerLbl);

        javax.swing.JTextArea stepsArea = new javax.swing.JTextArea(log.toString());
        stepsArea.setFont(new java.awt.Font("Consolas", java.awt.Font.PLAIN, 12));
        stepsArea.setEditable(false);
        stepsArea.setBackground(new Color(248, 248, 250));
        stepsArea.setMargin(new java.awt.Insets(10, 14, 10, 14));

        dialog.add(headerPnl, java.awt.BorderLayout.NORTH);
        dialog.add(new javax.swing.JScrollPane(stepsArea), java.awt.BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    /** Simulación determinista AFD paso a paso. */
    private boolean simulateAFD(String cadena, String estadoInicial,
            java.util.Set<String> estadosFinales,
            java.util.Map<String, java.util.Map<String, java.util.List<String>>> delta,
            StringBuilder log) {
        String current = estadoInicial;
        log.append("Tipo de autómata : AFD\n");
        log.append("Cadena           : \"").append(cadena).append("\"\n");
        log.append("Estado inicial   : ").append(current).append("\n\n");
        log.append(String.format("%-6s %-18s %-6s %-18s%n", "Paso", "Estado actual", "Símbolo", "Estado siguiente"));
        log.append("─────────────────────────────────────────────\n");

        for (int i = 0; i < cadena.length(); i++) {
            String sym = String.valueOf(cadena.charAt(i));
            java.util.List<String> targets = delta
                .getOrDefault(current, Collections.emptyMap()).get(sym);

            if (targets == null || targets.isEmpty()) {
                log.append(String.format("%-6d %-18s %-6s %-18s%n", i + 1, current, "'" + sym + "'", "∅  (trampa)"));
                log.append("\n❌  La cadena fue RECHAZADA — no existe δ(").append(current)
                   .append(", '").append(sym).append("').\n");
                return false;
            }
            String next = targets.get(0);
            log.append(String.format("%-6d %-18s %-6s %-18s%n", i + 1, current, "'" + sym + "'", next));
            current = next;
        }

        log.append("\nEstado final alcanzado: ").append(current).append("\n");
        boolean accepted = estadosFinales.contains(current);
        if (accepted) {
            log.append("✅  '").append(current).append("' es estado final → Cadena ACEPTADA.\n");
        } else {
            log.append("❌  '").append(current).append("' no es estado final → Cadena RECHAZADA.\n");
        }
        return accepted;
    }

    /** Simulación no determinista AFN con cierre-ε. */
    private boolean simulateAFN(String cadena, String estadoInicial,
            java.util.Set<String> estadosFinales,
            java.util.Map<String, java.util.Map<String, java.util.List<String>>> delta,
            StringBuilder log) {
        log.append("Tipo de autómata : AFN (con cierre-ε)\n");
        log.append("Cadena           : \"").append(cadena).append("\"\n\n");

        java.util.Set<String> current = epsilonClosure(
            Collections.singleton(estadoInicial), delta);
        log.append("Paso 0 : ε-cierre({").append(estadoInicial).append("}) = ")
           .append(current).append("\n\n");

        for (int i = 0; i < cadena.length(); i++) {
            String sym = String.valueOf(cadena.charAt(i));
            java.util.Set<String> rawNext = new java.util.LinkedHashSet<>();
            for (String state : current) {
                java.util.List<String> targets =
                    delta.getOrDefault(state, Collections.emptyMap()).get(sym);
                if (targets != null) rawNext.addAll(targets);
            }
            java.util.Set<String> next = epsilonClosure(rawNext, delta);
            log.append("Paso ").append(i + 1)
               .append(" : δ̂(").append(current).append(", '").append(sym).append("')")
               .append(" = ε-cierre(").append(rawNext).append(")")
               .append(" = ").append(next).append("\n");
            current = next;
            if (current.isEmpty()) {
                log.append("\nConjunto de estados vacío → Cadena RECHAZADA.\n");
                return false;
            }
        }

        log.append("\nConjunto final: ").append(current).append("\n");
        boolean accepted = current.stream().anyMatch(estadosFinales::contains);
        if (accepted) {
            java.util.Set<String> inter = new java.util.LinkedHashSet<>(current);
            inter.retainAll(estadosFinales);
            log.append("✅  Contiene estado(s) final(es) ").append(inter).append(" → Cadena ACEPTADA.\n");
        } else {
            log.append("❌  Ningún estado en ").append(current).append(" es final → Cadena RECHAZADA.\n");
        }
        return accepted;
    }

    /** Calcula el cierre-ε de un conjunto de estados. */
    private java.util.Set<String> epsilonClosure(
            java.util.Set<String> states,
            java.util.Map<String, java.util.Map<String, java.util.List<String>>> delta) {
        java.util.Set<String> closure = new java.util.LinkedHashSet<>(states);
        java.util.Deque<String> stack  = new java.util.ArrayDeque<>(states);
        while (!stack.isEmpty()) {
            String st = stack.pop();
            java.util.List<String> eps =
                delta.getOrDefault(st, Collections.emptyMap()).get("ε");
            if (eps != null) {
                for (String t : eps) {
                    if (closure.add(t)) stack.push(t);
                }
            }
        }
        return closure;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new FlatIntelliJLaf());
            } catch (UnsupportedLookAndFeelException ex) {
                System.out.println("LookAndFeel no soportado: " + ex);
            }
            new Compilador().setVisible(true);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_Abrir;
    private javax.swing.JButton btn_Compilar;
    private javax.swing.JButton btn_Ejecutar;
    private javax.swing.JButton btn_Errores;
    private javax.swing.JButton btn_Guardar;
    private javax.swing.JButton btn_GuardarC;
    private javax.swing.JButton btn_Nuevo;
    private javax.swing.JButton btn_Simbolos;
    private javax.swing.JButton btn_VerArbol;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextPane panel_Codigo;
    private javax.swing.JPanel panel_Principal;
    private javax.swing.JTextPane panel_Salida;
    private javax.swing.JPanel panel_botones;
    private javax.swing.JPanel panel_botones_exec_comp;
    private javax.swing.JTable tbl_Token;
    // End of variables declaration//GEN-END:variables
}
