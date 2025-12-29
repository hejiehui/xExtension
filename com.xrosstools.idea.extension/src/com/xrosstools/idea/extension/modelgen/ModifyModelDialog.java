package com.xrosstools.idea.extension.modelgen;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;
import java.awt.*;

public class ModifyModelDialog extends DialogWrapper {
    private JTextArea textArea;
    private String initialText;

    public ModifyModelDialog(String initialText) {
        super(true); // true 表示模态对话框
        this.initialText = initialText;
        setTitle("Modification Requirement");
        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        // 创建主面板
        JPanel panel = new JPanel(new BorderLayout(0, 5));

        // 创建标签
        JLabel label = new JLabel("Modification requirement:");
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        panel.add(label, BorderLayout.NORTH);

        // 创建文本区域
        textArea = new JTextArea(10, 40); // 10行，40列
        if (initialText != null) {
            textArea.setText(initialText);
        }
        textArea.setLineWrap(true); // 自动换行
        textArea.setWrapStyleWord(true); // 按单词换行

        // 添加滚动条
        JScrollPane scrollPane = new JBScrollPane(textArea);
        scrollPane.setBorder(BorderFactory.createEtchedBorder());
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    public String getText() {
        return textArea.getText();
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return textArea; // 对话框打开时自动聚焦到文本区域
    }
}
