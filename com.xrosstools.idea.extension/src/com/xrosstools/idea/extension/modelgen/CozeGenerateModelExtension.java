package com.xrosstools.idea.extension.modelgen;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.Messages;
import com.xrosstools.idea.gef.extensions.GenerateModelExtension;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;


public class CozeGenerateModelExtension implements GenerateModelExtension {
    private static final String USER_ID = "xross_tools_user";
    private static final String BOT_ID = "7551808903206682667";
    private static final String QUESTION_AUTH_ID = "Bearer pat_wju8xivKNAzfRkl5Pwi5tFBqJyCz2m0irroaalsViEAdkG8gz8tyzqhr9XYqF5R7";
    private static final String STATUS_AUTH_ID = QUESTION_AUTH_ID;

    private static final String POST_QUESTION_URL = "https://api.coze.cn/v3/chat";
    private static final String GET_STATUS_URL = "https://api.coze.cn/v3/chat/retrieve";
    private static final String GET_ANSER_URL = "https://api.coze.cn/v3/chat/message/list";

    private static final String CONVERSATION_ID = "conversation_id";
    private static final String CHAT_ID = "id";

    private static final String CONVERSATION_CHAT_ID_TPL = "?conversation_id=%s&chat_id=%s";

    private static final int TIMEOUT = 15000;

    private static final String STATUS = "status";
    private static final String COMPLETED = "completed";

    private static final String TYPE = "type";
    private static final String ANSWER = "answer";

    private static final String CONTENT = "content";

    private static final Gson gson = new Gson();

    @Override
    public boolean isGenerateModelSupported(String modelType) {
        return "xflow".equalsIgnoreCase(modelType);
    }

