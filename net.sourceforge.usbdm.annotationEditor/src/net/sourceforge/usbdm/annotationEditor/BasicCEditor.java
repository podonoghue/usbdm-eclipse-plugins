package net.sourceforge.usbdm.annotationEditor;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.TextEditor;

public class BasicCEditor extends TextEditor {

   private ColorManager colorManager;

   public BasicCEditor() {
      super();
      colorManager = new ColorManager();
      setSourceViewerConfiguration(new ViewConfiguration(colorManager));
      setDocumentProvider(new DocumentProvider());
   }

   @Override
   public void dispose() {
      colorManager.dispose();
      super.dispose();
   }
   
   @Override
   public void init(IEditorSite site, IEditorInput input) throws PartInitException {
      super.init(site, input);
   }

}
