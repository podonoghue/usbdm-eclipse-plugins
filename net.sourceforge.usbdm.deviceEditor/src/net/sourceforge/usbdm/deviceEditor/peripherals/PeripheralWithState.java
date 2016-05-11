package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.dialogs.DialogSettings;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo.Variable;
import net.sourceforge.usbdm.deviceEditor.information.FileUtility;
import net.sourceforge.usbdm.deviceEditor.information.FileUtility.IKeyMaker;
import net.sourceforge.usbdm.deviceEditor.information.Peripheral;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.CategoryModel;
import net.sourceforge.usbdm.deviceEditor.model.IModelChangeListener;
import net.sourceforge.usbdm.deviceEditor.model.IModelEntryProvider;
import net.sourceforge.usbdm.deviceEditor.model.ObservableModel;
import net.sourceforge.usbdm.deviceEditor.model.PeripheralPageModel;
import net.sourceforge.usbdm.deviceEditor.xmlParser.ParseMenuXML;
import net.sourceforge.usbdm.deviceEditor.xmlParser.ParseMenuXML.Data;
import net.sourceforge.usbdm.peripheralDatabase.InterruptEntry;
import net.sourceforge.usbdm.peripheralDatabase.VectorTable;

public abstract class PeripheralWithState extends Peripheral implements IModelEntryProvider, IModelChangeListener{

   /** Data about model loaded from file */
   protected Data fData = null;

   protected PeripheralWithState(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
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
   
   @Override
   public BaseModel getModels(BaseModel parent) {
      fData.fRootModel.setParent(parent);
      return fData.fRootModel;
   }

   @Override
   public void writeInfoConstants(DocumentUtilities pinMappingHeaderFile) throws IOException {
      super.writeInfoConstants(pinMappingHeaderFile);
      pinMappingHeaderFile.write(substitute(fData.fTemplate));
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
      Variable variable = fDeviceInfo.createVariable(keyMaker.makeKey(key), value);
      variable.addListener(this);
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
    * @param map   Map of key->replacement values
    * 
    * @return Modified string or original if no changes
    * @throws Exception 
    */
   String substitute(String input, Map<String, String> map) {
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
      return substitute(input, fDeviceInfo.getSimpleMap());
   }
   
   /**
    * Checks if a variable is true when interpreted as a C value <br>
    * i.e. non-zero or "true"
    * 
    * @param key to Access variable
    * 
    * @return
    */
   public boolean isCTrueValue(String key) {
      String value = null;
      try {
         value = getVariableValue(key);
      } catch (Exception e1) {
      }
      if (value == null) {
         return false;
      }
      try {
         return Long.decode(value) != 0;
      }
      catch (NumberFormatException e){
      }
      return value.equalsIgnoreCase("true");
   }

   public void modifyVectorTable(VectorTable vectorTable, String pattern) {
      final String headerFileName = getBaseName().toLowerCase()+".h";
      boolean handlerSet = false;
      Pattern p = Pattern.compile(pattern);
      for (InterruptEntry entry:vectorTable.getEntries()) {
         if (entry != null) {
            Matcher m = p.matcher(entry.getName());
            if (m.matches()) {
               if (isCTrueValue("IRQ"+m.group(1)+"_HANDLER")) {
                  entry.setHandlerName(DeviceInfo.NAME_SPACE+"::"+getClassName()+"::irq"+m.group(1)+"Handler");
                  entry.setClassMemberUsedAsHandler(true);
                  handlerSet = true;
               }
            }
         }
         if (handlerSet) {
            // Add include file
            vectorTable.addIncludeFile(headerFileName);
         }
      }
   }
   
   @Override
   public void modifyVectorTable(VectorTable vectorTable) {
      modifyVectorTable(vectorTable, "^"+fName+"((\\d+)?).*");
   }

   @Override
   public void modelElementChanged(ObservableModel observableModel) {
   }

   @Override
   public void modelStructureChanged(ObservableModel observableModel) {
   }

}
