package com.xrosstools.idea.extension.modelgen;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class ConfigAgentDialog extends DialogWrapper {
    private final String[] sites = CozeAgentCreator.SITES;
    private ComboBox<String> siteCombo;
    private JTextField tokenField;
    private JTextField botIdField;

    public ConfigAgentDialog(@Nullable Project project) {
        super(project);
        setTitle("Configure Agent");
        setOKButtonText("Save");
        init();
        loadCurrentConfig();
    }

    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        panel.setPreferredSize(new Dimension(400, 80));

        JPanel labelPanel = new JPanel(new GridLayout(3, 1, 10, 20));

        labelPanel.add(new JLabel("Site:"));
        labelPanel.add(new JLabel("Token:"));
        labelPanel.add(new JLabel("Bot ID:"));

        JPanel inputPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        // Site 输入
        siteCombo = new ComboBox<>(sites);
        siteCombo.setPreferredSize(new Dimension(300, 25));
        inputPanel.add(siteCombo);

        // Token 输入
        tokenField = new JTextField();
        tokenField.setPreferredSize(new Dimension(300, 25));
        inputPanel.add(tokenField);

        // Bot ID 输入
        botIdField = new JTextField();
        inputPanel.add(botIdField);

        panel.add(labelPanel);
        panel.add(inputPanel);

        return panel;
    }

    private void loadCurrentConfig() {
        CozeAgentConfig config = CozeAgentConfig.getInstance();
        siteCombo.setSelectedItem(config.getSite() != null ? config.getSite() : "");
        tokenField.setText(config.getToken() != null ? config.getToken() : "");
        botIdField.setText(config.getBotId() != null ? config.getBotId() : "");
    }

    @Override
    protected void doOKAction() {
        // 保存配置
        CozeAgentConfig config = CozeAgentConfig.getInstance();
        config.setSite((String) siteCombo.getSelectedItem());
        config.setToken(tokenField.getText().trim());
        config.setBotId(botIdField.getText().trim());

        super.doOKAction();
    }
}