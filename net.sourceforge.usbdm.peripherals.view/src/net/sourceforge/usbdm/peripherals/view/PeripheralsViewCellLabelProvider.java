package net.sourceforge.usbdm.peripherals.view;

import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

import net.sourceforge.usbdm.peripherals.model.BaseModel;
import net.sourceforge.usbdm.peripherals.model.FieldModel;
import net.sourceforge.usbdm.peripherals.model.PeripheralModel;
import net.sourceforge.usbdm.peripherals.model.RegisterHolder;
import net.sourceforge.usbdm.peripherals.model.RegisterModel;

/**
 * Provides labels for the tree view cells
 */
public class PeripheralsViewCellLabelProvider extends CellLabelProvider implements ITableLabelProvider, ITableFontProvider, ITableColorProvider {

   final FontRegistry registry = new FontRegistry();
   final Color changedColour = Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW);
   final Font boldFont = registry.getBold(Display.getCurrent().getSystemFont().getFontData()[0].getName());

   final UsbdmDevicePeripheralsView view;
   
   PeripheralsViewCellLabelProvider(UsbdmDevicePeripheralsView view) {
      this.view = view;
   }
   
   /*
    * (non-Javadoc)
    * 
    * @see
    * org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
    */
   @Override
   public Image getColumnImage(Object element, int columnIndex) {
      if (columnIndex == 0) {
         // Only provide images for column 1
         if (element instanceof RegisterHolder) {
            return view.getMyImage(Activator.ID_PERIPHERAL_IMAGE);
         }
         else if (element instanceof RegisterModel) {
            if (((RegisterModel) element).getAccessMode() == "RO") {
               return view.getMyImage(Activator.ID_REGISTER_READ_ONLY_IMAGE);
            } 
            else {
               return view.getMyImage(Activator.ID_REGISTER_READ_WRITE_IMAGE);
            }
         } 
         else if (element instanceof FieldModel) {
            if (((FieldModel) element).getAccessMode() == "RO") {
               return view.getMyImage(Activator.ID_FIELD_READ_ONLY_IMAGE);
            } 
            else {
               return view.getMyImage(Activator.ID_FIELD_READ_WRITE_IMAGE);
            }
         }
      }
      return null;
   }

   @Override
   public void dispose() {
      super.dispose();
      if (changedColour != null) {
         changedColour.dispose();
      }
      if (boldFont != null) {
         boldFont.dispose();
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
    */
   public String getColumnText(Object element, int columnIndex) {
      BaseModel item = (BaseModel) element;
      switch (columnIndex) {
      case UsbdmDevicePeripheralsView.NAME_COL:
         return item.getName();
      
      case UsbdmDevicePeripheralsView.VALUE_COL: 
         return item.safeGetValueAsString();

      case UsbdmDevicePeripheralsView.FIELD_INFO_COL: {
         if (element instanceof FieldModel) {
            FieldModel field = (FieldModel) element;
            return field.getFieldValueDescription();
         }
         return "";
      }
      case UsbdmDevicePeripheralsView.MODE_COL:
         return item.getAccessMode();

      case UsbdmDevicePeripheralsView.LOCATION_COL:
         return item.getAddressAsString();

      case UsbdmDevicePeripheralsView.DESCRIPTION_COL: {
         return item.getShortDescription();
      }
      default:
         return "";
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * org.eclipse.jface.viewers.ITableFontProvider#getFont(java.lang.Object,
    * int)
    */
   public Font getFont(Object element, int columnIndex) {
      return (element instanceof PeripheralModel) ? boldFont : null;
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * org.eclipse.jface.viewers.ITableColorProvider#getBackground(java.lang
    * .Object, int)
    */
   public Color getBackground(Object element, int columnIndex) {
      if ((columnIndex == 1) && (element instanceof BaseModel)) {
         try {
            return ((BaseModel) element).isChanged() ? changedColour : null;
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
      return null;
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * org.eclipse.jface.viewers.ITableColorProvider#getForeground(java.lang
    * .Object, int)
    */
   public Color getForeground(Object element, int columnIndex) {
      return null;
   }

   @Override
   public void update(ViewerCell cell) {
      // System.err.println("PeripheralCellLabelProvider.update()");
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * org.eclipse.jface.viewers.ViewerLabelProvider#getTooltipText(java.lang
    * .Object)
    */
   public String getToolTipText(Object element) {
      return "Tooltip (" + element.toString() + ")";
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * org.eclipse.jface.viewers.ViewerLabelProvider#getTooltipDisplayDelayTime
    * (java.lang.Object)
    */
   public int getToolTipDisplayDelayTime(Object object) {
      return 1000;
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * org.eclipse.jface.viewers.ViewerLabelProvider#getTooltipShift(java.
    * lang.Object)
    */
   public Point getToolTipShift(Object object) {
      return new Point(5, 5);
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * org.eclipse.jface.viewers.ViewerLabelProvider#getTooltipTimeDisplayed
    * (java.lang.Object)
    */
   public int getToolTipTimeDisplayed(Object object) {
      return 5000;
   }
}

