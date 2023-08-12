package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.swt.graphics.Image;

import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.PeripheralSignalsModel;
import net.sourceforge.usbdm.deviceEditor.model.PinModel;
import net.sourceforge.usbdm.deviceEditor.model.SignalModel;

public class CodeIdentifierColumnLabelProvider extends BaseLabelProvider {

   public CodeIdentifierColumnLabelProvider() {
      super();
   }

   @Override
   public String getText(BaseModel model) {
      if (model instanceof PinModel) {
         Pin pin = ((PinModel)model).getPin();
         return pin.getMappedSignalsCodeIdentifiers();
      }
      return CodeIdentifierColumnEditingSupport.getEditableCodeIdentifier(model);
   }

   @Override
   public Image getImage(BaseModel model) {
      return null;
   }

   @Override
   public String getToolTipText(Object element) {
      final String tooltip =
            "List of C identifiers separated by '/'\n" +
            "These will be used to create named C objects representing the peripheral or signal";
      if (element instanceof PeripheralSignalsModel) {
         return tooltip;
      }
      if (element instanceof SignalModel) {
         return tooltip;
      }
      if (element instanceof PinModel) {
         return tooltip+
               "\n" +
               "These are generated from mapped signals\n" +
               "Only editable here if a single signal is mapped";
      }
      return super.getToolTipText(element);
   }

   public static String getColumnToolTipText() {
      return "C Identifier for code generation\n"+
            "If not blank, code will be generated for the peripheral or signal";
   }
   
}
