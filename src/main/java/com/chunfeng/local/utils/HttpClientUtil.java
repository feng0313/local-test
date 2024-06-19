package com.chunfeng.local.utils;

import okhttp3.*;

import java.io.IOException;
import java.util.Map;

/**
 * 接口定义HTTP请求
 * @author chunfeng
 */
public class HttpClientUtil {

    private static final OkHttpClient CLIENT = new OkHttpClient();

    public static String sendHttpGetRequest(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                return null;
            }
            return response.body().string();
        }
    }

    public static String sendHttpPostRequest(String url, Map<String, String> params) throws IOException {
        RequestBody requestBody = buildFormBody(params);
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                return null;
            }
            return response.body().string();
        }
    }

    public static String sendHttpPutRequest(String url, Map<String, String> params) throws IOException {
        RequestBody requestBody = buildFormBody(params);
        Request request = new Request.Builder()
                .url(url)
                .put(requestBody)
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                return null;
            }
            return response.body().string();
        }
    }

    public static String sendHttpDeleteRequest(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .delete()
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                return null;
            }
            return response.body().string();
        }
    }

    private static RequestBody buildFormBody(Map<String, String> params) {
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            formBodyBuilder.add(entry.getKey(), entry.getValue());
        }
        return formBodyBuilder.build();
    }
}