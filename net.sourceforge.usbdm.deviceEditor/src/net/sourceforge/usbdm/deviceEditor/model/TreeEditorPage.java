package net.sourceforge.usbdm.deviceEditor.model;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import net.sourceforge.usbdm.deviceEditor.editor.TreeEditor;

public class TreeEditorPage implements EditorPage {

   private TreeEditor fEditor = null;

   public TreeEditorPage() {
   }

   @Override
   public Control createComposite(Composite parent) {
      if (fEditor == null) {
         fEditor = new TreeEditor();
      }
      TreeViewer treeViewer = fEditor.createControls(parent);
      return treeViewer.getControl();
   }

   @Override
   public void update(PeripheralPageModel peripheralPageModel) {
      fEditor.setModel((TreeViewModel) peripheralPageModel);
   }
}
