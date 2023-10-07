package com.github.tvbox.osc.util;

import android.os.Environment;
import android.text.TextUtils;
import android.util.Base64;

import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.server.ControlManager;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.model.HttpHeaders;
import com.orhanobut.hawk.Hawk;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileUtils {

    public static File open(String str) {
        return new File(App.getInstance().getExternalCacheDir().getAbsolutePath() + "/qjscache_" + str + ".js");
    }

    public static boolean writeSimple(byte[] data, File dst) {
        try {
            if (dst.exists())
                dst.delete();
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(dst));
            bos.write(data);
            bos.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static byte[] readSimple(File src) {
        try {
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(src));
            int len = bis.available();
            byte[] data = new byte[len];
            bis.read(data);
            bis.close();
            return data;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    public static String readFileToString(String path, String charsetName) {
        // 定义返回结果
        StringBuilder jsonString = new StringBuilder();

        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(path), charsetName));// 读取文件
            String thisLine;
            while ((thisLine = in.readLine()) != null) {
                jsonString.append(thisLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException el) {
                }
            }
        }
        // 返回拼接好的JSON String
        return jsonString.toString();
    }

    public static void copyFile(File source, File dest) throws IOException {
        try (InputStream is = new FileInputStream(source); OutputStream os = new FileOutputStream(dest)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        }
    }
    public static void recursiveDelete(File file) {
        try {
            if (!file.exists())
                return;
            if (file.isDirectory()) {
                for (File f : file.listFiles()) {
                    recursiveDelete(f);
                }
            }
            file.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static final Pattern URLJOIN = Pattern.compile("^http.*\\.(js|txt|json|m3u)$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    public static String get(String str, Object o) {
        return get(str, null);
    }
    public static String loadModule(String name) {
        try {
            if (name.contains("cheerio.min.js")) {
                name = "cheerio.min.js";
            } else if (name.contains("crypto-js.js")) {
                name = "crypto-js.js";
            } else if (name.contains("dayjs.min.js")) {
                name = "dayjs.min.js";
            } else if (name.contains("uri.min.js")) {
                name = "uri.min.js";
            } else if (name.contains("underscore-esm-min.js")) {
                name = "underscore-esm-min.js";
            } else if (name.contains("similarity.js")) {
                name = "similarity.js";
            } else if (name.contains("gbk.js")) {
                name = "gbk.js";
            } else if (name.contains("模板.js")) {
                name = "模板.js";
            } else if (name.contains("drpy2.min.js")) {
                name = "drpy2.min.js";
            } else if (name.contains("drpy.min.js")) {
                name = "drpy.min.js";
            } else if (name.contains("alist.min.js")) {
                name = "alist.min.js";
            } else if (name.contains("mod.js")) {
                name = "mod.js";
            } else if (name.contains("sortName.js")) {
                name = "sortName.js";
            } else if (name.contains("live2cms.js")) {
                name = "live2cms.js";
            } else if (name.contains("cat.js")) {
                name = "cat.js";
            }
           /* if (name.startsWith("http://") || name.startsWith("https://")) {
                String cache = getCache(MD5.encode(name));
                if (JSUtils.isEmpty(cache)) {
                    String netStr = OkHttpUtil.get(name);
                    if (!TextUtils.isEmpty(netStr)) {
                        setCache(604800, MD5.encode(name), netStr);
                    }
                    return netStr;
                }
                return cache;
            } else if (name.startsWith("assets://")) {
                return getAsOpen(name.substring(9));
            } else if (isAsFile(name, "js/lib")) {
                return getAsOpen("js/lib/" + name);
            } else if (name.startsWith("file://")) {
                return OkHttpUtil.get(ControlManager.get().getAddress(true) + "file/" + name.replace("file:///", "").replace("file://", ""));
            } else if (name.startsWith("clan://localhost/")) {
                return OkHttpUtil.get(ControlManager.get().getAddress(true) + "file/" + name.replace("clan://localhost/", ""));
            } else if (name.startsWith("clan://")) {
                String substring = name.substring(7);
                int indexOf = substring.indexOf(47);
                return OkHttpUtil.get("http://" + substring.substring(0, indexOf) + "/file/" + substring.substring(indexOf + 1));
            }

            */
            Matcher m = URLJOIN.matcher(name);
            if (m.find()) {
                if (!Hawk.get(HawkConfig.DEBUG_OPEN, false)) {
                    String cache = getCache(MD5.encode(name));
                    if (StringUtils.isEmpty(cache)) {
                        String netStr = get(name, null);
                        if (!TextUtils.isEmpty(netStr)) {
                            setCache(604800, MD5.encode(name), netStr);
                        }
                        return netStr;
                    }
                    return cache;
                } else {
                    return get(name, null);
                }
            } else if (name.startsWith("assets://")) {
                return getAsOpen(name.substring(9));
            } else if (isAsFile(name, "js/lib")) {
                return getAsOpen("js/lib/" + name);
            } else if (name.startsWith("file://")) {
                return get(ControlManager.get()
                        .getAddress(true) + "file/" + name.replace("file:///", "")
                        .replace("file://", ""), null);
            } else if (name.startsWith("clan://localhost/")) {
                return get(ControlManager.get()
                        .getAddress(true) + "file/" + name.replace("clan://localhost/", ""), null);
            } else if (name.startsWith("clan://")) {
                String substring = name.substring(7);
                int indexOf = substring.indexOf(47);
                return get("http://" + substring.substring(0, indexOf) + "/file/" + substring.substring(indexOf + 1), null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return name;
        }
        return name;
    }

    public static boolean isAsFile(String name, String path) {
        try {
            for (String fname : App.getInstance().getAssets().list(path)) {
                if (fname.equals(name.trim())) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String getAsOpen(String name) {
        try {
            InputStream is = App.getInstance().getAssets().open(name);
            byte[] data = new byte[is.available()];
            is.read(data);
            return new String(data, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String getCache(String name) {
        try {
            String code = "";
            File file = open(name);
            if (file.exists()) {
                code = new String(readSimple(file));
            }
            if (TextUtils.isEmpty(code)) {
                return "";
            }
            JsonObject asJsonObject = (new Gson()
                    .fromJson(code, JsonObject.class))
                    .getAsJsonObject();
            if (((long) asJsonObject.get("expires")
                    .getAsInt()) > System.currentTimeMillis() / 1000) {
                return new String(Base64.decode(asJsonObject.get("data")
                        .getAsString(), Base64.URL_SAFE));
            }
            recursiveDelete(open(name));
            return "";
        } catch (Exception e4) {
            return "";
        }
    }
    public static byte[] getCacheByte(String name) {
        try {
            File file = open("B_" + name);
            if (file.exists()) {
                return readSimple(file);
            }
            return null;
        } catch (Exception e4) {
            return null;
        }
    }
    public static void setCacheByte(String name, byte[] data) {
        try {
            writeSimple(Base64.encode(data, Base64.URL_SAFE), open("B_" + name));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static String get(String str) {
        return get(str, null);
    }
    public static String get(String str, Map<String, String> headerMap) {
        try {
            HttpHeaders h = new HttpHeaders();
            if (headerMap != null) {
                for (String key : headerMap.keySet()) {
                    h.put(key, headerMap.get(key));
                }
                return OkGo.<String>get(str).headers(h).execute().body().string();
            } else {
                return OkGo.<String>get(str).headers("User-Agent", str.startsWith("https://gitcode.net/") ? UA.random() : "okhttp/3.15").execute().body().string();
            }

        } catch (IOException e) {
            return "";
        }
    }

    public static void setCache(int time, String name, String data) {
        try {
            JSONObject jSONObject = new JSONObject();
            jSONObject.put("expires", (int)(time + (System.currentTimeMillis() / 1000)));
            jSONObject.put("data", Base64.encodeToString(data.getBytes(), Base64.URL_SAFE));
            writeSimple(jSONObject.toString()
                    .getBytes(), open(name));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static File getCacheDir() {
        return App.getInstance().getCacheDir();
    }

    public static File getExternalCacheDir() {
        return App.getInstance().getExternalCacheDir();
    }

    public static String getExternalCachePath() {
        return getExternalCacheDir().getAbsolutePath();
    }

    public static String getCachePath() {
        return getCacheDir().getAbsolutePath();
    }

    public static void cleanPlayerCache() {
        String thunderCachePath = getCachePath() + "/thunder/";
        File thunderCacheDir = new File(thunderCachePath);
        try {
            if (thunderCacheDir.exists()) recursiveDelete(thunderCacheDir);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String ijkCachePath = getExternalCachePath() + "/ijkcaches/";
        File ijkCacheDir = new File(ijkCachePath);
        try {
            if (ijkCacheDir.exists()) recursiveDelete(ijkCacheDir);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String jpaliCachePath = getExternalCachePath() + "/jpali/Downloads/";
        File jpaliCacheDir = new File(jpaliCachePath);
        try {
            if (jpaliCacheDir.exists()) recursiveDelete(jpaliCacheDir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getFileNameWithoutExt(String filePath) {
        if (TextUtils.isEmpty(filePath)) return "";
        String fileName = filePath;
        int p = fileName.lastIndexOf(File.separatorChar);
        if (p != -1) {
            fileName = fileName.substring(p + 1);
        }
        p = fileName.indexOf('.');
        if (p != -1) {
            fileName = fileName.substring(0, p);
        }
        return fileName;
    }

    public static String getRootPath() {
        return Environment.getExternalStorageDirectory()
                .getAbsolutePath();
    }
}