package net.sourceforge.usbdm.deviceEditor.model;

import net.sourceforge.usbdm.deviceEditor.editor.TreeEditor;

public abstract class TreeEditorPage implements IEditorPage {

   protected TreeEditor fEditor = null;

   public TreeEditorPage() {
   }

   public TreeEditor getEditor() {
      return fEditor;
   }

   public void setEditor(TreeEditor fEditor) {
      this.fEditor = fEditor;
   }

   @Override
   public void update(IPage pageModel) {
      fEditor.setModel((TreeViewModel) pageModel.getModel());
   }
   
}
