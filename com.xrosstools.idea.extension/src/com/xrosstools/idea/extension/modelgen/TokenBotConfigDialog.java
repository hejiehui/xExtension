package com.xrosstools.idea.extension.modelgen;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class TokenBotConfigDialog extends DialogWrapper {

    private final Project project;
    private JTextField tokenField;
    private JTextField botIdField;

    public TokenBotConfigDialog(@Nullable Project project) {
        super(project);
        this.project = project;
        setTitle("Configure Token and Bot ID");
        setOKButtonText("Save");
        init();
        loadCurrentConfig();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        panel.setPreferredSize(new Dimension(400, 80));

        // Token 输入
        panel.add(new JLabel("Token:"));
        tokenField = new JTextField();
        panel.add(tokenField);

        // Bot ID 输入
        panel.add(new JLabel("Bot ID:"));
        botIdField = new JTextField();
        panel.add(botIdField);

        return panel;
    }

    private void loadCurrentConfig() {
        XrossToolsBotConfig config = XrossToolsBotConfig.getInstance(project);
        tokenField.setText(config.getToken() != null ? config.getToken() : "");
        botIdField.setText(config.getBotId() != null ? config.getBotId() : "");
    }

    @Override
    protected void doOKAction() {
        // 保存配置
        XrossToolsBotConfig config = XrossToolsBotConfig.getInstance(project);
        config.setToken(tokenField.getText().trim());
        config.setBotId(botIdField.getText().trim());

        super.doOKAction();
    }
}