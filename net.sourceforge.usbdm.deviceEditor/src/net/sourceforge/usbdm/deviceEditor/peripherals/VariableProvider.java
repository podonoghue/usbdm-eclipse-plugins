package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.validators.Validator;
import net.sourceforge.usbdm.packageParser.IKeyMaker;
import net.sourceforge.usbdm.packageParser.ISubstitutionMap;

public class VariableProvider {

   /** Name of provider e.g. FTM2 */
   private final String                fName;
   
   /** Device information associated with this provider */
   protected final DeviceInfo            fDeviceInfo;
   
   /** Validators for variable changes */
   private final ArrayList<Validator>  fValidators = new ArrayList<Validator>();

   private boolean suspended = true;
   
   protected final class KeyMaker implements IKeyMaker {
      
      /**
       * Provides key prefixed by provider name if <b>name</b> is relative.<br>
       * Otherwise the key is simply the <b>name</b>.
       * <pre>
       *    "path/clockSpeed"  => "/FTM2/path/clockSpeed"
       *    "/path/clockSpeed" => "/path/clockSpeed"
       * </pre>
       */
      @Override
      public String makeKey(String name) {
         if (name.charAt(0) == '/') {
            // Don't modify explicit variables
            return name;
         }
         return "/"+getName()+"/"+name;
      }
   }
   
   /**
    * KeyMaker that adds indices to key
    */
   protected final class IndexKeyMaker implements IKeyMaker {
      private final String    fIndex;
      private final IKeyMaker fKeyMaker;

      /**
       * Constructor
       * 
       * @param keyMaker   KeyMaker to wrap
       * @param index      Index to add
       */
      public IndexKeyMaker(IKeyMaker keyMaker, int index) {
         fIndex    = "[" + index + "]";
         fKeyMaker = keyMaker;
      }
      
      @Override
      public String makeKey(String name) {
         name = fKeyMaker.makeKey(name);
         return name.replaceAll("\\.$", fIndex);
      }
   }
   
   protected final IKeyMaker fKeyMaker = new KeyMaker();

   /**
    * Constructor
    * 
    * @param name          Name for provider. Used to qualify variables in shared device map
    * @param deviceInfo    Device information. Used to access device map.
    */
   public VariableProvider(String name, DeviceInfo deviceInfo) {
      fName       = name;
      fDeviceInfo = deviceInfo;
   }
   
   /**
    * Get name used to identify this provider e.g. FTM2
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
    * If the name is relative then the key will be prefixed with the provider path (e.g. ClockFreq => /PDB0/ClockFreq)<br>
    * Otherwise the original name is returned as the key unchanged (e.g. /SIM/system_bus_clock would be unchanged)
    */
   public String makeKey(String name) {
      return fKeyMaker.makeKey(name);
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
      return fDeviceInfo.getVariable(key);
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
      return fDeviceInfo.getVariableValue(key);
   }

   /**
    * Get value of variable
    * 
    * @param key     Key used to identify variable
    * 
    * @return Value for variable
    */
   public String safeGetVariableValue(String key) {
      Variable var = safeGetVariable(key);
      if (var == null) {
         return null;
      }
      return var.getValueAsString();
   }

   /**
    * Adds a variable
    * 
    * @param variable  Variable to add
    * 
    * @throws Exception if variable already exists
    */
   public void addVariable(Variable variable) {
      fDeviceInfo.addVariable(variable);
   }

   /**
    * Removes a variable
    * 
    * @param variable  Variable to remove
    * 
    * @throws Exception if variable does not exist
    */
   public void removeVariable(Variable variable) {
      fDeviceInfo.removeVariable(variable.getKey());
   }

   /**
    * Does variable substitution in a string<br>
    * Keymaker from provider is used i.e. prefix is this.getName()
    * 
    * @param input   String to process
    * @param map     Map of key->replacement values
    * 
    * @return Modified string or original if no changes
    * @throws Exception 
    */
   public String substitute(String input, ISubstitutionMap map) {
      return map.substitute(input, fKeyMaker);
   }
   
   /**
    * Does variable substitution in a string using the device variable map
    * The following names are automatically added:
    *    <li> _name           Name used to identify this provider e.g. FTM2
    * 
    * @param input  String to process
    * 
    * @return Modified string or original if no changes
    */
   public String substitute(String input) {
      ISubstitutionMap map = fDeviceInfo.getVariablesSymbolMap();
      map.addValue(makeKey("_name"),     getName());
      return substitute(input, map);
   }

   /**
    * Adds validator for VariableProvider (Peripheral)
    * 
    * @param validator
    */
   public void addValidator(Validator validator) {
      fValidators.add(validator);
   }
   
   /**
    * Gets validators for VariableProvider (Peripheral)
    *
    * @return List of validators
    */
   public ArrayList<Validator> getValidators() {
      return fValidators;
   }
   /**
    * Sets whether variable changes are notified to listeners
    * 
    * @param suspend true to suspend notiofication
    */
   public void setSuspended(boolean suspend) {
      suspended = suspend;
   }
   
   public void variableChanged(Variable variable) {
//      System.err.println("variableChanged()" + variable.toString());
      fDeviceInfo.setDirty(true);
      if (suspended) {
         return;
      }
      for (Validator v:fValidators) {
         v.variableChanged(variable);
      }
   }
   
   /**
    * Set editor dirty via deviceInfo
    */
   public void setDirty(boolean dirty) {
      if (fDeviceInfo != null) {
         fDeviceInfo.setDirty(dirty);
      }
   }
   
   /**
    * Set editor dirty via deviceInfo
    * @return 
    */
   public DeviceInfo getDeviceInfo() {
      return fDeviceInfo;
   }
   


}
