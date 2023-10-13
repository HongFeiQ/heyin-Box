package com.github.tvbox.osc.util.js;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.Keep;

import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.server.ControlManager;
import com.github.tvbox.osc.util.FileUtils;
import com.github.tvbox.osc.util.LOG;
import com.github.tvbox.osc.util.OkGoHelper;
import com.github.tvbox.osc.util.StringUtils;
import com.google.gson.Gson;
import com.lzy.okgo.OkGo;
import com.whl.quickjs.wrapper.JSArray;
import com.whl.quickjs.wrapper.JSCallFunction;
import com.whl.quickjs.wrapper.JSMethod;
import com.whl.quickjs.wrapper.JSModule;
import com.whl.quickjs.wrapper.JSObject;
import com.whl.quickjs.wrapper.JSUtils;
import com.whl.quickjs.wrapper.QuickJSContext;
import com.whl.quickjs.wrapper.UriUtil;

import org.json.JSONObject;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/** @noinspection ALL*/
public class JSEngine {
    private static final String TAG = "JSEngine";
    static ConcurrentHashMap<String, String> moduleCache = new ConcurrentHashMap<>();
    private static JSEngine instance = null;
    private ConcurrentHashMap<String, JSThread> threads = new ConcurrentHashMap<>();

    @SuppressLint("NewApi")
    static String loadModule(String name) {
        try {
            String cache = moduleCache.get(name);
            if (cache != null && !cache.isEmpty())
                return cache;
            String content = null;
            if (name.startsWith("http://") || name.startsWith("https://")) {
                content = OkGo.<String>get(name).headers("User-Agent", "Mozilla/5.0").execute().body().string();
            }
            if (name.startsWith("assets://")) {
                InputStream is = App.getInstance().getAssets().open(name.substring(9));
                byte[] data = new byte[is.available()];
                is.read(data);
                content = new String(data, StandardCharsets.UTF_8);
            }
            if (content != null && !content.isEmpty()) {
                moduleCache.put(name, content);
                return content;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static JSEngine getInstance() {
        if (instance == null)
            instance = new JSEngine();
        return instance;
    }

    public void create() {
        System.loadLibrary("hlhwan");
    }


    public JSThread getJSThread(Class<?> cls) {
        byte count = Byte.MAX_VALUE;
        JSThread thread = null;
        for (String name : threads.keySet()) {
            JSThread jsThread = threads.get(name);
            if (jsThread != null && jsThread.retain < count && jsThread.retain < 1) {
                thread = jsThread;
                count = jsThread.retain;
            }
        }
        if (thread == null) {
            Object[] objects = new Object[2];
            String name = "QuickJS-Thread-" + threads.size();
            HandlerThread handlerThread = new HandlerThread(name + "-0");
            handlerThread.start();
            Handler handler = new Handler(handlerThread.getLooper());
            handler.post(() -> {
                objects[0] = QuickJSContext.create();
                synchronized (objects) {
                    objects[1] = true;
                    objects.notify();
                }
            });
            synchronized (objects) {
                try {
                    if (objects[1] == null) {
                        objects.wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            JSModule.setModuleLoader(new JSModule.ModuleLoader() {
                @Override
                public String convertModuleName(String moduleBaseName, String moduleName) {
                    return UriUtil.resolve(moduleBaseName, moduleName);
                }

                @Override
                public String getModuleScript(String moduleName) {
                    return FileUtils.loadModule(moduleName);
                }
            });
            JSThread jsThread = new JSThread();
            jsThread.handler = handler;
            jsThread.thread = handlerThread;
            jsThread.jsContext = (QuickJSContext) objects[0];
            jsThread.retain = 0;
            thread = jsThread;
            try {
                jsThread.postVoid((ctx, globalThis) -> {
                    jsThread.init(cls);
                    return null;
                });
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
            threads.put(name, jsThread);
        }
        thread.retain++;
        String name = thread.thread.getName();
        name = name.substring(0, name.lastIndexOf("-") + 1) + thread.retain;
        thread.thread.setName(name);
        return thread;
    }

    public void destroy() {
        for (String name : threads.keySet()) {
            JSThread jsThread = threads.get(name);
            if (jsThread != null) {
                if (jsThread.thread != null) {
                    jsThread.thread.interrupt();
                }
                if (jsThread.jsContext != null) {
                    jsThread.jsContext.destroy();
                }
            }
        }
        threads.clear();
    }

    public void stopAll() {
        for (String name : threads.keySet()) {
            JSThread jsThread = threads.get(name);
            if (jsThread != null) {
                jsThread.cancelByTag("js_okhttp_tag");
                if (jsThread.handler != null) {
                    jsThread.handler.removeCallbacksAndMessages(null);
                }
            }
        }
    }

    public JSThread getJSThread() {
        return null;
    }


    public interface Event<T> {
        T run(QuickJSContext ctx, JSObject globalThis);
    }


    //////////////////////////////////////////////////////////


    public class JSThread {
        private static final String TAG = "JSEngine";
        public QuickJSContext jsContext;
        public Handler handler;
        public Thread thread;
        private OkHttpClient okHttpClient;
        private volatile byte retain;

        public void init(Class<?> cls) {
            initProperty();
            initConsole();
            if (cls != null) {
                getGlobalObj().setProperty("jsapi", (JSCallFunction) getJsContext().createNewJSObject());
                Class<?>[] classes = cls.getDeclaredClasses();
                JSObject apiObj = getGlobalObj().getJSObject("jsapi");
                LOG.e("cls", "" + classes.length);
                for (Class<?> classe : classes) {
                    Object javaObj = null;
                    try {
                        javaObj = classe.getDeclaredConstructor(cls).newInstance(cls.getDeclaredConstructor(QuickJSContext.class).newInstance(getJsContext()));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (javaObj == null) {
                        throw new NullPointerException("The JavaObj cannot be null. An error occurred in newInstance!");
                    }
                    JSObject claObj = getJsContext().createNewJSObject();
                    Method[] methods = classe.getDeclaredMethods();
                    for (Method method : methods) {
                        if (method.isAnnotationPresent(JSMethod.class)) {
                            Object finalJavaObj = javaObj;
                            claObj.setProperty(method.getName(), args -> {
                                try {
                                    return method.invoke(finalJavaObj, args);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                return null;
                            });
                        }
                    }
                    apiObj.setProperty(classe.getSimpleName(), (JSCallFunction) claObj);
                    LOG.e("cls", classe.getSimpleName());
                }
            }
        }

        @Keep
        public String getProxy(boolean local) {
            return ControlManager.get().getAddress(local) + "proxy?do=js";
        }

        @Keep
        public String pdfh(String html, String rule) {
            try {
                return HtmlParser.parseDomForUrl(html, rule, "");
            } catch (Exception th) {
                LOG.e(th);
                return "";
            }
        }

        @Keep
        public Object pdfa(String html, String rule) {
            try {
                return getJsContext().parse(new Gson().toJson(HtmlParser.parseDomForArray(html, rule)));
            } catch (Exception th) {
                LOG.e(th);
                return getJsContext().createNewJSArray();
            }
        }

        @Keep
        public Object pdfl(String html, String p1, String list_text, String list_url, String urlKey) {
            try {
                return getJsContext().parse(new Gson().toJson(HtmlParser.parseDomForList(html, p1, list_text, list_url, urlKey)));
            } catch (Exception th) {
                LOG.e(th);
                return getJsContext().createNewJSArray();
            }
        }

        @Keep
        public String pd(String html, String rule, String urlKey) {
            try {
                return HtmlParser.parseDomForUrl(html, rule, urlKey);
            } catch (Exception th) {
                LOG.e(th);
                return "";
            }
        }

        @Keep
        public Object req(String url, Object o2) {
            try {
                JSONObject opt = ((JSObject) o2).toJSONObject();
                //JSONObject opt = new JSONObject(jsContext.stringify((JSObject) o2));
                Headers.Builder headerBuilder = new Headers.Builder();
                JSONObject optHeader = opt.optJSONObject("headers");
                if (optHeader != null) {
                    Iterator<String> hdKeys = optHeader.keys();
                    while (hdKeys.hasNext()) {
                        String k = hdKeys.next();
                        String v = optHeader.optString(k);
                        headerBuilder.add(k, v);
                    }
                }
                Headers headers = headerBuilder.build();
                Request.Builder requestBuilder = new Request.Builder().url(url).headers(headers);
                requestBuilder.tag("js_okhttp_tag");
                Request request;
                String contentType = null;
                if (!StringUtils.isEmpty(headers.get("content-type"))) {
                    contentType = headers.get("Content-Type");
                }
                String method = opt.optString("method").toLowerCase();
                String charset = "utf-8";
                if (contentType != null && contentType.split("charset=").length > 1) {
                    charset = contentType.split("charset=")[1];
                }

                if (method.equals("post")) {
                    RequestBody body = null;
                    String data = opt.optString("data", "").trim();
                    if (!StringUtils.isEmpty(data)) {
                        body = RequestBody.create(MediaType.parse("application/json"), data);
                    }
                    if (body == null) {
                        String dataBody = opt.optString("body", "").trim();
                        if (!StringUtils.isEmpty(dataBody) && contentType != null) {
                            body = RequestBody.create(MediaType.parse(contentType), opt.optString("body", ""));
                        }
                    }
                    if (body == null) {
                        body = RequestBody.create(null, "");
                    }
                    request = requestBuilder.post(body).build();
                } else if (method.equals("header")) {
                    request = requestBuilder.head().build();
                } else {
                    request = requestBuilder.get().build();
                }
                okHttpClient = opt.optInt("redirect", 1) == 1 ? OkGoHelper.getDefaultClient() : OkGoHelper.getNoRedirectClient();
                OkHttpClient.Builder builder = okHttpClient.newBuilder();
                if (opt.has("timeout")) {
                    long timeout = opt.optInt("timeout");
                    builder.readTimeout(timeout, TimeUnit.MILLISECONDS);
                    builder.writeTimeout(timeout, TimeUnit.MILLISECONDS);
                    builder.connectTimeout(timeout, TimeUnit.MILLISECONDS);
                }
                Response response = builder.build().newCall(request).execute();
                JSObject jsObject = jsContext.createNewJSObject();
                JSObject resHeader = jsContext.createNewJSObject();

                for (Map.Entry<String, List<String>> entry : response.headers().toMultimap().entrySet()) {
                    if (entry.getValue().size() == 1) {
                        resHeader.setProperty(entry.getKey(), entry.getValue().get(0));
                    }
                    if (entry.getValue().size() >= 2) {
                        JSArray array = jsContext.createNewJSArray();
                        List<String> items = entry.getValue();
                        if (items == null || items.isEmpty()) return array;
                        for (int i = 0; i < items.size(); i++) {
                            array.set(items.get(i), i);
                        }
                        resHeader.setProperty(entry.getKey(), (JSCallFunction) array);
                    }
                    ;
                }

                jsObject.setProperty("headers", (JSCallFunction) resHeader);
                jsObject.setProperty("code", String.valueOf(response.code()));

                int returnBuffer = opt.optInt("buffer", 0);
                if (returnBuffer == 1) {
                    JSArray array = jsContext.createNewJSArray();
                    byte[] bytes = response.body().bytes();
                    for (int i = 0; i < bytes.length; i++) {
                        array.set(bytes[i], i);
                    }
                    jsObject.setProperty("content", (JSCallFunction) array);
                } else if (returnBuffer == 2) {
                    jsObject.setProperty("content", Base64.encodeToString(response.body().bytes(), Base64.DEFAULT));
                } else {
                    String res;
                    byte[] responseBytes = UTF8BOMFighter.removeUTF8BOM(response.body().bytes());
                    res = new String(responseBytes, charset);
                    jsObject.setProperty("content", res);
                }
                return jsObject;
            } catch (Throwable throwable) {
                LOG.e(throwable);
                return "";
            }
        }

        public void cancelByTag(Object tag) {
            try {
                if (okHttpClient != null) {
                    for (Call call : okHttpClient.dispatcher().queuedCalls()) {
                        if (tag.equals(call.request().tag())) {
                            call.cancel();
                        }
                    }
                    for (Call call : okHttpClient.dispatcher().runningCalls()) {
                        if (tag.equals(call.request().tag())) {
                            call.cancel();
                        }
                    }
                }
                OkGo.getInstance().cancelTag(tag);
            } catch (Exception e) {
                LOG.e(e);
            }
        }

        public QuickJSContext getJsContext() {
            return jsContext;
        }

        public JSObject getGlobalObj() {
            return jsContext.getGlobalObject();
        }

        public <T> T post(Event<T> event) throws Throwable {
            if ((thread != null && thread.isInterrupted())) {
                Log.e("QuickJS", "QuickJS is released");
                return null;
            }
            if (Thread.currentThread() == thread) {
                return event.run(jsContext, getGlobalObj());
            }
            if (handler == null) {
                return event.run(jsContext, getGlobalObj());
            }
            Object[] result = new Object[2];
            RuntimeException[] errors = new RuntimeException[1];
            handler.post(() -> {
                try {
                    result[0] = event.run(jsContext, getGlobalObj());
                } catch (RuntimeException e) {
                    errors[0] = e;
                }
                synchronized (result) {
                    result[1] = true;
                    result.notifyAll();
                }
            });
            synchronized (result) {
                try {
                    if (result[1] == null) {
                        result.wait();
                    }
                } catch (InterruptedException e) {
                    LOG.e(e);
                }
            }
            if (errors[0] != null) {
                LOG.e(errors[0]);
                throw errors[0];
            }
            return (T) result[0];
        }

        public void postVoid(Event<Void> event) throws Throwable {
            postVoid(event, true);
        }

        public void postVoid(Event<Void> event, boolean block) throws Throwable {
            if ((thread != null && thread.isInterrupted())) {
                Log.e("QuickJS", "QuickJS is released");
                return;
            }
            if (Thread.currentThread() == thread) {
                event.run(jsContext, getGlobalObj());
                return;
            }
            if (handler == null) {
                event.run(jsContext, getGlobalObj());
                return;
            }
            Object[] result = new Object[2];
            RuntimeException[] errors = new RuntimeException[1];
            handler.post(() -> {
                try {
                    event.run(jsContext, getGlobalObj());
                } catch (RuntimeException e) {
                    errors[0] = e;
                }
                if (block) {
                    synchronized (result) {
                        result[1] = true;
                        result.notifyAll();
                    }
                }
            });
            if (block) {
                synchronized (result) {
                    try {
                        if (result[1] == null) {
                            result.wait();
                        }
                    } catch (InterruptedException e) {
                        LOG.e(e);
                    }
                }
                if (errors[0] != null) {
                    LOG.e(errors[0]);
                    throw errors[0];
                }
            }
        }

        private void initProperty() {
            Method[] methods = this.getClass().getMethods();
            for (Method method : methods) {
                JSMethod an = method.getAnnotation(JSMethod.class);
                if (an == null) continue;
                String functionName = method.getName();

                getGlobalObj().setProperty(functionName, args -> {
                    try {
                        return method.invoke(this, args);
                    } catch (Exception e) {
                        LOG.e(e);
                        throw new RuntimeException(e);
                    }
                });

                if (JSUtils.isNotEmpty(an.alias())) {
                    getJsContext().evaluate("var " + an.alias() + " = " + functionName + ";\n");
                }
            }
        }

        public void init() {
            initConsole();
            initOkHttp();
            initLocalStorage();
        }

        public String joinUrl(String parent, String child) {
            return HtmlParser.joinUrl(parent, child);
        }

        @Keep
        @JSMethod(alias = "pdfh")
        public String parseDomForHtml(String html, String rule) {
            try {
                return HtmlParser.parseDomForUrl(html, rule, "");
            } catch (Exception th) {
                LOG.e(th);
                return "";
            }
        }

        @Keep
        @JSMethod(alias = "pdfa")
        public Object parseDomForArray(String html, String rule) {
            try {
                return getJsContext().parse(new Gson().toJson(HtmlParser.parseDomForArray(html, rule)));
            } catch (Exception th) {
                LOG.e(th);
                return getJsContext().createNewJSArray();
            }
        }

        @Keep
        @JSMethod(alias = "pdfl")
        public Object parseDomForList(String html, String p1, String list_text, String list_url, String urlKey) {
            try {
                return getJsContext().parse(new Gson().toJson(HtmlParser.parseDomForList(html, p1, list_text, list_url, urlKey)));
            } catch (Exception th) {
                LOG.e(th);
                return getJsContext().createNewJSArray();
            }
        }

        @Keep
        @JSMethod(alias = "pd")
        public String parseDom(String html, String rule, String urlKey) {
            try {
                return HtmlParser.parseDomForUrl(html, rule, urlKey);
            } catch (Exception th) {
                LOG.e(th);
                return "";
            }
        }

        @Keep
        @JSMethod(alias = "req")
        public Object ajax(String url, Object o2) {
            try {
                JSONObject opt = ((JSObject) o2).toJSONObject();
                Headers.Builder headerBuilder = new Headers.Builder();
                JSONObject optHeader = opt.optJSONObject("headers");
                if (optHeader != null) {
                    Iterator<String> hdKeys = optHeader.keys();
                    while (hdKeys.hasNext()) {
                        String k = hdKeys.next();
                        String v = optHeader.optString(k);
                        headerBuilder.add(k, v);
                    }
                }
                Headers headers = headerBuilder.build();
                Request.Builder requestBuilder = new Request.Builder().url(url).headers(headers);
                requestBuilder.tag("js_okhttp_tag");
                Request request;
                String contentType = null;
                if (!JSUtils.isEmpty(headers.get("content-type"))) {
                    contentType = headers.get("Content-Type");
                }
                String method = opt.optString("method").toLowerCase();
                String charset = "utf-8";
                if (contentType != null && contentType.split("charset=").length > 1) {
                    charset = contentType.split("charset=")[1];
                }

                if (method.equals("post")) {
                    RequestBody body = null;
                    String data = opt.optString("data", "").trim();
                    if (!data.isEmpty()) {
                        body = RequestBody.create(MediaType.parse("application/json"), data);
                    }
                    if (body == null) {
                        String dataBody = opt.optString("body", "").trim();
                        if (!dataBody.isEmpty() && contentType != null) {
                            body = RequestBody.create(MediaType.parse(contentType), opt.optString("body", ""));
                        }
                    }
                    if (body == null) {
                        body = RequestBody.create(null, "");
                    }
                    request = requestBuilder.post(body).build();
                } else if (method.equals("header")) {
                    request = requestBuilder.head().build();
                } else {
                    request = requestBuilder.get().build();
                }
                okHttpClient = opt.optInt("redirect", 1) == 1 ? OkGoHelper.getDefaultClient() : OkGoHelper.getNoRedirectClient();
                OkHttpClient.Builder builder = okHttpClient.newBuilder();
                if (opt.has("timeout")) {
                    long timeout = opt.optInt("timeout");
                    builder.readTimeout(timeout, TimeUnit.MILLISECONDS);
                    builder.writeTimeout(timeout, TimeUnit.MILLISECONDS);
                    builder.connectTimeout(timeout, TimeUnit.MILLISECONDS);
                }
                Response response = builder.build().newCall(request).execute();
                JSObject jsObject = jsContext.createNewJSObject();
                Set<String> resHeaders = response.headers().names();
                JSObject resHeader = jsContext.createNewJSObject();
                for (String header : resHeaders) {
                    resHeader.setProperty(header, response.header(header));
                }
                jsObject.setProperty("headers", (JSCallFunction) resHeader);
                jsObject.setProperty("code", String.valueOf(response.code()));

                int returnBuffer = opt.optInt("buffer", 0);
                if (returnBuffer == 1) {
                    JSArray array = jsContext.createNewJSArray();
                    byte[] bytes = response.body().bytes();
                    for (int i = 0; i < bytes.length; i++) {
                        array.set(bytes[i], i);
                    }
                    jsObject.setProperty("content", (JSCallFunction) array);
                } else if (returnBuffer == 2) {
                    jsObject.setProperty("content", Base64.encodeToString(response.body().bytes(), Base64.DEFAULT));
                } else {
                    String res;
                    byte[] responseBytes = UTF8BOMFighter.removeUTF8BOM(response.body().bytes());
                    res = new String(responseBytes, charset);
                    jsObject.setProperty("content", res);
                }
                return jsObject;
            } catch (Throwable throwable) {
                LOG.e(throwable);
                return "";
            }
        }

        void initConsole() {
            getGlobalObj().setProperty("local", String.valueOf(local.class));
            jsContext.evaluate("var console = {};");
            JSObject console = (JSObject) jsContext.getGlobalObject().getProperty("console");
            console.setProperty("log", new JSCallFunction() {
                @Override
                public Object call(Object... args) {
                    StringBuilder b = new StringBuilder();
                    for (Object o : args) {
                        b.append(o == null ? "null" : o.toString());
                    }
                    LOG.i("QuickJS", b.toString());
                    return null;
                }
            });
        }

        void initLocalStorage() {
            jsContext.evaluate("var local = {};");
            JSObject console = (JSObject) jsContext.getGlobalObject().getProperty("local");
            console.setProperty("get", new JSCallFunction() {
                @Override
                public Object call(Object... args) {
                    SharedPreferences sharedPreferences = App.getInstance().getSharedPreferences("js_engine_" + args[0].toString(), Context.MODE_PRIVATE);
                    return sharedPreferences.getString(args[1].toString(), "");
                }
            });
            console.setProperty("set", new JSCallFunction() {
                @Override
                public Object call(Object... args) {
                    SharedPreferences sharedPreferences = App.getInstance().getSharedPreferences("js_engine_" + args[0].toString(), Context.MODE_PRIVATE);
                    sharedPreferences.edit().putString(args[1].toString(), args[2].toString()).commit();
                    return null;
                }
            });
            console.setProperty("delete", new JSCallFunction() {
                @Override
                public Object call(Object... args) {
                    SharedPreferences sharedPreferences = App.getInstance().getSharedPreferences("js_engine_" + args[0].toString(), Context.MODE_PRIVATE);
                    sharedPreferences.edit().remove(args[1].toString()).commit();
                    return null;
                }
            });
        }

        void initOkHttp() {
            jsContext.getGlobalObject().setProperty("req", new JSCallFunction() {
                @Override
                public Object call(Object... args) {
                    try {
                        String url = args[0].toString();
                        JSONObject opt = new JSONObject(jsContext.stringify((JSObject) args[1]));
                        Headers.Builder headerBuilder = new Headers.Builder();
                        JSONObject optHeader = opt.optJSONObject("headers");
                        if (optHeader != null) {
                            Iterator<String> hdKeys = optHeader.keys();
                            while (hdKeys.hasNext()) {
                                String k = hdKeys.next();
                                String v = optHeader.optString(k);
                                headerBuilder.add(k, v);
                            }
                        }
                        Headers headers = headerBuilder.build();
                        String method = opt.optString("method", "get");
                        Request.Builder requestBuilder = new Request.Builder().url(url).headers(headers).tag("js_okhttp_tag");
                        Request request = null;
                        if (method.equalsIgnoreCase("post")) {
                            RequestBody body = null;
                            String data = opt.optString("data", "").trim();
                            if (!data.isEmpty()) {
                                body = RequestBody.create(MediaType.parse("application/json"), data);
                            }
                            if (body == null) {
                                String dataBody = opt.optString("body", "").trim();
                                if (!dataBody.isEmpty() && headers.get("Content-Type") != null) {
                                    body = RequestBody.create(MediaType.parse(headers.get("Content-Type")), opt.optString("body", ""));
                                }
                            }
                            if (body == null) {
                                body = RequestBody.create(null, "");
                            }
                            request = requestBuilder.post(body).build();
                        } else if (method.equalsIgnoreCase("header")) {
                            request = requestBuilder.head().build();
                        } else {
                            request = requestBuilder.get().build();
                        }
                        int redirect = opt.optInt("redirect", 1);
                        OkHttpClient client = null;
                        if (redirect == 1) {
                            client = OkGoHelper.getDefaultClient();
                        } else {
                            client = OkGoHelper.getNoRedirectClient();
                        }
                        OkHttpClient.Builder clientBuilder = client.newBuilder();
                        int timeout = 10000;
                        if (opt.has("timeout")) {
                            timeout = opt.optInt("timeout");
                        }
                        clientBuilder.readTimeout(timeout, TimeUnit.MILLISECONDS);
                        clientBuilder.writeTimeout(timeout, TimeUnit.MILLISECONDS);
                        clientBuilder.connectTimeout(timeout, TimeUnit.MILLISECONDS);
                        Response response = clientBuilder.build().newCall(request).execute();

                        JSObject jsObject = jsContext.createNewJSObject();
                        Set<String> resHeaders = response.headers().names();
                        JSObject resHeader = jsContext.createNewJSObject();
                        for (String header : resHeaders) {
                            resHeader.setProperty(header, response.header(header));
                        }
                        jsObject.setProperty("headers", (JSCallFunction) resHeader);
                        int returnBuffer = opt.optInt("buffer", 0);
                        if (returnBuffer == 1) {
                            JSArray array = jsContext.createNewJSArray();
                            byte[] bytes = response.body().bytes();
                            for (int i = 0; i < bytes.length; i++) {
                                array.set(bytes[i], i);
                            }
                            jsObject.setProperty("content", (JSCallFunction) array);
                        } else if (returnBuffer == 2) {
                            jsObject.setProperty("content", Base64.encodeToString(response.body().bytes(), Base64.DEFAULT));
                        } else {
                            String res;
                            if (headers.get("Content-Type") != null && headers.get("Content-Type").contains("=")) {
                                byte[] responseBytes = UTF8BOMFighter.removeUTF8BOM(response.body().bytes());
                                res = new String(responseBytes, headers.get("Content-Type").split("=")[1].trim());
                            } else {
                                res = response.body().string();
                            }
                            jsObject.setProperty("content", res);
                        }
                        return jsObject;
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                    JSObject jsObject = jsContext.createNewJSObject();
                    JSObject resHeader = jsContext.createNewJSObject();
                    jsObject.setProperty("headers", (JSCallFunction) resHeader);
                    jsObject.setProperty("content", "");
                    return jsObject;
                }
            });
            jsContext.getGlobalObject().setProperty("joinUrl", new JSCallFunction() {
                @Override
                public String call(Object... args) {
                    URL url;
                    String q = "";
                    try {
                        String parent = args[0].toString();
                        String child = args[1].toString();
                        // TODO
                        if (parent.isEmpty()) {
                            return child;
                        }
                        url = new URL(new URL(parent), child);
                        q = url.toExternalForm();
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                    return q;
                }
            });
            jsContext.getGlobalObject().setProperty("pdfh", new JSCallFunction() {
                @Override
                public String call(Object... args) {
                    try {
//                        LOG.i("pdfh----------------:"+args[1].toString().trim());
                        String html = args[0].toString();
                        return HtmlParser.parseDomForUrl(html, args[1].toString().trim(), "");
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                    return "";
                }
            });
            jsContext.getGlobalObject().setProperty("pdfa", new JSCallFunction() {
                @Override
                public Object call(Object... args) {
                    try {
//                        LOG.i("pdfa----------------:"+args[1].toString().trim());
                        String html = args[0].toString();
                        return jsContext.parseJSON(new Gson().toJson(HtmlParser.parseDomForList(html, args[1].toString().trim())));
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                    return null;
                }
            });
            jsContext.getGlobalObject().setProperty("pd", new JSCallFunction() {
                @Override
                public String call(Object... args) {
                    try {
//                        LOG.i("pd----------------:"+args[2].toString().trim());
                        String html = args[0].toString();
                        return HtmlParser.parseDomForUrl(html, args[1].toString().trim(), args[2].toString());
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                    return "";
                }
            });
        }

    }
}











