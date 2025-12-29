package com.xrosstools.idea.extension.modelgen;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.function.Consumer;

public class CreateAgentWizard extends DialogWrapper implements CozeConstants{
    private final Project project;
    private JPanel cardsPanel;
    private CardLayout cardLayout;

    private int currentStep = 0;
    private static final int TOTAL_STEPS = 5;

    // 预定义选项
    private final String[] urls = CozeConstants.SITES;

    private Map<String, String> spaceIdMap;

    private CozeAgentCreator creator;

    // UI 组件
    private ComboBox<String> siteCombo;
    private JTextField tokenField;
    private ComboBox<String> spaceIdCombo;
    private JCheckBox unitCheckBox;
    private JCheckBox stateCheckBox;
    private JCheckBox decisionCheckBox;
    private JCheckBox behaviorCheckBox;
    private JCheckBox flowCheckBox;

    private JLabel unitCreationStatus;
    private JLabel stateCreationStatus;
    private JLabel decisionCreationStatus;
    private JLabel flowCreationStatus;
    private JLabel behaviorCreationStatus;


    public CreateAgentWizard(Project project) {
        super(project);
        this.project = project;
        setTitle("Agent Configuration Wizard");
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
        cardsPanel.add(createStep5(), "step5");

        showStep(0);
        return cardsPanel;
    }

    private JPanel createStep1() {
        JPanel panel = new JPanel(new FlowLayout());
        panel.add(new JLabel("Select Site:"));

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
        panel.add(new JLabel("Select Space:"));

        spaceIdCombo = new ComboBox<>();
        spaceIdCombo.setPreferredSize(new Dimension(200, 25));
        panel.add(spaceIdCombo);

        return panel;
    }

    private JPanel createStep4() {
        // 创建顶部的复选框面板
        JPanel checkBoxPanel = new JPanel(new GridLayout(5, 1, 0, 5)); // 5行1列，垂直间距5
        checkBoxPanel.setBorder(BorderFactory.createTitledBorder("Select Xross Features"));

        // 创建5个复选框
        unitCheckBox = new JCheckBox(XROSS_UNIT);
        decisionCheckBox = new JCheckBox(XROSS_DECISION);
        stateCheckBox = new JCheckBox(XROSS_STATE);
        behaviorCheckBox = new JCheckBox(XROSS_BEHAVIOR);
        flowCheckBox = new JCheckBox(XROSS_FLOW);

        // 将所有复选框添加到面板
        checkBoxPanel.add(unitCheckBox);
        checkBoxPanel.add(decisionCheckBox);
        checkBoxPanel.add(stateCheckBox);
        checkBoxPanel.add(behaviorCheckBox);
        checkBoxPanel.add(flowCheckBox);

        return checkBoxPanel;
    }

