package com.xrosstools.idea.extension.modelgen;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ComboBox;

import javax.swing.*;
import java.awt.*;

public class CreateBotWizard extends DialogWrapper {
    private final Project project;
    private JPanel cardsPanel;
    private CardLayout cardLayout;

    private int currentStep = 0;
    private static final int TOTAL_STEPS = 4;

    // 预定义选项
    private final String[] urls = CozeBotCreator.SITES;

    private final String[] spaceIds = {
            "space-001",
            "space-002",
            "space-003"
    };

    private CozeBotCreator creator;

    // UI 组件
    private ComboBox<String> siteCombo;
    private JTextField tokenField;
    private ComboBox<String> spaceIdCombo;
    private JLabel resultLabel;

    public CreateBotWizard(Project project) {
        super(project);
        this.project = project;
        setTitle("Bot Configuration Wizard");
        setModal(true);
        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        cardLayout = new CardLayout();
        cardsPanel = new JPanel(cardLayout);

        // 创建四个步骤的面板
        cardsPanel.add(createStep1(), "step1");
        cardsPanel.add(createStep2(), "step2");
        cardsPanel.add(createStep3(), "step3");
        cardsPanel.add(createStep4(), "step4");

        showStep(0);
        return cardsPanel;
    }

    private JPanel createStep1() {
        JPanel panel = new JPanel(new FlowLayout());
        panel.add(new JLabel("Select AI Site:"));

        siteCombo = new ComboBox<>(urls);
        siteCombo.setPreferredSize(new Dimension(200, 25));
        panel.add(siteCombo);

        return panel;
    }

    private JPanel createStep2() {
        JPanel panel = new JPanel(new FlowLayout());
        panel.add(new JLabel("Enter Token:"));

        tokenField = new JTextField(20);
        panel.add(tokenField);

        return panel;
    }

    private JPanel createStep3() {
        JPanel panel = new JPanel(new FlowLayout());
        panel.add(new JLabel("Select Space ID:"));

        spaceIdCombo = new ComboBox<>(spaceIds);
        spaceIdCombo.setPreferredSize(new Dimension(200, 25));
        panel.add(spaceIdCombo);

        return panel;
    }

    private JPanel createStep4() {
        JPanel panel = new JPanel(new BorderLayout());

        JButton createButton = new JButton("Create Bot");
        createButton.addActionListener(e -> createBot());

        resultLabel = new JLabel("Click 'Create Bot' to generate bot ID");
        resultLabel.setHorizontalAlignment(SwingConstants.CENTER);

        panel.add(createButton, BorderLayout.NORTH);
        panel.add(resultLabel, BorderLayout.CENTER);

        return panel;
    }

    @Override
    protected JComponent createSouthPanel() {
        JPanel panel = new JPanel(new FlowLayout());

        JButton prevButton = new JButton("Previous");
        prevButton.addActionListener(e -> previousStep());

        JButton nextButton = new JButton("Next");
        nextButton.addActionListener(e -> nextStep());

        panel.add(prevButton);
        panel.add(nextButton);

        return panel;
    }

    private void showStep(int step) {
        currentStep = step;
        cardLayout.show(cardsPanel, "step" + (step + 1));

        // 加载当前配置到相应步骤
        if (step == 0) {
            loadCurrentConfig();
        }
    }

    private void loadCurrentConfig() {
        XrossToolsBotConfig config = XrossToolsBotConfig.getInstance(project);

        if (config.getSite() != null) {
            siteCombo.setSelectedItem(config.getSite());
        }
        if (config.getToken() != null) {
            tokenField.setText(config.getToken());
        }
        if (config.getSpaceId() != null) {
            spaceIdCombo.setSelectedItem(config.getSpaceId());
        }
    }

    private void previousStep() {
        if (currentStep > 0) {
            showStep(currentStep - 1);
        }
    }

    private void nextStep() {
        if (currentStep < TOTAL_STEPS - 1) {
            showStep(currentStep + 1);
        } else {
            // 最后一步，保存并关闭
            saveConfig();
            close(OK_EXIT_CODE);
        }
    }

    private void createBot() {
        String site = (String) siteCombo.getSelectedItem();
        String token = tokenField.getText();
        String spaceId = (String) spaceIdCombo.getSelectedItem();

        if (site == null || token.isEmpty() || spaceId == null) {
            resultLabel.setText("Please complete all previous steps first");
            return;
        }

        // 模拟创建bot
        String botId = "bot-" + System.currentTimeMillis();
        resultLabel.setText("Bot created successfully! ID: " + botId);

        // 保存bot ID
        XrossToolsBotConfig config = XrossToolsBotConfig.getInstance(project);
        config.setBotId(botId);
    }

    private void saveConfig() {
        XrossToolsBotConfig config = XrossToolsBotConfig.getInstance(project);

        config.setUrl((String) siteCombo.getSelectedItem());
        config.setToken(tokenField.getText());
        config.setSpaceId((String) spaceIdCombo.getSelectedItem());

        // botId 在创建时已经保存
    }
}