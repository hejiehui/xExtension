package com.xrosstools.idea.extension.modelgen;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class CreateAgentWizardAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project != null) {
            CreateAgentWizard wizard = new CreateAgentWizard(project);
            wizard.show();
        }
    }
}