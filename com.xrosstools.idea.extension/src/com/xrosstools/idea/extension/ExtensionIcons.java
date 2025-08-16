package com.xrosstools.idea.extension;

import com.intellij.openapi.util.IconLoader;
import com.xrosstools.idea.gef.GefIcons;

import javax.swing.*;

public interface ExtensionIcons {
    Icon SEARCH = IconLoader.getIcon("/icons/search.png", ExtensionIcons.class);
    Icon EXPORT_PDF = IconLoader.getIcon("/icons/export_pdf.png", ExtensionIcons.class);
}
