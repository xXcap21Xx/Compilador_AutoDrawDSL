package compilerTools;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Clase de utilidades para funciones comunes del compilador
 * @author MiStErX
 */
public class Functions {

    /**
     * Agrega números de línea a un JTextPane
     * @param textPane Componente de texto
     */
    public static void setLineNumberOnJTextComponent(JTextPane textPane) {
        // Implementación simple - simplemente habilita el número de línea
        textPane.putClientProperty("lineNumbers", true);
    }

    /**
     * Inserta un asterisco en el nombre de la ventana cuando hay cambios
     * @param frame Ventana principal
     * @param textComponent Componente de texto a monitorear
     * @param listener Acción a ejecutar cuando hay cambios
     */
    public static void insertAsteriskInName(JFrame frame, JTextPane textComponent, Runnable listener) {
        String originalTitle = frame.getTitle();
        
        textComponent.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if (!frame.getTitle().endsWith("*")) {
                    frame.setTitle(originalTitle + " *");
                }
                if (listener != null) {
                    listener.run();
                }
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (!frame.getTitle().endsWith("*")) {
                    frame.setTitle(originalTitle + " *");
                }
                if (listener != null) {
                    listener.run();
                }
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                if (listener != null) {
                    listener.run();
                }
            }
        });
    }

    /**
     * Agrega autocompletado a un JTextPane
     * @param suggestions Sugerencias de autocompletado
     * @param textComponent Componente de texto
     * @param listener Acción a ejecutar
     */
    public static void setAutocompleterJTextComponent(String[] suggestions, JTextPane textComponent, Runnable listener) {
        // Implementación simplificada de autocompletado
        textComponent.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if (listener != null) {
                    listener.run();
                }
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (listener != null) {
                    listener.run();
                }
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                if (listener != null) {
                    listener.run();
                }
            }
        });
    }

    /**
     * Colorea el texto en un JTextPane basado en lista de TextColor
     * @param colors Lista de colores a aplicar
     * @param textPane Componente de texto a colorear
     * @param backgroundColor Color de fondo
     */
    public static void colorTextPane(List<TextColor> colors, JTextPane textPane, Color backgroundColor) {
        if (colors == null || colors.isEmpty()) {
            return;
        }

        try {
            // Limpiar estilos previos
            SimpleAttributeSet defaultStyle = new SimpleAttributeSet();
            StyleConstants.setForeground(defaultStyle, Color.BLACK);
            textPane.getStyledDocument().setCharacterAttributes(0, textPane.getText().length(), defaultStyle, true);

            // Aplicar nuevo color de fondo
            textPane.setBackground(backgroundColor != null ? backgroundColor : Color.WHITE);

            // Aplicar cada color
            for (TextColor tc : colors) {
                SimpleAttributeSet style = new SimpleAttributeSet();
                StyleConstants.setForeground(style, tc.getColor());
                textPane.getStyledDocument().setCharacterAttributes(
                        tc.getStartPosition(),
                        tc.getLength(),
                        style,
                        false
                );
            }
        } catch (Exception e) {
            System.err.println("Error al colorear texto: " + e.getMessage());
        }
    }

    /**
     * Limpia todos los datos de una tabla
     * @param table Tabla a limpiar
     */
    public static void clearDataInTable(JTable table) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        while (model.getRowCount() > 0) {
            model.removeRow(0);
        }
    }

    /**
     * Agrega una fila de datos a una tabla
     * @param table Tabla donde agregar la fila
     * @param rowData Datos de la fila (array de Objects)
     */
    public static void addRowDataInTable(JTable table, Object[] rowData) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.addRow(rowData);
    }

    /**
     * Ordena una lista de errores por línea y columna
     * @param errors Lista de errores a ordenar
     */
    public static void sortErrorsByLineAndColumn(List<ErrorLSSL> errors) {
        if (errors == null || errors.isEmpty()) {
            return;
        }

        Comparator<ErrorLSSL> comparator = (error1, error2) -> {
            Token token1 = error1.getToken();
            Token token2 = error2.getToken();

            if (token1 == null || token2 == null) {
                return 0;
            }

            // Primero por línea
            if (token1.getLine() != token2.getLine()) {
                return Integer.compare(token1.getLine(), token2.getLine());
            }

            // Si están en la misma línea, por columna
            return Integer.compare(token1.getColumn(), token2.getColumn());
        };

        Collections.sort(errors, comparator);
    }

    /**
     * Obtiene el nombre de archivo a partir de una ruta completa
     * @param filePath Ruta completa del archivo
     * @return Nombre del archivo
     */
    public static String getFileName(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "Sin título";
        }
        String[] parts = filePath.replace("\\", "/").split("/");
        return parts[parts.length - 1];
    }

    /**
     * Obtiene el nombre del archivo sin extensión
     * @param filePath Ruta completa del archivo
     * @return Nombre sin extensión
     */
    public static String getFileNameWithoutExtension(String filePath) {
        String fileName = getFileName(filePath);
        int lastDot = fileName.lastIndexOf(".");
        if (lastDot > 0) {
            return fileName.substring(0, lastDot);
        }
        return fileName;
    }

    /**
     * Valida si una cadena es un identificador válido
     * @param text Texto a validar
     * @return true si es un identificador válido
     */
    public static boolean isValidIdentifier(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return text.matches("[a-zA-Z_][a-zA-Z0-9_]*");
    }

    /**
     * Cuenta el número de líneas en un texto
     * @param text Texto a contar
     * @return Número de líneas
     */
    public static int countLines(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.split("\n").length;
    }
}