    @Override
    public void generateModel(String description, Consumer<String> callback) {
        ProgressManager.getInstance().run(new Task.Backgroundable(null, "Calling Coze API", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setText("Sending request to Coze API...");

                try {
                    String promptPayload = buildPromptPayload(description);

                    String response = postCozeChatAPI(promptPayload);

                     String conversationQuery = buildConversationQuery(response);

                    // 切换到UI线程更新界面
                    ApplicationManager.getApplication().invokeLater(() -> {
                        StatusDialog dialog = new StatusDialog();
                        dialog.startPolling(createCheckStatusTask(conversationQuery, dialog, callback));
                        dialog.show();
                    });

                } catch (Exception e) {
                    // 错误处理
                    ApplicationManager.getApplication().invokeLater(() -> {
                        failed(e);
                    });
                }
            }
        });
    }

    public String postCozeChatAPI(String jsonPayload) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(POST_QUESTION_URL);
            setHeader(httpPost);
            StringEntity entity = new StringEntity(jsonPayload, StandardCharsets.UTF_8);
            httpPost.setEntity(entity);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();

                return getResponseBody(statusCode, response);
            }
        }
    }

    private String  getRequest(String url, String token) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

            HttpGet httpGet = new HttpGet(url);
            setHeader(httpGet);

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                int statusCode = response.getStatusLine().getStatusCode();
                return getResponseBody(statusCode, response);
            }
        }
    }

    private String getResponseBody(int statusCode, CloseableHttpResponse response) throws IOException {
        String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        if (statusCode == 200) {
            return responseBody;
        } else {
            throw new RuntimeException("HTTP error: " + statusCode + ", response: " + responseBody);
        }
    }

    private void setHeader(HttpRequestBase httpRequest) {
        httpRequest.setHeader("Content-Type", "application/json");
        httpRequest.setHeader("Authorization", QUESTION_AUTH_ID);
        httpRequest.setHeader("Accept-Charset", "UTF-8");
        httpRequest.setHeader("Accept", "text/xml; charset=UTF-8");
    }

    private void failed(Exception e) {
        Messages.showErrorDialog("API调用失败: " + e.getMessage(), "错误");
    }

    // 手动构建JSON请求体
    private String buildPromptPayload(String content) {
        // 构建请求JSON数据
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.bot_id = BOT_ID;
        chatRequest.user_id = USER_ID;
        chatRequest.stream = false;
        chatRequest.auto_save_history = true;
        chatRequest.additional_messages = new ArrayList<>();
        chatRequest.additional_messages.add(new Message(content));

        return gson.toJson(chatRequest);

//        JsonObject requestData = new JsonObject();
//        requestData.addProperty("bot_id", BOT_ID);
//        requestData.addProperty("user_id", "xross_tools_user");
//        requestData.addProperty("stream", false);
//        requestData.addProperty("auto_save_history", true);
//
//        JsonArray additionalMessages = new JsonArray();
//        JsonObject message = new JsonObject();
//        message.addProperty("content", content);
//        message.addProperty("content_type", "text");
//        message.addProperty("role", "user");
//        message.addProperty("type", "question");
//        additionalMessages.add(message);
//
//        requestData.add("additional_messages", additionalMessages);
//        requestData.add("parameters", new JsonObject());
//
//        return requestData.toString();
    }

    private String buildConversationQuery(String response) {
        Gson gson = new Gson();
        JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
        JsonObject data = jsonResponse.getAsJsonObject("data");
        String conversationId = data.get(CONVERSATION_ID).getAsString();
        String chat_id = data.get(CHAT_ID).getAsString();
        return String.format(CONVERSATION_CHAT_ID_TPL, conversationId, chat_id);
    }

    private boolean isAnswerCompleted(String status) {
        return COMPLETED.equals(status);
    }

    private String getStatus(String response) {
        Gson gson = new Gson();
        JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
        JsonObject data = jsonResponse.getAsJsonObject("data");
        return data.get(STATUS).getAsString();
    }

    private String getAnswer(String response) {
        Gson gson = new Gson();
        JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
        JsonArray data = jsonResponse.getAsJsonArray("data");
        for(int i = 0; i < data.size();i++) {
            JsonObject  element = data.get(i).getAsJsonObject();
            if(ANSWER.equals(element.get(TYPE).getAsString())) {
                String rawContent = element.get(CONTENT).getAsString();
                return rawContent.substring(6+1, rawContent.length() - (3+1));
            }
        }

        return null;
    }

    private TimerTask createCheckStatusTask(String conversationQuery, StatusDialog dialog, Consumer<String> callback) {
        return new TimerTask() {
            AtomicBoolean done = new AtomicBoolean(false);
            int i = 0;
            @Override
            public void run() {
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    try {
                        if(done.get())
                            return;
                        String response = getRequest(GET_STATUS_URL + conversationQuery, STATUS_AUTH_ID);
                        String status = getStatus(response);
                        if(isAnswerCompleted(status)) {
                            done.set(true);
                            final String finalResponse = getAnswer(getRequest(GET_ANSER_URL + conversationQuery, QUESTION_AUTH_ID));
                            System.out.println(finalResponse);
                            SwingUtilities.invokeLater(() -> dialog.close(0));
                            ApplicationManager.getApplication().invokeLater(() -> {
                                callback.accept(finalResponse);
                            });
                        }else {
                            SwingUtilities.invokeLater(() -> dialog.setMessage(String.format("Generating status: %s .... %d", status, i++)));
                            if(i == 30)
                                SwingUtilities.invokeLater(() -> dialog.setMessage("Generate model timeout!" + i++));
                        }
                    } catch (Exception e) {
                        ApplicationManager.getApplication().invokeLater(() -> failed(e));
                        SwingUtilities.invokeLater(() -> dialog.dispose());
                    }
                });

            }
        };
    }

    // 轮询获取对话结果（非流式响应）
    private String getChatResult(CloseableHttpClient httpClient, String conversationId, String chatId) throws Exception {
        // 简化实现：实际需轮询直到 status 为 completed
        Thread.sleep(2000); // 等待 2 秒后查询结果
        HttpPost request = new HttpPost("https://api.coze.cn/v1/chat/" + chatId + "/messages");
        setHeader(request);

        String response = httpClient.execute(request, new BasicResponseHandler());
        ChatResultResponse resultResponse = gson.fromJson(response, ChatResultResponse.class);
        if (resultResponse.code != 0) {
            throw new RuntimeException("获取对话结果失败：" + resultResponse.msg);
        }
        return resultResponse.data.get(0).content; // 返回智能体回复
    }
    static class ChatRequest {
        public String bot_id;
        public String user_id;
        public boolean stream = false;
        public boolean auto_save_history = true;
        public List<Message> additional_messages;
    }


    static class Message {
        public String role = "user";
        public String content;
        public String content_type = "text";
        public String type = "question";
        public Message(String content) {this.content = content;}
    }

    static class ChatResponse {
        public int code;
        public String msg;
        public ChatData data;
    }

    static class ChatData {
        public String conversation_id;
        public String id; // chat_id
    }

    static class ChatResultResponse {
        public int code;
        public String msg;
        public List<Message> data;
    }
}
