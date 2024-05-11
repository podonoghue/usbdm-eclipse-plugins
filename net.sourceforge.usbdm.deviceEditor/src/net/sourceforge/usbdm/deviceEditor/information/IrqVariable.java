package net.sourceforge.usbdm.deviceEditor.information;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.IrqVariableModel;
import net.sourceforge.usbdm.deviceEditor.model.VariableModel;
import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;
import net.sourceforge.usbdm.peripheralDatabase.InterruptEntry.Mode;

public class IrqVariable extends BooleanVariable {
   
   /** Set pattern to match IRQ handler name */
   private String fPattern = null;
   
   /** Class handler name */
   private String fClassHandler = null;

   public IrqVariable(VariableProvider provider, String name, String key) {
      super(provider, name, key);
      setTrueValue(new ChoiceData("Installed", "$"+Mode.ClassMethod.name()));
      setFalseValue(new ChoiceData("Not installed", "$"+Mode.NotInstalled.name()));
   }

   /**
    * {@inheritDoc}
    * 
    * @return String for text substitutions (in C code) either "true" or "false"
    */
   @Override
   public String getSubstitutionValue() {
      return super.getValueAsBoolean()?"true":"false";
   }

   /**
    * Get mode indicating how the vector is handled
    * 
    * @return
    */
   public Mode getMode() {
      return getValueAsBoolean()?Mode.ClassMethod:Mode.NotInstalled;
   }
   
   @Override
   public String getValueAsString() {
      return getValueAsBoolean()?"Interrupt trampoline installed":"Interrupt trampoline not installed";
   }
   
   @Override
   protected VariableModel privateCreateModel(BaseModel parent) {
      return new IrqVariableModel(parent, this);
   }

   /**
    * Set pattern to match IRQ handler name
    * 
    * @param pattern
    */
   public void setPattern(String pattern) {
      fPattern = pattern;
   }

   /**
    * Get pattern to match IRQ handler name
    * 
    * @return pattern
    */
   public String getPattern() {
      return fPattern;
   }

   /**
    * Set name of class method that handles this interrupt
    * 
    * @param handler Name of handler
    */
   public void setClassHandler(String handler) {
      fClassHandler = handler;
   }

   /**
    * Get name of class method that handles this interrupt
    * 
    * @return Name of handler
    */
   public String getClassHandler() {
      return fClassHandler;
   }

   /**
    * {@inheritDoc}
    * 
    * @return Either "$ClassMethod" or "$NotInstalled"
    */
   @Override
   public String getPersistentValue() {
      return super.getValueAsBoolean()?"$"+Mode.ClassMethod.name():"$"+Mode.NotInstalled.name();
   }

   /**
    * {@inheritDoc}
    * 
    * @param value true/false (as String)
    */
   @Override
   public void setPersistentValue(String value) {
      setValue((value.equalsIgnoreCase("$ClassMethod"))?Mode.ClassMethod:Mode.NotInstalled);
   }
   
   @Override
   protected Object clone() throws CloneNotSupportedException {
      return super.clone();
   }
   
   @Override
   public String getEnumValue() {
      return Boolean.toString(getValueAsBoolean());
   }

   /**
    * {@inheritDoc}
    * 
    * @param value Mode value/true/false etc.
    */
   @Override
   public Boolean translate(Object value) {
      
      if (value instanceof Mode) {
         Mode mode = (Mode) value;
         return (mode==Mode.ClassMethod)?true:false;
      }
      return super.translate(value);
   }

}
