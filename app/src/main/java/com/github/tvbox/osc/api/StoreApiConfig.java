package com.github.tvbox.osc.api;

import android.content.Context;
import android.widget.Toast;

import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.LOG;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.model.Response;
import com.orhanobut.hawk.Hawk;

import java.util.ArrayList;
import java.util.HashMap;

public class StoreApiConfig {
    private static StoreApiConfig instance;

    public static StoreApiConfig get() {
        if (instance == null) {
            synchronized (StoreApiConfig.class) {
                if (instance == null) {
                    instance = new StoreApiConfig();
                }
            }
        }
        return instance;
    }

    public void MyRequest(String url, StoreApiConfigCallback callback) {
        OkGo.<String>get(url).headers("User-Agent", "okhttp/3.15").headers("Accept",
                "text/html," + "application/xhtml+xml,application/xml;q=0.9,image/avif," +
                        "image/webp,image/apng," + "*/*;q=0.8,application/signed-exchange;v=b3;" +
                        "q=0.9").execute(new AbsCallback<String>() {
            @Override
            public void onSuccess(Response<String> response) {
                callback.success(response.body());
            }

            @Override
            public String convertResponse(okhttp3.Response response) throws Throwable {
                assert response.body() != null;
                return response.body().string();
            }
        });
    }


    public void Subscribe(Context context) {

        Toast.makeText(context, "开始获取订阅，网络慢的话可能需要较长时间！！！", Toast.LENGTH_SHORT).show();

        // 获取多仓地址
        HashMap<String, String> storeMap = Hawk.get(HawkConfig.STORE_API_MAP, new HashMap<>());
        ArrayList<String> storeNameHistory = Hawk.get(HawkConfig.STORE_API_NAME_HISTORY, new ArrayList<>());
        String storeName = Hawk.get(HawkConfig.STORE_API_NAME, "仓库名称");

        if (storeMap.isEmpty()) {
            Toast.makeText(context, "仓库为空，使用默认仓库", Toast.LENGTH_SHORT).show();
            String name = "仓库名称";
            String sotreApi = "";
            storeMap.put(name, sotreApi);
            storeNameHistory.add(name);
            Hawk.put(HawkConfig.STORE_API_NAME_HISTORY, storeNameHistory);
            Hawk.put(HawkConfig.STORE_API_NAME, name);
            Hawk.put(HawkConfig.STORE_API_MAP, storeMap);
            Hawk.put(HawkConfig.STORE_API, sotreApi);
        }

        String storeUrl = storeMap.get(storeName);

        LOG.i("订阅仓库地址：" + storeUrl);

        // 处理多仓获取多节点
        StoreApiConfig.get().MyRequest(storeUrl, sourceJson -> {

            JsonObject json = new Gson().fromJson(sourceJson, JsonObject.class);

            if (null == json.get("urls")) {
                // 仓库链接，先获取多源，再获取多配置

                JsonObject infoJson = new Gson().fromJson(sourceJson, JsonObject.class);
                JsonArray storeHouses = infoJson.get("storeHouse").getAsJsonArray();
                JsonObject defStoreHouse = storeHouses.get(0).getAsJsonObject();

                // 多仓线路作为历史添加
                // 默认添加30条，多出的直接放弃
                for (int i = 0; i < storeHouses.size(); i++) {

                    JsonObject storeHouse = infoJson.get("storeHouse").getAsJsonArray().get(i).getAsJsonObject();

                    String sourceName = storeHouse.get("sourceName").getAsString();
                    String sourceUrl = storeHouse.get("sourceUrl").getAsString();

                    if (!storeMap.containsValue(sourceUrl)) {
                        storeMap.put(sourceName, sourceUrl);
                        storeNameHistory.add(sourceName);
                    }
                    // if (history.size() > 30)
                    //     // history.remove(30);
                    //     break;
                    if (0 == i) {

                        String name = defStoreHouse.get("sourceName").getAsString();
                        String url = defStoreHouse.get("sourceUrl").getAsString();
                        Toast.makeText(context, name, Toast.LENGTH_SHORT).show();

                        Hawk.put(HawkConfig.STORE_API, url);
                        Hawk.put(HawkConfig.STORE_API_NAME, name);

                        // 配置默认配置线路
                        StoreApiConfig.get().MyRequest(url, urlsJson -> {
                            String result = MutiUrl(urlsJson);
                            Toast.makeText(context, result, Toast.LENGTH_SHORT).show();
                        });
                    }


                }
                Toast.makeText(context, "多仓订阅结束，到多源历史中切换！！！", Toast.LENGTH_SHORT).show();
            } else {
                // 单源链接，直接请求
                // 获取单源中的配置线路

                String result = MutiUrl(sourceJson);
                Toast.makeText(context, "单源链接，" + result, Toast.LENGTH_SHORT).show();
                String currentStoreName = Hawk.get(HawkConfig.STORE_API_NAME, "");
                if (!storeNameHistory.contains(currentStoreName)) {
                    storeNameHistory.add(0, currentStoreName);
                }

            }

            Hawk.put(HawkConfig.STORE_API_MAP, storeMap);
            Hawk.put(HawkConfig.STORE_API_NAME_HISTORY, storeNameHistory);
        });

    }

    private String MutiUrl(String urlsJson) {

        // ArrayList<String> history = Hawk.get(HawkConfig.API_NAME_HISTORY, new ArrayList<>());
        // HashMap<String, String> map = Hawk.get(HawkConfig.API_MAP, new HashMap<>());

        ArrayList<String> history = new ArrayList<>();
        HashMap<String, String> map = new HashMap<>();

        history.add(Hawk.get(HawkConfig.API_NAME));
        map.put(Hawk.get(HawkConfig.API_NAME), Hawk.get(HawkConfig.API_URL));

        JsonObject urlsObject = new Gson().fromJson(urlsJson, JsonObject.class);

        if (null == urlsObject.get("urls"))
            return "订阅出错，失败！！！";

        JsonArray urlsObjects = urlsObject.get("urls").getAsJsonArray();


        for (JsonElement element : urlsObjects) {
            JsonObject obj = element.getAsJsonObject();
            String name = obj.get("name").getAsString();
            String url = obj.get("url").getAsString();
            if (!map.containsValue(url)) {
                history.add(name);
                map.put(name, url);
            }
        }
//        Hawk.put(HawkConfig.API_NAME, history.get(0));
//        Hawk.put(HawkConfig.API_URL, map.get(history.get(0)));
        Hawk.put(HawkConfig.API_NAME_HISTORY, history);
        Hawk.put(HawkConfig.API_MAP, map);
        return "订阅结束，点击线路可切换";
    }

    public interface StoreApiConfigCallback {
        void success(String json);
    }
}
