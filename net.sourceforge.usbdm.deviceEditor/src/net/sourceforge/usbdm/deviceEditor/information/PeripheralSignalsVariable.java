package net.sourceforge.usbdm.deviceEditor.information;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.PeripheralSignalsModel;
import net.sourceforge.usbdm.deviceEditor.model.VariableModel;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

public class PeripheralSignalsVariable extends Variable {

   public PeripheralSignalsVariable(String name, String key) {
      super(name, key);
      setToolTip("Signals associated with this peripheral");
   }

   @Override
   public String getSubstitutionValue() {
      throw new RuntimeException("This method should never be called");
   }

   @Override
   public String getValueAsString() {
      return "Peripheral Signals";
   }

   @Override
   public boolean setValue(Object value) {
      throw new RuntimeException("This method should never be called");
   }

   @Override
   public void setValueQuietly(Object value) {
      throw new RuntimeException("This method should never be called");
   }

   @Override
   public String getPersistentValue() {
      throw new RuntimeException("This method should never be called");
   }

   @Override
   public void setPersistentValue(String value) throws Exception {
      throw new RuntimeException("This method should never be called");
   }

   @Override
   public void setDefault(Object value) {
      throw new RuntimeException("This method should never be called");
   }

   @Override
   public void setDisabledValue(Object value) {
      throw new RuntimeException("This method should never be called");
   }

   @Override
   Object getDefault() {
      throw new RuntimeException("This method should never be called");
   }

   @Override
   protected VariableModel privateCreateModel(BaseModel parent) {
      PeripheralWithState p = (PeripheralWithState) getProvider();
      PeripheralSignalsModel m = p.createPeripheralSignalsModel(parent);
      return m;
   }

   @Override
   public boolean isDefault() {
      return true;
   }

   @Override
   public Object getNativeValue() {
      throw new RuntimeException("This method should never be called");
   }

}