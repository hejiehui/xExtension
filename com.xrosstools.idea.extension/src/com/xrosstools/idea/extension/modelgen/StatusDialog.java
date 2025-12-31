package com.xrosstools.idea.extension.modelgen;

import com.intellij.openapi.ui.DialogWrapper;

import javax.swing.*;
import java.awt.*;
import java.util.Timer;
import java.util.TimerTask;

public class StatusDialog extends DialogWrapper {
    private JLabel statusLabel;
    private Timer timer;

    public StatusDialog() {
        super(true); // 设置为模态对话框
        setTitle("Generating model");
        setResizable(false);
        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(400, 100));

        statusLabel = new JLabel("Generating(may take a few seconds)....", SwingConstants.CENTER);
        statusLabel.setFont(new Font(statusLabel.getFont().getName(), Font.PLAIN, 14));

        panel.add(statusLabel, BorderLayout.CENTER);
        return panel;
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
