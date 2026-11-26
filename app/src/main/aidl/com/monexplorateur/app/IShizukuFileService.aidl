package com.monexplorateur.app;

interface IShizukuFileService {
    void destroy() = 16777114;
    String listFiles(String path);
    boolean createDirectory(String path);
    boolean deletePath(String path);
    boolean renamePath(String oldPath, String newPath);
    boolean copyPath(String srcPath, String destPath);
    boolean copyToLocalCache(String srcPath, String cacheDestPath);
}