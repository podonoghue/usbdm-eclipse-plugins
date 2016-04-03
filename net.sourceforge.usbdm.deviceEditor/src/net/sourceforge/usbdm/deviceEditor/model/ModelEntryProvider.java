package net.sourceforge.usbdm.deviceEditor.model;

public interface ModelEntryProvider {

   /**
    * Get models contributed by this object
    * 
    * @param parent
    * @return
    */
   BaseModel[] getModels(BaseModel parent);
   
   /**
    * Set a variable
    * 
    * @param key     Key used to identify variable
    * @param value   New value for variable
    */
   void setValue(String key, String value);

   /**
    * Get the value of a variable
    * 
    * @param key     Key used to identify variable
    * 
    * @return Value for variable
    */
   String getValue(String key);

}
