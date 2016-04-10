package net.sourceforge.usbdm.deviceEditor.model;

public abstract class EditableModel extends BaseModel {

   public EditableModel(BaseModel parent, String name, String description) {
      super(parent, name, description);
   }
   
   @Override
   public boolean canEdit() {
      return true;
   }

   /** 
    * Set value of underlying data<br>
    * The String value given may need to be converted to suitable type 
    * 
    * @param value
    */
   public abstract void setValueAsString(String value);
}
