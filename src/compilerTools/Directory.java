package compilerTools;

import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Clase para gestionar operaciones de archivos (Nuevo, Abrir, Guardar)
 * @author MiStErX
 */
public class Directory {
    private JFrame mainFrame;
    private JTextPane textComponent;
    private String applicationTitle;
    private String fileExtension;
    private String currentFilePath;
    private boolean isFileSaved;

    /**
     * Constructor de Directory
     * @param mainFrame Ventana principal de la aplicación
     * @param textComponent Componente de texto donde se edita el código
     * @param applicationTitle Título de la aplicación
     * @param fileExtension Extensión de archivos (.draw, .txt, etc.)
     */
    public Directory(JFrame mainFrame, JTextPane textComponent, String applicationTitle, String fileExtension) {
        this.mainFrame = mainFrame;
        this.textComponent = textComponent;
        this.applicationTitle = applicationTitle;
        this.fileExtension = fileExtension;
        this.currentFilePath = null;
        this.isFileSaved = true;
    }

    /**
     * Crea un nuevo archivo
     */
    public void New() {
        if (!isFileSaved) {
            int response = JOptionPane.showConfirmDialog(mainFrame,
                    "¿Deseas guardar los cambios antes de crear un nuevo archivo?",
                    "Archivo sin guardar",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (response == JOptionPane.YES_OPTION) {
                Save();
            } else if (response == JOptionPane.CANCEL_OPTION) {
                return;
            }
        }
        
        textComponent.setText("");
        currentFilePath = null;
        isFileSaved = true;
        updateWindowTitle();
    }

    /**
     * Abre un archivo existente
     * @return true si se abrió correctamente, false si se canceló
     */
    public boolean Open() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().endsWith(fileExtension);
            }

            @Override
            public String getDescription() {
                return "Archivos " + fileExtension + " (*" + fileExtension + ")";
            }
        });

        int result = fileChooser.showOpenDialog(mainFrame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                StringBuilder content = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(new FileInputStream(selectedFile), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append("\n");
                    }
                }
                
                textComponent.setText(content.toString());
                currentFilePath = selectedFile.getAbsolutePath();
                isFileSaved = true;
                updateWindowTitle();
                return true;
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(mainFrame,
                        "Error al abrir el archivo: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
        return false;
    }

    /**
     * Guarda el archivo actual (si existe) o pide ruta (si es nuevo)
     * @return true si se guardó correctamente
     */
    public boolean Save() {
        if (currentFilePath == null) {
            return SaveAs();
        }

        try {
            File file = new File(currentFilePath);
            try (FileOutputStream fos = new FileOutputStream(file);
                 OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
                writer.write(textComponent.getText());
            }
            isFileSaved = true;
            updateWindowTitle();
            JOptionPane.showMessageDialog(mainFrame, "Archivo guardado correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            return true;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(mainFrame,
                    "Error al guardar el archivo: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /**
     * Guarda el archivo con un nuevo nombre
     * @return true si se guardó correctamente
     */
    public boolean SaveAs() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().endsWith(fileExtension);
            }

            @Override
            public String getDescription() {
                return "Archivos " + fileExtension + " (*" + fileExtension + ")";
            }
        });

        if (currentFilePath != null) {
            fileChooser.setCurrentDirectory(new File(currentFilePath).getParentFile());
        }

        int result = fileChooser.showSaveDialog(mainFrame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String filePath = selectedFile.getAbsolutePath();

            // Si no tiene extensión, la agregamos
            if (!filePath.endsWith(fileExtension)) {
                filePath += fileExtension;
            }

            try {
                File file = new File(filePath);
                try (FileOutputStream fos = new FileOutputStream(file);
                     OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
                    writer.write(textComponent.getText());
                }
                
                currentFilePath = filePath;
                isFileSaved = true;
                updateWindowTitle();
                JOptionPane.showMessageDialog(mainFrame, "Archivo guardado correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                return true;
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(mainFrame,
                        "Error al guardar el archivo: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        return false;
    }

    /**
     * Maneja la salida de la aplicación
     */
    public void Exit() {
        if (!isFileSaved) {
            int response = JOptionPane.showConfirmDialog(mainFrame,
                    "¿Deseas guardar los cambios antes de salir?",
                    "Archivo sin guardar",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (response == JOptionPane.YES_OPTION) {
                Save();
            } else if (response == JOptionPane.CANCEL_OPTION) {
                return;
            }
        }
    }

    /**
     * Obtiene la ruta del archivo actual
     * @return Ruta del archivo o null si no hay archivo
     */
    public String getCurrentFilePath() {
        return currentFilePath;
    }

    /**
     * Verifica si el archivo está guardado
     * @return true si está guardado
     */
    public boolean isFileSaved() {
        return isFileSaved;
    }

    /**
     * Marca el archivo como modificado
     */
    public void markAsModified() {
        isFileSaved = false;
        updateWindowTitle();
    }

    /**
     * Actualiza el título de la ventana con el nombre del archivo
     */
    private void updateWindowTitle() {
        String title = applicationTitle;
        if (currentFilePath != null) {
            title += " - " + new File(currentFilePath).getName();
        }
        if (!isFileSaved) {
            title += " *";
        }
        mainFrame.setTitle(title);
    }
}
