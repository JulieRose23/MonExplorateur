package com.monexplorateur.app;

interface IShizukuFileService {
    void destroy() = 16777114;
    String listFiles(String path) = 1;
    boolean createDirectory(String path) = 2;
    boolean deletePath(String path) = 3;
    boolean renamePath(String oldPath, String newPath) = 4;
    boolean copyPath(String srcPath, String destPath) = 5;
    boolean copyToLocalCache(String srcPath, String cacheDestPath) = 6;
}