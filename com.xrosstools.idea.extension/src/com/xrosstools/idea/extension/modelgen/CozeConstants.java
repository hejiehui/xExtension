package com.xrosstools.idea.extension.modelgen;

public interface CozeConstants {
    String SERVICE_NAME = "com.xrosstools.idea.extension";
    String USER_NAME = "api_access_token";

    String PROMPTS_ROOT = "prompts/";

    String GET_SPACE_CMD = "/v1/workspaces?page_num=1&page_size=20";
    String CREATE_CMD = "/v1/bot/create";
    String PUBLISH_CMD = "/v1/bot/publish";

    String CHAT_CMD = "/v3/chat";
    String GET_STATUS_CMD = "/v3/chat/retrieve";
    String GET_ANSER_CMD = "/v3/chat/message/list";


    String MODEL_ID = "1706077826"; // 豆包模型 ID

    String[] SITES = {
            "https://www.coze.com",
            "https://www.coze.cn",
    };

    String[] APIS = {
            "https://api.coze.com",
            "https://api.coze.cn",
    };

    String[] PROMPTS = {
            "xflowPrompt.txt"
    };

    default String getApiUrl(String site) {
        return APIS[site.equals(SITES[0]) ? 0 : 1];
    }

    default String getPrompt(String site) {
        return PROMPTS[0];
    }
}
