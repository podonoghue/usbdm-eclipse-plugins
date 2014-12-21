
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
   final UsbdmDevicePeripheralsView view;
   
   public PeripheralsViewCellModifier(UsbdmDevicePeripheralsView view) {
      this.view = view;
   }
   
   @Override
   public void modify(Object element, String property, Object value) {
      // System.err.println("PeripheralsViewCellModifier.modify("+element.getClass()+", "+value.toString()+")");
      if (element instanceof TreeItem) {
         // update element and tree model
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
               treeItem.setText(1, registerModel.safeGetValueAsString());
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
               treeItem.setText(1, fieldModel.safeGetValueAsString());
            } catch (NumberFormatException e) {
//               System.err.println("PeripheralsViewCellModifier.modify(FieldModel, ...) - format error");
            }
         }
         PeripheralsInformationPanel panel = view.getInformationPanel();
         if (panel != null) {
            view.getInformationPanel().updateContent();
         }
      }
   }

   @Override
   public Object getValue(Object element, String property) {
      if (element instanceof RegisterModel) {
         // System.err.println("PeripheralsViewCellModifier.getValue(RegisterModel, "+((RegisterModel)element).getValueAsString()+")");
         return ((RegisterModel) element).safeGetValueAsString();
      } else if (element instanceof FieldModel) {
         // System.err.println("PeripheralsViewCellModifier.getValue(FieldModel, "+((FieldModel)element).getValueAsString()+")");
         return ((FieldModel) element).safeGetValueAsString();
      }
      // System.err.println("PeripheralsViewCellModifier.getValue("+element.getClass()+", "+element.toString()+")");
      return element.toString();
   }

   @Override
   public boolean canModify(Object element, String property) {
      if (element instanceof RegisterModel) {
         return ((RegisterModel) element).isWriteable();
      }
      if (element instanceof FieldModel) {
         return ((FieldModel) element).isWriteable();
      }
      return false;
   }
}
