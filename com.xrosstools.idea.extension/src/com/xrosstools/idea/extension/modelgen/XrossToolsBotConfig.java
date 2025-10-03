package com.xrosstools.idea.extension.modelgen;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(
        name = "XrossToolsBotConfig",
        storages = {@Storage("XrossToolsBotConfig.xml")}
)
public class XrossToolsBotConfig implements PersistentStateComponent<XrossToolsBotConfig> {
    private String site;
    private String url;
    private String token;
    private String spaceId;
    private String botId;

    @Nullable
    @Override
    public XrossToolsBotConfig getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull XrossToolsBotConfig state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    // Getters and Setters
    public String getSite() { return site; }
    public void setSite(String site) { this.site = site; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getSpaceId() { return spaceId; }
    public void setSpaceId(String spaceId) { this.spaceId = spaceId; }

    public String getBotId() { return botId; }
    public void setBotId(String botId) { this.botId = botId; }

    public static XrossToolsBotConfig getInstance(Project project) {
        return ServiceManager.getService(project, XrossToolsBotConfig.class);
    }
}