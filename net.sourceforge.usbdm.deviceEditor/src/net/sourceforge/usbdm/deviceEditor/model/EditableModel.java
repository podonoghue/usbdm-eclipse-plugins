package net.sourceforge.usbdm.deviceEditor.model;

/**
 * Model that can be edited
 * 
 * @author podonoghue
 */
public abstract class EditableModel extends BaseModel {

   /**
    * Constructor
    * 
    * @param parent        Parent model
    * @param name          Display name
    * @param description   Display description
    * 
    * @note Added as child of parent if not null
    */
   public EditableModel(BaseModel parent, String name, String description) {
      super(parent, name, description);
   }
   
   @Override
   public boolean canEdit() {
      return true;
   }

   /** 
    * Set value of underlying data<br>
    * The string value given may need to be converted to suitable type 
    * 
    * @param value
    */
   public abstract void setValueAsString(String value);
}
