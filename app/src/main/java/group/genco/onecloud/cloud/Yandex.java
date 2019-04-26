package group.genco.onecloud.cloud;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.*;

import group.genco.onecloud.R;
import group.genco.onecloud.http;

import static android.app.Notification.GROUP_ALERT_SUMMARY;
import static android.content.Context.NOTIFICATION_SERVICE;

public class Yandex implements iDrive {
    private Context _context;

    private String _userName = "";
    private String _token;

    private long _size;
    private long _used;

    public Yandex(String accessToken, Context context){
        _context = context;
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
                String downPath = http.get("https://cloud-api.yandex.net:443/v1/disk/resources/download?path=" + paths[0].replaceAll("//","/"), "Authorization:OAuth " + _token);
                downPath = downPath.substring(downPath.indexOf("\"href\":") + 8);
                downPath = downPath.substring(0, downPath.lastIndexOf("\""));

                URL url = new URL(downPath);
                conn = (HttpURLConnection)url.openConnection();
                conn.addRequestProperty("Authorization", "OAuth " + _token);
                conn.setRequestMethod("GET");
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
