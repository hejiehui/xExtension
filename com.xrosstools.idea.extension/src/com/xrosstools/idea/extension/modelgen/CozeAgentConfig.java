package com.xrosstools.idea.extension.modelgen;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(
        name = "CozeAgentConfig",
        storages = {@Storage("CozeAgentConfig.xml")}
)
public class CozeAgentConfig implements PersistentStateComponent<CozeAgentConfig>, CozeConstants {
    private String site;
    private String spaceId;
    private String xunitBotId;
    private String xstateBotId;
    private String xdecisionBotId;
    private String xbehaviorBotId;
    private String xflowBotId;

    @Nullable
    @Override
    public CozeAgentConfig getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull CozeAgentConfig state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    // Getters and Setters
    public String getSite() { return site; }
    public void setSite(String site) { this.site = site; }

    public String getSpaceId() { return spaceId; }
    public void setSpaceId(String spaceId) { this.spaceId = spaceId; }

    public static CozeAgentConfig getInstance() {
        return ServiceManager.getService(CozeAgentConfig.class);
    }

    public static String getToken() {
        return PasswordSafe.getInstance().getPassword(new CredentialAttributes(SERVICE_NAME, USER_NAME));
    }

    public static void setToken(String token) {
        PasswordSafe.getInstance().setPassword(new CredentialAttributes(SERVICE_NAME, USER_NAME), token);
    }

    public String getXunitBotId() {
        return xunitBotId;
    }

    public void setXunitBotId(String xunitBotId) {
        this.xunitBotId = xunitBotId;
    }

    public String getXstateBotId() {
        return xstateBotId;
    }

    public void setXstateBotId(String xstateBotId) {
        this.xstateBotId = xstateBotId;
    }

    public String getXdecisionBotId() {
        return xdecisionBotId;
    }

    public void setXdecisionBotId(String xdecisionBotId) {
        this.xdecisionBotId = xdecisionBotId;
    }

    public String getXflowBotId() {
        return xflowBotId;
    }

    public void setXflowBotId(String xflowBotId) {
        this.xflowBotId = xflowBotId;
    }

    public String getXbehaviorBotId() {
        return xbehaviorBotId;
    }

    public void setXbehaviorBotId(String xbehaviorBotId) {
        this.xbehaviorBotId = xbehaviorBotId;
    }
}