package com.xrosstools.idea.extension.modelgen;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.xrosstools.idea.gef.extensions.GenerateModelExtension;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;


public class CozeGenerateModelExtension implements CozeConstants, GenerateModelExtension {
    private static final String USER_ID = "xross_tools_user";
    private static final String TOKEN_HEADER = "Bearer ";

    private static final String CONVERSATION_CHAT_ID_TPL = "?conversation_id=%s&chat_id=%s";

    private static final String COMPLETED = "completed";
    private static final String IN_PROGRESS = "in_progress";
    private static final String FAILED = "failed";

    private static final String TYPE = "type";
    private static final String ANSWER = "answer";

    private static final String CONTENT = "content";

    private static final String MODEL_START ="<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    private static final Gson gson = new Gson();

    private String apiUrl;

    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static final AtomicReference<String> tokenRef = new AtomicReference<>(null);
    private static final AtomicReference<CozeAgentConfig> configRef = new AtomicReference<>(null);

    private String modelType;
    private String botId;

    private static Set<String> notified = new HashSet<>();

    private void init() {
        if(!initialized.get()) {
            initialized.set(true);
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                configRef.set(CozeAgentConfig.getInstance());
                tokenRef.set(PasswordSafe.getInstance().getPassword(new CredentialAttributes(SERVICE_NAME, USER_NAME)));
            });
        }
    }

    public static void setToken(String token) {
        PasswordSafe.getInstance().setPassword(new CredentialAttributes(SERVICE_NAME, USER_NAME), token);
        tokenRef.set(token);
    }

    public static String getToken() {
        return tokenRef.get();
    }

    public static CozeAgentConfig getConfig() {
        return configRef.get();
    }

    @Override
    public boolean isGenerateModelSupported(String modelType) {
        if(!initialized.get()) {
            init();
            return false;
        }

        if(configRef.get() == null || tokenRef.get() == null) {
            return false;
        }

        //Get agent config
        CozeAgentConfig config = configRef.get();
        if(config.getSite() == null || config.getSite().trim().isEmpty()) {
            show("Coze Site");
            return false;
        }

        if(getToken() == null || getToken().trim().isEmpty()) {
            show("Coze Token");
            return false;
        }

        apiUrl = getApiUrl(config.getSite());

        this.modelType = modelType;
        switch (modelType) {
            case XUNIT:
                botId = config.getXunitBotId();
                break;
            case XSTATE:
                botId = config.getXstateBotId();
                break;
            case XDECISION:
                botId = config.getXdecisionBotId();
                break;
            case XBEHAVIOR:
                botId = config.getXbehaviorBotId();
                break;
            case XFLOW:
                botId = config.getXflowBotId();
                break;
            default:
                botId = null;
                break;
        }

        if(botId == null || botId.trim().isEmpty()) {
            show(modelType);
            return false;
        }

        return true;
    }

    @Override
    public void generateModel(String description, Consumer<String> callback) {
        generateModel(description, callback, false);
    }

    @Override
    public void generateModel(String description, Consumer<String> callback, boolean streamMode) {
        //Reload config in case of old version of GEF
        isGenerateModelSupported(modelType);

        if(streamMode)
            generateWithStreamMode(description, callback);
        else
            generateWithoutStreamMode(description, callback);
    }

    private void show(String configItem) {
        if(notified.contains(configItem))
            return;

        Project project;
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        if (projects.length == 0)
            return;

        project = projects[0];
        Notification notification = getNotification(configItem);
        notification.setImportant(false);
        Notifications.Bus.notify(notification, project);

        notified.add(configItem);
    }

    private  Notification getNotification(String configItem) {
        String content = configItem + " is not configured";//.<br> <a href="action:Xross.TokenBot.Wizard">Agent Configuration Wizard</a>

        // 创建通知对象
        Notification notification = new Notification(
                "Xross.Notification.Group",
                "Agent Configuration Incomplete",
                content,
                NotificationType.INFORMATION
        );

        notification.addAction(new AnAction("Agent Configuration Wizard") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                new CreateAgentWizardAction().actionPerformed(e);
            }
        });
        return notification;
    }

    private void generateWithStreamMode(String description, Consumer<String> callback) {
        ApplicationManager.getApplication().invokeLater(() -> {
            AtomicBoolean cancelFlag = new AtomicBoolean(false);
            StatusDialog dialog = new StatusDialog(true, cancelFlag);

            // 在后台线程执行HTTP请求
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                String promptPayload = buildPromptPayload(description, true);
                generateWithStreamMode(promptPayload, callback, dialog, cancelFlag);
            });

            // 显示对话框（阻塞直到关闭）
            dialog.show();
        });

    }
    private void generateWithStreamMode(String promptPayload, Consumer<String> callback, StatusDialog dialog, AtomicBoolean cancelFlag) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(apiUrl + CHAT_CMD);
            setHeader(httpPost);
            StringEntity strEntity = new StringEntity(promptPayload, StandardCharsets.UTF_8);
            httpPost.setEntity(strEntity);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    // 3. 逐行读取流式响应
                    BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent(), StandardCharsets.UTF_8));
                    String line;
                    String fullResponse = null;

                    boolean completed = false;
                    while ((line = reader.readLine()) != null && fullResponse == null && !cancelFlag.get()) {
                        if (line.startsWith("event")) {
                            completed = handleStreamEvent(line, dialog, completed);
                        } else if (line.startsWith("data")) {
                            fullResponse = handleStreamData(line, dialog, completed);
                        }
                    }

                    EntityUtils.consume(entity);

                    if(cancelFlag.get() || fullResponse == null)
                        return;

                    appendFeedback(dialog, "\nGeneration completed, please hold on a second...");
                    close(dialog);
                    generate(callback, fullResponse);
                }
            }

        } catch (Exception e) {
            // 错误处理
            ApplicationManager.getApplication().invokeLater(() -> failed(e));
        }
    }

    private void generateWithoutStreamMode(String description, Consumer<String> callback){
        try {
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                String promptPayload = buildPromptPayload(description, false);
                HttpPost httpPost = new HttpPost(apiUrl + CHAT_CMD);
                setHeader(httpPost);
                StringEntity entity = new StringEntity(promptPayload, StandardCharsets.UTF_8);
                httpPost.setEntity(entity);

                try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                    int statusCode = response.getStatusLine().getStatusCode();

                    String responseBody = checkResponse(statusCode, response);

                    String conversationQuery = buildConversationQuery(responseBody);

                    // 切换到UI线程更新界面
                    ApplicationManager.getApplication().invokeLater(() -> {
                        AtomicBoolean cancelFlag = new AtomicBoolean(false);
                        StatusDialog dialog = new StatusDialog(false, cancelFlag);
                        dialog.startPolling(createCheckStatusTask(conversationQuery, dialog, callback, cancelFlag));
                        dialog.show();
                    });
                }
            }

        } catch (Exception e) {
            // 错误处理
            ApplicationManager.getApplication().invokeLater(() -> failed(e));
        }
    }

    private String request(String url) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

            HttpGet httpGet = new HttpGet(url);
            setHeader(httpGet);

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                int statusCode = response.getStatusLine().getStatusCode();
                return checkResponse(statusCode, response);
            }
        }
    }

    private String checkResponse(int statusCode, CloseableHttpResponse response) throws IOException {
        String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        if (statusCode == 200) {
            return responseBody;
        } else {
            throw new RuntimeException("HTTP error: " + statusCode + ", response: " + responseBody);
        }
    }

    private void setHeader(HttpRequestBase httpRequest) {
        httpRequest.setHeader("Content-Type", "application/json");
        httpRequest.setHeader("Authorization", TOKEN_HEADER + tokenRef.get());
        httpRequest.setHeader("Accept-Charset", "UTF-8");
        httpRequest.setHeader("Accept", "text/xml; charset=UTF-8");
    }

    private void failed(Exception e) {
        Messages.showErrorDialog("Failed when calling API: " + e.getMessage(), "Error");
    }

    // 手动构建JSON请求体
    private String buildPromptPayload(String content, boolean streamMode) {
        // 构建请求JSON数据
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.bot_id = botId;
        chatRequest.user_id = USER_ID;
        chatRequest.stream = streamMode;
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

    private String getAnswer(String response) {
        Gson gson = new Gson();
        JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
        JsonArray data = jsonResponse.getAsJsonArray("data");
        for(int i = 0; i < data.size();i++) {
            JsonObject  element = data.get(i).getAsJsonObject();
            if(ANSWER.equals(element.get(TYPE).getAsString())) {
                String rawContent = element.get(CONTENT).getAsString();
                return rawContent;
            }
        }

        return null;
    }

    private TimerTask createCheckStatusTask(String conversationQuery, StatusDialog dialog, Consumer<String> callback, AtomicBoolean cancelFlag) {
        return new TimerTask() {
            AtomicBoolean done = new AtomicBoolean(false);
            int i = 0;
            @Override
            public void run() {
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    try {
                        if(done.get() || cancelFlag.get())
                            return;

                        String responseStr = request(apiUrl + GET_STATUS_CMD + conversationQuery);
                        StatusResponse statusResponse = gson.fromJson(responseStr, StatusResponse.class);
                        String status = statusResponse.data.status;
                        switch (status) {
                            case COMPLETED:
                                if(done.get() || cancelFlag.get())
                                    return;
                                done.set(true);
                                String finalResponse = getAnswer(request(apiUrl + GET_ANSWER_CMD + conversationQuery));
                                close(dialog);
                                generate(callback, finalResponse);
                                break;
                            case IN_PROGRESS:
                                showStatus(dialog, String.format("%s .... %d", status, i++));
                                break;
                            case FAILED:
                                showStatus(dialog, String.format("%s .... %s", status, statusResponse.data.last_error.msg));
                                dialog.stopPolling();
                                done.set(true);
                                return;
                        }
                    } catch (Exception e) {
                        ApplicationManager.getApplication().invokeLater(() -> failed(e));
                        close(dialog);
                    }
                });

            }
        };
    }

    private boolean handleStreamEvent(String line, StatusDialog dialog, boolean completed) {
        if (line.startsWith("event")) {
            String eventType = line.substring(6);
            switch (eventType) {
                case "conversation.chat.created":
                    showStatus(dialog, "created");
                    break;
                case "conversation.chat.in_progress":
                case "conversation.message.delta":
                    showStatus(dialog, "in progress");
                    break;
                case "conversation.message.completed":
                case "conversation.chat.completed":
                    // 消息完成，返回完整结果
                    showStatus(dialog, "completed");
                    return true;
                case "conversation.chat.failed":
                case "error":
                    // 错误处理
                    showStatus(dialog, "error");
                    break;
            }
        }
        return false;
    }

    private String handleStreamData(String line, StatusDialog dialog, boolean completed) {
        //Should never be here
        if(line.startsWith("data:\"[DONE]\""))
            return null;

        Data data = gson.fromJson(line.substring("data:".length()), Data.class);
        if ("error".equals(data.msg)) {
            appendFeedback(dialog, String.format("\nError code: %s\nMessage: %s", data.code, data.msg));
            return null;
        }

        if ("failed".equals(data.status)) {
            appendFeedback(dialog, String.format("\nError code: %s\nMessage: %s", data.last_error.code, data.last_error.msg));
            return null;
        }

        if(data.type == null)
            return null;

        if("answer".equals(data.type)) {
            String content = data.content;
            String reasoning_content = data.reasoning_content;

            if(completed)
                return content;
            else
                appendFeedback(dialog, content.length() == 0 ? reasoning_content : content);
        } else {
            if("text".equals(data.content_type))
                appendFeedback(dialog, String.format("\n%s: %s\n", data.type, data.content_type));
        }

        return null;
    }

    private void showStatus(StatusDialog dialog, String status) {
        ApplicationManager.getApplication().invokeLater(() -> dialog.setMessage("Current status: " + status), ModalityState.any());
    }

    private void appendFeedback(StatusDialog dialog, String feedback) {
        ApplicationManager.getApplication().invokeLater(() -> dialog.appendFeedback(feedback), ModalityState.any());
    }

    private void close(StatusDialog dialog) {
        ApplicationManager.getApplication().invokeLater(() -> dialog.close(DialogWrapper.OK_EXIT_CODE), ModalityState.any());
    }

    private void generate(Consumer<String> callback, String modelContent) {
        ApplicationManager.getApplication().invokeLater(() -> callback.accept(checkAnswer(modelContent)), ModalityState.NON_MODAL);
    }

    private String checkAnswer(String rawContent) {
        rawContent = rawContent.trim();
        if(rawContent.startsWith("```xml") && rawContent.endsWith("```"))
            rawContent = rawContent.substring(6, rawContent.length() - 3).trim();

        if(rawContent.startsWith(MODEL_START))
            return rawContent;

        Messages.showErrorDialog(rawContent, "The generated content is invalid");
        throw new IllegalArgumentException("Wrong model content: " + rawContent);
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

    static class StatusResponse {
        public String code;
        public Data data;
    }
    static class Data {
//        public String id;
//        public String conversation_id;
//        public String bot_id;
//        public String role;
        public String type;
        public String content;
        public String reasoning_content;
        public String content_type;
        public String code;
        public String msg;
        public String status;
        public Error last_error;
    }

    static class Error {
        public String code;
        public String msg;
    }
}
