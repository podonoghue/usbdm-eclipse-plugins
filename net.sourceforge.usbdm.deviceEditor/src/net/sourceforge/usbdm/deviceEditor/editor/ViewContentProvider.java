package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.RootModel;

public class ViewContentProvider implements ITreeContentProvider {
   public void inputChanged(Viewer v, Object oldInput, Object newInput) {
   }

   @Override
   public void dispose() {
   }

   @Override
   public Object[] getElements(Object inputElement) {
      return ((RootModel) inputElement).getChildren().toArray();
   }

   @Override
   public Object[] getChildren(Object parentElement) {
      return ((BaseModel) parentElement).getChildren().toArray();
   }

   @Override
   public Object getParent(Object element) {
      return ((BaseModel) element).getParent();
   }

   @Override
   public boolean hasChildren(Object element) { 
      return ((BaseModel) element).hasChildren();
   }
}
