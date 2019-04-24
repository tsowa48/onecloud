package group.genco.onecloud.cloud;

import java.util.List;

public interface iDrive {
    void login(String user, String password);
    void logout();
    List<File> list(String filePath);
    File get(String filePath);
    void download(String filePath, String realPath);
    void upload(String realPath, String filePath);
    void rm(String filePath);
    void mv(String oldPath, String newPath);

    long size();
    long free();

    String getUserName();

    int getIcon();
}
