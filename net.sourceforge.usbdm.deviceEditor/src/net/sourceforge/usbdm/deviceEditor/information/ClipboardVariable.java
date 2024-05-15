package net.sourceforge.usbdm.deviceEditor.information;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.ClipboardModel;
import net.sourceforge.usbdm.deviceEditor.model.VariableModel;
import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;

public class ClipboardVariable extends Variable {

   String fText = null;
   
   public ClipboardVariable(VariableProvider provider, String name, String key) {
      super(provider, name, key);
   }

   @Override
   public String getValueAsString() {
      return fText ;
   }

   @Override
   public boolean setValueQuietly(Object value) {
      boolean changed = fText != value.toString();
      fText = value.toString();
      return changed;
   }

   @Override
   public String getPersistentValue() {
      return fText;
   }

   @Override
   public void setPersistentValue(String value) throws Exception {
      fText = value;
   }

   @Override
   public void setDefault(Object value) {
   }

   @Override
   public void setDisabledValue(Object value) {
   }

   @Override
   public Object getValue() {
      return fText;
   }

   @Override
   public Object getDefault() {
      return null;
   }

   @Override
   protected VariableModel privateCreateModel(BaseModel parent) {
      return new ClipboardModel(parent, this);
   }

   @Override
   public boolean isDefault() {
      return false;
   }

   @Override
   public String getSubstitutionValue() {
      return fText;
   }

   @Override
   public Object getNativeValue() {
      return fText;
   }
   
   @Override
   public boolean isZero() {
      return true;
   }

}
