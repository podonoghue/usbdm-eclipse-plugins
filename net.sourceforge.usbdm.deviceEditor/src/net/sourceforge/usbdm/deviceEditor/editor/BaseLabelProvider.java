package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;

abstract class BaseLabelProvider extends LabelProvider implements IStyledLabelProvider {

   protected final TreeEditor view;

   public BaseLabelProvider(TreeEditor configViewer) {
      this.view = configViewer;
   }

   /**
    * Gets the text to display
    * 
    * @param element Element to obtain text for
    * 
    * @return
    */
   abstract public StyledString getStyledText(BaseModel element);

   @Override
   public StyledString getStyledText(Object element) {
      if (element instanceof BaseModel) {
         return getStyledText((BaseModel)element);
      }
      return null;
   }
}
