package net.sourceforge.usbdm.cdt.ui.actions;

import org.eclipse.core.resources.IContainer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;

public class SelectionListener implements ISelectionListener {

   SelectionListener() {
   }
   
   @Override
   public void selectionChanged(IWorkbenchPart part, ISelection selection) {
      if (selection instanceof IStructuredSelection) {
         IStructuredSelection ssel = (IStructuredSelection)selection;
         if (ssel.getFirstElement() instanceof IContainer) {
            
         }
         else {
            
         }
      }
      
   }

}
