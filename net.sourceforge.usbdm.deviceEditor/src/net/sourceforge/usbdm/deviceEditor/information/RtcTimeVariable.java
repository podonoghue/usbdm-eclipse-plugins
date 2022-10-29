package net.sourceforge.usbdm.deviceEditor.information;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.RtcTimeModel;
import net.sourceforge.usbdm.deviceEditor.model.VariableModel;

public class RtcTimeVariable extends LongVariable {

   public RtcTimeVariable(String name, String key) {
      super(name, key);
   }

   @Override
   public VariableModel createModel(BaseModel parent) {
      return new RtcTimeModel(parent, this);
   }

}
