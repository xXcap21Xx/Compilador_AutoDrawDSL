
import com.formdev.flatlaf.FlatIntelliJLaf;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import compilerTools.ASTNode;
import compilerTools.Directory;
import compilerTools.ErrorLSSL;
import compilerTools.Functions;
import compilerTools.TextColor;
import compilerTools.Token;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author MiStErX
 */


public class Compilador extends javax.swing.JFrame {

    /**
     * Offsets entre el número de línea reportado por los tokens del lexer y la
     * línea visible en el editor (gutter), y entre ese número y el índice de
     * elemento del Document de JTextPane.
     *
     * Causa raíz identificada:
     *   JFlex cuenta líneas con yyline (base 0) y el Lexer suma +1 → getLine()
     *   es 1-indexed y coincide directamente con el gutter correcto.
     *   El gutter propio (inicializarNumeradorLineas) muestra elemento N como
     *   línea N+1, por lo que: gutter = getLine() → TOKEN_LINE_OFFSET = 0.
     *
     *   getDefaultRootElement().getElement(N) es 0-indexed; para la línea L
     *   (1-indexed) el elemento es L-1 → DOC_ELEM_OFFSET = 1.
     *
     *   Nota histórica: compilerTools.Functions.setLineNumberOnJTextComponent
     *   tenía un bug de +2 en la numeración (mostraba elemento N como línea N+2,
     *   arrancaba en 3 y omitía la última línea). Los offsets anteriores eran 1 y 2
     *   para compensarlo; al reemplazar el gutter por uno correcto se normalizan a 0 y 1.
     *
     * Reglas de uso:
     *   errorLine  = token.getLine() + TOKEN_LINE_OFFSET   → número para mostrar
     *   elemIdx    = errorLine       - DOC_ELEM_OFFSET      → índice de Document
     */
    private static final int TOKEN_LINE_OFFSET = 0;
    private static final int DOC_ELEM_OFFSET   = 1;

    private String title;
    private Directory Directorio;
    private ArrayList<Token> tokens;
    private ArrayList<ErrorLSSL> errors;
    private ArrayList<TextColor> textsColor;
    private Timer timerKeyReleased;
    private boolean codeHasBeenCompiled = false;
    private java.util.List<SimboloDSL> listaSimbolosGlobal = new java.util.ArrayList<>();
    private javax.swing.JComboBox<String> jComboBoxOperaciones;
    private javax.swing.JButton           btn_AplicarOperacion;

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
        inicializarNumeradorLineas();
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

        // ── Combobox de operaciones sobre autómatas ──────────────────────────
        jComboBoxOperaciones = new javax.swing.JComboBox<>(new String[]{
            "Complemento (¬A)", "Kleene* (A*)", "Kleene+ (A+)",
            "Unión (A∪B)", "Intersección (A∩B)", "Concatenación (A·B)", "Diferencia (A−B)"
        });
        jComboBoxOperaciones.setFont(new java.awt.Font("Consolas", java.awt.Font.PLAIN, 11));
        jComboBoxOperaciones.setPreferredSize(new java.awt.Dimension(210, 24));
        jComboBoxOperaciones.setMaximumSize(new java.awt.Dimension(210, 24));

        btn_AplicarOperacion = new javax.swing.JButton("▶ Aplicar");
        btn_AplicarOperacion.setFont(new java.awt.Font("Consolas", java.awt.Font.BOLD, 11));
        btn_AplicarOperacion.addActionListener(e -> aplicarOperacion());

        menuBar.add(javax.swing.Box.createHorizontalGlue());
        menuBar.add(new javax.swing.JLabel("  Operación: "));
        menuBar.add(jComboBoxOperaciones);
        menuBar.add(javax.swing.Box.createRigidArea(new java.awt.Dimension(6, 0)));
        menuBar.add(btn_AplicarOperacion);
        menuBar.add(javax.swing.Box.createRigidArea(new java.awt.Dimension(8, 0)));

