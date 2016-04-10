package net.sourceforge.usbdm.deviceEditor.model;

public interface ModelEntryProvider {

   public static class VariableInfo {
      public final String description;
      public String value;
      public final long min;
      public final long max;
      public final long defaultValue;
      
      public VariableInfo(String value, String description, long defaultValue, long min, long max) {
         this.value        = value;
         this.description  = description;
         this.min          = min;
         this.max          = max;
         this.defaultValue = defaultValue;
      }

      public VariableInfo(String value, String description) {
         this(value, description, 0, Long.MIN_VALUE, Long.MAX_VALUE);
      }
   }
   
   /**
    * Get models contributed by this object
    * 
    * @param parent
    * @return
    */
   public BaseModel[] getModels(BaseModel parent);
   
   /**
    * Set a variable
    * 
    * @param key     Key used to identify variable
    * @param value   New value for variable
    */
   public void setValue(String key, String value);

   /**
    * Get the value of a variable
    * 
    * @param key     Key used to identify variable
    * 
    * @return Value for variable
    */
   public String getValueAsString(String key);

   /**
    * Get the VariableInfo for a variable
    * 
    * @param key     Key used to identify variable
    * 
    * @return Value for variable
    */
   public VariableInfo getVariableInfo(String key);
}
