package com.xrosstools.idea.extension.modelgen;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.xrosstools.idea.gef.extensions.GenerateModelExtension;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
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


public class CozeGenerateModelExtension implements CozeConstants, GenerateModelExtension {
    private static final String USER_ID = "xross_tools_user";
    private static final String TOKEN_HEADER = "Bearer ";

    private static final String CONVERSATION_CHAT_ID_TPL = "?conversation_id=%s&chat_id=%s";

    private static final int TIMEOUT = 15000;

    private static final String STATUS = "status";
    private static final String COMPLETED = "completed";

    private static final String TYPE = "type";
    private static final String ANSWER = "answer";

    private static final String CONTENT = "content";

    private static final String MODEL_START ="<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    private static final Gson gson = new Gson();

    private String apiUrl;
    private String token;
    private String modelType;
    private String botId;

    @Override
    public boolean isGenerateModelSupported(String modelType) {
        //Get agent config
        CozeAgentConfig config = CozeAgentConfig.getInstance();
        apiUrl = getApiUrl(config.getSite());
        token = config.getToken();

        this.modelType = modelType;
        if(XUNIT.equals(modelType)) {
            botId = config.getXunitBotId();
        } else if(XSTATE.equals(modelType)) {
            botId = config.getXstateBotId();
        } else if(XDECISION.equals(modelType)) {
            botId = config.getXdecisionBotId();
        } else if(XBEHAVIOR.equals(modelType)) {
            botId = config.getXbehaviorBotId();
        } else if(XFLOW.equals(modelType)) {
            botId = config.getXflowBotId();
        } else
            botId = null;

        return botId != null;
    }

    @Override
    public void generateModel(String description, Consumer<String> callback) {
        //Reload config in case of old version of GEF
        isGenerateModelSupported(modelType);

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

    private String postCozeChatAPI(String jsonPayload) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(apiUrl + CHAT_CMD);
            setHeader(httpPost);
            StringEntity entity = new StringEntity(jsonPayload, StandardCharsets.UTF_8);
            httpPost.setEntity(entity);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();

                return getResponseBody(statusCode, response);
            }
        }
    }

    private String  getRequest(String url) throws IOException {
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
        httpRequest.setHeader("Authorization", TOKEN_HEADER + token);
        httpRequest.setHeader("Accept-Charset", "UTF-8");
        httpRequest.setHeader("Accept", "text/xml; charset=UTF-8");
    }

    private void failed(Exception e) {
        Messages.showErrorDialog("Failed when calling API: " + e.getMessage(), "Error");
    }

    // 手动构建JSON请求体
    private String buildPromptPayload(String content) {
        // 构建请求JSON数据
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.bot_id = botId;
        chatRequest.user_id = USER_ID;
        chatRequest.stream = false;
        chatRequest.auto_save_history = true;
        chatRequest.additional_messages = new ArrayList<>();
        chatRequest.additional_messages.add(new Message(content));

        return gson.toJson(chatRequest);
    }

    private String buildConversationQuery(String response) {
        Gson gson = new Gson();
        ChatResponse chatResponse = gson.fromJson(response, ChatResponse.class);
        return String.format(CONVERSATION_CHAT_ID_TPL, chatResponse.data.conversation_id, chatResponse.data.id);
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
                if(rawContent.startsWith(MODEL_START))
                    return rawContent;
                Messages.showErrorDialog(rawContent, "The generated content is invalid");
                throw new IllegalArgumentException("Wrong model content: " + rawContent);
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

                        String response = getRequest(apiUrl + GET_STATUS_CMD + conversationQuery);
                        String status = getStatus(response);
                        if(isAnswerCompleted(status)) {
                            if(done.get())
                                return;
                            done.set(true);
                            String finalResponse = getAnswer(getRequest(apiUrl + GET_ANSWER_CMD + conversationQuery));
                            SwingUtilities.invokeLater(() -> dialog.dispose());
                            ApplicationManager.getApplication().invokeLater(() -> callback.accept(finalResponse));
                        }else {
                            SwingUtilities.invokeLater(() -> dialog.setMessage(String.format("Current status: %s .... %d", status, i++)));
                        }
                    } catch (Exception e) {
                        ApplicationManager.getApplication().invokeLater(() -> failed(e));
                        SwingUtilities.invokeLater(() -> dialog.dispose());
                    }
                });

            }
        };
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
        public String id;
    }

    static class ChatResultResponse {
        public int code;
        public String msg;
        public List<Message> data;
    }
}
