package com.xrosstools.idea.extension;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.xrosstools.idea.gef.ExtensionAdapter;

public class ToolbarExtension extends ExtensionAdapter {
    @Override
    public void extendToolbar(ActionGroup toolbar) {
        DefaultActionGroup actionGroup = (DefaultActionGroup)toolbar;
        if(actionGroup.getChildrenCount() > 0) {
            actionGroup.addSeparator();
        }

        actionGroup.add(new SearchModelAction(getEditorPanel()));
        actionGroup.add(new ExportPngAction(getEditorPanel()));
    }
}
