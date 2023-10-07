package com.github.tvbox.osc.util.js;

import android.util.Base64;

import com.github.catvod.net.OkHttp;
import com.github.tvbox.osc.util.LOG;
import com.google.common.net.HttpHeaders;
import com.lzy.okgo.OkGo;
import com.github.tvbox.quickjs.JSObject;
import com.github.tvbox.quickjs.JSUtils;
import com.github.tvbox.quickjs.QuickJSContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Connect {
    static OkHttpClient client;

    public static Call to(String url, Req req) {
        client = req.getRedirect() == 1 ? OkHttp.client() : OkHttp.noRedirect();
        client = client.newBuilder()
                .connectTimeout(req.getTimeout(), TimeUnit.MILLISECONDS)
                .readTimeout(req.getTimeout(), TimeUnit.MILLISECONDS)
                .writeTimeout(req.getTimeout(), TimeUnit.MILLISECONDS).build();
        return client.newCall(getRequest(url, req, Headers.of(req.getHeader())));
    }

    public static JSObject success(QuickJSContext ctx, Req req, Response res) {
        try {
            JSObject jsObject = ctx.createJSObject();
            JSObject jsHeader = ctx.createJSObject();
            setHeader(ctx, res, jsHeader);
            jsObject.set("headers", jsHeader);
            if (req.getBuffer() == 0) jsObject.set("content", new String(res.body().bytes(), req.getCharset()));
            if (req.getBuffer() == 1) jsObject.set("content", new JSUtils<>().toArray(ctx, res.body().bytes()));
            if (req.getBuffer() == 2) jsObject.set("content", Base64.encodeToString(res.body().bytes(), Base64.DEFAULT));
            return jsObject;
        } catch (Exception e) {
            return error(ctx);
        }
    }

    public static JSObject error(QuickJSContext ctx) {
        JSObject jsObject = ctx.createJSObject();
        JSObject jsHeader = ctx.createJSObject();
        jsObject.set("headers", jsHeader);
        jsObject.set("content", "");
        return jsObject;
    }

    private static Request getRequest(String url, Req req, Headers headers) {
        if (req.getMethod().equalsIgnoreCase("post")) {
            return new Request.Builder().url(url).tag("js_okhttp_tag").headers(headers).post(getPostBody(req, headers.get(HttpHeaders.CONTENT_TYPE))).build();
        } else if (req.getMethod().equalsIgnoreCase("header")) {
            return new Request.Builder().url(url).tag("js_okhttp_tag").headers(headers).head().build();
        } else {
            return new Request.Builder().url(url).tag("js_okhttp_tag").headers(headers).get().build();
        }
    }

    private static RequestBody getPostBody(Req req, String contentType) {
        if (req.getData() != null && req.getPostType().equals("json")) return getJsonBody(req);
        if (req.getData() != null && req.getPostType().equals("form")) return getFormBody(req);
        if (req.getBody() != null && contentType != null) return RequestBody.create(MediaType.get(contentType), req.getBody());
        return RequestBody.create(null, "");
    }

    private static RequestBody getJsonBody(Req req) {
        return RequestBody.create(MediaType.get("application/json"), req.getData().toString());
    }

    private static RequestBody getFormBody(Req req) {
        FormBody.Builder formBody = new FormBody.Builder();
        Map<String, String> params = Json.toMap(req.getData());
        for (String key : params.keySet()) formBody.add(key, params.get(key));
        return formBody.build();
    }

    private static void setHeader(QuickJSContext ctx, Response res, JSObject object) {
        for (Map.Entry<String, List<String>> entry : res.headers().toMultimap().entrySet()) {
            if (entry.getValue().size() == 1) object.set(entry.getKey(), entry.getValue().get(0));
            if (entry.getValue().size() >= 2) object.set(entry.getKey(), new JSUtils<String>().toArray(ctx, entry.getValue()));
        }
    }
    public static void cancelByTag(Object tag) {
        try {
            if (client != null) {
                for (Call call : client.dispatcher().queuedCalls()) {
                    if (tag.equals(call.request().tag())) {
                        call.cancel();
                    }
                }
                for (Call call : client.dispatcher().runningCalls()) {
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
}
