package net.sourceforge.usbdm.deviceEditor.model;

public class VariableModel extends NumericModel {

   private final ModelEntryProvider fProvider;
   private final String fKey;
   
   public VariableModel(BaseModel parent, String name, String description, ModelEntryProvider provider) {
      super(parent, name, description);
      fProvider = provider;
      fKey      = name;
   }

   @Override
   public String getValueAsString() {
      return fProvider.getValue(fKey);
   }

   @Override
   void setValue(String value) {
      fProvider.setValue(fKey, value);
   }
   
}
