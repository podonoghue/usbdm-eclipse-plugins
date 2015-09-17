
package net.sourceforge.usbdm.peripherals.view;

import net.sourceforge.usbdm.peripherals.model.FieldModel;
import net.sourceforge.usbdm.peripherals.model.RegisterModel;

import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.swt.widgets.TreeItem;

/**
 * Handles modifying tree elements
 * 
 * This only applies to register or register field values
 * 
 */
public class PeripheralsViewCellModifier implements ICellModifier {
   
   final UsbdmDevicePeripheralsView fView;
   
   /**
    * Cell modifier for tree
    *  
    * @param view
    */
   public PeripheralsViewCellModifier(UsbdmDevicePeripheralsView view) {
      fView = view;
   }
   
   /* (non-Javadoc)
    * @see org.eclipse.jface.viewers.ICellModifier#modify(java.lang.Object, java.lang.String, java.lang.Object)
    */
   @Override
   public void modify(Object element, String property, Object value) {
      // System.err.println("PeripheralsViewCellModifier.modify("+element.getClass()+", "+value.toString()+")");
      if (element instanceof TreeItem) {
         // Update element and tree model
         TreeItem treeItem = (TreeItem) element;
         Object treeItemData = treeItem.getData();
         if (treeItemData instanceof RegisterModel) {
            // System.err.println("PeripheralsViewCellModifier.modify(RegisterModel, "+value.toString()+")");
            RegisterModel registerModel = (RegisterModel) treeItemData;
            try {
               String s = value.toString().trim();
               if (s.startsWith("0b")) {
                  registerModel.setValue(Long.parseLong(s.substring(2, s.length()), 2));
               } else {
                  registerModel.setValue(Long.decode(s));
               }
            } catch (NumberFormatException e) {
//               System.err.println("PeripheralsViewCellModifier.modify(RegisterModel, ...) - format error");
            }
         } else if (treeItemData instanceof FieldModel) {
            FieldModel fieldModel = (FieldModel) treeItemData;
            try {
               String s = value.toString().trim();
               if (s.startsWith("0b")) {
                  fieldModel.setValue(Long.parseLong(s.substring(2, s.length()), 2));
               } else {
                  fieldModel.setValue(Long.decode(s));
               }
            } catch (NumberFormatException e) {
//               System.err.println("PeripheralsViewCellModifier.modify(FieldModel, ...) - format error");
            }
         }
      }
   }

   /* (non-Javadoc)
    * @see org.eclipse.jface.viewers.ICellModifier#getValue(java.lang.Object, java.lang.String)
    */
   @Override
   public Object getValue(Object element, String property) {
      if (element instanceof RegisterModel) {
         RegisterModel reg = (RegisterModel) element;
//         System.err.println("PeripheralsViewCellModifier.getValue(RegisterModel, "+reg.safeGetValueAsString()+")");
         return reg.safeGetValueAsString();
      } else if (element instanceof FieldModel) {
         FieldModel field = (FieldModel) element;
//         System.err.println("PeripheralsViewCellModifier.getValue(FieldModel, "+field.getValueAsString()+")");
         return field.safeGetValueAsString();
      }
      // System.err.println("PeripheralsViewCellModifier.getValue("+element.getClass()+", "+element.toString()+")");
      return element.toString();
   }

   /* (non-Javadoc)
    * @see org.eclipse.jface.viewers.ICellModifier#canModify(java.lang.Object, java.lang.String)
    */
   @Override
   public boolean canModify(Object element, String property) {
      if (element instanceof RegisterModel) {
         return ((RegisterModel) element).isWritable();
      }
      if (element instanceof FieldModel) {
         return ((FieldModel) element).isWritable();
      }
      return false;
   }
}
