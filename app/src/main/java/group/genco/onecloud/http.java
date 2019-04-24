package group.genco.onecloud;

import android.os.AsyncTask;
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

public class http {

    public static class POST extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                for(int i = 2; i < urls.length; ++i)
                    urlConnection.addRequestProperty(urls[i].split(":")[0], urls[i].substring(urls[i].indexOf(":") + 1));
                urlConnection.setRequestMethod("POST");
                urlConnection.getOutputStream().write(urls[1].getBytes(Charset.forName("utf-8")));
                urlConnection.connect();
                //System.out.println("CODE:"+urlConnection.getResponseCode());
                BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                String ret = "";
                String line;
                while((line = in.readLine()) != null) {
                    ret += line + "\n";
                }
                urlConnection.disconnect();
                return ret;
            }catch(Exception ex) {
                ex.printStackTrace();
                return null;
            }
        }
    }

    public static class DELETE extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                for(int i = 1; i < urls.length; ++i)
                    urlConnection.addRequestProperty(urls[i].split(":")[0], urls[i].substring(urls[i].indexOf(":") + 1));
                urlConnection.setRequestMethod("DELETE");
                urlConnection.connect();
                //System.out.println("CODE:"+urlConnection.getResponseCode());
                BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                String ret = "";
                String line;
                while((line = in.readLine()) != null) {
                    ret += line + "\n";
                }
                urlConnection.disconnect();
                return ret;
            }catch(Exception ex) {
                ex.printStackTrace();
                return null;
            }
        }
    }

    public static class GET extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                for(int i = 1; i < urls.length; ++i)
                    urlConnection.addRequestProperty(urls[i].split(":")[0], urls[i].substring(urls[i].indexOf(":") + 1));
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();
                //System.out.println("CODE:"+urlConnection.getResponseCode());
                BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                String ret = "";
                String line;
                while((line = in.readLine()) != null) {
                    ret += line + "\n";
                }
                urlConnection.disconnect();
                return ret;
            }catch(Exception ex) {
                ex.printStackTrace();
                return null;
            }
        }
    }

    public static String get(String... urls) {
        try {
            URL url = new URL(urls[0]);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            for(int i = 1; i < urls.length; ++i)
                urlConnection.addRequestProperty(urls[i].split(":")[0], urls[i].substring(urls[i].indexOf(":") + 1));
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();
            //System.out.println("CODE:"+urlConnection.getResponseCode());
            BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String ret = "";
            String line;
            while((line = in.readLine()) != null) {
                ret += line + "\n";
            }
            urlConnection.disconnect();
            return ret;
        }catch(Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static String delete(String... urls) {
        try {
            URL url = new URL(urls[0]);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            for(int i = 1; i < urls.length; ++i)
                urlConnection.addRequestProperty(urls[i].split(":")[0], urls[i].substring(urls[i].indexOf(":") + 1));
            urlConnection.setRequestMethod("DELETE");
            urlConnection.connect();
            //System.out.println("CODE:"+urlConnection.getResponseCode());
            BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String ret = "";
            String line;
            while((line = in.readLine()) != null) {
                ret += line + "\n";
            }
            urlConnection.disconnect();
            return ret;
        }catch(Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static String post(String... urls) {
        try {
            URL url = new URL(urls[0]);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            for(int i = 2; i < urls.length; ++i)
                urlConnection.addRequestProperty(urls[i].split(":")[0], urls[i].substring(urls[i].indexOf(":") + 1));
            urlConnection.setRequestMethod("POST");
            urlConnection.getOutputStream().write(urls[1].getBytes(Charset.forName("utf-8")));
            urlConnection.connect();
            //System.out.println("CODE:"+urlConnection.getResponseCode());
            BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String ret = "";
            String line;
            while((line = in.readLine()) != null) {
                ret += line + "\n";
            }
            urlConnection.disconnect();
            return ret;
        }catch(Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static class POST2 extends AsyncTask<String, String, byte[]> {//For download file

        @Override
        protected byte[] doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                for (int i = 2; i < urls.length; ++i)
                    urlConnection.addRequestProperty(urls[i].split(":")[0], urls[i].substring(urls[i].indexOf(":") + 1));
                urlConnection.setRequestMethod("POST");
                urlConnection.getOutputStream().write(urls[1].getBytes(Charset.forName("utf-8")));
                urlConnection.connect();

                ByteArrayOutputStream ret = new ByteArrayOutputStream();
                BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                in.lines().forEach(L -> {
                    try {
                        ret.write(L.getBytes());
                        ret.write(13);//newLine
                    } catch (IOException e) {}
                });

                urlConnection.disconnect();
                return ret.toByteArray();
            } catch(FileNotFoundException fnfe) {
                return null;
            } catch(Exception ex) {
                ex.printStackTrace();
                return null;
            }
        }
    }
}
