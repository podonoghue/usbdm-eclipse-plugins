package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.dialogs.DialogSettings;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Peripheral;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.ModelEntryProvider;
import net.sourceforge.usbdm.deviceEditor.model.VariableModel;

public abstract class PeripheralWithState extends Peripheral implements ModelEntryProvider {

   public class VariableInfo {
      final String description;
      String value;
      
      public VariableInfo(String value, String description) {
         this.value        = value;
         this.description  = description;
      }
   }
   
   protected PeripheralWithState(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
   }

   protected Map<String, VariableInfo> fVariableMap = new HashMap<String, VariableInfo>();

   @Override
   public BaseModel[] getModels(BaseModel parent) {
      BaseModel models[] = {
            new BaseModel(parent, getName(), getDescription()),
         };
      for (String key:fVariableMap.keySet()) {
         VariableInfo variable = fVariableMap.get(key);
         new VariableModel(models[0], key, variable.description, this);
      }
      return models;
   }

   /**
    * Create a variable
    * 
    * @param key     Key used to identify variable
    * @param value   New value for variable
    */
   public void createValue(String key, String value, String description) {
      VariableInfo variable = fVariableMap.get(key);
      if (variable != null) {
         System.err.println("Variable " + key + " already exists");
         return;
      }
      variable = new VariableInfo(value, description);
      fVariableMap.put(key, variable);
   }

   @Override
   public void setValue(String key, String value) {
      VariableInfo variable = fVariableMap.get(key);
      if (variable == null) {
         System.err.println("Variable " + key + " not found");
         return;
      }
      variable.value = value;
   }

   @Override
   public String getValue(String key) {
      VariableInfo variable = fVariableMap.get(key);
      if (variable == null) {
         System.err.println("Variable " + key + " not found");
         return "";
      }
      return variable.value;
   }

   @Override
   public void loadSettings(DialogSettings settings) {
      super.loadSettings(settings);

      for (String key:fVariableMap.keySet()) {
         String value = settings.get(fName+"_"+key);
         if ((value != null) && !value.isEmpty()) {
            setValue(key, value);
         }
      }
   }

   @Override
   public void saveSettings(DialogSettings settings) {
      super.saveSettings(settings);
      
      for (String key:fVariableMap.keySet()) {
         VariableInfo variable = fVariableMap.get(key);
         settings.put(fName+"_"+key, variable.value);
      }
   }

}
