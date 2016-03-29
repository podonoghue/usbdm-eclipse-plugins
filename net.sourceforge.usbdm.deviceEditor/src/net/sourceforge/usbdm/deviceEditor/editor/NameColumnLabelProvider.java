package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import net.sourceforge.usbdm.deviceEditor.Activator;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;

public class NameColumnLabelProvider extends ColumnLabelProvider{
//   private Image uncheckedImage = null;
//   private Image checkedImage   = null;
//   private Image lockedImage    = null;
   private  Image errorImage     = null;
   private  Image warningImage   = null;
   final    Color disabledColour = Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW);

   NameColumnLabelProvider() {
      super();
   }
   
   @Override
   public Image getImage(Object element) {
      if ((errorImage == null) && (Activator.getDefault() != null)) {
         errorImage = Activator.getDefault().getImageDescriptor(Activator.ID_ERROR_NODE_IMAGE).createImage();
      }
      if ((warningImage == null) && (Activator.getDefault() != null)) {
         warningImage = Activator.getDefault().getImageDescriptor(Activator.ID_WARNING_NODE_IMAGE).createImage();
      }
      if (element instanceof BaseModel) {
         BaseModel model = (BaseModel) element;
         if (model.isError()) {
            return errorImage;
         }
         else if (model.isWarning()) {
            return warningImage;
         }
      }
      return null;
   }
   
   @Override
   public String getText(Object element) {
      try {
         return ((BaseModel) element).getName();
      } catch (Exception e) {
//         e.printStackTrace();
         return e.getMessage();
      }
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
      if (!((BaseModel) element).isEnabled()) {
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
      if (warningImage != null) {
         warningImage.dispose();
      }
      if (errorImage != null) {
         errorImage.dispose();
      }
   }
}
