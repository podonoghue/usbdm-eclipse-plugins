package net.sourceforge.usbdm.deviceEditor.information;

public class ClockSelectionVariable extends ChoiceVariable {

   public ClockSelectionVariable(String name, String key) {
      super(name, key);
   }

   @Override
   public boolean setValue(String value) {
      
//      fDeviceInfo.getVariable(value);
      int index = getIndex(value);
      fDeviceInfo.setActiveClockSelection(index);
      
      boolean changed = super.setValue(value);
      return changed;
   }

}
