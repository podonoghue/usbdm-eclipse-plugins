package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.swt.graphics.Image;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.SignalModel;

public class ModifierColumnLabelProvider extends BaseLabelProvider {

   String  fToolTip = null;
   
   @Override
   public String getText(BaseModel baseModel) {

      ModifierEditorInterface me = ModifierEditingSupport.getModifierEditor(baseModel);
      if (me == null) {
         return null;
      }
      return me.getText((SignalModel)baseModel);
   }

   @Override
   public Image getImage(BaseModel baseModel) {

      ModifierEditorInterface me = ModifierEditingSupport.getModifierEditor(baseModel);
      if (me == null) {
         return null;
      }
      return me.getImage((SignalModel)baseModel);
   }

   @Override
   public String getToolTipText(Object baseModel) {

      ModifierEditorInterface me = ModifierEditingSupport.getModifierEditor(baseModel);
      if (me == null) {
         return null;
      }
      return me.getModifierHint((SignalModel)baseModel);
   }

   /**
    * Provide toolTip for column heading
    * 
    * @return
    */
   public static String getColumnToolTipText() {
      return "Modifies the instance or type";
   }

}
