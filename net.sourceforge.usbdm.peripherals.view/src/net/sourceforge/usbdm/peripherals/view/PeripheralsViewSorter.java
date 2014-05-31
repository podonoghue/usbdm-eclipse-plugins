package net.sourceforge.usbdm.peripherals.view;

import net.sourceforge.usbdm.peripherals.model.BaseModel;
import net.sourceforge.usbdm.peripherals.model.FieldModel;
import net.sourceforge.usbdm.peripherals.model.RegisterModel;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

/**
 *  Class to allow sorting Peripheral View in various ways
 */
public class PeripheralsViewSorter extends ViewerComparator {

   public enum SortCriteria {
      PeripheralNameOrder,  // Sort by peripheral name
      AddressOrder,     // Sort by peripheral address
   };
   
   /**
    *  Criteria to sort by
    */
   private SortCriteria criteria;
   
   
   /**
    * @param criteria Criteria to sort by
    */
   public PeripheralsViewSorter(SortCriteria criteria) {
      this.criteria = criteria;
   }
   
   @Override
   public int compare(Viewer viewer, Object e1, Object e2) {
      BaseModel element1 = (BaseModel) e1;
      BaseModel element2 = (BaseModel) e2;

      int diff = 0;

      // Fields - sort by bitOffset then name
      if (e1 instanceof FieldModel) {
         diff = ((FieldModel)e2).getBitOffset() - ((FieldModel)e1).getBitOffset();
         if (diff == 0) {
            diff = element1.getName().compareTo(element2.getName());
         }
         return diff;
      }
      
      // Registers sorted by address then name
      if (e1 instanceof RegisterModel) { 
         if (element1.getAddress()>element2.getAddress()) {
            diff = 1;
         }
         else if (element1.getAddress()<element2.getAddress()) {
            diff = -1;
         }
         else {
            diff = element1.getName().compareTo(element2.getName());
         }
         return diff;
      }
      
      // Sort criteria affects how peripherals are sorted only
      switch (criteria) {
      case PeripheralNameOrder:
         // Sort by name then address (unlikely!)
         diff = element1.getName().compareTo(element2.getName());
         if (diff == 0) {
            if (element1.getAddress()>element2.getAddress()) {
               diff = 1;
            }
            else if (element1.getAddress()<element2.getAddress()) {
               diff = -1;
            }
            else {
               diff = element1.getName().compareTo(element2.getName());
            }
         }
         break;
      case AddressOrder:
         // Sort by address then name
         if (element1.getAddress()>element2.getAddress()) {
            diff = 1;
         }
         else if (element1.getAddress()<element2.getAddress()) {
            diff = -1;
         }
         else {
            diff = element1.getName().compareTo(element2.getName());
         }
         break;
      }
      return diff;
   }

}
