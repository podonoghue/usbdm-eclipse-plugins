package net.sourceforge.usbdm.peripherals.view;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

   class PeripheralsViewFilter extends ViewerFilter {

      public enum SelectionCriteria {
         SelectAll,
         SelectNone
      };
      
      private SelectionCriteria criteria;
      
      public PeripheralsViewFilter(SelectionCriteria criteria) {
         this.criteria = criteria;
      }
      
      @Override
      public boolean select(Viewer viewer, Object parentElement, Object element) {
         switch (criteria) {
         case SelectAll:  return true;
         case SelectNone: return false;
         }
         return false;
      }
   }
