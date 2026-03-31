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
    String GET_ANSWER_CMD = "/v3/chat/message/list";

    String[] SITES = {
            "https://www.coze.com",
            "https://www.coze.cn",
    };

    String[] APIS = {
            "https://api.coze.com",
            "https://api.coze.cn",
    };

    String XUNIT = "xunit";
    String XSTATE = "xstate";
    String XDECISION = "xdecision";
    String XBEHAVIOR = "xbehavior";
    String XFLOW = "xflow";

    String XROSS_UNIT = "Xross Unit";
    String XROSS_DECISION = "Xross Decision";
    String XROSS_STATE = "Xross State";
    String XROSS_BEHAVIOR = "Xross Behavior";
    String XROSS_FLOW = "Xross Flow";


    default String getApiUrl(String site) {
        return APIS[site.equals(SITES[0]) ? 0 : 1];
    }
}
