import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import javax.swing.JPanel;

/**
 * Panel que dibuja gráficamente un autómata finito (AFD/AFN).
 * Estados: círculos. Finales: doble círculo. Inicial: flecha entrante.
 * Soporta resaltado de estados visitados durante una simulación.
 */
public class PanelDiagramaAutomata extends JPanel {

    private static final int  STATE_R   = 30;  // radio del círculo
    private static final int  FINAL_GAP = 5;   // separación doble círculo
    private static final int  INIT_ARROW = 38; // longitud flecha inicial
    private static final int  MARGIN    = 70;

    private final String                                          tipoAutomata;
    private final java.util.List<String>                         states;
    private final String                                         initialState;
    private final java.util.Set<String>                          finalStates;
    private final java.util.Map<String,
            java.util.Map<String, java.util.List<String>>>       delta;
    private final java.util.Set<String>                          visitedStates; // nullable
    private final String                                         lastState;     // nullable
    private final Boolean                                        accepted;      // nullable

    private java.util.Map<String, Point> positions;

    public PanelDiagramaAutomata(
            String tipoAutomata,
            java.util.List<String> states,
            String initialState,
            java.util.Set<String> finalStates,
            java.util.Map<String, java.util.Map<String, java.util.List<String>>> delta,
            java.util.Set<String> visitedStates,
            String lastState,
            Boolean accepted) {
        this.tipoAutomata  = tipoAutomata;
        this.states        = states;
        this.initialState  = initialState;
        this.finalStates   = finalStates;
        this.delta         = delta;
        this.visitedStates = visitedStates;
        this.lastState     = lastState;
        this.accepted      = accepted;
        setBackground(new Color(248, 249, 252));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (states == null || states.isEmpty()) {
            g.setColor(Color.GRAY);
            g.setFont(new Font("Consolas", Font.ITALIC, 13));
            g.drawString("Sin estados que mostrar.", MARGIN, MARGIN + 20);
            return;
        }
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,    RenderingHints.VALUE_STROKE_PURE);

