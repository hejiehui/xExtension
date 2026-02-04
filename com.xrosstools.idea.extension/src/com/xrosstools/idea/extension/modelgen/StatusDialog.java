package com.xrosstools.idea.extension.modelgen;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;

import javax.swing.*;
import java.awt.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class StatusDialog extends DialogWrapper {
    private JLabel statusLabel;
    private Timer timer;
    private boolean streamMode;
    private JTextArea descriptionArea;
    private AtomicBoolean cancelFlag;

    public StatusDialog(boolean streamMode, AtomicBoolean cancelFlag) {
        super(true); // 设置为模态对话框
        setTitle("Generating model");
        setResizable(false);
        this.streamMode = streamMode;
        this.cancelFlag = cancelFlag;
        init();
    }

    @Override
    protected Action[] createActions() {
        // 返回只包含Cancel按钮的数组
        return new Action[]{getCancelAction()};
    }

    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        statusLabel = new JLabel("Generating(may take a few seconds)....", SwingConstants.CENTER);
        statusLabel.setFont(new Font(statusLabel.getFont().getName(), Font.PLAIN, 14));

        if(streamMode) {
            panel.setPreferredSize(new Dimension(400, 600));
            panel.add(statusLabel, BorderLayout.NORTH);

            descriptionArea = new JBTextArea(20, 50);
            descriptionArea.setLineWrap(true);
            descriptionArea.setWrapStyleWord(true);

            JBScrollPane scrollPane = new JBScrollPane(descriptionArea);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

            panel.add(scrollPane, BorderLayout.CENTER);
        }else {
            panel.setPreferredSize(new Dimension(400, 100));
            panel.add(statusLabel, BorderLayout.CENTER);
        }

        return panel;
    }

    @Override
    public void doCancelAction() {
        cancelFlag.set(true);
        super.doCancelAction();
    }

    @Override
    protected void dispose() {
        // 对话框关闭时停止定时器
        stopPolling();
        super.dispose();
    }

    public void setMessage(String message) {
        statusLabel.setText(message);
    }

    public void appendFeedback(String feedback) {
        descriptionArea.append(feedback);
    }

    /**
     * 开始定时轮询
     */
    public void startPolling(TimerTask task) {
        timer = new Timer("API-Polling-Timer", true);
        timer.scheduleAtFixedRate(task, 0, 1000); // 立即开始，然后每隔1秒执行一次
    }

    /**
     * 停止定时轮询
     */
    public void stopPolling() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}
