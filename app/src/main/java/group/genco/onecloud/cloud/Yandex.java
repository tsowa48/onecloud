package group.genco.onecloud.cloud;

import java.net.URLEncoder;
import java.util.*;
import java.util.stream.*;

import group.genco.onecloud.R;
import group.genco.onecloud.http;

public class Yandex implements iDrive {

    private String _userName = "";
    private String _token;

    private long _size;
    private long _used;

    public Yandex(String accessToken){
        _token = accessToken;
        try {
            String json = new http.GET().execute("https://cloud-api.yandex.net/v1/disk", "Authorization:OAuth " + _token).get();
            List<String> paramList = new ArrayList<>(Arrays.asList(json.split("[,]")));
            paramList.stream().filter(P -> P.contains("used_space") || P.contains("total_space") || P.contains("login")).forEach(M -> {
                if(M.contains("total_space"))
                    _size = Long.parseLong(M.replace("\"total_space\":","").replaceAll(" ",""));
                else if(M.contains("used_space"))
                    _used = Long.parseLong(M.replace("\"used_space\":","").replaceAll(" ",""));
                else
                    _userName = M.replace("\"login\":","").replaceAll(" ","").replaceAll("\"","");
            });
        } catch(Exception ex) { }
    }

    @Override
    public void login(String user, String password) {
        //_userName = user;
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void logout() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public List<File> list(String filePath) {
        try {
            String getParam = URLEncoder.encode(filePath.startsWith("disk:") ? filePath.substring(6) : (filePath.startsWith("//") ? filePath.substring(1) : filePath),"UTF-8");
            String json = http.get("https://cloud-api.yandex.net/v1/disk/resources?path="+ getParam, "Authorization:OAuth " + _token);
            List<String> fileList = new ArrayList<>(Arrays.asList(json.split("[\\\"][n][a][m][e][\\\"][:]")));
            fileList.remove(0);
            fileList.remove(fileList.size() - 1);
            final List<File> files = fileList.stream().map(F -> {
                List<String> param = Arrays.asList(F.split("[,][\"]"));
                int type = param.stream().filter(P -> P.contains("type\":")).mapToInt(P -> (P.contains("dir") ? 0 : 1)).findFirst().getAsInt();
                String name = param.get(0).substring(1, param.get(0).length() - 1);//name
                long size = 0;//type == 1 ? (param[0].contains("folder") ? 0 : Long.parseUnsignedLong(param[8].replace("\"size\": ", ""))) : 0;//size
                String id = param.stream().filter(P -> P.contains("resource_id\":")).toArray()[0].toString().replace("resource_id\":\"", "");//resource_id":"155329031:a3e70c7d19438a7784b10b841f3a05526b2a3296275d422e0327de69fe7e3310"
                id = id.substring(0, id.length() - 1);
                String path = param.stream().filter(P -> P.contains("path\":")).toArray()[0].toString().replace("path\":\"disk:", "");//path":"disk:/Загрузки"
                path = path.substring(0, path.length() - 1);
                return new File(type, name, size, path, id);
            }).collect(Collectors.toList());
            return files;
        } catch(Exception ex) {
            return new ArrayList<>();
        }
    }

    @Override
    public File get(String filePath) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void download(String filePath, String realPath) {
        //throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void upload(String realPath, String filePath) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void rm(String filePath) {
        try {
            String getParam = URLEncoder.encode(filePath.startsWith("disk:") ? filePath.substring(6) : (filePath.startsWith("//") ? filePath.substring(1) : filePath),"UTF-8");
            new http.DELETE().execute("https://cloud-api.yandex.net/v1/disk/resources?permanently=true&path="+ getParam, "Authorization:OAuth " + _token).get();
        } catch(Exception ex) { }
    }

    @Override
    public void mv(String oldPath, String newPath) {
        throw new UnsupportedOperationException("not implemented");
        //Move & Rename
    }

    @Override
    public long size() {
        return _size;
    }

    @Override
    public long free() {
        return _size - _used;
    }

    @Override
    public String getUserName() {
        return _userName;
    }

    @Override
    public int getIcon() {
        return R.drawable.ic_yandex;
    }

}