        setJMenuBar(menuBar);
    }

    private void colorAnalysis() {
        textsColor.clear();
        try {
            LexerColor lexerColor = new LexerColor(new java.io.StringReader(panel_Codigo.getText()));
            while (true) {
                TextColor textColor = lexerColor.yylex();
                if (textColor == null) break;
                textsColor.add(textColor);
            }
        } catch (IOException ex) {
            System.err.println("Error en análisis de color: " + ex.getMessage());
        }
        Functions.colorTextPane(textsColor, panel_Codigo, Color.WHITE);
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
            JOptionPane.showMessageDialog(this, "La tabla de símbolos está vacía.\nAsegúrate de compilar código que tenga declaraciones (ej. ESTADOS { q0, q1 }; o TIPO AFD;)", "Tabla Vacía", JOptionPane.WARNING_MESSAGE);
            return;
        }

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
        codeHasBeenCompiled = false;
        panel_Codigo.getHighlighter().removeAllHighlights();
    }

    /** Instala un gutter de numeración de líneas correcto (1-indexed, todas las líneas). */
    private void inicializarNumeradorLineas() {
        javax.swing.JComponent gutter = new javax.swing.JComponent() {
            private static final int PAD = 6;
            {
                setBackground(new java.awt.Color(240, 240, 240));
                setForeground(new java.awt.Color(110, 110, 110));
                setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 13));
                setOpaque(true);
                setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 0, 1,
                        new java.awt.Color(200, 200, 200)));
            }

            @Override
            public java.awt.Dimension getPreferredSize() {
                int n = panel_Codigo.getDocument().getDefaultRootElement().getElementCount();
                java.awt.FontMetrics fm = getFontMetrics(getFont());
                int w = fm.stringWidth(String.valueOf(Math.max(n, 99))) + PAD * 2;
                return new java.awt.Dimension(w, panel_Codigo.getHeight());
            }

            @Override
            protected void paintComponent(java.awt.Graphics g) {
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(getForeground());
                g.setFont(getFont());
                java.awt.FontMetrics fm = g.getFontMetrics();
                javax.swing.text.Element root =
                        panel_Codigo.getDocument().getDefaultRootElement();
                for (int i = 0, n = root.getElementCount(); i < n; i++) {
                    try {
                        java.awt.Rectangle r = panel_Codigo.modelToView(
                                root.getElement(i).getStartOffset());
                        if (r == null) continue;
                        String num = String.valueOf(i + 1);
                        g.drawString(num,
                                getWidth() - fm.stringWidth(num) - PAD,
                                r.y + fm.getAscent());
                    } catch (javax.swing.text.BadLocationException ex) { /* skip */ }
                }
            }
        };

        // Repintar cuando el documento cambia
        panel_Codigo.getDocument().addDocumentListener(
                new javax.swing.event.DocumentListener() {
                    public void insertUpdate (javax.swing.event.DocumentEvent e) { repaint(); }
                    public void removeUpdate (javax.swing.event.DocumentEvent e) { repaint(); }
                    public void changedUpdate(javax.swing.event.DocumentEvent e) { repaint(); }
                    void repaint() { gutter.revalidate(); gutter.repaint(); }
                });

        // Sincronizar repintado al hacer scroll
        jScrollPane1.getViewport().addChangeListener(e -> gutter.repaint());

        jScrollPane1.setRowHeaderView(gutter);
    }



    private void semanticAnalysis(ASTNode root) {
        if (root == null) return;

        Set<String> declaredStates = new HashSet<>();
        Set<String> finalStates    = new HashSet<>();
        Set<String> initialStates  = new HashSet<>();
        boolean hasAutomatonType   = false;
        boolean hasEpsilon         = false;

        for (SimboloDSL sym : listaSimbolosGlobal) {
            if (sym.tipo == null) continue;
            switch (sym.tipo) {
                case "Epsilon_Symbol":
                    hasEpsilon = true;
                    break;
                case "State_Declared":
                case "Final_State":
                case "Initial_State":
                    declaredStates.add(sym.nombre);
                    break;
            }
            if ("Tipo_Automata_AFD".equals(sym.tipo) || "Tipo_Automata_AFN".equals(sym.tipo))
                hasAutomatonType = true;
            if ("Final_State".equals(sym.tipo))   finalStates.add(sym.nombre);
            if ("Initial_State".equals(sym.tipo)) initialStates.add(sym.nombre);
        }

        // Alfabeto extraído solo del nodo AlphabetDefinition del AST
        Set<String> alphabetSymbols = new HashSet<>();
        if (hasEpsilon) alphabetSymbols.add("EPSILON");
        collectAlphabet(root, alphabetSymbols);

        if (!hasAutomatonType)
            errors.add(new ErrorLSSL(1, "[SemError 001] No se declaró el tipo de autómata. | ✏ Agrega al inicio del programa: TIPO AFD;  ó  TIPO AFN;", null));
        if (initialStates.isEmpty())
            errors.add(new ErrorLSSL(1, "[SemError 002] No se declaró el estado inicial. | ✏ Agrega: INICIO q0;  (debe ir después de ESTADOS o ALFABETO)", null));
        if (finalStates.isEmpty())
            errors.add(new ErrorLSSL(1, "[SemError 003] No se declaró ningún estado final. | ✏ Agrega antes de las transiciones: FINAL q2;  ó  FINAL q1, q2;", null));
        if (alphabetSymbols.isEmpty())
            errors.add(new ErrorLSSL(1, "[SemError 004] No se declaró el alfabeto. | ✏ Agrega: ALFABETO { 'a', 'b' };  (después de TIPO o FONDO, antes de ESTADOS/INICIO)", null));

        boolean isAFD = false;
        for (SimboloDSL sym : listaSimbolosGlobal) {
            if ("Tipo_Automata_AFD".equals(sym.tipo)) { isAFD = true; break; }
        }

        int transitionCount = validateTransitions(root, declaredStates, alphabetSymbols, hasEpsilon,
                new HashMap<>(), new HashSet<>());

        if (transitionCount == 0)
            errors.add(new ErrorLSSL(1, "[SemError 009] No se encontró ninguna transición en el programa. | ✏ Agrega al menos una: q0 -> q1 ['a'];", null));

        if (isAFD && transitionCount > 0)
            checkDeterminism(root);

        // SemError 012: AFD con épsilon — incompatible por definición
        if (isAFD && hasEpsilon) {
            Token epsTok = null;
            for (SimboloDSL s : listaSimbolosGlobal) {
                if ("Epsilon_Symbol".equals(s.tipo)) {
                    epsTok = new Token("EPSILON", "EPSILON", s.linea, s.columna);
                    break;
                }
            }
            errors.add(new ErrorLSSL(1,
                "[SemError 012] Un AFD no puede tener transiciones ε. EPSILON solo es válido en AFN. | ✏ Cambia a TIPO AFN;  o elimina EPSILON del alfabeto y sus transiciones ε.",
                epsTok));
        }

        // SemError 013: AFN declarado pero el autómata es determinista → sugerencia
        if (!isAFD && !hasEpsilon && transitionCount > 0 && checkIfActuallyDeterministic(root))
            errors.add(new ErrorLSSL(1,
                "[SemError 013] El autómata declarado como AFN es en realidad determinista (sin ε y sin bifurcaciones). | ✏ Considera cambiarlo a TIPO AFD;",
                null));

        // SemError 011 requiere un AST completo. Si hay errores sintácticos, el modo
        // pánico del parser pudo haber descartado transiciones, haciendo que estados
        // realmente usados aparezcan como "no usados" en el AST. Se omite en ese caso.
        boolean hasSyntaxErrors = errors.stream().anyMatch(
                e -> e.getDescription() != null && e.getDescription().contains("SinError"));
        if (!hasSyntaxErrors) {
            checkUnusedStates(root, initialStates, finalStates);
            if (transitionCount > 0) {
                checkInitialAndFinalInTransitions(root, initialStates, finalStates);
                checkReachabilityFromInitial(root, initialStates, finalStates);
                checkUnreachableFinalStates(root, initialStates, finalStates);
            }
        }
    }

    /** Recorre el AST hasta AlphabetDefinition y recoge sus Symbol hijos. */
    private void collectAlphabet(ASTNode node, Set<String> out) {
        if (node == null) return;
        if ("AlphabetDefinition".equals(node.label)) {
            gatherSymbols(node, out);
            return;
        }
        for (ASTNode child : node.children)
            collectAlphabet(child, out);
    }

    /** Recoge recursivamente todos los nodos Symbol dentro de un subárbol. */
    private void gatherSymbols(ASTNode node, Set<String> out) {
        if (node == null) return;
        if ("Symbol".equals(node.label) && node.value != null)
            out.add(node.value);
        for (ASTNode child : node.children)
            gatherSymbols(child, out);
    }

    /** Recorre el AST validando cada nodo Transition; retorna el total encontrado. */
    private int validateTransitions(ASTNode node, Set<String> declaredStates,
                                    Set<String> alphabetSymbols, boolean hasEpsilon,
                                    HashMap<String, Integer> counter, Set<String> reportedErrors) {
        if (node == null) return 0;
        if ("Transition".equals(node.label)) {
            String origin = null, destination = null;
            Set<String> usedSymbols = new HashSet<>();
            for (ASTNode child : node.children) {
                if ("From".equals(child.label))                   origin      = child.value;
                else if ("To".equals(child.label))                destination = child.value;
                else if ("TransitionSymbols".equals(child.label)) gatherSymbols(child, usedSymbols);
            }
            Token locTok = findTransitionToken(origin, destination, counter);
            if (origin != null && !declaredStates.contains(origin)) {
                String msg = "[SemError 005] El estado origen '" + origin + "' no fue declarado. | ✏ Agrégalo en: ESTADOS { " + origin + ", ... };  (o INICIO " + origin + "; si es el estado inicial)";
                if (reportedErrors.add(msg))
                    errors.add(new ErrorLSSL(1, msg, locTok));
            }
            if (destination != null && !declaredStates.contains(destination)) {
                String msg = "[SemError 006] El estado destino '" + destination + "' no fue declarado. | ✏ Agrégalo en: ESTADOS { " + destination + ", ... };  (antes de las transiciones)";
                if (reportedErrors.add(msg))
                    errors.add(new ErrorLSSL(1, msg, locTok));
            }
            for (String sym : usedSymbols) {
                if ("EPSILON".equals(sym) && !hasEpsilon) {
                    String msg = "[SemError 007] Se usa EPSILON en una transición pero no está en el alfabeto. | ✏ Agrega EPSILON al alfabeto: ALFABETO { ..., EPSILON };  (solo válido en AFN)";
                    if (reportedErrors.add(msg))
                        errors.add(new ErrorLSSL(1, msg, locTok));
                } else if (!"EPSILON".equals(sym) && !alphabetSymbols.contains(sym)) {
                    String msg = "[SemError 008] El símbolo '" + sym + "' se usa en una transición pero no pertenece al alfabeto. | ✏ Agrégalo al alfabeto: ALFABETO { ..., '" + sym + "' };";
                    if (reportedErrors.add(msg))
                        errors.add(new ErrorLSSL(1, msg, locTok));
                }
            }
            return 1;
        }
        int count = 0;
        for (ASTNode child : node.children)
            count += validateTransitions(child, declaredStates, alphabetSymbols, hasEpsilon, counter, reportedErrors);
        return count;
    }

    /** B1: Verifica que el AFD no tenga múltiples transiciones desde el mismo estado con el mismo símbolo. */
    private void checkDeterminism(ASTNode root) {
        HashMap<String, Set<String>> seenSymbols = new HashMap<>();
        HashMap<String, Integer> counter = new HashMap<>();
        checkDeterminismNode(root, seenSymbols, counter);
    }

    private void checkDeterminismNode(ASTNode node, HashMap<String, Set<String>> seenSymbols,
                                      HashMap<String, Integer> counter) {
        if (node == null) return;
        if ("Transition".equals(node.label)) {
            String origin = null, destination = null;
            Set<String> usedSymbols = new HashSet<>();
            for (ASTNode child : node.children) {
                if ("From".equals(child.label))                   origin      = child.value;
                else if ("To".equals(child.label))                destination = child.value;
                else if ("TransitionSymbols".equals(child.label)) gatherSymbols(child, usedSymbols);
            }
            Token locTok = findTransitionToken(origin, destination, counter);
            if (origin != null) {
                Set<String> existing = seenSymbols.computeIfAbsent(origin, k -> new HashSet<>());
                for (String sym : usedSymbols) {
                    if (!existing.add(sym))
                        errors.add(new ErrorLSSL(1,
                            "[SemError 010] AFD no determinista: el estado '" + origin + "' tiene más de una transición con el símbolo '" + sym + "'. | ✏ En un AFD cada par (estado, símbolo) debe tener exactamente un estado destino.",
                            locTok));
                }
            }
            return;
        }
        for (ASTNode child : node.children)
            checkDeterminismNode(child, seenSymbols, counter);
    }

    /** Retorna true si ningún par (estado, símbolo) tiene más de un destino — el autómata es determinista. */
    private boolean checkIfActuallyDeterministic(ASTNode root) {
        HashMap<String, Set<String>> seen = new HashMap<>();
        return isDeterministicNode(root, seen);
    }

    private boolean isDeterministicNode(ASTNode node, HashMap<String, Set<String>> seen) {
        if (node == null) return true;
        if ("Transition".equals(node.label)) {
            String origin = null;
            Set<String> usedSymbols = new HashSet<>();
            for (ASTNode child : node.children) {
                if ("From".equals(child.label))                   origin = child.value;
                else if ("TransitionSymbols".equals(child.label)) gatherSymbols(child, usedSymbols);
            }
            if (origin != null) {
                Set<String> existing = seen.computeIfAbsent(origin, k -> new HashSet<>());
                for (String sym : usedSymbols) {
                    if (!existing.add(sym)) return false;
                }
            }
            return true;
        }
        for (ASTNode child : node.children) {
            if (!isDeterministicNode(child, seen)) return false;
        }
        return true;
    }

    /** B2: Detecta estados declarados con ESTADO que nunca aparecen en ninguna transición. */
    private void checkUnusedStates(ASTNode root, Set<String> initialStates, Set<String> finalStates) {
        Set<String> usedInTransitions = new HashSet<>();
        collectTransitionStates(root, usedInTransitions);
        for (SimboloDSL sym : listaSimbolosGlobal) {
            if (!"State_Declared".equals(sym.tipo)) continue;
            String state = sym.nombre;
            if (!usedInTransitions.contains(state)
                    && !initialStates.contains(state)
                    && !finalStates.contains(state)) {
                Token locTok = new Token(state, "IDENTIFICADOR", sym.linea, sym.columna);
                errors.add(new ErrorLSSL(1,
                    "[SemError 011] El estado '" + state + "' está declarado pero no se usa en ninguna transición. | ✏ ¿Olvidaste agregar transiciones desde o hacia '" + state + "'?",
                    locTok));
            }
        }
    }

    /** SemError 014/015: estado inicial o final que no aparece en ninguna transición. */
    private void checkInitialAndFinalInTransitions(ASTNode root,
            Set<String> initialStates, Set<String> finalStates) {
        Set<String> usedInTransitions = new HashSet<>();
        collectTransitionStates(root, usedInTransitions);

        for (String init : initialStates) {
            if (!usedInTransitions.contains(init)) {
                Token tok = findStateToken(init);
                errors.add(new ErrorLSSL(1,
                    "[SemError 014] El estado inicial '" + init + "' no aparece en ninguna transición. | ✏ Agrega al menos una transición desde o hacia '" + init + "'.",
                    tok));
            }
        }

        for (String fin : finalStates) {
            if (!usedInTransitions.contains(fin)) {
                Token tok = findStateToken(fin);
                errors.add(new ErrorLSSL(1,
                    "[SemError 015] El estado final '" + fin + "' no aparece en ninguna transición. | ✏ Agrega al menos una transición desde o hacia '" + fin + "'.",
                    tok));
            }
        }
    }

    /**
     * SemError 016: BFS desde el estado inicial. Si ningún estado final es alcanzable,
     * el autómata nunca acepta ninguna cadena y el estado inicial es inútil.
     */
    private void checkReachabilityFromInitial(ASTNode root,
            Set<String> initialStates, Set<String> finalStates) {
        if (initialStates.isEmpty() || finalStates.isEmpty()) return;

        // Construir grafo de adyacencia origen → destinos
        Map<String, Set<String>> graph = new HashMap<>();
        buildTransitionGraph(root, graph);

        // BFS desde todos los estados iniciales
        Set<String> reachable = new HashSet<>(initialStates);
        java.util.Queue<String> queue = new java.util.LinkedList<>(initialStates);
        while (!queue.isEmpty()) {
            String state = queue.poll();
            for (String next : graph.getOrDefault(state, Collections.emptySet()))
                if (reachable.add(next)) queue.add(next);
        }

        // Si ningún estado final es alcanzable → error
        boolean anyFinalReachable = finalStates.stream().anyMatch(reachable::contains);
        if (!anyFinalReachable) {
            String initStr  = String.join(", ", initialStates);
            String finalStr = String.join(", ", finalStates);
            Token tok = findStateToken(initialStates.iterator().next());
            errors.add(new ErrorLSSL(1,
                "[SemError 016] El estado inicial '" + initStr + "' no puede alcanzar ningún estado final (" + finalStr + "). El autómata nunca aceptará ninguna cadena. | ✏ Revisa las transiciones: debe existir un camino desde '" + initStr + "' hasta al menos un estado final.",
                tok));
        }
    }

    /**
     * SemError 017: estado final individual que no es alcanzable desde el estado inicial.
     * Se omite si SemError 016 ya disparó (todos los finales son inalcanzables, ya reportado globalmente).
     */
    private void checkUnreachableFinalStates(ASTNode root,
            Set<String> initialStates, Set<String> finalStates) {
        if (initialStates.isEmpty() || finalStates.isEmpty()) return;

        // Si SemError 016 ya reportó que NINGÚN final es alcanzable, no duplicar
        boolean sem016Fired = errors.stream().anyMatch(
            e -> e.getDescription() != null && e.getDescription().contains("SemError 016"));
        if (sem016Fired) return;

        // BFS desde el estado inicial
        Map<String, Set<String>> graph = new HashMap<>();
        buildTransitionGraph(root, graph);

        Set<String> reachable = new HashSet<>(initialStates);
        java.util.Queue<String> queue = new java.util.LinkedList<>(initialStates);
        while (!queue.isEmpty()) {
            String state = queue.poll();
            for (String next : graph.getOrDefault(state, Collections.emptySet()))
                if (reachable.add(next)) queue.add(next);
        }

        // Reportar cada estado final inalcanzable individualmente
        for (String fin : finalStates) {
            if (!reachable.contains(fin)) {
                Token tok = findStateToken(fin);
                errors.add(new ErrorLSSL(1,
                    "[SemError 017] El estado final '" + fin + "' no es alcanzable desde el estado inicial. Nunca podrá ser alcanzado. | ✏ Agrega transiciones que conecten el estado inicial con '" + fin + "', o elimina su declaración FINAL.",
                    tok));
            }
        }
    }

    /** Construye un mapa origen → conjunto de destinos recorriendo los nodos Transition del AST. */
    private void buildTransitionGraph(ASTNode node, Map<String, Set<String>> graph) {
        if (node == null) return;
        if ("Transition".equals(node.label)) {
            String origin = null, destination = null;
            for (ASTNode child : node.children) {
                if ("From".equals(child.label))  origin      = child.value;
                else if ("To".equals(child.label)) destination = child.value;
            }
            if (origin != null && destination != null)
                graph.computeIfAbsent(origin, k -> new HashSet<>()).add(destination);
            return;
        }
        for (ASTNode child : node.children) buildTransitionGraph(child, graph);
    }

    private Token findStateToken(String stateName) {
        for (SimboloDSL s : listaSimbolosGlobal) {
            if (stateName.equals(s.nombre) && (
                    "Initial_State".equals(s.tipo) ||
                    "Final_State".equals(s.tipo)   ||
                    "State_Declared".equals(s.tipo))) {
                return new Token(stateName, "IDENTIFICADOR", s.linea, s.columna);
            }
        }
        return null;
    }

    private void collectTransitionStates(ASTNode node, Set<String> states) {
        if (node == null) return;
        if ("Transition".equals(node.label)) {
            for (ASTNode child : node.children) {
                if (("From".equals(child.label) || "To".equals(child.label)) && child.value != null)
                    states.add(child.value);
            }
            return;
        }
        for (ASTNode child : node.children)
            collectTransitionStates(child, states);
    }

    /**
     * Detecta transiciones donde falta el ';' final y que el modo pánico del parser
     * consumió silenciosamente. Para cada CORCHETE_DER que cierra una transición válida
     * y no va seguido de PUNTO_Y_COMA, reconstruye la transición completa y agrega
     * SinError 019 con el texto real de la línea afectada.
     *
     * Nota: tk.getLine() tiene un desfase de -1 respecto al número de línea visible
     * en el editor; se compensa con +1 al crear el token de error.
     */
    private boolean detectMissingTransitionSemicolons() {
        boolean found = false;
        for (int i = 0; i < tokens.size(); i++) {
            Token tk = tokens.get(i);
            if (!"CORCHETE_DER".equals(tk.getLexicalComp())) continue;

            // Verificar que este ] cierra realmente una transición
            if (!isTransitionClosingBracket(i)) continue;

            // Correcto si va seguido de ;
            boolean followedBySemicolon = (i + 1 < tokens.size())
                    && "PUNTO_Y_COMA".equals(tokens.get(i + 1).getLexicalComp());
            if (followedBySemicolon) continue;

            int errorLine = tk.getLine() + TOKEN_LINE_OFFSET;
            if (hasTransitionSemicolonErrorOnLine(errorLine)) continue;

            String label = reconstructTransitionLabel(i);
            String suggestion = label.isEmpty() ? "q0 -> q1 ['a'];" : label + ";";

            removeTransitionSemicolonCascades(errorLine);
            Token errTok = new Token("]", "CORCHETE_DER", errorLine, tk.getColumn());
            errors.add(new ErrorLSSL(1,
                "[SinError 019] Falta ';' al final de la transición. | ✏ Correcto: " + suggestion,
                errTok));
            found = true;
        }
        return found;
    }

    private boolean hasTransitionSemicolonErrorOnLine(int line) {
        for (ErrorLSSL e : errors) {
            String desc = e.getDescription();
            if (e.getLine() == line && desc != null
                    && desc.contains("SinError 019")) {
                return true;
            }
        }
        return false;
    }

    private void removeTransitionSemicolonCascades(int line) {
        errors.removeIf(e -> {
            String desc = e.getDescription();
            return e.getLine() == line && desc != null
                    && ((desc.contains("Después de ']'")
                         && desc.contains("se esperaba ';'")
                         && desc.contains("transici"))
                        || (desc.contains("<fin de archivo>")
                            && desc.contains("símbolo")));
        });
    }

    private boolean detectMissingConfigSemicolons() {
        boolean found = false;
        for (int i = 0; i < tokens.size(); i++) {
            Token keyword = tokens.get(i);
            String comp = keyword.getLexicalComp();
            if (!"TIPO".equals(comp) && !"FONDO".equals(comp)) continue;

            int line = keyword.getLine() + TOKEN_LINE_OFFSET;
            Token value = null;
            boolean hasSemicolonOnLine = false;
            for (int j = i + 1; j < tokens.size(); j++) {
                Token current = tokens.get(j);
                if (current.getLine() + TOKEN_LINE_OFFSET != line) break;
                if ("PUNTO_Y_COMA".equals(current.getLexicalComp())) {
                    hasSemicolonOnLine = true;
                    break;
                }
                value = current;
            }
            if (hasSemicolonOnLine || value == null || hasConfigSemicolonErrorOnLine(line, comp)) continue;

            String sentence = keyword.getLexeme() + " " + value.getLexeme();
            Token errTok = new Token(keyword.getLexeme(), comp, line, keyword.getColumn());
            errors.add(new ErrorLSSL(1,
                "[SinError 010] Después de " + sentence
                + " se esperaba ';' para cerrar la instrucción. | ✏ Correcto: "
                + sentence + ";",
                errTok));
            found = true;
        }
        return found;
    }

    private boolean hasConfigSemicolonErrorOnLine(int line, String comp) {
        for (ErrorLSSL e : errors) {
            String desc = e.getDescription();
            if (e.getLine() == line && desc != null && desc.contains(comp)
                    && desc.contains("se esperaba ';'")) {
                return true;
            }
        }
        return false;
    }

    private boolean detectMissingStartFinalSemicolons() {
        boolean found = false;
        Set<Integer> reportedLines = new HashSet<>();
        for (ErrorLSSL e : errors) {
            if (e.getLine() > 0) reportedLines.add(e.getLine());
        }

        for (int i = 0; i < tokens.size(); i++) {
            Token keyword = tokens.get(i);
            String comp = keyword.getLexicalComp();
            if (!"INICIO".equals(comp) && !"FINAL".equals(comp)) continue;

            int line = keyword.getLine() + TOKEN_LINE_OFFSET;
            boolean hasSemicolonOnLine = false;
            Token lastTokenOnLine = keyword;

            for (int j = i + 1; j < tokens.size(); j++) {
                Token current = tokens.get(j);
                if (current.getLine() + TOKEN_LINE_OFFSET != line) break;
                lastTokenOnLine = current;
                if ("PUNTO_Y_COMA".equals(current.getLexicalComp())) {
                    hasSemicolonOnLine = true;
                    break;
                }
            }

            if (hasSemicolonOnLine || hasStartFinalSemicolonErrorOnLine(line, comp)) continue;

            String estado = lastTokenOnLine != keyword ? " " + lastTokenOnLine.getLexeme() : "";
            String etiqueta = "INICIO".equals(comp)
                    ? "la declaración del estado inicial"
                    : "la declaración de estados finales";
            String correcto = "INICIO".equals(comp)
                    ? "INICIO" + estado + ";"
                    : "FINAL" + estado + ";";

            Token errTok = new Token(keyword.getLexeme(), comp, line, keyword.getColumn());
            errors.add(new ErrorLSSL(1,
                "[SinError 010] Después de " + keyword.getLexeme() + estado
                + " se esperaba ';' para cerrar " + etiqueta + ". | ✏ Correcto: " + correcto,
                errTok));
            reportedLines.add(line);
            found = true;
        }
        return found;
    }

    private boolean hasStartFinalSemicolonErrorOnLine(int line, String comp) {
        for (ErrorLSSL e : errors) {
            String desc = e.getDescription();
            if (e.getLine() == line && desc != null && desc.contains(comp)
                    && desc.contains("se esperaba ';'")) {
                return true;
            }
        }
        return false;
    }

    private boolean hasSpecificSemicolonSyntaxError() {
        for (ErrorLSSL e : errors) {
            String desc = e.getDescription();
            if (desc == null) continue;
            if (desc.contains("SinError") &&
                    (desc.contains("se esperaba ';'")
                     || desc.contains("Falta ';'"))) {
                return true;
            }
        }
        return false;
    }

    private boolean detectMissingFinalBeforeTransitions() {
        boolean hasFinal = false;
        for (Token token : tokens) {
            if ("FINAL".equals(token.getLexicalComp())) {
                hasFinal = true;
                break;
            }
        }
        if (hasFinal || hasErrorCode("SinError 026")) return false;

        for (int i = 1; i + 1 < tokens.size(); i++) {
            if (!"FLECHA".equals(tokens.get(i).getLexicalComp())) continue;
            if (!isIdentToken(tokens.get(i - 1)) || !isIdentToken(tokens.get(i + 1))) continue;

            Token origin = tokens.get(i - 1);
            errors.add(new ErrorLSSL(1,
                "[SinError 026] Falta la declaración de estados finales. | ✏ Agrega antes de las transiciones: FINAL q2;  ó  FINAL q1, q2;",
                origin));
            return true;
        }
        return false;
    }

    private boolean hasErrorCode(String code) {
        for (ErrorLSSL e : errors) {
            String desc = e.getDescription();
            if (desc != null && desc.contains(code)) return true;
        }
        return false;
    }

    private boolean detectMissingBraceSectionSemicolons() {
        boolean found = false;
        for (int i = 0; i < tokens.size(); i++) {
            Token closeBrace = tokens.get(i);
            if (!"LLAVE_DER".equals(closeBrace.getLexicalComp())) continue;

            String section = findBraceSectionKeyword(i);
            if (!"ALFABETO".equals(section) && !"ESTADOS".equals(section)) continue;

            boolean followedBySemicolon = (i + 1 < tokens.size())
                    && "PUNTO_Y_COMA".equals(tokens.get(i + 1).getLexicalComp());
            if (followedBySemicolon) continue;

            int line = closeBrace.getLine() + TOKEN_LINE_OFFSET;
            if (hasBraceSectionSemicolonErrorOnLine(line, section)) continue;

            String suggestion = "ALFABETO".equals(section)
                    ? "ALFABETO { 'a', 'b' };"
                    : "ESTADOS { q1, q2, ... };";
            Token errTok = new Token("}", "LLAVE_DER", line, closeBrace.getColumn());
            errors.add(new ErrorLSSL(1,
                "[SinError 010] Después de '}' se esperaba ';' para cerrar "
                + section + ". | ✏ Correcto: " + suggestion,
                errTok));
            found = true;
        }
        return found;
    }

    private String findBraceSectionKeyword(int closeBraceIdx) {
        int depth = 0;
        for (int i = closeBraceIdx; i >= 0; i--) {
            String comp = tokens.get(i).getLexicalComp();
            if ("LLAVE_DER".equals(comp)) {
                depth++;
            } else if ("LLAVE_IZQ".equals(comp)) {
                depth--;
                if (depth == 0 && i > 0) {
                    return tokens.get(i - 1).getLexicalComp();
                }
            }
        }
        return "";
    }

    private boolean hasBraceSectionSemicolonErrorOnLine(int line, String section) {
        for (ErrorLSSL e : errors) {
            String desc = e.getDescription();
            if (e.getLine() == line && desc != null && desc.contains(section)
                    && desc.contains("se esperaba ';'")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Detecta transiciones donde falta el corchete de cierre ']'.
     * Busca cada '[' de transición (precedido por Ident FLECHA Ident) y verifica
     * que haya un ']' correspondiente. Si no lo hay, reporta SinError 020.
     * Debe llamarse ANTES de detectMissingTransitionSemicolons() para que las
     * líneas afectadas queden en reportedLines y no se genere también SinError 019.
     */
    private void detectMissingTransitionClosingBracket() {
        Set<Integer> reportedLines = new HashSet<>();
        for (ErrorLSSL e : errors) {
            if (e.getLine() > 0) reportedLines.add(e.getLine());
        }

        for (int i = 3; i < tokens.size(); i++) {
            Token openBracket = tokens.get(i);
            if (!"CORCHETE_IZQ".equals(openBracket.getLexicalComp())) continue;

            // Debe ser una transición: Ident FLECHA Ident [
            if (!isIdentToken(tokens.get(i - 1))
                    || !"FLECHA".equals(tokens.get(i - 2).getLexicalComp())
                    || !isIdentToken(tokens.get(i - 3))) continue;

            // Escanear hacia adelante buscando el ]
            boolean foundClose = false;
            int j = i + 1;
            while (j < tokens.size()) {
                String comp = tokens.get(j).getLexicalComp();
                if ("CORCHETE_DER".equals(comp)) { foundClose = true; break; }
                // Tokens que no pueden estar dentro de [...] → salimos sin ]
                if ("FLECHA".equals(comp) || "PUNTO_Y_COMA".equals(comp)
                        || "LLAVE_IZQ".equals(comp) || "LLAVE_DER".equals(comp)
                        || "TIPO".equals(comp) || "ALFABETO".equals(comp)
                        || "INICIO".equals(comp) || "FINAL".equals(comp)
                        || "ESTADOS".equals(comp) || "FONDO".equals(comp)
                        || "AFD".equals(comp)    || "AFN".equals(comp)) break;
                j++;
            }

            if (foundClose) continue;

            // ] faltante: usar la línea del '[' como referencia de error
            int errorLine = openBracket.getLine() + TOKEN_LINE_OFFSET;
            if (reportedLines.contains(errorLine)) continue;

            // Reconstruir la etiqueta de la transición
            Token origin = tokens.get(i - 3);
            Token dest   = tokens.get(i - 1);
            ArrayList<String> syms = new ArrayList<>();
            for (int k = i + 1; k < j; k++) {
                Token st = tokens.get(k);
                if ("EPSILON".equals(st.getLexicalComp())) {
                    syms.add("EPSILON");
                } else if ("COMILLA_SIMPLE".equals(st.getLexicalComp())
                        && k + 2 < tokens.size()
                        && isIdentToken(tokens.get(k + 1))
                        && "COMILLA_SIMPLE".equals(tokens.get(k + 2).getLexicalComp())) {
                    syms.add("'" + tokens.get(k + 1).getLexeme() + "'");
                    k += 2;
                }
            }
            String symStr = syms.isEmpty() ? "..." : String.join(", ", syms);
            String suggestion = origin.getLexeme() + " -> " + dest.getLexeme()
                                + " [" + symStr + "];";

            Token errTok = new Token("]", "CORCHETE_DER", errorLine, openBracket.getColumn());
            errors.add(new ErrorLSSL(1,
                "[SinError 020] Falta el corchete de cierre ']' en la transición. | ✏ Correcto: " + suggestion,
                errTok));
            reportedLines.add(errorLine);
        }
    }

    /** Verifica que el CORCHETE_DER en la posición dada cierra una transición
     *  (existe un CORCHETE_IZQ precedido del patrón Ident FLECHA Ident). */
    private boolean isTransitionClosingBracket(int closeBracketIdx) {
        for (int i = closeBracketIdx - 1; i >= 0; i--) {
            String lc = tokens.get(i).getLexicalComp();
            if ("CORCHETE_IZQ".equals(lc)) {
                return i >= 3
                    && "FLECHA".equals(tokens.get(i - 2).getLexicalComp())
                    && isIdentToken(tokens.get(i - 1))
                    && isIdentToken(tokens.get(i - 3));
            }
            // Si cruzamos otro ] o ; antes de encontrar [ no es cierre de transición
            if ("CORCHETE_DER".equals(lc) || "PUNTO_Y_COMA".equals(lc)) break;
        }
        return false;
    }

    /** Reconstruye la etiqueta de la transición (ej: "q0 -> q1 ['a', 'b']")
     *  leyendo hacia atrás desde el CORCHETE_DER indicado. */
    private String reconstructTransitionLabel(int closeBracketIdx) {
        int openIdx = -1;
        for (int i = closeBracketIdx - 1; i >= 0; i--) {
            String lc = tokens.get(i).getLexicalComp();
            if ("CORCHETE_IZQ".equals(lc)) { openIdx = i; break; }
            if ("CORCHETE_DER".equals(lc) || "PUNTO_Y_COMA".equals(lc)) break;
        }
        if (openIdx < 3) return "";

        Token dest   = tokens.get(openIdx - 1);
        Token arrow  = tokens.get(openIdx - 2);
        Token origin = tokens.get(openIdx - 3);
        if (!"FLECHA".equals(arrow.getLexicalComp())) return "";
        if (!isIdentToken(dest) || !isIdentToken(origin)) return "";

        // Recolectar los símbolos entre [ y ]
        ArrayList<String> syms = new ArrayList<>();
        for (int i = openIdx + 1; i < closeBracketIdx; i++) {
            Token t = tokens.get(i);
            if ("EPSILON".equals(t.getLexicalComp())) {
                syms.add("EPSILON");
            } else if ("COMILLA_SIMPLE".equals(t.getLexicalComp())
                    && i + 2 < closeBracketIdx
                    && isIdentToken(tokens.get(i + 1))
                    && "COMILLA_SIMPLE".equals(tokens.get(i + 2).getLexicalComp())) {
                syms.add("'" + tokens.get(i + 1).getLexeme() + "'");
                i += 2;
            }
        }

        String symStr = syms.isEmpty() ? "..." : String.join(", ", syms);
        return origin.getLexeme() + " -> " + dest.getLexeme() + " [" + symStr + "]";
    }

    private boolean isIdentToken(Token t) {
        String lc = t.getLexicalComp();
        return "IDENTIFICADOR".equals(lc) || "COLOR".equals(lc);
    }

    /**
     * Busca la N-ésima ocurrencia de origin->destination en listaSimbolosGlobal.
     * El contador asegura que cada nodo Transition del AST recibe su token correcto
     * incluso cuando varias transiciones comparten el mismo par origen-destino (AFN).
     */
    private Token findTransitionToken(String origin, String destination,
                                      HashMap<String, Integer> counter) {
        if (origin == null) return null;
        String key = origin + "->" + (destination != null ? destination : "");
        int idx = counter.getOrDefault(key, 0);
        counter.put(key, idx + 1);
        int found = 0;
        for (SimboloDSL sym : listaSimbolosGlobal) {
            if ("Transition_Used".equals(sym.tipo) && key.equals(sym.nombre)) {
                if (found == idx)
                    return new Token(origin, "IDENTIFICADOR", sym.linea, sym.columna);
                found++;
            }
        }
        // Fallback: buscar solo por estado origen
        for (SimboloDSL sym : listaSimbolosGlobal) {
            if ("Transition_Used".equals(sym.tipo) && sym.nombre.startsWith(origin + "->"))
                return new Token(origin, "IDENTIFICADOR", sym.linea, sym.columna);
        }
        return null;
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

        listaSimbolosGlobal.clear();

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
            ArrayList<java_cup.runtime.Symbol> symbolList = new ArrayList<>();
            Lexer lexerTabla = new Lexer(new java.io.StringReader(input));
            while (true) {
                java_cup.runtime.Symbol symbol = lexerTabla.next_token();
                if (symbol == null || symbol.sym == sym.EOF) {
                    symbolList.add(new java_cup.runtime.Symbol(sym.EOF));
                    break;
                }
                symbolList.add(symbol);

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

                    if (token.getLexicalComp().equals("ERROR_LEXICO")) {
                        errors.add(new ErrorLSSL(1, "[LexError 001] Error Léxico: Carácter '" + token.getLexeme() + "' no reconocido. | ✏ Elimina o reemplaza el carácter; solo se permiten letras, dígitos y los símbolos del DSL.", token));
                    } else if (token.getLexicalComp().equals("ERROR_COLOR")) {
                        errors.add(new ErrorLSSL(1,
                            "[LexError 002] Color '" + token.getLexeme() + "' no es un color válido para FONDO. | ✏ Colores válidos: blanco, negro, rojo, azul, verde, amarillo, naranja, gris, rosa, morado, violeta, cyan, marron",
                            token));
                    } else if (token.getLexicalComp().equals("ERROR_DIGIT_IDENT")) {
                        String lex = token.getLexeme();
                        String letras  = lex.replaceAll("^[0-9]+", "");
                        String digitos = lex.replaceAll("[^0-9].*", "");
                        errors.add(new ErrorLSSL(1,
                            "[LexError 003] El identificador '" + lex + "' no puede comenzar con dígito. | ✏ Los nombres deben comenzar con letra. Sugerencia: '" + letras + digitos + "'",
                            token));
                    } else if (token.getLexicalComp().equals("ERROR_DIGIT")) {
                        errors.add(new ErrorLSSL(1,
                            "[LexError 003] Se encontró el número '" + token.getLexeme() + "'. | ✏ Los nombres de estados y símbolos deben comenzar con letra, no con dígito.",
                            token));
                    }
                }
            }

            // ==========================================
            // PASO 2: ANÁLISIS SINTÁCTICO (replay del mismo escaneo)
            // ==========================================
            java_cup.runtime.Scanner replayScanner = new java_cup.runtime.Scanner() {
                private int pos = 0;
                @Override
                public java_cup.runtime.Symbol next_token() {
                    return pos < symbolList.size()
                            ? symbolList.get(pos++)
                            : new java_cup.runtime.Symbol(sym.EOF);
                }
            };
            Parser parser = new Parser(replayScanner);

            java_cup.runtime.Symbol parseResult = null;
            try {
                parseResult = parser.parse();
            } catch (Exception ex) {
                System.err.println("Error inesperado durante el análisis sintáctico: " + ex.getMessage());
            }

            // Recolectamos los errores sintácticos
            if (parser.errors != null && !parser.errors.isEmpty()) {
                errors.addAll(parser.errors);
            }

            // Post-chequeo: detectar ] faltante primero (más grave),
            // luego ; faltante. El orden importa: el primero "reclama" la línea
            // en reportedLines y el segundo no duplica sobre ella.
            boolean missingConfigSemicolon = detectMissingConfigSemicolons();
            boolean missingBraceSectionSemicolon = detectMissingBraceSectionSemicolons();
            boolean missingStartOrFinalSemicolon = detectMissingStartFinalSemicolons();
            boolean missingFinalBeforeTransitions = detectMissingFinalBeforeTransitions();
            detectMissingTransitionClosingBracket();
            boolean missingTransitionSemicolon = detectMissingTransitionSemicolons();
            if (missingConfigSemicolon || missingBraceSectionSemicolon
                    || missingStartOrFinalSemicolon || missingFinalBeforeTransitions
                    || missingTransitionSemicolon
                    || hasSpecificSemicolonSyntaxError()) {
                errors.removeIf(e -> e.getDescription() != null
                        && e.getDescription().contains("SinError 011"));
            }
            Functions.sortErrorsByLineAndColumn(errors);

            // ========================================================
            // 🌟 ESTA ES LA PARTE QUE LLENA LA TABLA DE SÍMBOLOS
            // ========================================================
            if (parser.symbols != null && !parser.symbols.isEmpty()) {
                listaSimbolosGlobal.addAll(parser.symbols);
            }

            // ========================================================
            // 🌟 VALIDACIÓN SEMÁNTICA BÁSICA
            // ========================================================
            // E6: run semantic analysis unless there is an unrecoverable syntax error
            // (SinError 011 means the AST is too incomplete to trust)
            boolean hasUnrecoveredError = parser.errors != null && parser.errors.stream()
                    .anyMatch(e -> e.getDescription() != null && e.getDescription().contains("SinError 011"));
            if (!hasUnrecoveredError) {
                ASTNode astRoot = (parseResult != null && parseResult.value instanceof ASTNode)
                        ? (ASTNode) parseResult.value : null;
                semanticAnalysis(astRoot);
            }

        } catch (Exception ex) {
            System.err.println("Error general en la compilación: " + ex.getMessage());
        }

        // ==========================================
        // PASO 3: MOSTRAR RESULTADOS EN LA CONSOLA
        // ==========================================
        if (errors.isEmpty()) {
            // Si no hay errores, mensaje de éxito
            panel_Salida.setForeground(new Color(0, 150, 0)); // Verde
            panel_Salida.setText("✅ Compilación exitosa.\nNo se encontraron errores léxicos, sintácticos ni semánticos.");
        } else {
            panel_Salida.setForeground(Color.RED);
            StringBuilder consola = new StringBuilder();

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

                String fragmento = "";
                int elemIdx = numLinea - DOC_ELEM_OFFSET;
                if (elemIdx >= 0) {
                    try {
                        javax.swing.text.Element docRoot = panel_Codigo.getDocument().getDefaultRootElement();
                        if (elemIdx < docRoot.getElementCount()) {
                            javax.swing.text.Element el = docRoot.getElement(elemIdx);
                            fragmento = panel_Codigo.getDocument()
                                .getText(el.getStartOffset(), el.getEndOffset() - el.getStartOffset())
                                .trim();
                        }
                    } catch (javax.swing.text.BadLocationException ex) {
                        // ignorar
                    }
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
        if (!codeHasBeenCompiled) return;
        if (!errors.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No se puede ejecutar: el código tiene errores. Corrígelos primero.",
                "Errores detectados", JOptionPane.ERROR_MESSAGE);
            return;
        }
        mostrarSimulacionCadena();
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

            ArrayList<ErrorLSSL> treeErrors = new ArrayList<>(parser.errors);

            if (!treeErrors.isEmpty()) {
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

        javax.swing.text.Element root = panel_Codigo.getDocument().getDefaultRootElement();
        int totalLines = root.getElementCount();
        javax.swing.text.Highlighter.HighlightPainter painter =
            new javax.swing.text.DefaultHighlighter.DefaultHighlightPainter(new Color(255, 200, 200));

        for (ErrorLSSL error : errors) {
            int lineNum = error.getLine();
            int elemIdx = lineNum - DOC_ELEM_OFFSET;
            if (elemIdx >= 0 && elemIdx < totalLines) {
                try {
                    javax.swing.text.Element lineElem = root.getElement(elemIdx);
                    int start = lineElem.getStartOffset();
                    int end   = lineElem.getEndOffset() - 1;
                    if (end < start) end = start;
                    hl.addHighlight(start, end, painter);
                } catch (javax.swing.text.BadLocationException ex) {
                    // ignorar — la línea puede haber cambiado
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // OPERACIONES SOBRE AUTÓMATAS
    // ══════════════════════════════════════════════════════════════════════

    private void aplicarOperacion() {
        if (!codeHasBeenCompiled) {
            JOptionPane.showMessageDialog(this, "Primero debes compilar el código.", "Sin compilar", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!errors.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El código tiene errores. Corrígelos antes de aplicar operaciones.", "Errores detectados", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String op       = (String) jComboBoxOperaciones.getSelectedItem();
        boolean binary  = op.contains("∪") || op.contains("∩") || op.contains("·") || op.contains("−");

        // Autómata A = compilación actual
        Automata automatonA = Automata.fromCompiled(listaSimbolosGlobal, panel_Codigo.getText());

        // Autómata B = segundo archivo (solo para operaciones binarias)
        Automata automatonB = null;
        if (binary) {
            javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
            fc.setDialogTitle("Cargar segundo autómata (B) — archivo .draw");
            fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Autómatas DSL (*.draw)", "draw"));
            if (fc.showOpenDialog(this) != javax.swing.JFileChooser.APPROVE_OPTION) return;
            try {
                String content = new String(
                    java.nio.file.Files.readAllBytes(fc.getSelectedFile().toPath()),
                    java.nio.charset.StandardCharsets.UTF_8);
                automatonB = Automata.fromText(content);
                if (automatonB.initialState == null) {
                    JOptionPane.showMessageDialog(this,
                        "El archivo seleccionado no tiene un INICIO declarado.\nVerifica que sea un autómata válido.",
                        "Archivo inválido", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "Error al leer el archivo:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        Automata result;
        try {
            switch (op) {
                case "Complemento (¬A)":    result = Automata.complement(automatonA);                break;
                case "Kleene* (A*)":         result = Automata.kleeneStar(automatonA);               break;
                case "Kleene+ (A+)":         result = Automata.kleenePlus(automatonA);               break;
                case "Unión (A∪B)":         result = Automata.union(automatonA, automatonB);         break;
                case "Intersección (A∩B)":  result = Automata.intersection(automatonA, automatonB);  break;
                case "Concatenación (A·B)":  result = Automata.concatenation(automatonA, automatonB); break;
                case "Diferencia (A−B)":    result = Automata.difference(automatonA, automatonB);    break;
                default: return;
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Error al aplicar la operación:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        new VentanaOperacion(this, op, result).setVisible(true);
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

        // ── Recopilar orden de estados para el diagrama ─────────────────────
        java.util.List<String> stateOrder = new java.util.ArrayList<>();
        for (SimboloDSL s : listaSimbolosGlobal) {
            if (s.tipo == null) continue;
            switch (s.tipo) {
                case "Initial_State":
                    if (!stateOrder.contains(s.nombre)) stateOrder.add(0, s.nombre); break;
                case "State_Declared": case "Final_State":
                    if (!stateOrder.contains(s.nombre)) stateOrder.add(s.nombre);    break;
            }
        }

        // ── Simular ─────────────────────────────────────────────────────────
        StringBuilder         log          = new StringBuilder();
        java.util.Set<String> visitedOut   = new java.util.LinkedHashSet<>();
        String[]              lastStateOut = {null};
        boolean accepted;
        if ("AFN".equals(tipoAutomata)) {
            accepted = simulateAFN(cadena, estadoInicial, estadosFinales, delta, log, visitedOut, lastStateOut);
        } else {
            accepted = simulateAFD(cadena, estadoInicial, estadosFinales, delta, log, visitedOut, lastStateOut);
        }

        // ── Construir diálogo combinado (diagrama + traza) ───────────────────
        javax.swing.JDialog dialog = new javax.swing.JDialog(this,
            "Ejecutar — " + tipoAutomata + "  |  \"" + cadena + "\"", true);
        dialog.setSize(1080, 600);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new java.awt.BorderLayout(0, 0));

        // Cabecera aceptado/rechazado
        JPanel headerPnl = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 14, 8));
        headerPnl.setBackground(accepted ? new Color(0, 110, 45) : new Color(170, 0, 0));
        JLabel headerLbl = new JLabel(accepted
            ? "  ✅  Cadena ACEPTADA:  \"" + cadena + "\""
            : "  ❌  Cadena RECHAZADA:  \"" + cadena + "\"");
        headerLbl.setForeground(Color.WHITE);
        headerLbl.setFont(new java.awt.Font("Consolas", java.awt.Font.BOLD, 14));
        headerPnl.add(headerLbl);

        // Panel izquierdo: diagrama del autómata
        PanelDiagramaAutomata diagramPanel = new PanelDiagramaAutomata(
            tipoAutomata, stateOrder, estadoInicial, estadosFinales, delta,
            visitedOut, lastStateOut[0], accepted);
        diagramPanel.setPreferredSize(new java.awt.Dimension(560, 500));
        javax.swing.JScrollPane diagramScroll = new javax.swing.JScrollPane(diagramPanel);
        diagramScroll.setBorder(javax.swing.BorderFactory.createTitledBorder(
            javax.swing.BorderFactory.createLineBorder(new Color(180, 180, 200)),
            "Diagrama del Autómata"));

        // Panel derecho: traza de simulación
        javax.swing.JTextArea stepsArea = new javax.swing.JTextArea(log.toString());
        stepsArea.setFont(new java.awt.Font("Consolas", java.awt.Font.PLAIN, 12));
        stepsArea.setEditable(false);
        stepsArea.setBackground(new Color(248, 248, 252));
        stepsArea.setMargin(new java.awt.Insets(10, 14, 10, 14));
        javax.swing.JScrollPane traceScroll = new javax.swing.JScrollPane(stepsArea);
        traceScroll.setBorder(javax.swing.BorderFactory.createTitledBorder(
            javax.swing.BorderFactory.createLineBorder(new Color(180, 180, 200)),
            "Traza de Simulación"));
        traceScroll.setPreferredSize(new java.awt.Dimension(460, 500));

        javax.swing.JSplitPane split = new javax.swing.JSplitPane(
            javax.swing.JSplitPane.HORIZONTAL_SPLIT, diagramScroll, traceScroll);
        split.setDividerLocation(560);
        split.setDividerSize(5);
        split.setResizeWeight(0.6);

        // Botón cerrar
        javax.swing.JButton btnClose = new javax.swing.JButton("Cerrar");
        btnClose.setFont(new java.awt.Font("Consolas", java.awt.Font.PLAIN, 12));
        btnClose.addActionListener(e -> dialog.dispose());
        JPanel southPnl = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 12, 6));
        southPnl.add(btnClose);

        dialog.add(headerPnl, java.awt.BorderLayout.NORTH);
        dialog.add(split,     java.awt.BorderLayout.CENTER);
        dialog.add(southPnl,  java.awt.BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    /** Simulación determinista AFD paso a paso. */
    private boolean simulateAFD(String cadena, String estadoInicial,
            java.util.Set<String> estadosFinales,
            java.util.Map<String, java.util.Map<String, java.util.List<String>>> delta,
            StringBuilder log,
            java.util.Set<String> visitedOut,
            String[] lastStateOut) {
        String current = estadoInicial;
        if (visitedOut != null) visitedOut.add(current);

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
                if (lastStateOut != null) lastStateOut[0] = current;
                return false;
            }
            String next = targets.get(0);
            log.append(String.format("%-6d %-18s %-6s %-18s%n", i + 1, current, "'" + sym + "'", next));
            current = next;
            if (visitedOut != null) visitedOut.add(current);
        }

        if (lastStateOut != null) lastStateOut[0] = current;
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
            StringBuilder log,
            java.util.Set<String> visitedOut,
            String[] lastStateOut) {
        log.append("Tipo de autómata : AFN (con cierre-ε)\n");
        log.append("Cadena           : \"").append(cadena).append("\"\n\n");

        java.util.Set<String> current = epsilonClosure(
            Collections.singleton(estadoInicial), delta);
        if (visitedOut != null) visitedOut.addAll(current);
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
            if (visitedOut != null) visitedOut.addAll(current);
            if (current.isEmpty()) {
                log.append("\nConjunto de estados vacío → Cadena RECHAZADA.\n");
                if (lastStateOut != null) lastStateOut[0] = null;
                return false;
            }
        }

        log.append("\nConjunto final: ").append(current).append("\n");
        boolean accepted = current.stream().anyMatch(estadosFinales::contains);
        if (accepted) {
            java.util.Set<String> inter = new java.util.LinkedHashSet<>(current);
            inter.retainAll(estadosFinales);
            log.append("✅  Contiene estado(s) final(es) ").append(inter).append(" → Cadena ACEPTADA.\n");
            if (lastStateOut != null) lastStateOut[0] = inter.iterator().next();
        } else {
            log.append("❌  Ningún estado en ").append(current).append(" es final → Cadena RECHAZADA.\n");
            if (lastStateOut != null) lastStateOut[0] = current.isEmpty() ? null : current.iterator().next();
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
                System.err.println("LookAndFeel no soportado: " + ex);
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
