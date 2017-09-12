package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.util.ArrayList;
import java.util.Map;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.FileUtility;
import net.sourceforge.usbdm.deviceEditor.information.FileUtility.IKeyMaker;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.validators.Validator;

public class VariableProvider {

   /** Name of peripheral e.g. FTM2 */
   private final String                fName;
   
   /** Device information associated with this peripheral */
   private final DeviceInfo            fDeviceInfo;
   
   /** Validators for variable changes */
   private final ArrayList<Validator>  validators = new ArrayList<Validator>();

   /** Index to append to variables */
   private int fIndex = 0;

   protected final class KeyMaker implements IKeyMaker {
      int Index;
      
      public int getIndex() {
         return Index;
      }

      public void setIndex(int index) {
         this.Index = index;
      }

      @Override
      public String makeKey(String name) {
         if (name.indexOf("#") >= 0) {
            name = name+fIndex;
         }
         if (name.charAt(0) == '/') {
            // Don't modify explicit variables
            return name;
         }
         return "/"+getName()+"/"+name;
      }
   }
   
   protected final class IndexKeyMaker implements IKeyMaker {
//      private final String fIndex;
      
      public IndexKeyMaker(int index) {
//         fIndex = "[" + index + "]";
      }
      @Override
      public String makeKey(String name) {
         if (name.charAt(0) == '/') {
            return name;
         }
         return "/"+getName()+"/"+name;
      }
   }
   
   protected final KeyMaker keyMaker = new KeyMaker();

   /**
    * Constructor
    * 
    * @param name          Name for provider. Used to qualify variables in shared device map
    * @param deviceInfo    Device information . Used to access device map.
    */
   public VariableProvider(String name, DeviceInfo deviceInfo) {
      fName       = name;
      fDeviceInfo = deviceInfo;
   }
   
   /**
    * Get name used to identify this provider
    * 
    * @return
    */
   public String getName() {
      return fName;
   }

   /**
    * Create key for variable owned by this provider
    * 
    * @param name
    * 
    * @return key for the name<br>
    * If the name is relative then the key will be prefixed with the provider path (e.g. ClockFreq => /PDB/ClockFreq)<br>
    * Otherwise the original name is returned as the key unchanged (e.g. /SIM/system_bus_clock would be unchanged)
    */
   public String makeKey(String name) {
      return keyMaker.makeKey(name);
   }

   /**
    * Get variable with given key
    * 
    * @param key     Key to lookup variable
    * 
    * @return variable or null if not found
    */
   public Variable safeGetVariable(String key) {
         return fDeviceInfo.safeGetVariable(key);
   }

   /**
    * Get variable with given key
    * 
    * @param key     Key to lookup variable
    * 
    * @return variable
    * @throws Exception
    */
   public Variable getVariable(String key) throws Exception {
      try {
         return fDeviceInfo.getVariable(key);
      } catch (Exception e) {
         throw new Exception("Variable error in provider "+getName()+", var="+key, e);
      }
   }

   /**
    * Set value of variable
    * 
    * @param key     Key used to identify variable
    * @param value   New value for variable
    */
   public void setVariableValue(String key, String value) {
      fDeviceInfo.setVariableValue(key, value);
   }

   /**
    * Get value of variable
    * 
    * @param key     Key used to identify variable
    * 
    * @return Value for variable
    */
   public String getVariableValue(String key) throws Exception {
      try {
         return fDeviceInfo.getVariableValue(key);
      } catch (Exception e) {
         throw new Exception("Variable error in peripheral "+getName(), e);
      }
   }

   /**
    * Add a variable
    * 
    * @param key     Key identifying variable
    * @param value   Initial value for variable
    * 
    * @throws Exception if variable already exists
    */
   public void addVariable(Variable variable) {
      fDeviceInfo.addVariable(variable.getKey(), variable);
   }

   /**
    * Does variable substitution in a string
    * 
    * @param input   String to process
    * @param map     Map of key->replacement values
    * 
    * @return Modified string or original if no changes
    * @throws Exception 
    */
   public String substitute(String input, Map<String, String> map) {
      return FileUtility.substitute(input, map, keyMaker);
   }
   
   /**
    * Does variable substitution in a string using the device variable map
    * 
    * @param input  String to process
    * 
    * @return Modified string or original if no changes
    */
   String substitute(String input) {
      Map<String, String> map = fDeviceInfo.getSimpleSymbolMap();
      map.put(makeKey("_name"),     getName());
      return substitute(input, map);
   }

   public void addValidator(Validator validator) {
      validators.add(validator);
   }
   
   public void variableChanged(Variable variable) {
//      System.err.println("variableChanged()" + variable.toString());
      fDeviceInfo.setDirty(true);
      for (Validator v:validators) {
         v.variableChanged(variable);
      }
   }

   /**
    * Set index for dimensioned variables
    * 
    * @param index 0 => not dimensioned
    */
   public void setIndex(int index) {
      fIndex = index;
   }

}
