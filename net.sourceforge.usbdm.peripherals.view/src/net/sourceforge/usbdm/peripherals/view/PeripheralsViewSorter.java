package net.sourceforge.usbdm.peripherals.view;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.peripherals.model.BaseModel;
import net.sourceforge.usbdm.peripherals.model.ClusterModel;
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
      AddressOrder,         // Sort by peripheral address
   };
   
   /**
    *  Criteria to sort by
    */
   private SortCriteria criteria;
   
   static final Pattern registerPattern = Pattern.compile("(^.*)([0-9]*)");
   
   static int compare(String s1, String s2) {
      Matcher m1 = registerPattern.matcher(s1);
      Matcher m2 = registerPattern.matcher(s1);
      String s1Base   = s1;
      String s1Suffix = "";
      String s2Base   = s2;
      String s2Suffix = "";
      if (m1.matches()) {
         s1Base   = m1.group(1);
         s1Suffix = m1.group(2);
      }
      if (m2.matches()) {
         s2Base   = m2.group(1);
         s2Suffix = m2.group(2);
      }
      int diff = s1Base.compareTo(s2Base);
      if (diff != 0) {
         return diff;
      }
      return s1Suffix.compareTo(s2Suffix);
   }
   
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
         if (diff != 0) {
            return diff;
         }
         diff = ((FieldModel)e2).getBitWidth() - ((FieldModel)e1).getBitWidth();
         if (diff != 0) {
            return diff;
         }
         diff = element1.getName().compareTo(element2.getName());
         return diff;
      }
      
      // Registers sorted by address then name
      if ((e1 instanceof RegisterModel) || (e1 instanceof ClusterModel)) { 
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
