package upload;

/**
 * Classe représentant un fichier uploadé
 */
public class FileUpload {
    private String fileName;        // Nom original du fichier
    private String contentType;     // Type MIME (image/png, application/pdf, etc.)
    private byte[] bytes;           // Contenu du fichier en bytes
    private long size;              // Taille du fichier

    public FileUpload() {}

    public FileUpload(String fileName, String contentType, byte[] bytes) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.bytes = bytes;
        this.size = (bytes != null) ? bytes.length : 0;
    }

    // Getters et Setters
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
        this.size = (bytes != null) ? bytes.length : 0;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    /**
     * Vérifie si le fichier est vide
     */
    public boolean isEmpty() {
        return bytes == null || bytes.length == 0;
    }

    /**
     * Retourne l'extension du fichier
     */
    public String getExtension() {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    @Override
    public String toString() {
        return "FileUpload{" +
                "fileName='" + fileName + '\'' +
                ", contentType='" + contentType + '\'' +
                ", size=" + size +
                '}';
    }
}