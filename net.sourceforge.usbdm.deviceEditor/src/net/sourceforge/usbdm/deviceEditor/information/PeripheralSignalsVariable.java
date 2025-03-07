package net.sourceforge.usbdm.deviceEditor.information;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.PeripheralSignalsModel;
import net.sourceforge.usbdm.deviceEditor.model.VariableModel;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

public class PeripheralSignalsVariable extends Variable {

   public PeripheralSignalsVariable(PeripheralWithState peripheral) {
      super(peripheral, peripheral.getName(), peripheral.makeKey("_signals"));
      setToolTip("Signals associated with this peripheral");
   }

   @Override
   public String getSubstitutionValue() {
      return "(nul)";
   }

   @Override
   public String getValueAsString() {
      return "Peripheral Signals";
   }

   @Override
   public boolean setValueQuietly(Object value) {
      return false;
   }

   @Override
   public String getPersistentValue() {
      throw new RuntimeException("This method should never be called");
   }

   @Override
   public void setPersistentValue(String value) {
   }

   @Override
   public void setDefault(Object value) {
   }

   @Override
   public void setDisabledValue(Object value) {
   }

   @Override
   public Object getDefault() {
      throw new RuntimeException("This method should never be called");
   }

   @Override
   protected VariableModel privateCreateModel(BaseModel parent) {
      PeripheralWithState    p = (PeripheralWithState) getProvider();
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

   @Override
   protected Object clone() throws CloneNotSupportedException {
      return super.clone();
   }

   @Override
   public Object getValue() {
      return null;
   }

   @Override
   public boolean isZero() {
      return true;
   }

}
