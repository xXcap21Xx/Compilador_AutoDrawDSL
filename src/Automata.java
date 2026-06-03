import java.util.*;
import java.util.regex.*;

/**
 * Modelo de autómata finito con operaciones de teoría de lenguajes.
 * Soporta AFD y AFN (con ε-transiciones).
 */
public class Automata {

    // ── Campos ──────────────────────────────────────────────────────────────
    public String       tipo;          // "AFD" o "AFN"
    public List<String> states;        // estados en orden de declaración
    public List<String> alphabet;      // símbolos del alfabeto
    public String       initialState;
    public Set<String>  finalStates;
    /** estado → símbolo → conjunto de destinos */
    public Map<String, Map<String, Set<String>>> delta;

    public Automata() {
        states      = new ArrayList<>();
        alphabet    = new ArrayList<>();
        finalStates = new LinkedHashSet<>();
        delta       = new LinkedHashMap<>();
    }

    // ══════════════════════════════════════════════════════════════════════
    // FACTORÍAS
    // ══════════════════════════════════════════════════════════════════════

    /** Construye el autómata desde la compilación actual del IDE. */
    public static Automata fromCompiled(List<SimboloDSL> symbols, String src) {
        Automata a = new Automata();
        a.tipo = "AFD";
        for (SimboloDSL s : symbols) {
            if (s.tipo == null) continue;
            switch (s.tipo) {
                case "Tipo_Automata_AFN": a.tipo = "AFN"; break;
                case "Alphabet_Symbol":
                    String sym = s.nombre.replace("'", "");
                    if (!a.alphabet.contains(sym)) a.alphabet.add(sym);
                    break;
                case "Epsilon_Symbol":
                    if (!a.alphabet.contains("ε")) a.alphabet.add("ε");
                    break;
                case "Initial_State":
                    if (!a.states.contains(s.nombre)) a.states.add(0, s.nombre);
                    a.initialState = s.nombre;
                    break;
                case "State_Declared":
                    if (!a.states.contains(s.nombre)) a.states.add(s.nombre);
                    break;
                case "Final_State":
                    if (!a.states.contains(s.nombre)) a.states.add(s.nombre);
                    a.finalStates.add(s.nombre);
                    break;
            }
        }
        parseDeltaInto(a, src);
        return a;
    }

    /** Construye el autómata desde el texto de un archivo .draw (segundo autómata). */
    public static Automata fromText(String src) {
        Automata a = new Automata();
        a.tipo = "AFD";

        Matcher m = Pattern.compile("TIPO\\s+(AFD|AFN)\\s*;").matcher(src);
        if (m.find()) a.tipo = m.group(1);

        m = Pattern.compile("ALFABETO\\s*\\{([^}]+)\\}").matcher(src);
        if (m.find()) {
            for (String tok : m.group(1).split(",")) {
                String sym = tok.trim().replace("'", "");
                if (sym.equalsIgnoreCase("EPSILON")) sym = "ε";
                if (!sym.isEmpty() && !a.alphabet.contains(sym)) a.alphabet.add(sym);
            }
        }

        m = Pattern.compile("INICIO\\s+(\\w+)\\s*;").matcher(src);
        if (m.find()) {
            a.initialState = m.group(1);
            if (!a.states.contains(a.initialState)) a.states.add(0, a.initialState);
        }

        m = Pattern.compile("ESTADO\\s+(\\w+)\\s*;").matcher(src);
        while (m.find()) {
            String st = m.group(1);
            if (!a.states.contains(st)) a.states.add(st);
        }
        m = Pattern.compile("ESTADOS\\s*\\{([^}]+)\\}").matcher(src);
        if (m.find()) {
            for (String tok : m.group(1).split(",")) {
                String st = tok.trim();
                if (!st.isEmpty() && !a.states.contains(st)) a.states.add(st);
            }
        }

        m = Pattern.compile("FINAL\\s+([\\w,\\s]+)\\s*;").matcher(src);
        if (m.find()) {
            for (String tok : m.group(1).split(",")) {
                String st = tok.trim();
                if (!st.isEmpty()) {
                    a.finalStates.add(st);
                    if (!a.states.contains(st)) a.states.add(st);
                }
            }
        }

        parseDeltaInto(a, src);
        return a;
    }

