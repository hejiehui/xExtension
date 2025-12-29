package com.xrosstools.idea.extension;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.xrosstools.idea.extension.modelgen.ModifyModelAction;
import com.xrosstools.idea.gef.extensions.ToolbarExtensionAdapter;

public class ToolbarExtension extends ToolbarExtensionAdapter {
    @Override
    public void extendToolbar(ActionGroup toolbar) {
        DefaultActionGroup actionGroup = (DefaultActionGroup)toolbar;
        if(actionGroup.getChildrenCount() > 0) {
            actionGroup.addSeparator();
        }

        actionGroup.add(new ModifyModelAction(getEditorPanel()));
    }
}
