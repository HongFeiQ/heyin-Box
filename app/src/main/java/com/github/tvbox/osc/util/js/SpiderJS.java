package com.github.tvbox.osc.util.js;

import android.content.Context;

import com.github.catvod.crawler.Spider;
import com.github.tvbox.osc.util.FileUtils;
import com.github.tvbox.osc.util.LOG;
import com.github.tvbox.osc.util.MD5;
import com.whl.quickjs.wrapper.Function;
import com.whl.quickjs.wrapper.JSArray;
import com.whl.quickjs.wrapper.JSCallFunction;
import com.whl.quickjs.wrapper.JSFunction;
import com.whl.quickjs.wrapper.JSModule;
import com.whl.quickjs.wrapper.JSObject;
import com.whl.quickjs.wrapper.JSUtils;
import com.whl.quickjs.wrapper.QuickJSContext;
import com.whl.quickjs.wrapper.UriUtil;

import org.json.JSONArray;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SpiderJS extends Spider {

    private final String ext;
    //////////////////////////////////////////////////////////
    private final String key;
    public QuickJSContext runtime;
    public ExecutorService executor;
    private JSEngine.JSThread jsThread = null;
    private String js;
    private JSObject jsObject;
    public SpiderJS(String key, String js, String ext) {
        this.key = key;
        this.js = js;
        this.ext = ext;
    }
    public SpiderJS(String key, String js, Class<?> cls, String ext) throws Exception {
        this.js = js;
        this.ext = ext;
        this.executor = Executors.newSingleThreadExecutor();
        this.key = "J" + MD5.encode(key);
        initjs(cls);
    }

    void checkLoaderJS() {
        if (jsThread == null) {
            jsThread = JSEngine.getInstance().getJSThread();
        }
        if (jsObject == null && jsThread != null) {
            try {
                jsThread.postVoid((ctx, globalThis) -> {
                    String moduleKey = "__" + UUID.randomUUID().toString().replace("-", "") + "__";
                    String jsContent = JSEngine.getInstance().loadModule(js);
                    try {
                        if (js.contains(".js?")) {
                            int spIdx = js.indexOf(".js?");
                            String[] query = js.substring(spIdx + 4).split("&|=");
                            js = js.substring(0, spIdx);
                            for (int i = 0; i < query.length; i += 2) {
                                String key = query[i];
                                String val = query[i + 1];
                                String sub = JSModule.convertModuleName(js, val);
                                String content = JSEngine.getInstance().loadModule(sub);
                                jsContent = jsContent.replace("__" + key.toUpperCase() + "__", content);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (jsContent.contains("export default{")) {
                        jsContent = jsContent.replace("export default{", "globalThis." + moduleKey + " ={");
                    } else if (jsContent.contains("export default {")) {
                        jsContent = jsContent.replace("export default {", "globalThis." + moduleKey + " ={");
                    } else {
                        jsContent = jsContent.replace("__JS_SPIDER__", "globalThis." + moduleKey);
                    }
                    ctx.evaluateModule(jsContent, js);
                    jsObject = (JSObject) ctx.getProperty(globalThis, moduleKey);
                    jsObject.getJSFunction("init").call(ext);
                    return null;
                });
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }

    String postFunc(String func, Object... args) {
        checkLoaderJS();
        if (jsObject != null) {
            try {
                return jsThread.post((ctx, globalThis) -> (String) jsObject.getJSFunction(func).call(args));
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
        return "";
    }

    public Object[] proxyInvoke(Map<String, String> params) {
        checkLoaderJS();
        try {
            return jsThread.post((ctx, globalThis) -> {
                try {
                    JSObject o = ctx.createNewJSObject();
                    if (params != null) {
                        for (String s : params.keySet()) {
                            o.setProperty(s, params.get(s));
                        }
                    }
                    JSONArray opt = new JSONArray(((JSArray) jsObject.getJSFunction("proxy").call(new Object[]{o})).stringify());
                    Object[] result = new Object[3];
                    result[0] = opt.opt(0);
                    result[1] = opt.opt(1);
                    Object obj = opt.opt(2);
                    ByteArrayInputStream baos;
                    if (obj instanceof JSONArray) {
                        JSONArray json = (JSONArray) obj;
                        byte[] b = new byte[json.length()];
                        for (int i = 0; i < json.length(); i++) {
                            b[i] = (byte) json.optInt(i);
                        }
                        baos = new ByteArrayInputStream(b);
                    } else {
                        baos = new ByteArrayInputStream(opt.opt(2).toString().getBytes());
                    }
                    result[2] = baos;
                    return result;
                } catch (Throwable throwable) {
                    LOG.e(throwable);
                    return new Object[0];
                }
            });
        } catch (Throwable throwable) {
            LOG.e(throwable);
            return new Object[0];
        }
    }

    public void destroy() {
        submit(() -> {
            executor.shutdownNow();
            runtime.destroy();
        });
    }

    private void submit(Runnable runnable) {
        executor.submit(runnable);
    }

    private <T> Future<T> submit(Callable<T> callable) {
        return executor.submit(callable);
    }

    private Object call(String func, Object... args) throws Exception {
        return executor.submit(FunCall.call(jsObject, func, args)).get();
    }

    private void initjs(Class<?> cls) throws Exception {
        submit(() -> {
            if (runtime == null) this.runtime = QuickJSContext.create();
            runtime.setModuleLoader(new QuickJSContext.DefaultModuleLoader() {
                @Override
                public String getModuleStringCode(String moduleName) {
                    return FileUtils.loadModule(moduleName);
                }

                @Override
                public String moduleNormalizeName(String moduleBaseName, String moduleName) {
                    return UriUtil.resolve(moduleBaseName, moduleName);
                }
            });

            initConsole();
            runtime.getGlobalObject().bind(new Global(executor));

            if (cls != null) {
                Class<?>[] classes = cls.getDeclaredClasses();
                JSObject apiObj = runtime.createJSObject();

                LOG.e("cls", "" + classes.length);
                for (Class<?> classe : classes) {
                    Object javaObj = null;
                    try {
                        javaObj = classe.getDeclaredConstructor(cls).newInstance(cls.getDeclaredConstructor(QuickJSContext.class).newInstance(runtime));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (javaObj == null) {
                        throw new NullPointerException("The JavaObj cannot be null. An error occurred in newInstance!");
                    }
                    JSObject claObj = runtime.createJSObject();
                    Method[] methods = classe.getDeclaredMethods();
                    for (Method method : methods) {
                        if (method.isAnnotationPresent(Function.class)) {
                            Object finalJavaObj = javaObj;
                            claObj.set(method.getName(), new JSCallFunction() {
                                @Override
                                public Object call(Object... objects) {
                                    try {
                                        return method.invoke(finalJavaObj, objects);
                                    } catch (Throwable e) {
                                        return null;
                                    }
                                }
                            });
                        }
                    }
                    apiObj.set(classe.getSimpleName(), claObj);
                    LOG.e("cls", classe.getSimpleName());
                }
                runtime.getGlobalObject().set("jsapi", apiObj);
            }
            String jsContent = FileUtils.loadModule(js);

            if (jsContent.contains("__jsEvalReturn")) {
                runtime.evaluate("req = http");
                jsContent = jsContent + "\n\nglobalThis." + key + " = __jsEvalReturn()";
            } else if (jsContent.contains("export default{") || jsContent.contains("export default {")) {
                jsContent = jsContent.replaceAll("export default.*?[{]", "globalThis." + key + " = {");
            } else {
                jsContent = jsContent.replace("__JS_SPIDER__", "globalThis." + key);
            }
            //LOG.e("cls", jsContent);
            runtime.evaluateModule(jsContent + "\n\n;console.log(typeof(" + key + ".init));\n\nconsole.log(typeof(req));\n\nconsole.log(Object.keys(" + key + "));", js);
            jsObject = (JSObject) runtime.get(runtime.getGlobalObject(), key);
            return null;
        }).get();
    }

    private void initConsole() {
        JSObject local = runtime.createJSObject();
        runtime.getGlobalObject().set("local", local);
        local.bind(new local());

        runtime.setConsole(new QuickJSContext.Console() {
            @Override
            public void log(String s) {
                LOG.i("QuJs", s);
            }
        });

        runtime.evaluate(FileUtils.loadModule("net.js"));
    }

    public void cancelByTag() {
        Connect.cancelByTag("js_okhttp_tag");
    }

    @Override
    public void init(Context context, final String extend) throws Exception {
        super.init(context, extend);
        String ext = FileUtils.loadModule(extend);
        call("init", Json.valid(ext) ? runtime.parse(ext) : ext);
    }

    @Override
    public String homeContent(boolean filter) throws Exception {
        return (String) call("home", filter);
    }

    @Override
    public String homeVideoContent() throws Exception {
        return (String) call("homeVod");
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        JSObject obj = submit(() -> new JSUtils<String>().toObj(runtime, extend)).get();
        return (String) call("category", tid, pg, filter, obj);
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        return (String) call("detail", ids.get(0));
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        JSArray array = submit(() -> new JSUtils<String>().toArray(runtime, vipFlags)).get();
        return (String) call("play", flag, id, array);
    }

    @Override
    public String searchContent(String key, boolean quick) throws Exception {
        return (String) call("search", key, quick);
    }

    @Override
    public String searchContent(String key, boolean quick, String pg) throws Exception {
        return (String) call("search", key, quick, pg);
    }

    @Override
    public Object[] proxyLocal(Map<String, String> params) throws Exception {
        return submit(() -> {
            try {
                JSObject o = new JSUtils<String>().toObj(runtime, params);
                JSFunction jsFunction = jsObject.getJSFunction("proxy");
                JSONArray opt = new JSONArray(jsFunction.call(null, new Object[]{o}).toString());
                Object[] result = new Object[3];
                result[0] = opt.opt(0);
                result[1] = opt.opt(1);
                Object obj = opt.opt(2);
                ByteArrayInputStream baos;
                if (obj instanceof JSONArray) {
                    JSONArray json = (JSONArray) obj;
                    byte[] b = new byte[json.length()];
                    for (int i = 0; i < json.length(); i++) {
                        b[i] = (byte) json.optInt(i);
                    }
                    baos = new ByteArrayInputStream(b);
                } else {
                    baos = new ByteArrayInputStream(opt.opt(2).toString().getBytes());
                }
                result[2] = baos;
                return result;
            } catch (Throwable throwable) {
                LOG.e(throwable);
                return new Object[0];
            }
        }).get();
    }

    @Override
    public boolean manualVideoCheck() throws Exception {
        return (Boolean) call("sniffer");
    }

    @Override
    public boolean isVideoFormat(String url) throws Exception {
        return (Boolean) call("isVideo", url);
    }
}

