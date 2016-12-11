package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.widgets.Tree;

public interface CellEditorProvider {
   public abstract CellEditor createCellEditor(Tree tree);
}
