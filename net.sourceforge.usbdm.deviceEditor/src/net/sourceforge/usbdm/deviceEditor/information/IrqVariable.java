package net.sourceforge.usbdm.deviceEditor.information;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.IrqVariableModel;
import net.sourceforge.usbdm.deviceEditor.model.VariableModel;
import net.sourceforge.usbdm.peripheralDatabase.InterruptEntry.Mode;

public class IrqVariable extends BooleanVariable {
   
   /** Set pattern to match IRQ handler name */
   private String fPattern = null;
   
   /** Class handler name */
   private String fClassHandler = null;

   public IrqVariable(String name, String key) {
      super(name, key);
      setTrueValue(new Pair("Installed", "$"+Mode.ClassMethod.name()));
      setFalseValue(new Pair("Not installed", "$"+Mode.NotInstalled.name()));
   }

   /* (non-Javadoc)
    * @see net.sourceforge.usbdm.deviceEditor.information.BooleanVariable#getSubstitutionValue()
    */
   @Override
   public String getSubstitutionValue() {
      return super.getValueAsBoolean()?"1":"0";
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
      return getValueAsBoolean()?"Software (Use setCallback() or override class method)":"No handler installed";
   }
   
   @Override
   public VariableModel createModel(BaseModel parent) {
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

   /* (non-Javadoc)
    * @see net.sourceforge.usbdm.deviceEditor.information.BooleanVariable#getPersistentValue()
    */
   @Override
   public String getPersistentValue() {
      return super.getValueAsBoolean()?"$"+Mode.ClassMethod.name():"$"+Mode.NotInstalled.name();
   }

   @Override
   public boolean isDefault() {
      return getValueAsBoolean() == false;
   }

   @Override
   public void setDisabledValue(Object value) {
   }
   
}
