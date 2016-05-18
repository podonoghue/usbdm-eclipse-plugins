package net.sourceforge.usbdm.deviceEditor.editor;

//import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

import net.sourceforge.usbdm.deviceEditor.model.ModelFactory;

public class DeviceEditorOutlinePage extends ContentOutlinePage {
   
   public DeviceEditorOutlinePage(ModelFactory fFactory, DeviceEditor deviceEditor) {
   }

   public void createControl(Composite parent) {
      super.createControl(parent);
//      TreeViewer viewer= getTreeViewer();
//      viewer.setContentProvider(new MyContentProvider());
//      viewer.setLabelProvider(new MyLabelProvider());
//      viewer.addSelectionChangedListener(this);
//      viewer.setInput(myInput);
   }

   public void setInput(IEditorInput editorInput) {
   }

}
