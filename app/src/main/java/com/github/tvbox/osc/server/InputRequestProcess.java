package com.github.tvbox.osc.server;


import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;

import java.util.Map;


/**
 * @author pj567
 * @date :2021/1/5
 * @description: 响应按键和输入
 */

public class InputRequestProcess implements RequestProcess {
    private RemoteServer remoteServer;

    public InputRequestProcess(RemoteServer remoteServer) {
        this.remoteServer = remoteServer;
    }

    @Override
    public boolean isRequest(IHTTPSession session, String fileName) {
        if (session.getMethod() == Method.POST) {
            switch (fileName) {
                case "/action":
                    return true;
            }
        }
        return false;
    }

    @Override
    public Response doResponse(IHTTPSession session, String fileName, Map<String, String> params, Map<String, String> files) {
        DataReceiver mDataReceiver = remoteServer.getDataReceiver();
        switch (fileName) {
            case "/action":
                if (params.get("do") != null && mDataReceiver != null) {
                    String action = params.get("do");

                    switch (action) {
                        case "search": {
                            mDataReceiver.onTextReceived(params.get("word").trim());
                            break;
                        }
                        case "api": {
                            mDataReceiver.onApiReceived(params.get("url").trim());
                            break;
                        }
                        case "live": {
                            mDataReceiver.onLiveReceived(params.get("url").trim());
                            break;
                        }
                        case "epg": {
                            mDataReceiver.onEpgReceived(params.get("url").trim());
                            break;
                        }
                        case "push": {
                            // 暂未实现
                            mDataReceiver.onPushReceived(params.get("url").trim());
                            break;
                        }
                        case "mirror": {
                            //推送当前电影、电视剧……
                            mDataReceiver.onMirrorReceived(params.get("id").trim(), params.get("sourceKey").trim());
                            return RemoteServer.createPlainTextResponse(Status.OK, "mirrored");
                        }
                        case "store": {
                            //String name = params.get("store_api_name");
                            //String url = params.get("store_api");
                            mDataReceiver.onStoreReceived(params.get("url").trim());
                            break;
                        }
                    }
                }
                return RemoteServer.createPlainTextResponse(Status.OK, "ok");
            default:
                return RemoteServer.createPlainTextResponse(Status.NOT_FOUND, "Error 404, file not found.");
        }
    }
}
