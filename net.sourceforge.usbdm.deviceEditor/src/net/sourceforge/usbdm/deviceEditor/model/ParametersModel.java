package net.sourceforge.usbdm.deviceEditor.model;

public class ParametersModel extends TreeViewModel {

   static final public String[] OTHER_COLUMN_LABELS = {"Peripheral.Parameter", "Value", "Description"};

   public ParametersModel(BaseModel parent, String title, String toolTip) {
      super(parent, title, toolTip, OTHER_COLUMN_LABELS);
   }

   public String[] getColumnLabels() {
      return OTHER_COLUMN_LABELS;
   }

}
