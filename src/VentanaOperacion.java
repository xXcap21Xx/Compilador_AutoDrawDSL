import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 * Diálogo que muestra el resultado de una operación sobre autómatas:
 * diagrama gráfico + tabla de transiciones + botón de simulación.
 */
public class VentanaOperacion extends JDialog {

    private final Automata automata;

    public VentanaOperacion(JFrame parent, String operationName, Automata result) {
        super(parent, "Resultado — " + operationName, true);
        this.automata = result;
        setSize(1150, 660);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(0, 0));

        // ── Cabecera ─────────────────────────────────────────────────────────
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 8));
        header.setBackground(new Color(41, 98, 155));
        JLabel lbl = new JLabel("  Operación: " + operationName
            + "     |     Tipo: " + result.tipo
            + "     |     Estados: " + result.states.size()
            + "     |     Finales: " + result.finalStates.size());
        lbl.setForeground(Color.WHITE);
        lbl.setFont(new Font("Consolas", Font.BOLD, 13));
        header.add(lbl);
        add(header, BorderLayout.NORTH);

        // ── Diagrama (izquierda) ──────────────────────────────────────────────
        PanelDiagramaAutomata diagram = new PanelDiagramaAutomata(
            result.tipo, result.states, result.initialState, result.finalStates,
            result.toDeltaList(), Collections.emptySet(), null, null);
        diagram.setPreferredSize(new Dimension(580, 540));
        JScrollPane diagramScroll = new JScrollPane(diagram);
        diagramScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(180, 180, 200)), "Diagrama del Autómata Resultante"));

        // ── Tabla de transiciones (derecha) ───────────────────────────────────
        JScrollPane tableScroll = buildTransitionTable(result);
        tableScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(180, 180, 200)), "Función de Transición δ"));
        tableScroll.setPreferredSize(new Dimension(510, 540));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, diagramScroll, tableScroll);
        split.setDividerLocation(580);
        split.setDividerSize(5);
        split.setResizeWeight(0.55);
        add(split, BorderLayout.CENTER);

        // ── Leyenda + botones ─────────────────────────────────────────────────
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        legend.setBackground(new Color(250, 250, 250));
        legend.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(200, 200, 200)));
        legend.add(chip(new Color(255, 255, 190), "→  Estado inicial"));
        legend.add(chip(new Color(200, 220, 255), "*  Estado final"));
        legend.add(chip(new Color(200, 240, 200), "→*  Inicial y final"));

        JButton btnSim = new JButton("▶ Simular cadena");
        btnSim.setFont(new Font("Consolas", Font.BOLD, 12));
        btnSim.addActionListener(e -> simularCadena());

        JButton btnClose = new JButton("Cerrar");
        btnClose.setFont(new Font("Consolas", Font.PLAIN, 12));
        btnClose.addActionListener(e -> dispose());

        JPanel south = new JPanel(new BorderLayout());
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        btnPanel.add(btnSim);
        btnPanel.add(btnClose);
        south.add(legend,   BorderLayout.WEST);
        south.add(btnPanel, BorderLayout.EAST);
        add(south, BorderLayout.SOUTH);
    }

    // ── Tabla de transiciones ─────────────────────────────────────────────────

    private JScrollPane buildTransitionTable(Automata a) {
        List<String> cols = new ArrayList<>();
        cols.add("Estado");
        for (String sym : a.alphabet) cols.add(sym);

        DefaultTableModel model = new DefaultTableModel(cols.toArray(), 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        for (String state : a.states) {
            Object[] row = new Object[cols.size()];
            boolean init = state.equals(a.initialState);
            boolean fin  = a.finalStates.contains(state);
            row[0] = (init && fin) ? "→* " + state : init ? "→ " + state : fin ? "*  " + state : "    " + state;
            for (int i = 1; i < cols.size(); i++) {
                Set<String> tgt = a.delta.getOrDefault(state, Collections.emptyMap()).get(cols.get(i));
                if (tgt == null || tgt.isEmpty()) row[i] = "—";
                else if (tgt.size() == 1)          row[i] = tgt.iterator().next();
                else                               row[i] = "{" + String.join(", ", tgt) + "}";
            }
            model.addRow(row);
        }

        JTable table = new JTable(model);
        table.setRowHeight(26);
        table.setFont(new Font("Consolas", Font.PLAIN, 11));
        table.getTableHeader().setFont(new Font("Consolas", Font.BOLD, 11));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                if (!sel) {
                    String sv = (String) t.getValueAt(row, 0);
                    if (sv != null && sv.contains("→") && sv.contains("*")) setBackground(new Color(200, 240, 200));
                    else if (sv != null && sv.contains("→"))                setBackground(new Color(255, 255, 190));
                    else if (sv != null && sv.contains("*"))                setBackground(new Color(200, 220, 255));
                    else setBackground(Color.WHITE);
                    setForeground(Color.BLACK);
                    setHorizontalAlignment(col == 0 ? LEFT : CENTER);
                }
                return this;
            }
        });
        return new JScrollPane(table);
    }

    // ── Simulación ────────────────────────────────────────────────────────────

    private void simularCadena() {
        String cadena = JOptionPane.showInputDialog(this,
            "Ingresa la cadena a simular:\n(deja vacío para la cadena ε vacía)",
            "Simular cadena en autómata resultante", JOptionPane.PLAIN_MESSAGE);
        if (cadena == null) return;

        StringBuilder   log      = new StringBuilder();
        Set<String>     visited  = new LinkedHashSet<>();
        String[]        lastSt   = {null};
        boolean         accepted = automata.simulate(cadena, log, visited, lastSt);

        // Diálogo de resultado de simulación
        JDialog sim = new JDialog(this,
            (accepted ? "✅ ACEPTADA" : "❌ RECHAZADA") + "  —  \"" + cadena + "\"", true);
        sim.setSize(1080, 600);
        sim.setLocationRelativeTo(this);
        sim.setLayout(new BorderLayout(0, 0));

        JPanel h = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 8));
        h.setBackground(accepted ? new Color(0, 110, 45) : new Color(170, 0, 0));
        JLabel hl = new JLabel(accepted
            ? "  ✅  Cadena ACEPTADA:  \"" + cadena + "\""
            : "  ❌  Cadena RECHAZADA:  \"" + cadena + "\"");
        hl.setForeground(Color.WHITE);
        hl.setFont(new Font("Consolas", Font.BOLD, 14));
        h.add(hl);

        PanelDiagramaAutomata dp = new PanelDiagramaAutomata(
            automata.tipo, automata.states, automata.initialState, automata.finalStates,
            automata.toDeltaList(), visited, lastSt[0], accepted);
        dp.setPreferredSize(new Dimension(560, 500));
        JScrollPane ds = new JScrollPane(dp);
        ds.setBorder(BorderFactory.createTitledBorder("Diagrama del Autómata"));

        JTextArea ta = new JTextArea(log.toString());
        ta.setFont(new Font("Consolas", Font.PLAIN, 12));
        ta.setEditable(false);
        ta.setBackground(new Color(248, 248, 252));
        ta.setMargin(new Insets(10, 14, 10, 14));
        JScrollPane ts = new JScrollPane(ta);
        ts.setBorder(BorderFactory.createTitledBorder("Traza de Simulación"));
        ts.setPreferredSize(new Dimension(460, 500));

        JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, ds, ts);
        sp.setDividerLocation(560); sp.setDividerSize(5); sp.setResizeWeight(0.6);

        JButton close = new JButton("Cerrar");
        close.setFont(new Font("Consolas", Font.PLAIN, 12));
        close.addActionListener(e -> sim.dispose());
        JPanel sp2 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 6));
        sp2.add(close);

        sim.add(h,   BorderLayout.NORTH);
        sim.add(sp,  BorderLayout.CENTER);
        sim.add(sp2, BorderLayout.SOUTH);
        sim.setVisible(true);
    }

    // ── Chip de leyenda ───────────────────────────────────────────────────────

    private JPanel chip(Color color, String text) {
        JPanel c = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        c.setBackground(new Color(250, 250, 250));
        JPanel sq = new JPanel();
        sq.setPreferredSize(new Dimension(13, 13));
        sq.setBackground(color);
        sq.setBorder(BorderFactory.createLineBorder(color.darker(), 1));
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Consolas", Font.PLAIN, 11));
        c.add(sq); c.add(lbl);
        return c;
    }
}
