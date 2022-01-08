package net.sourceforge.usbdm.deviceEditor.model;

/**
 * Simple model that displays a constant string
 * @author podonoghue
 *
 */
public final class ConstantModel extends StringModel {

   public ConstantModel(BaseModel parent, String name, String description, String value) {
      super(parent, name, description, value);
   }

   @Override
   protected void removeMyListeners() {
   }

   @Override
   public boolean showAsLocked() {
      return true;
   }
   
}
