package com.github.catvod.net;

import android.util.ArrayMap;

import com.github.tvbox.osc.util.OkGoHelper;
import com.github.tvbox.osc.util.urlhttp.BrotliInterceptor;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Dns;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;


public class OkHttp {

    private static final int TIMEOUT = 10 * 1000;
    private static final int CACHE = 50 * 1024 * 1024;
    private OkHttpClient client;
    private OkHttpClient noRedirect;
    private Dns dns;

    public static OkHttp get() {
        return Loader.INSTANCE;
    }

    public static OkHttpClient client() {
        if (get().client != null) return get().client;
        return get().client = client(TIMEOUT);
    }

    public static OkHttpClient noRedirect() {
        if (get().noRedirect != null) return get().noRedirect;
        return get().noRedirect = client().newBuilder().followRedirects(false).followSslRedirects(false).build();
    }

    public static Dns dns() {

        //return OkGoHelper.dnsOverHttps != null ? OkGoHelper.dnsOverHttps : Dns.SYSTEM;
        //        return get().dns != null ? get().dns : Dns.SYSTEM; // 由于 setDoh(Doh doh)没有被调用导致这里选择的是 Dns.SYSTEM
        return get().dns != null ? get().dns : OkGoHelper.dnsOverHttps;

    }

    public static OkHttpClient client(int timeout) {
        return new OkHttpClient.Builder().connectionSpecs(OkGoHelper.getConnectionSpec()).addInterceptor(new BrotliInterceptor()).connectTimeout(timeout, TimeUnit.MILLISECONDS).readTimeout(timeout, TimeUnit.MILLISECONDS).writeTimeout(timeout, TimeUnit.MILLISECONDS).dns(dns()).hostnameVerifier(SSLSocketFactoryCompat.hostnameVerifier).sslSocketFactory(new SSLSocketFactoryCompat(), SSLSocketFactoryCompat.trustAllCert).build();
    }

    public static Call newCall(String url) {
        return client().newCall(new Request.Builder().url(url).build());
    }

    public static Call newCall(OkHttpClient client, String url) {
        return client.newCall(new Request.Builder().url(url).build());
    }

    public static Call newCall(String url, Headers headers) {
        return client().newCall(new Request.Builder().url(url).headers(headers).build());
    }

    public static Call newCall(String url, Map<String, String> params) {
        return client().newCall(new Request.Builder().url(buildUrl(url, (ArrayMap<String, String>) params)).build());
    }


    public static Call newCall(String url, Headers headers, Map<String, String> params) {
        return client().newCall(new Request.Builder().url(buildUrl(url, (ArrayMap<String, String>) params)).headers(headers).build());
    }

    private static HttpUrl buildUrl(String url, Map<String, String> params) {
        HttpUrl.Builder builder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) builder.addQueryParameter(entry.getKey(), entry.getValue());
        return builder.build();
    }
    public static String string(String url, Map<String, String> headerMap) {
        try {
            return newCall(url, headerMap).execute().body().string();
        } catch (IOException e) {
            return "";
        }
    }

    public static String string(String url) {
        try {
            return newCall(url).execute().body().string();
        } catch (IOException e) {
            return "";
        }
    }

    private static class Loader {
        static volatile OkHttp INSTANCE = new OkHttp();
    }
}