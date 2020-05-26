package com.coolweather.android.util;

import android.util.Log;

import okhttp3.OkHttpClient;
import okhttp3.Request;
/*
*实现和服务器进行交互*/
public class HttpUtil {
    public static void sendOkHttpRequest(String address,okhttp3.Callback callback){
        OkHttpClient client=new OkHttpClient();
        Request request=new Request.Builder().url(address).build();
        Log.d("HttpUtil", String.valueOf(request));
        client.newCall(request).enqueue(callback);
    }
}
