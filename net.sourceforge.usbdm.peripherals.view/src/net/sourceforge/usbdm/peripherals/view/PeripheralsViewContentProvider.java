package net.sourceforge.usbdm.peripherals.view;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

import net.sourceforge.usbdm.peripherals.model.BaseModel;
import net.sourceforge.usbdm.peripherals.model.IModelChangeListener;
import net.sourceforge.usbdm.peripherals.model.ObservableModel;
import net.sourceforge.usbdm.peripherals.model.PeripheralModel;

/**
 * Provides the contents from the tree view (from model)
 */
class PeripheralsViewContentProvider implements ITreeContentProvider, IModelChangeListener {
   
   UsbdmDevicePeripheralsView view;
   
   public PeripheralsViewContentProvider(UsbdmDevicePeripheralsView view) {
      this.view = view;
   }
   
   private TreeViewer treeViewer = null;

   public void dispose() {
   }

   public Object[] getElements(Object inputElement) {
      return ((BaseModel) inputElement).getChildren().toArray();
   }

   public Object[] getChildren(Object parentElement) {
      return getElements(parentElement);
   }

   public Object getParent(Object element) {
      return ((BaseModel) element).getParent();
   }

   public boolean hasChildren(Object element) {
      return ((BaseModel) element).getChildren().size() > 0;
   }

   public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      // Save view
      this.treeViewer = (TreeViewer) viewer;
      if (oldInput != null) {
         // Remove old input as listener
         removeListenerFrom((BaseModel) oldInput);
      }
      if (newInput != null) {
         // Add new input as listener
         addListenerTo((BaseModel) newInput);
      }
   }

   protected void addListenerTo(BaseModel model) {
      // System.err.println("PeripheralsViewContentProvider.addListenerTo(), parent listener = "
      // + model.toString());
      model.addListener(this);
      for (Object childModel : model.getChildren()) {
         if (childModel instanceof PeripheralModel) {
            addListenerTo(((BaseModel) childModel));
         }
         // System.err.println("PeripheralsViewContentProvider.addListenerTo(), listener = "
         // + childModel.toString());
      }
   }

   protected void removeListenerFrom(BaseModel model) {
      model.removeListener(this);
      for (Object childModel : model.getChildren()) {
         removeListenerFrom(((BaseModel) childModel));
      }
   }

   @Override
   public void modelElementChanged(ObservableModel model) {
      if (!(model instanceof BaseModel)) {
         if (model == null) {
            System.err.println("PeripheralsViewContentProvider.modelElementChanged(\"null)\"");
         }
         else {
            System.err.println("PeripheralsViewContentProvider.modelElementChanged("+model.getClass().toString()+")");
         }
      }
      if ((treeViewer != null) && (model != null)) {
         treeViewer.refresh(model, true);
         //         ITreeSelection selection = (ITreeSelection) treeViewer.getSelection();
         //         if (selection.getFirstElement() == model) {
         //            PeripheralsInformationPanel panel = view.getInformationPanel();
         //            if (panel != null) {
         //               // TODO - Check if needed
         ////               panel.updateContent();
         //            }
         //         }
      }
   }

   @Override
   public void modelStructureChanged(ObservableModel model) {
      // Add listeners to new structure
      addListenerTo((BaseModel) model);
   }
}