    private JPanel createStep5() {
        JPanel panel = new JPanel(new BorderLayout());

        // 创建顶部的复选框面板
        JPanel statusPanel = new JPanel(new GridLayout(5, 1, 0, 5)); // 5行1列，垂直间距5
        statusPanel.setBorder(BorderFactory.createTitledBorder("Ready to create"));

        // 创建5个状态标签
        unitCreationStatus = new JLabel();
        decisionCreationStatus = new JLabel();
        stateCreationStatus = new JLabel();
        behaviorCreationStatus = new JLabel();
        flowCreationStatus = new JLabel();

        // 将所有复选框添加到面板
        statusPanel.add(unitCreationStatus);
        statusPanel.add(decisionCreationStatus);
        statusPanel.add(stateCreationStatus);
        statusPanel.add(behaviorCreationStatus);
        statusPanel.add(flowCreationStatus);

        // 创建按钮
        JButton createButton = new JButton("Create Bot");
        createButton.addActionListener(e -> {
            createBot();
        });

        // 将按钮放在单独的面板中以获得更好的布局
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(createButton);

        // 使用嵌套面板来组织布局
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(statusPanel, BorderLayout.CENTER);
        northPanel.add(buttonPanel, BorderLayout.SOUTH);

        panel.add(northPanel, BorderLayout.NORTH);

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
        } else if (step == 2) {
            String site = ((String) siteCombo.getSelectedItem());
            String token = tokenField.getText();
            creator = new CozeAgentCreator(token, site);
            loadSpaceList();
        } else if (step == 4) {
            initStatus();
        }
    }

    private void loadCurrentConfig() {
        CozeAgentConfig config = CozeAgentConfig.getInstance();

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

    private void loadSpaceList() {
        try {
            spaceIdMap = creator.getSpaces();
            spaceIdCombo.removeAllItems();
            for(String id: spaceIdMap.keySet())
                spaceIdCombo.addItem(id);
        } catch (Exception e) {
            String site = (String) siteCombo.getSelectedItem();
            Messages.showErrorDialog(e.getMessage(),"Error");
            close(CANCEL_EXIT_CODE);
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
        //Show the creation status panel
        showStep(4);

        String site = (String) siteCombo.getSelectedItem();
        String token = tokenField.getText();
        String spaceId = spaceIdMap.get(spaceIdCombo.getSelectedItem());

        if (site == null || token.isEmpty() || spaceId == null) {
            Messages.showErrorDialog("Please complete all previous steps first!", "Error");
            return;
        }

        CozeAgentConfig config = CozeAgentConfig.getInstance();
        // 在创建时获取复选框的状态
        if(unitCheckBox.isSelected()) {
            createBot(spaceId, XROSS_UNIT, unitCreationStatus, id -> config.setXunitBotId(id));
        }

        if(decisionCheckBox.isSelected()) {
            createBot(spaceId, XROSS_DECISION, decisionCreationStatus, id -> config.setXdecisionBotId(id));
        }

        if(stateCheckBox.isSelected()) {
            createBot(spaceId, XROSS_STATE, stateCreationStatus, id -> config.setXstateBotId(id));
        }

        if(behaviorCheckBox.isSelected()) {
            createBot(spaceId, XROSS_BEHAVIOR, behaviorCreationStatus, id -> config.setXbehaviorBotId(id));
        }

        if(flowCheckBox.isSelected()) {
            createBot(spaceId, XROSS_FLOW, flowCreationStatus, id -> config.setXflowBotId(id));
        }
    }

    private void initStatus() {
        initStatus(unitCreationStatus, XROSS_UNIT, unitCheckBox.isSelected());
        initStatus(decisionCreationStatus, XROSS_DECISION, decisionCheckBox.isSelected());
        initStatus(stateCreationStatus, XROSS_STATE, stateCheckBox.isSelected());
        initStatus(behaviorCreationStatus, XROSS_BEHAVIOR, behaviorCheckBox.isSelected());
        initStatus(flowCreationStatus, XROSS_FLOW, flowCheckBox.isSelected());
    }

    private void initStatus(JLabel label, String toolName, boolean selected) {
        label.setText(toolName + (selected ? " is selected" : " is skipped"));

        if(selected)
            label.getFont().deriveFont(Font.BOLD);
        else
            label.getFont().deriveFont(Font.PLAIN);
    }

    private void setStatus(JLabel label, String text) {
        SwingUtilities.invokeLater(() -> label.setText(text));
    }

    private void createBot(String spaceId, String toolName, JLabel status, Consumer<String> botIdSaver) {
        status.setText("Creating " + toolName + " modeler...");
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                String botId = null;
                botId = creator.createBot(spaceId, toolName);
                creator.publishBot(botId);
                setStatus(status, toolName + " modeler created: " + botId);
                botIdSaver.accept(botId);
            } catch (Exception e) {
                setStatus(status, toolName + " modeler: " + " creation failed");
                Messages.showErrorDialog("Failed to create bot:" + e.getMessage(), "Error");
                close(CANCEL_EXIT_CODE);
            }
        });
    }

    private void saveConfig() {
        CozeAgentConfig config = CozeAgentConfig.getInstance();

        config.setSite((String) siteCombo.getSelectedItem());
        config.setToken(tokenField.getText());
        config.setSpaceId((String) spaceIdCombo.getSelectedItem());

        // botId 在创建时已经保存
    }
}