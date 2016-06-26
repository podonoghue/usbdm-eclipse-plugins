package net.sourceforge.usbdm.deviceEditor.model;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import net.sourceforge.usbdm.deviceEditor.editor.TreeEditor;

public class TreeEditorPage implements IEditorPage {

   private TreeEditor fEditor = null;

   public TreeEditorPage() {
   }

   @Override
   public Control createComposite(Composite parent) {
      if (fEditor == null) {
         fEditor = new TreeEditor();
      }
      return fEditor.createControl(parent);
   }

   @Override
   public void update(IPage peripheralPageModel) {
      fEditor.setModel((TreeViewModel) peripheralPageModel.getModel());
   }
   
}
