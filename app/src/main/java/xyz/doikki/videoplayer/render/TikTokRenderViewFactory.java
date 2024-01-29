package xyz.doikki.videoplayer.render;

import android.content.Context;

public class TikTokRenderViewFactory extends RenderViewFactory {

    public static TikTokRenderViewFactory create() {
        return new TikTokRenderViewFactory();
    }

    @Override
    public IRenderView createRenderView(Context context) {
        return new TikTokRenderView(new TextureRenderView(context));
    }
}