        computeLayout();
        drawTransitions(g2);
        drawStates(g2);
        drawLegend(g2);
    }

    // ── Layout: circular para ≥3 estados, fila horizontal para ≤2 ──────────

    private void computeLayout() {
        positions = new java.util.LinkedHashMap<>();
        int n = states.size();
        if (n == 0) return;

        int w  = Math.max(getWidth(),  400);
        int h  = Math.max(getHeight(), 400);
        int cx = w / 2;
        int cy = h / 2;

        if (n == 1) {
            positions.put(states.get(0), new Point(cx, cy));
            return;
        }
        if (n == 2) {
            int gap = Math.min(w / 2 - MARGIN, 180);
            positions.put(states.get(0), new Point(cx - gap, cy));
            positions.put(states.get(1), new Point(cx + gap, cy));
            return;
        }

        // Circular: el estado inicial ocupa la posición "9 en punto" (ángulo = π)
        int initIdx = states.indexOf(initialState);
        if (initIdx < 0) initIdx = 0;

        int r = Math.min(w / 2 - MARGIN - STATE_R - INIT_ARROW,
                         h / 2 - MARGIN - STATE_R) - 10;
        r = Math.max(r, 80);

        for (int i = 0; i < n; i++) {
            int   shifted = (i - initIdx + n) % n;
            // Start at left (π) and go clockwise
            double angle = Math.PI - (2.0 * Math.PI * shifted) / n;
            int x = cx + (int)(r * Math.cos(angle));
            int y = cy + (int)(r * Math.sin(angle));
            positions.put(states.get(i), new Point(x, y));
        }
    }

    // ── Transiciones ────────────────────────────────────────────────────────

    private void drawTransitions(Graphics2D g2) {
        if (delta == null || positions == null) return;

        // Agrupar símbolos por par (origen, destino)
        java.util.Map<String, java.util.List<String>> edgeSymbols = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, java.util.Map<String, java.util.List<String>>> eOrig : delta.entrySet()) {
            String origin = eOrig.getKey();
            for (Map.Entry<String, java.util.List<String>> eSym : eOrig.getValue().entrySet()) {
                String symbol = eSym.getKey();
                for (String dest : eSym.getValue()) {
                    edgeSymbols.computeIfAbsent(origin + "\0" + dest,
                                               k -> new java.util.ArrayList<>()).add(symbol);
                }
            }
        }

        g2.setFont(new Font("Consolas", Font.PLAIN, 11));

        for (Map.Entry<String, java.util.List<String>> edge : edgeSymbols.entrySet()) {
            String[] parts  = edge.getKey().split("\0", 2);
            String   origin = parts[0];
            String   dest   = parts[1];
            String   label  = String.join(", ", edge.getValue());

            Point po = positions.get(origin);
            Point pd = positions.get(dest);
            if (po == null || pd == null) continue;

            boolean reverseExists = edgeSymbols.containsKey(dest + "\0" + origin);

            if (origin.equals(dest)) {
                drawSelfLoop(g2, po, label);
            } else {
                drawArrowEdge(g2, po, pd, label, reverseExists);
            }
        }
    }

    private void drawArrowEdge(Graphics2D g2, Point po, Point pd,
                                String label, boolean curved) {
        g2.setColor(new Color(50, 50, 60));
        g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        double dx   = pd.x - po.x;
        double dy   = pd.y - po.y;
        double dist = Math.hypot(dx, dy);
        if (dist < 1) return;
        double nx = dx / dist, ny = dy / dist;

        double sx = po.x + nx * STATE_R;
        double sy = po.y + ny * STATE_R;
        double ex = pd.x - nx * STATE_R;
        double ey = pd.y - ny * STATE_R;

        double midX = (sx + ex) / 2.0;
        double midY = (sy + ey) / 2.0;

        double offset  = curved ? 38 : 0;
        double cpX     = midX - ny * offset;
        double cpY     = midY + nx * offset;

        double labelX, labelY;

        if (curved) {
            var curve = new QuadCurve2D.Double(sx, sy, cpX, cpY, ex, ey);
            g2.draw(curve);
            // Punto ¼ del camino (visualmente cerca del arco)
            labelX = 0.25*sx + 0.5*cpX + 0.25*ex;
            labelY = 0.25*sy + 0.5*cpY + 0.25*ey;
            // Tangente en el punto final
            drawArrowhead(g2, ex, ey, ex - (0.5*sx + 0.5*cpX), ey - (0.5*sy + 0.5*cpY));
        } else {
            g2.draw(new Line2D.Double(sx, sy, ex, ey));
            labelX = midX;
            labelY = midY;
            drawArrowhead(g2, ex, ey, dx, dy);
        }

        drawEdgeLabel(g2, label, labelX, labelY);
    }

    private void drawSelfLoop(Graphics2D g2, Point p, String label) {
        g2.setColor(new Color(50, 50, 60));
        g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int loopD  = STATE_R + 14;
        int loopX  = p.x - loopD / 2;
        int loopY  = p.y - STATE_R - loopD;
        g2.drawOval(loopX, loopY, loopD, loopD);

        // Pequeña punta en la base derecha del óvalo
        drawArrowhead(g2, p.x + loopD / 4, p.y - STATE_R - 2, 1, 4);

        drawEdgeLabel(g2, label, p.x, p.y - STATE_R - loopD - 7);
    }

    private void drawArrowhead(Graphics2D g2, double tx, double ty, double dx, double dy) {
        double len = Math.hypot(dx, dy);
        if (len < 0.001) return;
        double nx = dx / len, ny = dy / len;
        int s = 9;
        int[] xs = {(int)tx, (int)(tx - nx*s - ny*s*0.5), (int)(tx - nx*s + ny*s*0.5)};
        int[] ys = {(int)ty, (int)(ty - ny*s + nx*s*0.5), (int)(ty - ny*s - nx*s*0.5)};
        g2.fill(new Polygon(xs, ys, 3));
    }

    private void drawEdgeLabel(Graphics2D g2, String text, double x, double y) {
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(text);
        int th = fm.getHeight();
        g2.setColor(new Color(255, 255, 255, 210));
        g2.fillRoundRect((int)x - tw/2 - 3, (int)y - th + 3, tw + 6, th + 2, 4, 4);
        g2.setColor(new Color(20, 20, 80));
        g2.drawString(text, (int)x - tw/2, (int)y + 1);
    }

    // ── Estados ─────────────────────────────────────────────────────────────

    private void drawStates(Graphics2D g2) {
        if (positions == null) return;
        for (Map.Entry<String, Point> e : positions.entrySet()) {
            String state = e.getKey();
            Point  p     = e.getValue();
            drawOneState(g2, state, p);
        }
    }

    private void drawOneState(Graphics2D g2, String state, Point p) {
        boolean isFinal   = finalStates != null && finalStates.contains(state);
        boolean isInitial = state.equals(initialState);
        Color   fill      = stateColor(state);

        // Sombra
        g2.setColor(new Color(0, 0, 0, 35));
        g2.fillOval(p.x - STATE_R + 3, p.y - STATE_R + 3, STATE_R*2, STATE_R*2);

        // Relleno
        g2.setColor(fill);
        g2.fillOval(p.x - STATE_R, p.y - STATE_R, STATE_R*2, STATE_R*2);

        // Borde exterior
        g2.setColor(new Color(40, 40, 40));
        g2.setStroke(new BasicStroke(2f));
        g2.drawOval(p.x - STATE_R, p.y - STATE_R, STATE_R*2, STATE_R*2);

        // Doble círculo para estado final
        if (isFinal) {
            g2.setStroke(new BasicStroke(1.5f));
            int r2 = STATE_R - FINAL_GAP;
            g2.drawOval(p.x - r2, p.y - r2, r2*2, r2*2);
        }

        // Nombre del estado
        g2.setFont(new Font("Consolas", Font.BOLD, 12));
        g2.setColor(new Color(20, 20, 20));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(state,
            p.x - fm.stringWidth(state)/2,
            p.y + fm.getAscent()/2 - 1);

        // Flecha de estado inicial
        if (isInitial) {
            g2.setColor(new Color(30, 30, 30));
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int ex = p.x - STATE_R;
            int sx = ex - INIT_ARROW;
            g2.drawLine(sx, p.y, ex, p.y);
            drawArrowhead(g2, ex, p.y, 1, 0);
        }
    }

    private Color stateColor(String state) {
        if (lastState != null && state.equals(lastState)) {
            return (accepted != null && accepted)
                ? new Color(140, 215, 140)  // verde = aceptado
                : new Color(230, 120, 120);  // rojo  = rechazado
        }
        if (visitedStates != null && visitedStates.contains(state))
            return new Color(255, 225, 90);   // amarillo = visitado
        if (finalStates != null && finalStates.contains(state))
            return new Color(195, 220, 255);  // azul claro = final
        if (state.equals(initialState))
            return new Color(210, 245, 210);  // verde claro = inicial
        return new Color(235, 235, 245);      // gris claro = normal
    }

    // ── Leyenda ─────────────────────────────────────────────────────────────

    private void drawLegend(Graphics2D g2) {
        if (visitedStates == null) return; // sin simulación → sin leyenda

        int lx = 12, ly = getHeight() - 16;
        g2.setFont(new Font("Consolas", Font.PLAIN, 11));

        Color[][] items = {
            {new Color(255, 225, 90),  null},
            {new Color(140, 215, 140), null},
            {new Color(230, 120, 120), null},
        };
        String[] labels = {"Visitado", "Aceptado", "Rechazado"};

        for (int i = 0; i < labels.length; i++) {
            g2.setColor(items[i][0]);
            g2.fillRoundRect(lx, ly - 12, 14, 14, 4, 4);
            g2.setColor(new Color(80, 80, 80));
            g2.drawRoundRect(lx, ly - 12, 14, 14, 4, 4);
            g2.drawString(labels[i], lx + 18, ly);
            lx += g2.getFontMetrics().stringWidth(labels[i]) + 30;
        }
    }
}
