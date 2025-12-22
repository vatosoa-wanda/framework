package upload;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;

/**
 * Parser pour les requêtes multipart/form-data (upload de fichiers)
 */
public class MultipartParser {

    private Map<String, String> formFields;           // Champs simples (texte)
    private Map<String, FileUpload> singleFiles;      // Fichier unique par nom
    private Map<String, List<FileUpload>> multiFiles; // Fichiers multiples par nom

    public MultipartParser() {
        this.formFields = new HashMap<>();
        this.singleFiles = new HashMap<>();
        this.multiFiles = new HashMap<>();
    }

    /**
     * Vérifie si la requête est de type multipart
     */
    public static boolean isMultipartRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().startsWith("multipart/");
    }

    /**
     * Parse la requête multipart et extrait les données
     */
    public void parse(HttpServletRequest request) throws IOException, ServletException {
        if (!isMultipartRequest(request)) {
            return;
        }

        for (Part part : request.getParts()) {
            String fieldName = part.getName();
            String fileName = getFileName(part);

            if (fileName == null || fileName.isEmpty()) {
                // C'est un champ de formulaire simple (texte)
                String value = readPartAsString(part);
                formFields.put(fieldName, value);
                System.out.printf("[MultipartParser] Champ texte: %s = %s%n", fieldName, value);
            } else {
                // C'est un fichier
                byte[] bytes = readPartAsBytes(part);
                String contentType = part.getContentType();
                
                FileUpload fileUpload = new FileUpload(fileName, contentType, bytes);
                
                // Stockage en tant que fichier unique
                singleFiles.put(fieldName, fileUpload);
                
                // Stockage en tant que fichier multiple (liste)
                multiFiles.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(fileUpload);
                
                System.out.printf("[MultipartParser] Fichier uploadé: %s (%s, %d bytes)%n", 
                    fileName, contentType, bytes.length);
            }
        }
    }

    /**
     * Extrait le nom de fichier depuis le header Content-Disposition
     */
    private String getFileName(Part part) {
        String contentDisposition = part.getHeader("content-disposition");
        if (contentDisposition != null) {
            for (String token : contentDisposition.split(";")) {
                if (token.trim().startsWith("filename")) {
                    String fileName = token.substring(token.indexOf('=') + 1).trim();
                    // Enlever les guillemets
                    if (fileName.startsWith("\"") && fileName.endsWith("\"")) {
                        fileName = fileName.substring(1, fileName.length() - 1);
                    }
                    return fileName;
                }
            }
        }
        return null;
    }

    /**
     * Lit le contenu d'une Part comme String (pour les champs texte)
     */
    private String readPartAsString(Part part) throws IOException {
        try (InputStream is = part.getInputStream();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            return baos.toString("UTF-8");
        }
    }

    /**
     * Lit le contenu d'une Part comme tableau de bytes (pour les fichiers)
     */
    private byte[] readPartAsBytes(Part part) throws IOException {
        try (InputStream is = part.getInputStream();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = is.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            return baos.toByteArray();
        }
    }

    // Getters
    public Map<String, String> getFormFields() {
        return formFields;
    }

    public Map<String, FileUpload> getSingleFiles() {
        return singleFiles;
    }

    public Map<String, List<FileUpload>> getMultiFiles() {
        return multiFiles;
    }

    /**
     * Récupère un champ texte par son nom
     */
    public String getField(String name) {
        return formFields.get(name);
    }

    /**
     * Récupère un fichier unique par son nom
     */
    public FileUpload getFile(String name) {
        return singleFiles.get(name);
    }

    /**
     * Récupère une liste de fichiers par nom (pour upload multiple)
     */
    public List<FileUpload> getFiles(String name) {
        return multiFiles.getOrDefault(name, new ArrayList<>());
    }

    /**
     * Vérifie si un fichier existe pour ce nom
     */
    public boolean hasFile(String name) {
        FileUpload file = singleFiles.get(name);
        return file != null && !file.isEmpty();
    }
}