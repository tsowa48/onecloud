package group.genco.onecloud.cloud;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.widget.ProgressBar;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import group.genco.onecloud.MainActivity;
import group.genco.onecloud.R;
import group.genco.onecloud.http;

import static android.app.Notification.GROUP_ALERT_SUMMARY;
import static android.content.Context.NOTIFICATION_SERVICE;
import static android.content.DialogInterface.*;

public final class DropBox implements iDrive {
    private Context _context;

    private String _userName = "";
    private String _token;

    public DropBox(String accessToken, Context context) {
        _context = context;
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
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(_context)//<------------ TODO: deprecated
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(filePath.substring(filePath.lastIndexOf("/") + 1))
                .setContentText("Скачивание файла");
        builder.setChannelId("group.genco.onecloud");
        NotificationChannel channel = new NotificationChannel("group.genco.onecloud", "OneCloud", NotificationManager.IMPORTANCE_LOW);
        channel.setSound(null,null);
        builder.setGroupAlertBehavior(GROUP_ALERT_SUMMARY).setGroup("group.genco.onecloud").setGroupSummary(false);
        builder.setProgress(0, 0, true);
        builder.setSound(null);
        NotificationManager manager = (NotificationManager)_context.getSystemService(NOTIFICATION_SERVICE);
        manager.createNotificationChannel(channel);

        DownloadTask dt = new DownloadTask(manager, builder);

        manager.notify(1, builder.build());

        dt.execute(filePath, realPath);
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


    private class DownloadTask extends AsyncTask<String, Integer, Boolean> {

        private NotificationCompat.Builder _builder;
        private NotificationManager _manager;

        public DownloadTask(NotificationManager manager, NotificationCompat.Builder builder) {
            _manager = manager;
            _builder = builder;
        }

        @Override
        protected Boolean doInBackground(String... paths) {
            InputStream in = null;
            OutputStream os = null;
            HttpURLConnection conn = null;
            try {
                URL url = new URL("https://content.dropboxapi.com/2/files/download");
                conn = (HttpURLConnection)url.openConnection();
                conn.addRequestProperty("Content-Type", "application/octet-stream");
                conn.addRequestProperty("Content-Length", "0");
                conn.addRequestProperty("Authorization", "Bearer " + _token);
                conn.addRequestProperty("Dropbox-API-Arg", "{\"path\":\"" + paths[0].replaceAll("//","/") + "\"}");
                conn.setRequestMethod("POST");
                conn.getOutputStream().write("".getBytes(Charset.forName("utf-8")));
                conn.connect();
                if(conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    //BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    //br.lines().forEach(X -> System.err.println(X));
                    //br.close();
                    return false;
                }
                int fileSize = conn.getContentLength();
                in = conn.getInputStream();
                os = new FileOutputStream(paths[1]);
                byte[] data = new byte[4096];
                long total = 0;
                int count;
                while((count = in.read(data)) != -1) {
                    if(isCancelled()) {
                        in.close();
                        return false;
                    }
                    total += count;
                    if(fileSize > 0)
                        publishProgress((int)total * 100 / fileSize);
                    os.write(data,0, count);
                }
            } catch(Exception ex) {
                ex.printStackTrace();
                return false;
            } finally {
                try {
                    if (os != null)
                        os.close();
                    if(in != null)
                        in.close();
                } catch(Exception ex1){}
                if(conn != null)
                    conn.disconnect();
                return true;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);

            _builder.setContentText(progress[0].toString() + "%").setProgress(100, progress[0], false);
            _manager.notify(1, _builder.build());
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            _builder.setContentText("Загрузка завершена").setVisibility(3000).setAutoCancel(true);
            _manager.notify(1, _builder.build());
        }
    }
}
