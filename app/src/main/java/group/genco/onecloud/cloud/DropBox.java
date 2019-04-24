package group.genco.onecloud.cloud;

import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import group.genco.onecloud.R;
import group.genco.onecloud.http;

public final class DropBox implements iDrive {
    private String _userName = "";
    private String _token;

    public DropBox(String accessToken) {
        _token = accessToken;
        try {
            String json = new http.POST().execute("https://api.dropboxapi.com/2/users/get_current_account", "null", "Content-Type:application/json; charset=utf-8", "Authorization:Bearer " + _token).get();
            _userName = json.substring(json.indexOf("\"email\": \"") + 10, json.indexOf("\",", json.indexOf("\"email\":")));
        } catch(Exception ex) { }
    }

    @Override
    public void login(String user, String password) {
        //GET https://www.dropbox.com/oauth2/authorize?client_id=q0i0wh31uxb6qpa&response_type=token&redirect_uri=http://localhost/
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void logout() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public List<File> list(String filePath) {
        try {
            String postParam = "{\"path\":\"" + (filePath.equals("/")||filePath.equals("") ? "" : (filePath.startsWith("/") && filePath.length() > 1 ? filePath : ("/" + filePath))) + "\",\"recursive\":false}";
            String json = http.post("https://api.dropboxapi.com/2/files/list_folder", postParam, "Content-Type:application/json; charset=utf-8", "Authorization:Bearer " + _token);
            String[] fileList = json.split("[}][,]");
            final List<File> files = Arrays.stream(fileList).map(F -> {
                String[] param = F.split("[,]");
                int type = param[0].contains("folder") ? 0 : 1;//.tag
                String name = param[1].substring(param[1].indexOf(":") + 3, param[1].length() - 1);//name
                long size = 0;//type == 1 ? (param[0].contains("folder") ? 0 : Long.parseUnsignedLong(param[8].replace("\"size\": ", ""))) : 0;//size
                String path = param[2].substring(param[2].indexOf(":") + 3, param[2].length() - 1);//path_lower
                String id = param[4].substring(param[4].indexOf(":") + 3, param[4].length() - 1);//id
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
        try {//application/x-www-form-urlencoded
            byte[] data = new http.POST2().execute("https://content.dropboxapi.com/2/files/download", "","Content-Type:application/octet-stream", "Content-Length:0",
                    "Authorization:Bearer " + _token, "Dropbox-API-Arg:{\"path\":\"" + filePath.replaceAll("//","/") + "\"}").get();
            if(data != null && data.length > 0)
                Files.write(Paths.get(realPath), data, StandardOpenOption.WRITE, StandardOpenOption.CREATE);//<------------TODO: AccessDeniedException (write file)
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void upload(String realPath, String filePath) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void rm(String filePath) {
        try {
            new http.POST().execute("https://api.dropboxapi.com/2/files/delete_v2", "{\"path\":\"" + filePath.replaceAll("//","/") + "\"}", "Content-Type:application/json; charset=utf-8", "Authorization:Bearer " + _token).get();
        } catch(Exception ex) { }
    }

    @Override
    public void mv(String oldPath, String newPath) {//TODO: test
        try {
            new http.POST().execute("https://api.dropboxapi.com/2/files/move_v2", "{\"from_path\":\"" + oldPath.replaceAll("//","/") +
                    "\",\"to_path\":\"" + newPath.replaceAll("//","/") +
                    "\"}", "Content-Type:application/json; charset=utf-8", "Authorization:Bearer " + _token).get();
        } catch(Exception ex) { }
    }

    @Override
    public long size() {
        try {
            String json =  new http.POST().execute("https://api.dropboxapi.com/2/users/get_space_usage", "null", "Content-Type:application/json; charset=utf-8", "Authorization:Bearer " + _token).get();
            return Arrays.stream(json.split("[,]")).filter(J -> J.contains("allocated")).mapToLong(M -> Long.parseLong(M.replace("\"allocated\":","").replaceAll(" ",""))).sum();
        } catch(Exception ex) {
            return 0;
        }
    }

    @Override
    public long free() {
        try {
            String json =  new http.POST().execute("https://api.dropboxapi.com/2/users/get_space_usage", "null", "Content-Type:application/json; charset=utf-8", "Authorization:Bearer " + _token).get();
            return Arrays.stream(json.split("[,]")).filter(J -> J.contains("allocated")|| J.contains("used")).mapToLong(M -> {
                if(M.contains("allocated"))
                    return Long.parseLong(M.replace("\"allocated\":","").replaceAll(" ","").replaceAll("}",""));
                else
                    return -Long.parseLong(M.replace("\"used\":","").replaceAll(" ","").replaceAll("{",""));
            }).sum();
        } catch(Exception ex) {
            return 0;
        }
    }

    @Override
    public String getUserName() {
        return _userName;
    }

    @Override
    public int getIcon() {
        //FF0d2481
        //FFd9f8ff
        return R.drawable.ic_dropbox;
    }
}
