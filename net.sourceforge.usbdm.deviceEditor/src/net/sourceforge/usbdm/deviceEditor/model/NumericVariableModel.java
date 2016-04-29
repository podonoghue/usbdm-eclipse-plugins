package net.sourceforge.usbdm.deviceEditor.model;

public class NumericVariableModel extends VariableModel {

   private long fMin  = Long.MIN_VALUE;
   private long fMax  = Long.MAX_VALUE;
   private long fStep = 1;

   /**
    * Constructor
    * 
    * @param parent        Parent model
    * @param name          Display name
    * @param description   Display description
    * 
    * @note Added as child of parent if not null
    */
   public NumericVariableModel(BaseModel parent, IModelEntryProvider provider, String key, String description) {
      super(parent, provider, key, description);
   }
   
   /**
    * Get minimum permitted value
    * 
    * @return
    */
   public long min() {
      return fMin;
   }

   /**
    * Set minimum permitted value
    * 
    * @param min Value to set
    */
   public void setMin(long min) {
      fMin = min;
   }
   
   /**
    * Get maximum permitted value
    * 
    * @return
    */
   public long max() {
      return fMax;
   }
   
   /**
    * Set maximum permitted value
    * 
    * @param max Value to set
    */
   public void setMax(long max) {
      fMax = max;
   }
   
   /**
    * Get step size of value<br>
    * i.e. the value must always be a multiple of this value
    * 
    * @return
    */
   public long getStep() {
      return fStep;
   }
   
   /**
    * Set step size
    * 
    * @param max Value to set
    */
   public void setStep(long step) {
      fStep = step;
   }
   
   /**
    * Round the given value to the nearest permitted value<br>
    * 
    * @return Value ( may be unchanged)
    */
   public long roundValue(long value) {
      if (value<fMin) {
         return fMin;
      }
      if (value>fMax) {
         return fMax;
      }
      long remainder = value % fStep;
      // This rounds towards zero
      value -= remainder;
      return value;
   }

   @Override
   public String getValueAsString() {
      return super.getValueAsString();
   }
   
}