    private static void parseDeltaInto(Automata a, String src) {
        Matcher m = Pattern.compile(
            "([A-Za-z0-9_]+)\\s*->\\s*([A-Za-z0-9_]+)\\s*\\[\\s*([^\\]]+)\\s*\\]\\s*;").matcher(src);
        while (m.find()) {
            String origin = m.group(1), dest = m.group(2);
            for (String rawSym : m.group(3).split(",")) {
                String sym = rawSym.trim().replace("'", "");
                if (sym.equalsIgnoreCase("EPSILON")) sym = "ε";
                a.delta.computeIfAbsent(origin, k -> new LinkedHashMap<>())
                       .computeIfAbsent(sym,    k -> new LinkedHashSet<>())
                       .add(dest);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // OPERACIONES
    // ══════════════════════════════════════════════════════════════════════

    /** Complemento ¬A — convierte a AFD si es necesario y completa el autómata. */
    public static Automata complement(Automata a) {
        Automata dfa     = ensureDFA(a);
        Automata complete = makeComplete(dfa);
        Automata r       = deepCopy(complete);
        r.tipo = "AFD";
        Set<String> newFinals = new LinkedHashSet<>();
        for (String st : r.states) if (!complete.finalStates.contains(st)) newFinals.add(st);
        r.finalStates = newFinals;
        return r;
    }

    /** Kleene* — cierre de Kleene (incluye la cadena vacía). */
    public static Automata kleeneStar(Automata a) {
        Automata r  = prefixedCopy(a, "s_");
        String   q0 = freshName("q0", r.states);
        r.states.add(0, q0);
        addEps(r, q0, "s_" + a.initialState);
        for (String f : a.finalStates) addEps(r, "s_" + f, q0);
        r.initialState = q0;
        r.finalStates  = new LinkedHashSet<>();
        r.finalStates.add(q0);
        for (String f : a.finalStates) r.finalStates.add("s_" + f);
        if (!r.alphabet.contains("ε")) r.alphabet.add("ε");
        r.tipo = "AFN";
        return r;
    }

    /** Kleene+ — debe consumir al menos un símbolo. */
    public static Automata kleenePlus(Automata a) {
        Automata r    = prefixedCopy(a, "p_");
        String   qLoop = freshName("qL", r.states);
        r.states.add(qLoop);
        addEps(r, qLoop, "p_" + a.initialState);
        for (String f : a.finalStates) addEps(r, "p_" + f, qLoop);
        r.initialState = "p_" + a.initialState;
        Set<String> newFinals = new LinkedHashSet<>();
        for (String f : a.finalStates) newFinals.add("p_" + f);
        r.finalStates = newFinals;
        if (!r.alphabet.contains("ε")) r.alphabet.add("ε");
        r.tipo = "AFN";
        return r;
    }

    /** Unión A∪B — construcción ε-AFN. */
    public static Automata union(Automata a, Automata b) {
        Automata ra = prefixedCopy(a, "A_");
        Automata rb = prefixedCopy(b, "B_");
        Automata r  = new Automata();
        r.tipo = "AFN";
        String q0 = "q_u0";
        r.states.add(q0);
        r.states.addAll(ra.states);
        r.states.addAll(rb.states);
        mergeAlpha(r, ra, rb);
        mergeDelta(r, ra);
        mergeDelta(r, rb);
        addEps(r, q0, ra.initialState);
        addEps(r, q0, rb.initialState);
        r.initialState = q0;
        r.finalStates.addAll(ra.finalStates);
        r.finalStates.addAll(rb.finalStates);
        if (!r.alphabet.contains("ε")) r.alphabet.add("ε");
        return r;
    }

    /** Intersección A∩B — producto cartesiano (convierte a AFD si es necesario). */
    public static Automata intersection(Automata a, Automata b) {
        return product(ensureDFA(a), ensureDFA(b), false);
    }

    /** Concatenación A·B — construcción ε-AFN. */
    public static Automata concatenation(Automata a, Automata b) {
        Automata ra = prefixedCopy(a, "A_");
        Automata rb = prefixedCopy(b, "B_");
        Automata r  = new Automata();
        r.tipo = "AFN";
        r.states.addAll(ra.states);
        r.states.addAll(rb.states);
        mergeAlpha(r, ra, rb);
        mergeDelta(r, ra);
        mergeDelta(r, rb);
        for (String f : ra.finalStates) addEps(r, f, rb.initialState);
        r.initialState = ra.initialState;
        r.finalStates.addAll(rb.finalStates);
        if (!r.alphabet.contains("ε")) r.alphabet.add("ε");
        return r;
    }

    /** Diferencia A−B = A ∩ ¬B. */
    public static Automata difference(Automata a, Automata b) {
        return product(ensureDFA(a), complement(b), false);
    }

    // ══════════════════════════════════════════════════════════════════════
    // CONSTRUCCIONES AUXILIARES
    // ══════════════════════════════════════════════════════════════════════

    /** Construye el AFD completo agregando un estado trampa para transiciones faltantes. */
    public static Automata makeComplete(Automata a) {
        Automata r    = deepCopy(a);
        String   trap = freshName("q_trap", r.states);
        boolean  need = false;
        for (String state : new ArrayList<>(r.states)) {
            for (String sym : r.alphabet) {
                if ("ε".equals(sym)) continue;
                Set<String> tgt = r.delta.getOrDefault(state, Collections.emptyMap()).get(sym);
                if (tgt == null || tgt.isEmpty()) {
                    r.delta.computeIfAbsent(state, k -> new LinkedHashMap<>())
                           .computeIfAbsent(sym,   k -> new LinkedHashSet<>()).add(trap);
                    need = true;
                }
            }
        }
        if (need) {
            r.states.add(trap);
            for (String sym : r.alphabet) {
                if ("ε".equals(sym)) continue;
                r.delta.computeIfAbsent(trap, k -> new LinkedHashMap<>())
                       .computeIfAbsent(sym,  k -> new LinkedHashSet<>()).add(trap);
            }
        }
        return r;
    }

    /** Convierte un AFN a AFD via construcción de subconjuntos. */
    public static Automata toDFA(Automata nfa) {
        Automata dfa = new Automata();
        dfa.tipo     = "AFD";
        for (String sym : nfa.alphabet) {
            if (!isEpsilonSymbol(sym) && !dfa.alphabet.contains(sym)) dfa.alphabet.add(sym);
        }

        Map<Set<String>, String> subsets = new LinkedHashMap<>();
        Queue<Set<String>>       queue   = new LinkedList<>();
        Set<String>              start   = epsClosure(Collections.singleton(nfa.initialState), nfa.delta);
        start = new LinkedHashSet<>(start);
        subsets.put(start, setLabel(start));
        queue.add(start);
        dfa.initialState = subsets.get(start);

        while (!queue.isEmpty()) {
            Set<String> cur     = queue.poll();
            String      curName = subsets.get(cur);
            if (!dfa.states.contains(curName)) dfa.states.add(curName);
            for (String s : cur) if (nfa.finalStates.contains(s)) { dfa.finalStates.add(curName); break; }

            for (String sym : dfa.alphabet) {
                Set<String> rawNext = new LinkedHashSet<>();
                for (String st : cur) {
                    Set<String> tgt = nfa.delta.getOrDefault(st, Collections.emptyMap()).get(sym);
                    if (tgt != null) rawNext.addAll(tgt);
                }
                Set<String> next = epsClosure(rawNext, nfa.delta);
                next = new LinkedHashSet<>(next);
                if (!subsets.containsKey(next)) {
                    subsets.put(next, setLabel(next));
                    queue.add(next);
                }
                dfa.delta.computeIfAbsent(curName, k -> new LinkedHashMap<>())
                         .computeIfAbsent(sym,     k -> new LinkedHashSet<>())
                         .add(subsets.get(next));
            }
        }
        return dfa;
    }

    /** Producto cartesiano de dos AFDs. unionMode=false → intersección, true → unión. */
    private static Automata product(Automata a, Automata b, boolean unionMode) {
        Automata ca = makeComplete(a);
        Automata cb = makeComplete(b);
        Automata r  = new Automata();
        r.tipo = "AFD";
        Set<String> alpha = new LinkedHashSet<>(ca.alphabet);
        alpha.addAll(cb.alphabet);
        alpha.remove("ε");
        r.alphabet.addAll(alpha);

        Map<String, String> pairName = new LinkedHashMap<>();
        Queue<String[]>     queue    = new LinkedList<>();
        String              startKey = ca.initialState + "," + cb.initialState;
        pairName.put(startKey, pairLabel(ca.initialState, cb.initialState));
        queue.add(new String[]{ca.initialState, cb.initialState});

        while (!queue.isEmpty()) {
            String[] pair = queue.poll();
            String qa = pair[0], qb = pair[1];
            String name = pairName.get(qa + "," + qb);
            if (!r.states.contains(name)) r.states.add(name);
            for (String sym : alpha) {
                String na = first(ca, qa, sym);
                String nb = first(cb, qb, sym);
                if (na == null || nb == null) continue;
                String nk = na + "," + nb;
                if (!pairName.containsKey(nk)) { pairName.put(nk, pairLabel(na, nb)); queue.add(new String[]{na, nb}); }
                r.delta.computeIfAbsent(name, k -> new LinkedHashMap<>())
                       .computeIfAbsent(sym,  k -> new LinkedHashSet<>()).add(pairName.get(nk));
            }
        }

        r.initialState = pairName.get(startKey);
        for (Map.Entry<String, String> e : pairName.entrySet()) {
            String[] pts = e.getKey().split(",", 2);
            boolean af = ca.finalStates.contains(pts[0]);
            boolean bf = cb.finalStates.contains(pts[1]);
            if (unionMode ? (af || bf) : (af && bf)) r.finalStates.add(e.getValue());
        }
        return r;
    }

    // ══════════════════════════════════════════════════════════════════════
    // SIMULACIÓN
    // ══════════════════════════════════════════════════════════════════════

    public boolean simulate(String input, StringBuilder log, Set<String> visited, String[] lastState) {
        return "AFN".equals(tipo)
            ? simulateNFA(input, log, visited, lastState)
            : simulateDFA(input, log, visited, lastState);
    }

    private boolean simulateDFA(String input, StringBuilder log, Set<String> visited, String[] last) {
        String cur = initialState;
        if (visited != null) visited.add(cur);
        String[] tokenError = {null};
        List<String> inputSymbols = tokenizeInputSymbols(input, tokenError);
        log.append("Tipo  : AFD\nCadena: \"").append(input).append("\"\nInicio: ").append(cur).append("\n\n");
        if (inputSymbols == null) {
            log.append("Alfabeto: ").append(alphabet).append("\n\n");
            log.append("❌ RECHAZADA — ").append(tokenError[0]).append("\n");
            if (last != null) last[0] = cur;
            return false;
        }
        log.append("Símbolos leídos: ").append(inputSymbols).append("\n\n");
        log.append(String.format("%-6s %-20s %-12s %-20s%n", "Paso", "Estado", "Símbolo", "Siguiente"));
        log.append("──────────────────────────────────────────────────\n");
        for (int i = 0; i < inputSymbols.size(); i++) {
            String sym = inputSymbols.get(i);
            Set<String> tgt = delta.getOrDefault(cur, Collections.emptyMap()).get(sym);
            if (tgt == null || tgt.isEmpty()) {
                log.append(String.format("%-6d %-20s %-12s ∅ (trampa)%n", i+1, cur, "'"+sym+"'"));
                log.append("\n❌ RECHAZADA — no existe δ(").append(cur).append(", '").append(sym).append("').\n");
                if (last != null) last[0] = cur;
                return false;
            }
            String next = tgt.iterator().next();
            log.append(String.format("%-6d %-20s %-12s %-20s%n", i+1, cur, "'"+sym+"'", next));
            cur = next;
            if (visited != null) visited.add(cur);
        }
        if (last != null) last[0] = cur;
        boolean ok = finalStates.contains(cur);
        log.append("\nEstado final alcanzado: ").append(cur).append("\n");
        log.append(ok ? "✅ Cadena ACEPTADA.\n" : "❌ Cadena RECHAZADA — el estado no es final.\n");
        return ok;
    }

    private boolean simulateNFA(String input, StringBuilder log, Set<String> visited, String[] last) {
        log.append("Tipo  : AFN (con cierre-ε)\nCadena: \"").append(input).append("\"\n\n");
        String[] tokenError = {null};
        List<String> inputSymbols = tokenizeInputSymbols(input, tokenError);
        if (inputSymbols == null) {
            log.append("Alfabeto: ").append(alphabet).append("\n\n");
            log.append("❌ RECHAZADA — ").append(tokenError[0]).append("\n");
            if (last != null) last[0] = initialState;
            return false;
        }
        log.append("Símbolos leídos: ").append(inputSymbols).append("\n\n");
        Set<String> cur = epsClosure(Collections.singleton(initialState), delta);
        if (visited != null) visited.addAll(cur);
        log.append("Paso 0: ε-cierre({").append(initialState).append("}) = ").append(cur).append("\n\n");
        for (int i = 0; i < inputSymbols.size(); i++) {
            String sym = inputSymbols.get(i);
            Set<String> rawNext = new LinkedHashSet<>();
            for (String st : cur) {
                Set<String> tgt = delta.getOrDefault(st, Collections.emptyMap()).get(sym);
                if (tgt != null) rawNext.addAll(tgt);
            }
            Set<String> next = epsClosure(rawNext, delta);
            log.append("Paso ").append(i+1).append(": δ̂(").append(cur).append(", '").append(sym).append("') = ").append(next).append("\n");
            cur = next;
            if (visited != null) visited.addAll(cur);
            if (cur.isEmpty()) { log.append("\n❌ Conjunto vacío — RECHAZADA.\n"); if (last!=null) last[0]=null; return false; }
        }
        boolean ok = cur.stream().anyMatch(finalStates::contains);
        log.append("\nConjunto final: ").append(cur).append("\n");
        if (ok) {
            Set<String> inter = new LinkedHashSet<>(cur); inter.retainAll(finalStates);
            log.append("✅ Contiene estado(s) final(es) ").append(inter).append(" → ACEPTADA.\n");
            if (last != null) last[0] = inter.iterator().next();
        } else {
            log.append("❌ Ningún estado final en ").append(cur).append(" → RECHAZADA.\n");
            if (last != null) last[0] = cur.isEmpty() ? null : cur.iterator().next();
        }
        return ok;
    }

    private List<String> tokenizeInputSymbols(String input, String[] errorOut) {
        List<String> symbols = new ArrayList<>();
        List<String> candidates = new ArrayList<>();
        for (String sym : alphabet) {
            if (sym == null || sym.isEmpty()) continue;
            if ("ε".equals(sym) || "EPSILON".equalsIgnoreCase(sym)) continue;
            if (!candidates.contains(sym)) candidates.add(sym);
        }
        candidates.sort((a, b) -> Integer.compare(b.length(), a.length()));

        int pos = 0;
        while (pos < input.length()) {
            String matched = null;
            for (String candidate : candidates) {
                if (input.startsWith(candidate, pos)) {
                    matched = candidate;
                    break;
                }
            }
            if (matched == null) {
                if (errorOut != null && errorOut.length > 0) {
                    errorOut[0] = "no se pudo leer un símbolo del alfabeto desde la posición "
                            + (pos + 1) + " cerca de \"" + input.substring(pos) + "\".";
                }
                return null;
            }
            symbols.add(matched);
            pos += matched.length();
        }
        return symbols;
    }

    // ══════════════════════════════════════════════════════════════════════
    // UTILIDADES
    // ══════════════════════════════════════════════════════════════════════

    /** Convierte delta a Map<String, Map<String, List<String>>> para PanelDiagramaAutomata. */
    public Map<String, Map<String, List<String>>> toDeltaList() {
        Map<String, Map<String, List<String>>> r = new LinkedHashMap<>();
        delta.forEach((st, inner) -> {
            Map<String, List<String>> m = new LinkedHashMap<>();
            inner.forEach((sym, dsts) -> m.put(sym, new ArrayList<>(dsts)));
            r.put(st, m);
        });
        return r;
    }

    private static Automata ensureDFA(Automata a) {
        return ("AFD".equals(a.tipo) && !a.alphabet.contains("ε")) ? a : toDFA(a);
    }

    private static boolean isEpsilonSymbol(String sym) {
        return sym != null && ("ε".equals(sym) || "EPSILON".equalsIgnoreCase(sym));
    }

    private static Set<String> epsClosure(Set<String> states, Map<String, Map<String, Set<String>>> delta) {
        Set<String>    closure = new LinkedHashSet<>(states);
        Deque<String>  stack   = new ArrayDeque<>(states);
        while (!stack.isEmpty()) {
            String st = stack.pop();
            Set<String> eps = delta.getOrDefault(st, Collections.emptyMap()).get("ε");
            if (eps != null) for (String t : eps) if (closure.add(t)) stack.push(t);
        }
        return closure;
    }

    private static Automata deepCopy(Automata a) {
        Automata r = new Automata();
        r.tipo = a.tipo; r.states = new ArrayList<>(a.states);
        r.alphabet = new ArrayList<>(a.alphabet); r.initialState = a.initialState;
        r.finalStates = new LinkedHashSet<>(a.finalStates);
        a.delta.forEach((k, v) -> {
            Map<String, Set<String>> m = new LinkedHashMap<>();
            v.forEach((sym, dsts) -> m.put(sym, new LinkedHashSet<>(dsts)));
            r.delta.put(k, m);
        });
        return r;
    }

    private static Automata prefixedCopy(Automata a, String p) {
        Automata r = new Automata();
        r.tipo = a.tipo; r.alphabet = new ArrayList<>(a.alphabet);
        a.states.forEach(st -> r.states.add(p + st));
        r.initialState = p + a.initialState;
        a.finalStates.forEach(f -> r.finalStates.add(p + f));
        a.delta.forEach((st, inner) -> {
            Map<String, Set<String>> m = new LinkedHashMap<>();
            inner.forEach((sym, dsts) -> { Set<String> ds = new LinkedHashSet<>(); dsts.forEach(d -> ds.add(p+d)); m.put(sym, ds); });
            r.delta.put(p + st, m);
        });
        return r;
    }

    private static void addEps(Automata a, String from, String to) {
        a.delta.computeIfAbsent(from, k -> new LinkedHashMap<>())
               .computeIfAbsent("ε", k -> new LinkedHashSet<>()).add(to);
    }

    private static void mergeDelta(Automata r, Automata src) {
        src.delta.forEach((st, inner) ->
            inner.forEach((sym, dsts) ->
                r.delta.computeIfAbsent(st,  k -> new LinkedHashMap<>())
                       .computeIfAbsent(sym, k -> new LinkedHashSet<>()).addAll(dsts)));
    }

    private static void mergeAlpha(Automata r, Automata a, Automata b) {
        a.alphabet.forEach(s -> { if (!r.alphabet.contains(s)) r.alphabet.add(s); });
        b.alphabet.forEach(s -> { if (!r.alphabet.contains(s)) r.alphabet.add(s); });
    }

    private static String first(Automata a, String state, String sym) {
        Set<String> t = a.delta.getOrDefault(state, Collections.emptyMap()).get(sym);
        return (t == null || t.isEmpty()) ? null : t.iterator().next();
    }

    private static String pairLabel(String a, String b) { return "(" + a + "," + b + ")"; }

    private static String setLabel(Set<String> s) {
        if (s.isEmpty()) return "VACIO";
        List<String> sorted = new ArrayList<>(s); Collections.sort(sorted);
        return "{" + String.join(",", sorted) + "}";
    }

    private static String freshName(String base, List<String> existing) {
        if (!existing.contains(base)) return base;
        int i = 0; while (existing.contains(base + i)) i++;
        return base + i;
    }
}
