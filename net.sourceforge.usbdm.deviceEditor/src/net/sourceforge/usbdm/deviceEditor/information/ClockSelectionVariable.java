package net.sourceforge.usbdm.deviceEditor.information;

public class ClockSelectionVariable extends ChoiceVariable {

   public ClockSelectionVariable(String name, String key) {
      super(name, key);
   }

   @Override
   public boolean setValue(int value) {

      fDeviceInfo.setActiveClockSelection(value);
      return super.setValue(value);
   }

   private String displayValue = null;
   
   @Override
   public void setDisplayValue(String value) {
      String t = displayValue;
      displayValue = value;
      if (t == displayValue) {
         return;
      }
      if ((t!=null) && !t.equals(displayValue)) {
         notifyListeners();
      }
   }
   
   @Override
   public String getDisplayValue() {
      return displayValue;
   }

}
