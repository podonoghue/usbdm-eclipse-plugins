package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import net.sourceforge.usbdm.deviceEditor.Activator;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.BinaryVariableModel;
import net.sourceforge.usbdm.deviceEditor.model.SelectionModel;

public class ValueColumnLabelProvider extends ColumnLabelProvider{
   private  Image lockedImage    = null;
   private  Image checkedImage   = null;
   private  Image uncheckedImage = null;
   final    Color disabledColour = Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW);

   ValueColumnLabelProvider() {
      super();
      if (Activator.getDefault() != null) {
         lockedImage = Activator.getDefault().getImageDescriptor(Activator.ID_LOCKED_NODE_IMAGE).createImage();
         checkedImage = Activator.getDefault().getImageDescriptor(Activator.ID_CHECKBOX_CHECKED_IMAGE).createImage();
         uncheckedImage = Activator.getDefault().getImageDescriptor(Activator.ID_CHECKBOX_UNCHECKED_IMAGE).createImage();
      }
   }

   @Override
   public Image getImage(Object element) {
      if (element instanceof BaseModel) {
         BaseModel model = (BaseModel) element;
         if (model.isLocked()) {
            return lockedImage;
         }
      }
      if (element instanceof BinaryVariableModel) {
         return ((Boolean)((BinaryVariableModel)element).getBooleanValue())?checkedImage:uncheckedImage;
      }
      return null;
   }

   @Override
   public String getText(Object element) {
      return ((BaseModel) element).getValueAsString();
   }

   @Override
   public Color getBackground(Object element) {
      if (!((BaseModel) element).isEnabled()) {
         return disabledColour;
      }
      return super.getBackground(element);
   }

   @Override
   public Color getForeground(Object element) {
      if (element instanceof SelectionModel) {
         SelectionModel model = (SelectionModel) element;
         if (model.isReset()) {
            return Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW);
         }
      }
      else if (!((BaseModel) element).isEnabled()) {
         return Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW);
      }
      return super.getForeground(element);
   }

   @Override
   public String getToolTipText(Object element) {
      return ((BaseModel) element).getToolTip();
   }

   @Override
   public Font getFont(Object element) {
      return super.getFont(element);
   }

   @Override
   public void dispose() {
      super.dispose();
      if (lockedImage != null) {
         lockedImage.dispose();
      }
   }
}
