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
    private JTextField xunitBotIdField;
    private JTextField xstateBotIdField;
    private JTextField xdecisionBotIdField;
    private JTextField xbehaviorBotIdField;
    private JTextField xflowBotIdField;

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

        JPanel labelPanel = new JPanel(new GridLayout(7, 1, 10, 20));

        labelPanel.add(new JLabel("Site:"));
        labelPanel.add(new JLabel("Token:"));
        labelPanel.add(new JLabel("Xross Unit Bot ID:"));
        labelPanel.add(new JLabel("Xross State Bot ID:"));
        labelPanel.add(new JLabel("Xross Decision Bot ID:"));
        labelPanel.add(new JLabel("Xross Behavior Bot ID:"));
        labelPanel.add(new JLabel("Xross Flow Bot ID:"));

        JPanel inputPanel = new JPanel(new GridLayout(7, 1, 10, 10));
        // Site 输入
        siteCombo = new ComboBox<>(sites);
        siteCombo.setPreferredSize(new Dimension(300, 25));
        inputPanel.add(siteCombo);

        // Token 输入
        tokenField = new JTextField();
        tokenField.setPreferredSize(new Dimension(300, 25));
        inputPanel.add(tokenField);

        // Bot ID 输入
        xunitBotIdField = new JTextField();
        inputPanel.add(xunitBotIdField);

        xstateBotIdField = new JTextField();
        inputPanel.add(xstateBotIdField);

        xdecisionBotIdField = new JTextField();
        inputPanel.add(xdecisionBotIdField);

        xbehaviorBotIdField = new JTextField();
        inputPanel.add(xbehaviorBotIdField);

        xflowBotIdField = new JTextField();
        inputPanel.add(xflowBotIdField);

        panel.add(labelPanel);
        panel.add(inputPanel);

        return panel;
    }

    private void loadCurrentConfig() {
        CozeAgentConfig config = CozeAgentConfig.getInstance();
        siteCombo.setSelectedItem(config.getSite() != null ? config.getSite() : "");
        tokenField.setText(config.getToken() != null ? config.getToken() : "");

        xunitBotIdField.setText(config.getXunitBotId() != null ? config.getXunitBotId() : "");
        xstateBotIdField.setText(config.getXstateBotId() != null ? config.getXstateBotId() : "");
        xdecisionBotIdField.setText(config.getXdecisionBotId() != null ? config.getXdecisionBotId() : "");
        xbehaviorBotIdField.setText(config.getXbehaviorBotId() != null ? config.getXbehaviorBotId() : "");
        xflowBotIdField.setText(config.getXflowBotId() != null ? config.getXflowBotId() : "");
    }

    @Override
    protected void doOKAction() {
        // 保存配置
        CozeAgentConfig config = CozeAgentConfig.getInstance();
        config.setSite((String) siteCombo.getSelectedItem());
        config.setToken(tokenField.getText().trim());

        config.setXunitBotId(xunitBotIdField.getText().trim());
        config.setXstateBotId(xstateBotIdField.getText().trim());
        config.setXdecisionBotId(xdecisionBotIdField.getText().trim());
        config.setXbehaviorBotId(xbehaviorBotIdField.getText().trim());
        config.setXflowBotId(xflowBotIdField.getText().trim());

        super.doOKAction();
    }
}