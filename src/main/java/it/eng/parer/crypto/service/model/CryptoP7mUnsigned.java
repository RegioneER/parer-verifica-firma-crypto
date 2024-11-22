package it.eng.parer.crypto.service.model;

import java.io.InputStream;
import java.io.Serializable;

public class CryptoP7mUnsigned implements Serializable {

    private static final long serialVersionUID = 1L;

    private String fileName;
    private String fileType;
    private transient InputStream data;

    public CryptoP7mUnsigned(String fileName, String fileType, InputStream data) {
        this.fileName = fileName;
        this.fileType = fileType;
        this.data = data;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public InputStream getData() {
        return data;
    }
}
