package com.xrosstools.idea.extension.modelgen;

import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CozeBotCreator {
    private int id;
    private String apiUrl;
    private String promptFile;
    private String token;
    private static final String MODEL_ID = "1706077826"; // 豆包模型 ID
    private static final Gson gson = new Gson();

    public static final String[] SITES = {
            "https://api.coze.com",
            "https://api.coze.cn",
    };

    private static final String[] APIS = {
            "https://www.coze.com",
            "https://www.coze.cn",
    };

    private static final String[] PROMPTS = {
            "xflowPrompt_EN.txt",
            "xflowPrompt_CN.txt",
    };

    public CozeBotCreator(String token, String site) {
        this.id = site.equals(SITES[0]) ? 0 : 1;
        this.apiUrl = SITES[id];
        promptFile = PROMPTS[id];
        this.token = token;
    }

    public Map<String, String> getSpaces() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // 1. 构建请求 URL（支持分页参数 page_num 和 page_size）
            String url = apiUrl + "/v1/workspaces?page_num=1&page_size=20";
            HttpGet request = new HttpGet(url);

            // 2. 添加授权 Header
            request.setHeader("Authorization", "Bearer " + token);
            request.setHeader("Content-Type", "application/json");

            // 3. 发送请求并获取响应
            String response = httpClient.execute(request, new BasicResponseHandler());
            SpaceListResponse spaceListResponse = gson.fromJson(response, SpaceListResponse.class);

            // 4. 处理响应结果
            if (spaceListResponse.code == 0) {
                Map<String, String> spaceMap = new LinkedHashMap<>();
                for (OpenSpace space : spaceListResponse.data.workspaces) {
                    spaceMap.put(String.format("%s(%s)", space.name, space.id), space.id);
                }
                return spaceMap;
            } else {
                throw new RuntimeException("Failed to get space list: " + spaceListResponse.msg + " (Error code" + spaceListResponse.code + ")");
            }
        }
    }

    private String readPrompt() throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(promptFile);
        return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    }

    // 1. 创建智能体
    public String createBot(String spaceId) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost("https://api.coze.cn/v1/bot/create");
            request.setHeader("Authorization", "Bearer " + token);
            request.setHeader("Content-Type", "application/json");

            // 构建请求体
            CreateBotRequest createRequest = new CreateBotRequest();
            createRequest.space_id = spaceId;
            createRequest.name = "xflow modeler";
            createRequest.description = "Agent created by Xross Tools Extension";
            createRequest.prompt_info = new PromptInfo(readPrompt());
            createRequest.model_info_config = new ModelInfoConfig(MODEL_ID);

            String requestBody = gson.toJson(createRequest);
            request.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));

            // 发送请求
            String response = httpClient.execute(request, new BasicResponseHandler());

            CreateBotResponse createResponse = gson.fromJson(response, CreateBotResponse.class);
            if (createResponse.code != 0) {
                throw new RuntimeException("Failed to create agent: " + createResponse.msg);
            }
            return createResponse.data.bot_id;
        }
    }

    // 2. 发布智能体
    public void publishBot(String botId) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost("https://api.coze.cn/v1/bot/publish");
            request.setHeader("Authorization", "Bearer " + token);
            request.setHeader("Content-Type", "application/json");

            // 构建请求体（API渠道 ID 为 1024）
            PublishBotRequest publishRequest = new PublishBotRequest();
            publishRequest.bot_id = botId;
            publishRequest.connector_ids = new String[]{"1024"};

            String requestBody = gson.toJson(publishRequest);
            request.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));

            // 发送请求
            String response = httpClient.execute(request, new BasicResponseHandler());
            PublishBotResponse publishResponse = gson.fromJson(response, PublishBotResponse.class);
            if (publishResponse.code != 0) {
                throw new RuntimeException("Failed to publish agent: " + publishResponse.msg);
            }
        }
    }

    // 定义响应体对应的 Java 类（根据 OpenAPI 文档生成）
    static class SpaceListResponse {
        public int code;
        public String msg;
        public SpaceListData data;
        public Detail detail;
    }

    static class SpaceListData {
        public List<OpenSpace> workspaces;
        public long total_count;
    }

    static class OpenSpace {
        public String id; // Space ID
        public String name; // 空间名称
        public String workspace_type; // 类型：personal（个人）/ team（团队）
        public String role_type; // 角色：owner / admin / member
        public String enterprise_id; // 企业 ID（个人空间为空）
    }

    static class Detail {
        public String logid; // 日志 ID，用于问题排查
    }

    // 请求/响应模型类
    static class CreateBotRequest {
        public String space_id;
        public String name;
        public String description;
        public PromptInfo prompt_info;
        public ModelInfoConfig model_info_config;
    }

    static class PromptInfo {
        public String prompt;
        public PromptInfo(String prompt) { this.prompt = prompt; }
    }

    static class ModelInfoConfig {
        public String model_id;
        public ModelInfoConfig(String modelId) { this.model_id = modelId; }
    }

    static class CreateBotResponse {
        public int code;
        public String msg;
        public CreateBotData data;
    }

    static class CreateBotData {
        public String bot_id;
    }

    static class PublishBotRequest {
        public String bot_id;
        public String[] connector_ids;
    }

    static class PublishBotResponse {
        public int code;
        public String msg;
        public PublishBotData data;
    }

    static class PublishBotData {
        public String bot_id;
        public String version;
    }
}