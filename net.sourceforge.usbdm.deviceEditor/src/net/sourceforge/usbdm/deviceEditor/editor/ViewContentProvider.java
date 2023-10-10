package net.sourceforge.usbdm.deviceEditor.editor;

import java.util.ArrayList;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;

public class ViewContentProvider implements ITreeContentProvider {
   @Override
   public void inputChanged(Viewer v, Object oldInput, Object newInput) {
   }

   @Override
   public void dispose() {
   }

   @Override
   public Object[] getElements(Object inputElement) {
      ArrayList<BaseModel> children = ((BaseModel) inputElement).getChildren();
      if (children == null) {
         return new Object[0];
      }
      ArrayList<BaseModel> visibleChildren = new ArrayList<BaseModel>();
      for (BaseModel child:children) {
//         if (!child.isHidden()) {
            visibleChildren.add(child);
//         }
      }
      return visibleChildren.toArray();
   
   }

   @Override
   public Object[] getChildren(Object parentElement) {
      ArrayList<BaseModel> children = ((BaseModel) parentElement).getChildren();
      if (children == null) {
         return new Object[0];
      }
      ArrayList<BaseModel> visibleChildren = new ArrayList<BaseModel>();
      for (BaseModel child:children) {
//         if (!child.isHidden()) {
            visibleChildren.add(child);
//         }
      }
      return visibleChildren.toArray();
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
