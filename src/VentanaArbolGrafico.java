import compilerTools.ASTNode;
import java.awt.*;
import javax.swing.*;

/**
 * Ventana (JFrame) que muestra el árbol de derivación AST
 * de forma gráfica usando PanelArbolGrafico.
 */
public class VentanaArbolGrafico extends JFrame {

    public VentanaArbolGrafico(ASTNode root) {
        super("Árbol de Derivación — AutoDrawDSL");
        setSize(950, 620);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // ── Barra de cabecera ──────────────────────────────────────────────
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        header.setBackground(new Color(41, 98, 155));
        JLabel lblTitle = new JLabel("  🌳  Árbol de Derivación Sintáctica");
        lblTitle.setFont(new Font("Consolas", Font.BOLD, 14));
        lblTitle.setForeground(Color.WHITE);
        header.add(lblTitle);

        // ── Panel gráfico del árbol ────────────────────────────────────────
        PanelArbolGrafico panelArbol = new PanelArbolGrafico(root);
        JScrollPane scroll = new JScrollPane(panelArbol);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // ── Panel de leyenda ───────────────────────────────────────────────
        JPanel legend = buildLegend();

        add(header, BorderLayout.NORTH);
        add(scroll,  BorderLayout.CENTER);
        add(legend,  BorderLayout.SOUTH);
    }

    // ── Leyenda de colores ─────────────────────────────────────────────────
    private JPanel buildLegend() {
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        legend.setBackground(new Color(245, 245, 248));
        legend.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(200, 200, 210)));

        JLabel lblLeyenda = new JLabel("Leyenda: ");
        lblLeyenda.setFont(new Font("Consolas", Font.BOLD, 11));
        legend.add(lblLeyenda);

        legend.add(chip(new Color(41,  98, 155), "Program"));
        legend.add(chip(new Color(30, 120, 100), "Section"));
        legend.add(chip(new Color(46, 139,  87), "Config / Alphabet / Background"));
        legend.add(chip(new Color(180, 120,   0), "State"));
        legend.add(chip(new Color(120,  30, 160), "Transition"));
        legend.add(chip(new Color(200,  80,   0), "Symbol"));
        legend.add(chip(new Color(190,   0,   0), "Error"));
        legend.add(chip(new Color( 80,  80,  80), "Otros"));

        return legend;
    }

    /** Crea un chip coloreado con etiqueta de texto para la leyenda. */
    private JPanel chip(Color color, String text) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        p.setBackground(new Color(245, 245, 248));

        JPanel square = new JPanel();
        square.setPreferredSize(new Dimension(13, 13));
        square.setBackground(color);
        square.setBorder(BorderFactory.createLineBorder(color.darker(), 1));

        JLabel label = new JLabel(text);
        label.setFont(new Font("Consolas", Font.PLAIN, 11));

        p.add(square);
        p.add(label);
        return p;
    }
}
