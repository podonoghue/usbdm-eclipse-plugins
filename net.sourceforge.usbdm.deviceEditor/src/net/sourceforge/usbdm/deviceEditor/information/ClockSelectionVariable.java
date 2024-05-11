package net.sourceforge.usbdm.deviceEditor.information;

import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;

public class ClockSelectionVariable extends ChoiceVariable {

   public ClockSelectionVariable(VariableProvider provider, String name, String key) {
      super(provider, name, key);
   }

   @Override
   public boolean setValueQuietly(Integer value) {
      getDeviceInfo().setActiveClockSelection(value);
      return super.setValueQuietly(value);
   }

}
