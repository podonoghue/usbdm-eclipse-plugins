package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.dialogs.DialogSettings;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.FileUtility;
import net.sourceforge.usbdm.deviceEditor.information.FileUtility.IKeyMaker;
import net.sourceforge.usbdm.deviceEditor.information.Peripheral;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.IModelChangeListener;
import net.sourceforge.usbdm.deviceEditor.model.IModelEntryProvider;
import net.sourceforge.usbdm.deviceEditor.model.ObservableModel;
import net.sourceforge.usbdm.deviceEditor.validators.Validator;
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

   private final class KeyMaker implements IKeyMaker {
      @Override
      public String makeKey(String name) {
         return getName()+"_"+name;
      }
   }
   
   private final KeyMaker keyMaker = new KeyMaker();

   /**
    * Get key for variable owned by this peripheral
    * 
    * @param name
    * @return
    */
   public String makeKey(String name) {
      return keyMaker.makeKey(name);
   }

   /**
    * Load the models and valudators for this class of peripheral
    * 
    * @return
    */
   public void loadModels() {
      loadModels(getVersion());
      for (ParseMenuXML.Validator v:fData.fValidators) {
         try {
            String className = v.getClassName();
            // Get validator class
            Class<?> clazz = Class.forName(className);
            Validator validatorClass = (Validator) clazz.getConstructor(PeripheralWithState.class, v.getParams().getClass()).newInstance(this, v.getParams());
            addValidator(validatorClass);
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
   }
   

   /**
    * Load the models for this class of peripheral
    * 
    * @return
    */
   public final void loadModels(String name) {
      try {
         fData = ParseMenuXML.parseFile(name, null, this);
      } catch (Exception e) {
         System.err.println("Failed to load models for " + getName());
         e.printStackTrace();
      }
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
   public void addVariable(Variable variable) {
      fDeviceInfo.addVariable(variable.getKey(), variable);
      variable.addListener(this);
   }

   @Override
   public void setVariableValue(String key, String value) {
      fDeviceInfo.setVariableValue(key, value);
   }

   @Override
   public String getVariableValue(String key) {
      return fDeviceInfo.getVariableValue(key);
   }

   @Override
   public Variable getVariable(String key) {
      return fDeviceInfo.getVariable(key);
   }

   @Override
   public Variable safeGetVariable(String key) {
      try {
         return fDeviceInfo.getVariable(key);
      } catch (Exception e) {
         return null;
      }
   }

   public Variable getPublicVariable(String key) {
      try {
         return fDeviceInfo.getVariable(FileUtility.publicKeyMaker.makeKey(key));
      } catch (Exception e) {
         return null;
      }
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
    * @param input   String to process
    * @param map     Map of key->replacement values
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
         value = getVariable(keyMaker.makeKey(key)).getSubstitutionValue();
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

   ArrayList<Validator> validators = new ArrayList<Validator>();
   
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
   
   @Override
   public void modelElementChanged(ObservableModel observableModel) {
      if (observableModel instanceof Variable) {
         variableChanged((Variable)observableModel);
      }
   }

   @Override
   public void modelStructureChanged(ObservableModel observableModel) {
   }

   /**
    * Get priority - a number used to order the instantiation of peripherals
    * 
    * @return Priority, larger is higher priority.
    */
   public int getPriority() {
      return 0;
   }

   @Override
   public String toString() {
      return this.getClassName()+"("+getName()+", "+getVersion()+")";
   }

}
