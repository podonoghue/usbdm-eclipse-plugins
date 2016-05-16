package net.sourceforge.usbdm.deviceEditor.model;

import net.sourceforge.usbdm.deviceEditor.information.Variable;

public interface IModelEntryProvider {

   /**
    * Get models contributed by this object
    * 
    * @param parent
    * @return
    */
   public BaseModel getModels(BaseModel parent);
   
   /**
    * Set value of variable
    * 
    * @param key     Key used to identify variable
    * @param value   New value for variable
    */
   public void setVariableValue(String key, String value);

   /**
    * Get value of variable
    * 
    * @param key     Key used to identify variable
    * 
    * @return Value for variable
    */
   public String getVariableValue(String key);

   /**
    * Get variable with given key
    * 
    * @param key
    * @return
    * @throws Exception if variable doesn't exist
    */
   public Variable getVariable(String key);

   /**
    * Get variable with given key
    * 
    * @param key
    * 
    * @return variable or null if not found
    */
   Variable safeGetVariable(String key);

}
