package com.treserve.storage;

import java.io.InputStream;

public interface FileStorageService {
    String uploadFile(String key, byte[] content, String contentType);
    InputStream downloadFile(String key);
    String getDownloadUrl(String key);
    boolean fileExists(String key);
}