package com.xrosstools.idea.extension.modelgen;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.messages.MessageDialog;
import com.xrosstools.idea.extension.ExtensionIcons;
import com.xrosstools.idea.gef.EditorPanel;
import org.jetbrains.annotations.NotNull;

public class ModifyModelAction extends AnAction {
    private static final String TITLE = "Modify";
    private static final String MESSAGE = "Text";
    private static final String NODE_SEPARATOR = "/";
    private CozeGenerateModelExtension modelExtension;
    private String modelType;

    private String MODIFY_REQUEST_TEMPLATE =
            "Modify model file according to requirement.\n" +
            "Requirement:\n" +
            "%s\n" +
            "\n" +
            "Original model:\n%s";

    private EditorPanel editorPanel;

    public ModifyModelAction(EditorPanel editorPanel) {
        super(TITLE, "Modify model by agent", ExtensionIcons.MODIFY);
        this.editorPanel = editorPanel;
        this.modelExtension = new CozeGenerateModelExtension();
        this.modelType = editorPanel.getFile().getExtension();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        if(!modelExtension.isGenerateModelSupported(modelType))
            return;

        ModifyModelDialog dialog = new ModifyModelDialog("");

        if (dialog.showAndGet()) {
            String requirement = dialog.getText();
            String modelText = FileDocumentManager.getInstance().getDocument(editorPanel.getFile()).getText();
            String request = String.format(MODIFY_REQUEST_TEMPLATE, requirement, modelText);
            modelExtension.generateModel(request, generated->update(anActionEvent.getProject(), generated));
        }
    }

    private void update(Project project, String newModelText) {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                try {
                    editorPanel.getFile().setBinaryContent(newModelText.getBytes(editorPanel.getFile().getCharset()));
                } catch (Throwable e) {
                    throw new IllegalStateException("Can not save document", e);
//                    Messages.showErrorDialog(project, e.getMessage(), "Error");
                }
            }
        });
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        Presentation presentation = e.getPresentation();
        presentation.setEnabled(modelExtension.isGenerateModelSupported(modelType));
    }
}
