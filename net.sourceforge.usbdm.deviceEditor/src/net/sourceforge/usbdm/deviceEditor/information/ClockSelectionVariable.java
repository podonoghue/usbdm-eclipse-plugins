package net.sourceforge.usbdm.deviceEditor.information;

import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;

public class ClockSelectionVariable extends ChoiceVariable {

   public ClockSelectionVariable(VariableProvider provider, String name, String key) {
      super(provider, name, key);
   }

   @Override
   public boolean setValueQuietly(int value) {
      getDeviceInfo().setActiveClockSelection(value);
      return super.setValueQuietly(value);
   }

//   private String displayValue = null;
   
//   @Override
//   public void setDisplayValue(String value) {
//      String t = displayValue;
//      displayValue = value;
//      if (t == displayValue) {
//         return;
//      }
//      if ((t!=null) && !t.equals(displayValue)) {
//         notifyListeners();
//      }
//   }
   
//   @Override
//   public String getDisplayValue() {
//      return displayValue;
//   }

}
