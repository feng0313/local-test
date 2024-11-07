package com.chunfeng.local.utils;

import com.chunfeng.local.mapper.DynamicDataWriter;
import com.chunfeng.local.mapper.TdengineWritter;
import com.chunfeng.local.model.DynamicDataRow;
import com.chunfeng.local.model.QueryDataRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.io.IOUtils;

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
            URL url = new URL("http://218.75.111.22/source/get"); // 替换为实际服务地址
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

    public static void main(String[] args) {
        String imageUrl = "http://localhost:5003/api/nokia";
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("请输入文本:");
            String inputText = scanner.nextLine();
            if ("exit".equalsIgnoreCase(inputText)) {
                break;
            }
            String requestJson = "{\"text\":\"" + inputText + "\"}";
            try {
                downloadAndSaveImageFromAPI(imageUrl, requestJson);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        scanner.close();
    }

    /**
     * 从 API 下载图片并保存到桌面
     * @param imageUrl 图片的 URL
     * @param requestJson 请求参数
     * @throws IOException 如果发生 I/O 异常
     */
    public static void downloadAndSaveImageFromAPI(String imageUrl, String requestJson) throws IOException {
        Path desktopPath = Paths.get(System.getProperty("user.home"), "Desktop");
        File desktopDir = desktopPath.toFile();
        if (!desktopDir.exists()) {
            desktopDir.mkdirs();
        }
        File outputFile = new File(desktopDir, System.currentTimeMillis()+"_image.png");
        URL url = new URL(imageUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setRequestProperty("Accept", "application/json");
        try (OutputStream os = connection.getOutputStream()) {
            os.write(requestJson.getBytes());
            os.flush();
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (InputStream is = connection.getInputStream()) {
                    String response = IOUtils.toString(is, StandardCharsets.UTF_8);
                    byte[] imageData = decodeBase64(response);
                    try (OutputStream out = Files.newOutputStream(outputFile.toPath())) {
                        out.write(imageData);
                    }
                    validateImageFile(outputFile);
                }
            } else {
                System.err.println("HTTP Error: " + responseCode);
            }
        }
    }

    /**
     * 解码 base64 字符串为字节数组
     * @param base64String base64 编码的字符串
     * @return 解码后的字节数组
     */
    private static byte[] decodeBase64(String base64String) {
        return java.util.Base64.getDecoder().decode(base64String);
    }

    /**
     * 验证图片文件是否可以打开
     * @param file 图片文件
     * @throws IOException 如果发生 I/O 异常
     */
    private static void validateImageFile(File file) throws IOException {
        try {
            javax.imageio.ImageIO.read(file);
            System.out.println("图片文件已成功保存并验证有效！");
        } catch (IOException e) {
            System.err.println("图片文件无效，请检查数据是否正确：" + file.getAbsolutePath());
            throw e;
        }
    }
}