package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.util.Map;

import org.eclipse.jface.dialogs.DialogSettings;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.FileUtility;
import net.sourceforge.usbdm.deviceEditor.information.FileUtility.IKeyMaker;
import net.sourceforge.usbdm.deviceEditor.information.Peripheral;
import net.sourceforge.usbdm.deviceEditor.model.IModelEntryProvider;
import net.sourceforge.usbdm.deviceEditor.xmlParser.ParseMenuXML;
import net.sourceforge.usbdm.deviceEditor.xmlParser.ParseMenuXML.Data;

public abstract class PeripheralWithState extends Peripheral implements IModelEntryProvider {

   /** Data about model loaded from file */
   protected Data fData = null;

   protected PeripheralWithState(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
   }

   // XXX Remove
   public void createValue(String key, String value, String string2, long min, long max) {
      createVariable(key, value);
   }

   // XXX Remove
   public void createValue(String key, String value, String string2) {
      createVariable(key, value);
   }

   private class KeyMaker implements IKeyMaker {
      @Override
      public String makeKey(String name) {
         return getName()+"_"+name;
      }
   }
   
   private KeyMaker keyMaker = new KeyMaker();

   /**
    * Load the models for this class of peripheral
    * 
    * @return
    */
   public void loadModels() {
      loadModels(getVersion());
   }
   
   /**
    * Load the models for this class of peripheral
    * 
    * @return
    */
   public final void loadModels(String name) {
      fData = ParseMenuXML.parseFile(name, null, this);
   }
   
   /**
    * Create a variable
    * 
    * @param key     Key identifying variable
    * @param value   Initial value for variable
    * 
    * @throws Exception if variable already exists
    */
   public void createVariable(String key, String value) {
      fDeviceInfo.createVariable(keyMaker.makeKey(key), value);
   }

   @Override
   public void setVariableValue(String key, String value) {
      fDeviceInfo.setVariableValue(keyMaker.makeKey(key), value);
   }

   @Override
   public String getVariableValue(String key) {
      return fDeviceInfo.getVariableValue(keyMaker.makeKey(key));
   }

   @Override
   public void loadSettings(DialogSettings settings) {
      super.loadSettings(settings);
   }

   @Override
   public void saveSettings(DialogSettings settings) {
      super.saveSettings(settings);
   }
   
   /**
    * Does variable substitution in a string
    * 
    * @param input         String to process
    * @param variableMap   Map of key->replacement values
    * 
    * @return Modified string or original if no changes
    * @throws Exception 
    */
   String substitute(String input, Map<String, String> variableMap) {
      return FileUtility.substitute(input, variableMap, keyMaker);
   }

   /**
    * Does variable substitution in a string using the device variable map
    * 
    * @param input  String to process
    * 
    * @return Modified string or original if no changes
    */
   String substitute(String input) {
      return substitute(input, fDeviceInfo.getVariableMap());
   }
}
