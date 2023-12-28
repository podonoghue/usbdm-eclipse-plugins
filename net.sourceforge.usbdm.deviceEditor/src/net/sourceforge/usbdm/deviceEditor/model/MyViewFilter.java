package net.sourceforge.usbdm.deviceEditor.model;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

public class MyViewFilter extends ViewerFilter {
   
   @Override
   public boolean select(Viewer viewer, Object parentElement, Object element) {
      
      if (element instanceof BaseModel) {
         BaseModel vm = (BaseModel) element;
         Boolean hidden = vm.isHidden();
//         if (vm.getName().contains("ftm_sc_ps")) {
//            System.err.println("select: vm = "+vm+", hidden = " + hidden);
//         }
         return !hidden;
      }
      return true;
   }

   @Override
   public boolean isFilterProperty(Object element, String property) {

      if (element instanceof BaseModel) {
//         BaseModel vm = (BaseModel) element;
//         Boolean hidden = vm.isHidden();
//         if (vm.getName().contains("ftm_sc_ps")) {
//            System.err.println("isFilterProperty: vm = "+vm+", hidden = " + hidden);
//         }
         if (ObservableModelInterface.PROP_HIDDEN[0].equals(property)) {
//            System.err.println("isFilterProperty: vm = "+vm+", hidden = " + hidden);
            return true;
         }
      }
      return false;
   }
}
