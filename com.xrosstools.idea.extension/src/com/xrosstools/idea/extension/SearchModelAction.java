package com.xrosstools.idea.extension;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.xrosstools.idea.gef.EditorPanel;
import com.xrosstools.idea.gef.parts.AbstractTreeEditPart;
import com.xrosstools.idea.gef.util.IPropertyDescriptor;
import com.xrosstools.idea.gef.util.IPropertySource;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.util.ArrayList;
import java.util.List;

public class SearchModelAction extends AnAction {
    private static final String TITLE = "Search";
    private static final String MESSAGE = "Text";

    private EditorPanel editorPanel;

    public SearchModelAction(EditorPanel editorPanel) {
        super(TITLE, "Search in model", ExtensionIcons.SEARCH);
        this.editorPanel = editorPanel;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        Messages.InputDialog dialog = new Messages.InputDialog(TITLE, MESSAGE, ExtensionIcons.SEARCH, "", new InputValidator() {
            @Override
            public boolean checkInput(String s) {
                return true;
            }

            @Override
            public boolean canClose(String s) {
                return true;
            }
        });
        dialog.show();

        List<Entry> found = new ArrayList<>();
        if(dialog.getExitCode() == 0) {
            search(dialog.getInputString(), found, editorPanel.getTreeRoot());
            if(found.isEmpty())
                Messages.showErrorDialog("No model found", "Error");
            else
                new ListDialog(anActionEvent.getProject(), found).show();
        }
    }

    private void search(String text, List<Entry> found, AbstractTreeEditPart part) {
        if(part == null)
            return;

        Object model = part.getModel();
        if(model instanceof IPropertySource && contains(text, (IPropertySource)model)) {
            Entry entry = new Entry();
            entry.model = model;
            entry.name = part.getText();
            found.add(entry);
        }

        for(Object child: part.getChildren()) {
            if(child != null && child instanceof AbstractTreeEditPart)
                search(text, found, (AbstractTreeEditPart)child);
        }
    }

    private boolean contains(String text, IPropertySource model) {
        for(IPropertyDescriptor descriptor: model.getPropertyDescriptors()){
            String category = descriptor.getCategory();
            Object value = category == null ? model.getPropertyValue(descriptor.getId()) : model.getPropertyValue(category, descriptor.getId());
            if(value != null && value.toString().contains(text))
                return true;
        }

        return false;
    }

    private class Entry {
        String name;
        Object model;
        public String toString() {
            return name;
        }
    }

    public class ListDialog extends DialogWrapper {
        private JBList<Entry> list;
        private final List<Entry> items;

        public ListDialog(Project project, List<Entry> items) {
            super(project);
            this.items = items;
            init();
            setTitle(String.format("Found %d occurrences", items.size()));
        }

        @Override
        protected JComponent createCenterPanel() {
            list = new JBList<Entry>(items);
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            list.setSelectedIndex(0);

            list.addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    if (!e.getValueIsAdjusting()) { // 确保只触发一次
                        Entry selected = list.getSelectedValue();
                        editorPanel.selectModel(selected.model);
                    }
                }
            });

            return new JBScrollPane(list);
        }
    }
}
