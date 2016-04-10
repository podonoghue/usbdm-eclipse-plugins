package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.jface.dialogs.DialogSettings;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Peripheral;
import net.sourceforge.usbdm.deviceEditor.model.ModelEntryProvider;

public abstract class PeripheralWithState extends Peripheral implements ModelEntryProvider {

   protected PeripheralWithState(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
   }

   protected Map<String, VariableInfo> fVariableMap = new HashMap<String, VariableInfo>();

   /**
    * Create a variable
    * 
    * @param key           Key used to identify variable
    * @param value         Initial value for variable
    * @param description   Description of variable
    * @param min           Minimum value (inclusive)
    * @param max           Maximum value (inclusive)
    */
   public void createValue(String key, String value, String description, long min, long max) {
      VariableInfo variable = fVariableMap.get(key);
      if (variable != null) {
         System.err.println("Variable " + key + " already exists");
         return;
      }
      variable = new VariableInfo(value, description, Long.parseLong(value), min, max);
      fVariableMap.put(key, variable);
   }

   /**
    * Create a variable with range [Long.MIN_VALUE, Long.MAX_VALUE]
    * 
    * @param key           Key used to identify variable
    * @param value         New value for variable
    * @param description   Description of variable
    */
   public void createValue(String key, String value, String description) {
      createValue(key, value, description, Long.MIN_VALUE, Long.MAX_VALUE);
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
   public String getValueAsString(String key) {
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

   /**
    * Does variable substitution in a string
    * 
    * @param s             String to process
    * @param variableMap   Map of key->replacement values
    * 
    * @return Modified string or original if no changes
    */
   static String substitute(String s, Map<String, VariableInfo> variableMap) {
      for (String key:variableMap.keySet()) {
         String keyPattern = Pattern.quote("${"+key+"}");
         if (s.contains(key)) {
            s = s.replaceAll(keyPattern, variableMap.get(key).value);
         }
      }
      return s;
   }
   
}
