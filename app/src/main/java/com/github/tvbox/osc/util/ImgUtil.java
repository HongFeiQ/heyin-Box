package com.github.tvbox.osc.util;

import static com.bumptech.glide.load.resource.bitmap.VideoDecoder.FRAME_OPTION;

import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.text.TextUtils;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.github.tvbox.osc.R;
import com.google.common.net.HttpHeaders;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;

/*

1. 减少方法重载：可以考虑减少  load()  方法的重载，将不同的参数设置为可选参数，以减少方法数量。

2. 使用链式调用：可以使用链式调用来简化代码，提高可读性和易用性。

3. 使用常量替代硬编码的值：将一些常用的值，如圆角半径、默认图片等，定义为常量，提高代码的可维护性。

4. 减少不必要的条件判断：可以通过合理的参数设置和默认值，减少一些不必要的条件判断。

5. 使用线程池加载图片：可以使用线程池来加载图片，提高加载效率。

6. 优化缓存策略：根据实际需求和网络情况，优化图片的缓存策略，以提高用户体验和网络性能。
 */
public class ImgUtil {
    private static final int DEFAULT_ROUNDING_RADIUS = 10;
    private static final int DEFAULT_DISK_CACHE_STRATEGY = 4;

    public static void load(String url, ImageView view) {
        load(url, view, DEFAULT_ROUNDING_RADIUS, ImageView.ScaleType.FIT_CENTER);
    }

    public static void load(String url, ImageView view, ImageView.ScaleType scaleType) {
        load(url, view, DEFAULT_ROUNDING_RADIUS, scaleType);
    }

    public static void load(String url, ImageView view, int roundingRadius, ImageView.ScaleType scaleType) {
        view.setScaleType(ImageView.ScaleType.FIT_CENTER);
        if (TextUtils.isEmpty(url)) {
            view.setImageResource(R.drawable.img_loading_placeholder);
        } else {
            if (roundingRadius == 0) roundingRadius = 1;
            RequestOptions requestOptions = new RequestOptions()
                    .format(DecodeFormat.PREFER_RGB_565)
                    .diskCacheStrategy(getDiskCacheStrategy(DEFAULT_DISK_CACHE_STRATEGY))
                    .dontAnimate()
                    .transform(new RoundedCorners(roundingRadius));
            loadInternal(url, view, requestOptions, scaleType);
        }
    }

    public static void load(String url, ImageView view, int roundingRadius) {
        view.setScaleType(ImageView.ScaleType.CENTER);
        if (TextUtils.isEmpty(url)) {
            view.setImageResource(R.drawable.img_loading_placeholder);
        } else {
            if (roundingRadius == 0) roundingRadius = 1;
            RequestOptions requestOptions = new RequestOptions()
                    .format(DecodeFormat.PREFER_RGB_565)
                    .diskCacheStrategy(getDiskCacheStrategy(0))
                    .dontAnimate()
                    .transform(new CenterCrop(), new RoundedCorners(roundingRadius));
            loadInternal(url, view, requestOptions, ImageView.ScaleType.FIT_XY);
        }
    }

    private static void loadInternal(String url, ImageView view, RequestOptions requestOptions, ImageView.ScaleType scaleType) {
        Glide.with(view)
                .load(getUrl(url))
                .error(R.drawable.img_loading_placeholder)
                .placeholder(R.drawable.img_loading_placeholder)
                .listener(getListener(view, scaleType))
                .apply(requestOptions)
                .into(view);
    }

    public static void loadVideoScreenshot(String uri, ImageView imageView, long frameTimeMicros) {
        RequestOptions requestOptions = RequestOptions.frameOf(frameTimeMicros * 1000)
                .set(FRAME_OPTION, MediaMetadataRetriever.OPTION_CLOSEST)
                .transform(new CenterCrop(), new RoundedCorners(10));

        Glide.with(imageView)
                .load(uri)
                .skipMemoryCache(true)
                .apply(requestOptions)
                .into(imageView);
    }

    public static String getDiskCacheStrategyName(int index) {
        String[] names = new String[]{
                "[NONE] 关闭", "[RESOURCE] 转换图片", "[DATA] 原始图片 ", "[ALL] 原始图片和转换图片", "[AUTOMATIC] 自动"
        };
        return names[index];
    }
    /*
     * 使用Glide方式获取视频某一帧
     * @param uri 视频地址
     * @param imageView 设置image
     * @param frameTimeMicros 获取某一时间帧.
     */

    public static DiskCacheStrategy getDiskCacheStrategy(int index) {
        switch (index) {
            case 1:
                return DiskCacheStrategy.RESOURCE;
            case 2:
                return DiskCacheStrategy.DATA;
            case 3:
                return DiskCacheStrategy.ALL;
            case 4:
                return DiskCacheStrategy.AUTOMATIC;
            default:
                return DiskCacheStrategy.NONE;
        }
    }

    private static Object getUrl(String url) {
        if (url.startsWith("data:")) return url;
        url = DefaultConfig.checkReplaceProxy(url);
        String header = null;
        String referer = null;
        String ua = UA.randomOne();
        String cookie = null;
        if (url.contains("doubanio.com") && !url.contains("@Referer=") && !url.contains("@User-Agent=")) {
            url += "@Referer=https://api.douban.com/@User-Agent=" + ua;
        }
        //检查链接里面是否有自定义header
        if (url.contains("@Headers=")) {
            header = url.split("@Headers=")[1].split("@")[0];
            try {
                header = URLDecoder.decode(header, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        if (url.contains("@Cookie=")) cookie = url.split("@Cookie=")[1].split("@")[0];
        if (url.contains("@User-Agent=")) ua = url.split("@User-Agent=")[1].split("@")[0];
        if (url.contains("@Referer=")) referer = url.split("@Referer=")[1].split("@")[0];
        url = url.split("@")[0];
        /*   AuthInfo authInfo = new AuthInfo(url);
        url = authInfo.url; */
        LazyHeaders.Builder builder = new LazyHeaders.Builder();
        /*     if (!authInfo.auth.isEmpty()) {
            builder.addHeader(HttpHeaders.AUTHORIZATION, authInfo.auth);
        }*/
        if (!TextUtils.isEmpty(header)) {
            JsonObject jsonInfo = new Gson()
                    .fromJson(header, JsonObject.class);
            for (String key : jsonInfo.keySet()) {
                String val = jsonInfo.get(key)
                        .getAsString();
                builder.addHeader(key, val);
            }
        } else {
            if (!TextUtils.isEmpty(cookie)) {
                builder.addHeader(HttpHeaders.COOKIE, cookie);
            }
            if (!TextUtils.isEmpty(ua)) {
                builder.addHeader(HttpHeaders.USER_AGENT, ua);
            } else {
                String mobile_UA = "Dalvik/2.1.0 (Linux; U; Android 13; M2102J2SC Build/TKQ1.220829.002)";
                builder.addHeader(HttpHeaders.USER_AGENT, mobile_UA);
            }
            if (!TextUtils.isEmpty(referer)) builder.addHeader(HttpHeaders.REFERER, referer);
        }
        try {
            URL imgUrl = new URL(url);
            String host = imgUrl.getHost();
            builder.addHeader(HttpHeaders.HOST, host);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return new GlideUrl(url, builder.build());
    }


    private static RequestListener<Drawable> getListener(final ImageView view, final ImageView.ScaleType scaleType) {
        return new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                view.setScaleType(scaleType);
                view.setImageResource(R.drawable.img_loading_placeholder);
                return true;
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                view.setScaleType(scaleType);
                return false;
            }
        };
    }
}


