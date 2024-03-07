package net.sourceforge.usbdm.deviceEditor.information;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.RtcTimeModel;
import net.sourceforge.usbdm.deviceEditor.model.VariableModel;
import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;

public class RtcTimeVariable extends LongVariable {

   public RtcTimeVariable(VariableProvider provider, String name, String key) {
      super(provider, name, key);
   }

   @Override
   public VariableModel createModel(BaseModel parent) {
      return new RtcTimeModel(parent, this);
   }

}
