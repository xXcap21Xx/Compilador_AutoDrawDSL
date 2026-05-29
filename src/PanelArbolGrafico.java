import compilerTools.ASTNode;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import javax.swing.JPanel;

/**
 * Panel que dibuja gráficamente el Árbol de Derivación (AST).
 * Usa un algoritmo simple: hojas colocadas izquierda→derecha,
 * nodos internos centrados sobre sus hijos.
 */
public class PanelArbolGrafico extends JPanel {

    private static final int NODE_W  = 155;
    private static final int NODE_H  = 32;
    private static final int H_GAP   = 16;   // espacio horizontal entre hermanos
    private static final int V_GAP   = 58;   // espacio vertical entre niveles
    private static final int MARGIN  = 30;

    private final ASTNode root;
    private final HashMap<ASTNode, Point> pos = new HashMap<>();

    public PanelArbolGrafico(ASTNode root) {
        this.root = root;
        setBackground(Color.WHITE);
        if (root != null) {
            int[] xCursor = { MARGIN };
            computePositions(root, 0, xCursor);
            int maxX = MARGIN, maxY = MARGIN;
            for (Point p : pos.values()) {
                maxX = Math.max(maxX, p.x + NODE_W + MARGIN);
                maxY = Math.max(maxY, p.y + NODE_H + MARGIN);
            }
            setPreferredSize(new Dimension(maxX, maxY));
        }
    }

    /**
     * Coloca hojas secuencialmente y centra padres sobre sus hijos.
     * Retorna el siguiente x disponible.
     */
    private void computePositions(ASTNode node, int depth, int[] xCursor) {
        int y = MARGIN + depth * (NODE_H + V_GAP);

        if (node.children == null || node.children.isEmpty()) {
            pos.put(node, new Point(xCursor[0], y));
            xCursor[0] += NODE_W + H_GAP;
            return;
        }

        int startX = xCursor[0];
        for (ASTNode child : node.children) {
            computePositions(child, depth + 1, xCursor);
        }
        // Centro sobre el primer y último hijo
        Point first = pos.get(node.children.get(0));
        Point last  = pos.get(node.children.get(node.children.size() - 1));
        int cx = (first.x + last.x) / 2;
        pos.put(node, new Point(cx, y));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (root == null) {
            g.setColor(Color.GRAY);
            g.setFont(new Font("Consolas", Font.ITALIC, 14));
            g.drawString("Sin árbol que mostrar.", MARGIN, MARGIN + 20);
            return;
        }
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        drawEdges(g2, root);
        drawNodes(g2, root);
    }

    // ── Fase 1: dibuja aristas (de abajo del padre al tope del hijo) ──
    private void drawEdges(Graphics2D g2, ASTNode node) {
        if (node.children == null) return;
        Point p = pos.get(node);
        if (p == null) return;
        g2.setColor(new Color(150, 150, 150));
        g2.setStroke(new BasicStroke(1.6f));
        for (ASTNode child : node.children) {
            Point c = pos.get(child);
            if (c != null) {
                g2.drawLine(p.x + NODE_W / 2, p.y + NODE_H,
                            c.x + NODE_W / 2, c.y);
            }
            drawEdges(g2, child);
        }
    }

    // ── Fase 2: dibuja nodos encima de las aristas ──
    private void drawNodes(Graphics2D g2, ASTNode node) {
        Point p = pos.get(node);
        if (p == null) return;

        Color fill = nodeColor(node.label);

        // Sombra
        g2.setColor(new Color(0, 0, 0, 25));
        g2.fill(new RoundRectangle2D.Float(p.x + 3, p.y + 3, NODE_W, NODE_H, 14, 14));

        // Relleno
        g2.setColor(fill);
        g2.fill(new RoundRectangle2D.Float(p.x, p.y, NODE_W, NODE_H, 14, 14));
        g2.setColor(fill.darker());
        g2.setStroke(new BasicStroke(1.8f));
        g2.draw(new RoundRectangle2D.Float(p.x, p.y, NODE_W, NODE_H, 14, 14));

        // Texto (truncado si es necesario)
        String full = buildLabel(node);
        String text = full;
        Font font = new Font("Consolas", Font.BOLD, 11);
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics(font);
        if (fm.stringWidth(text) > NODE_W - 10) {
            while (text.length() > 3 && fm.stringWidth(text + "…") > NODE_W - 10) {
                text = text.substring(0, text.length() - 1);
            }
            text += "…";
        }
        g2.setColor(Color.WHITE);
        int tx = p.x + (NODE_W - fm.stringWidth(text)) / 2;
        int ty = p.y + (NODE_H + fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(text, tx, ty);

        // Tooltip interno (tooltip real de Swing funciona solo en componentes, aquí es decorativo)
        if (node.children != null) {
            for (ASTNode child : node.children) drawNodes(g2, child);
        }
    }

    private String buildLabel(ASTNode node) {
        String l = node.label != null ? node.label : "?";
        if (node.value != null && !node.value.isEmpty()) l += ": " + node.value;
        return l;
    }

    /** Asigna color según el tipo de nodo. */
    private Color nodeColor(String label) {
        if (label == null) return new Color(100, 100, 100);
        if (label.contains("Program"))                                          return new Color(41,  98, 155);
        if (label.contains("Section"))                                          return new Color(30, 120, 100);
        if (label.contains("Config") || label.contains("Alphabet")
                || label.contains("Background"))                                return new Color(46, 139,  87);
        if (label.contains("StartState") || label.contains("FinalState")
                || label.contains("StateDecl") || label.contains("StateList")) return new Color(180, 120,   0);
        if (label.contains("Transition"))                                       return new Color(120,  30, 160);
        if (label.contains("Symbol"))                                           return new Color(200,  80,   0);
        if (label.contains("Error"))                                            return new Color(190,   0,   0);
        return new Color(80, 80, 80);
    }
}
