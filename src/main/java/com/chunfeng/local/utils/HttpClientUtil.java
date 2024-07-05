package com.chunfeng.local.utils;

import com.chunfeng.local.mapper.DynamicDataWriter;
import com.chunfeng.local.mapper.TdengineWritter;
import com.chunfeng.local.model.DynamicDataRow;
import com.chunfeng.local.model.QueryDataRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 接口定义HTTP请求
 *
 * @author chunfeng
 */
@Slf4j
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

    public void test(QueryDataRequest request) {
        try {
            URL url = new URL("http://huayi.ys7.com/source/get"); // 替换为实际服务地址
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setDoOutput(true);
            String jsonPayload = new ObjectMapper().writeValueAsString(request);
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            int code = connection.getResponseCode();
            if (code == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                String line;
                StringBuilder responseContent = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    responseContent.append(line);
                }
                reader.close();
                // 处理响应的JSON数据
                Map<String, Object> map = new ObjectMapper().readValue(responseContent.toString(), Map.class);
                Map<String, List<DynamicDataRow>> tableDataMap = new HashMap<>();
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    String tableName = entry.getKey();
                    List<DynamicDataRow> dataBatch = new ArrayList<>();
                    for (Object o : (List) entry.getValue()) {
                        DynamicDataRow dataRow = new DynamicDataRow(tableName);
                        Map<String, Object> map1 = (Map<String, Object>) o;
                        dataRow.setDataFields((Map<String, Object>) map1.get("dataFields"));
                        dataBatch.add(dataRow);
                    }
                    tableDataMap.computeIfAbsent(tableName, k -> new ArrayList<>()).addAll(dataBatch);
                    // 将处理后的数据行添加到新的映射中
                }
                log.info("所有表的数据接收完成，开始最终处理...");
                if (request.getUrl().contains("1234567890") || request.getUrl().contains("test")) {
//                        new TdengineWritter().writeDataToDatabase(tableDataMap, "db");
                    new TdengineWritter().writeDataToDatabase(tableDataMap, request.getUrl());
                } else {
                    DynamicDataWriter.writeDataToDatabase(tableDataMap);

                }
            } else {
                log.info("Failed to fetch data, HTTP error code : {}", code);
            }
            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}