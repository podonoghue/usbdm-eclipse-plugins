package net.sourceforge.usbdm.deviceEditor.model;

import net.sourceforge.usbdm.deviceEditor.information.Variable;

public interface IModelEntryProvider {

   /**
    * Get models contributed by this object
    * 
    * @param parent  Parent to add models to
    */
   public void getModels(BaseModel parent);
   
   /**
    * Get top-level model contributed by this object
    * 
    * @param parent  Parent to add models to
    * 
    * @return model or null if none
    */
   public BaseModel getModel(BaseModel parent);
   
   /**
    * Set value of variable
    * 
    * @param key     Key used to identify variable
    * @param value   New value for variable
    */
   public void setVariableValue(String key, String value) throws Exception;

   /**
    * Get value of variable
    * 
    * @param key     Key used to identify variable
    * 
    * @return Value for variable
    */
   public String getVariableValue(String key) throws Exception;

   /**
    * Get variable with given key
    * 
    * @param key     Key to lookup variable
    * 
    * @return Variable
    * 
    * @throws Exception if variable doesn't exist
    */
   public Variable getVariable(String key) throws Exception;

   /**
    * Get variable with given key
    * 
    * @param key     Key to lookup variable
    * 
    * @return variable or null if not found
    */
   public Variable safeGetVariable(String key);

}
