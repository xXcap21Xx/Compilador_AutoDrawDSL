
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

        // Crear la ventana (JDialog)
        javax.swing.JDialog ventanaErrores = new javax.swing.JDialog(this, "Tabla de Errores Detectados", true);
        ventanaErrores.setSize(850, 400);
        ventanaErrores.setLocationRelativeTo(this);

        // Columnas simplificadas y seguras
        String[] columnas = {"Código", "Línea", "Columna", "Descripción Detallada"};
        DefaultTableModel modelo = new DefaultTableModel(columnas, 0);
        javax.swing.JTable tabla = new javax.swing.JTable(modelo);

        // Llenar la tabla usando expresiones simples en lugar de getters bloqueados
        for (ErrorLSSL error : errors) {
            String textoCompleto = error.toString();
            String codigoStr = "Desconocido";

            // Detectamos el código que inyectamos en los pasos anteriores
            if (textoCompleto.contains("[LexError 001]")) {
                codigoStr = "LexError 001";
            } else if (textoCompleto.contains("[SinError 010]")) {
                codigoStr = "SinError 010";
            } else if (textoCompleto.contains("[SinError 011]")) {
                codigoStr = "SinError 011";
            } else if (textoCompleto.contains("[SinError 012]")) {
                codigoStr = "SinError 012";
            } else if (textoCompleto.contains("[SinError 013]")) {
                codigoStr = "SinError 013";
            } else if (textoCompleto.contains("[SinError 014]")) {
                codigoStr = "SinError 014";
            }

            // Agregamos la fila
            Object[] fila = {
                codigoStr,
                error.getLine(),
                error.getColumn(),
                textoCompleto // Mostramos el mensaje completo para mayor contexto
            };
            modelo.addRow(fila);
        }

        // Ajustes estéticos de la tabla
        tabla.setRowHeight(30);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(100); // Código
        tabla.getColumnModel().getColumn(1).setPreferredWidth(50);  // Línea
        tabla.getColumnModel().getColumn(2).setPreferredWidth(50);  // Columna
        tabla.getColumnModel().getColumn(3).setPreferredWidth(550); // Descripción

        javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(tabla);
        ventanaErrores.add(scroll);
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
                case "Símbolo Alfabeto":
                    alphabetSymbols.add(symbol.nombre.replace("'", ""));
                    break;
                case "Símbolo Épsilon":
                    hasEpsilon = true;
                    alphabetSymbols.add("EPSILON");
                    break;
                case "Estado Declarado":
                case "Estado Final":
                case "Estado Inicial":
                    declaredStates.add(symbol.nombre);
                    break;
            }
            if (symbol.tipo.equals("Tipo de Autómata")) {
                hasAutomatonType = true;
            }
            if (symbol.tipo.equals("Estado Final")) {
                finalStates.add(symbol.nombre);
            }
            if (symbol.tipo.equals("Estado Inicial")) {
                initialStates.add(symbol.nombre);
            }
        }

        if (!hasAutomatonType) {
            errors.add(new ErrorLSSL(1, "[SemError 001] Falta la declaración del tipo de autómata: TIPO AFD; o TIPO AFN;", null));
        }
        if (initialStates.isEmpty()) {
            errors.add(new ErrorLSSL(1, "[SemError 002] Falta declarar el estado inicial con INICIO <estado>;", null));
        }
        if (finalStates.isEmpty()) {
            errors.add(new ErrorLSSL(1, "[SemError 003] Falta declarar al menos un estado final con FINAL <estado>;", null));
        }
        if (alphabetSymbols.isEmpty()) {
            errors.add(new ErrorLSSL(1, "[SemError 004] Falta declarar el alfabeto con ALFABETO { 'a', 'b' };", null));
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
                errors.add(new ErrorLSSL(1, "[SemError 005] Origen de transición no declarado: " + origin, null));
            }
            if (!declaredStates.contains(destination)) {
                errors.add(new ErrorLSSL(1, "[SemError 006] Destino de transición no declarado: " + destination, null));
            }

            String[] parts = symbolText.split(",");
            for (String part : parts) {
                String tokenValue = part.trim();
                if (tokenValue.startsWith("'") && tokenValue.endsWith("'")) {
                    tokenValue = tokenValue.substring(1, tokenValue.length() - 1);
                }
                if (tokenValue.equals("EPSILON") && !hasEpsilon) {
                    errors.add(new ErrorLSSL(1, "[SemError 007] Se usa EPSILON en una transición, pero no se declaró en el alfabeto.", null));
                } else if (!tokenValue.equals("EPSILON") && !alphabetSymbols.contains(tokenValue)) {
                    errors.add(new ErrorLSSL(1, "[SemError 008] Símbolo de transición no pertenece al alfabeto: " + tokenValue, null));
                }
            }
        }

        if (transitionCount == 0) {
            errors.add(new ErrorLSSL(1, "[SemError 009] No se encontró ninguna transición válida. Un autómata debe contener al menos una transición.", null));
        }
    }

    private void printAST(ASTNode node, String indent) {
        if (node == null) {
            return;
        }
        System.out.println(indent + node.label);
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

        // 🌟 NUEVO: Limpiamos la tabla de símbolos de compilaciones anteriores
        if (listaSimbolos != null) {
            listaSimbolos.clear();
        } else {
            listaSimbolos = new java.util.ArrayList<>();
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
            // Si hay errores, los mostramos en rojo con un formato claro
            panel_Salida.setForeground(Color.RED);
            StringBuilder consola = new StringBuilder();

            // 🌟 1. Extraemos TODO el texto directamente de tu editor visual
            String codigoCompleto = panel_Codigo.getText();
            // 🌟 2. Lo dividimos en un arreglo (cada elemento es una línea)
            String[] lineasDeCodigo = codigoCompleto.split("\\r?\\n");

            consola.append("❌ Se encontraron ").append(errors.size()).append(" error(es):\n\n");

            for (ErrorLSSL error : errors) {
                int numLinea = error.getLine();
                String fragmentoCodigo = "<Línea vacía o no encontrada>";

                // Extraemos la línea de código exacta (los arreglos empiezan en 0, las líneas en 1)
                if (numLinea > 0 && numLinea <= lineasDeCodigo.length) {
                    fragmentoCodigo = lineasDeCodigo[numLinea - 1].trim();
                }

                // 🌟 3. Damos un formato estructurado y fácil de leer
                consola.append("► Error en la Línea ").append(numLinea).append(":\n");
                consola.append("   Detalle : ").append(error.toString()).append("\n");
                consola.append("   Código  : ").append(fragmentoCodigo).append("\n");
                consola.append("--------------------------------------------------------\n");
            }
            panel_Salida.setText(consola.toString());
        }

        codeHasBeenCompiled = true;
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
            StringBuilder sb = new StringBuilder();
            getASTAsString(root, "", sb);

            VentanaArbol ventana = new VentanaArbol(sb.toString());
            ventana.setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al generar el árbol: " + ex.getMessage());
        }
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
