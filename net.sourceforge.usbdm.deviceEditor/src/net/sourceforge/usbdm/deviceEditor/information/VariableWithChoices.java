package net.sourceforge.usbdm.deviceEditor.information;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo.InitPhase;
import net.sourceforge.usbdm.deviceEditor.parsers.Expression.VariableUpdateInfo;
import net.sourceforge.usbdm.deviceEditor.parsers.SimpleExpressionParser;
import net.sourceforge.usbdm.deviceEditor.parsers.SimpleExpressionParser.Mode;

public abstract class VariableWithChoices extends Variable {

   /** List of choices to be re-created each time */
   boolean fDynamicChoices = false;
   
   /** List of choice names */
   private String[] fChoices = null;

   public VariableWithChoices(String name, String key) {
      super(name, key);
   }
   
   /**
    * Get choice data available in GUI (includes disabled choices)
    * 
    * @return Visible choices (enable and disabled)
    */
   public abstract ChoiceData[] getChoiceData();

   /**
    * Get hidden choice data
    * 
    * @return Hidden choices (used for code generation only)
    */
   public abstract ChoiceData[] getHiddenChoiceData();

   /**
    * Convert an enum value into a complete enum for code use
    * 
    * @param enumValue
    * 
    * @return Converted value e.g. Disable => LowPower_Disabled
    */
   protected String makeEnum(String enumValue) {
      if (getTypeName() == null) {
         return null;
      }
      if (enumValue == null) {
         return null;
      }
      return getTypeName()+"_"+enumValue;
   }
   
   
   void clearCachedChoiceInformation() {
      fChoices = null;
   }
   
   /**
    * 
    */
   public void updateChoices() {
      String[] choices = getVisibleChoiceNames();
      if (getValueAsLong()>=choices.length) {
         setValue(0);
      }
      notifyListeners();
   }
   
//   /**
//    * Indicates that at least one choice is dynamic
//    *
//    * @return
//    */
//   public boolean hasDynamicChoices() {
//      if (fDynamicChoices) {
//         return true;
//      }
//      ChoiceData[] data = getChoiceData();
//      if (data == null) {
//         return false;
//      }
//      for (ChoiceData choice:data) {
//         if (choice.isDynamic()) {
//            fDynamicChoices = true;
//            return true;
//         }
//      }
//      return false;
//   }

   /**
    * @return The names of the choices currently available in GUI.<br>
    *         This is affected by enabled choices.
    * 
    * @throws Exception
    */
   public String[] getVisibleChoiceNames() {
      
      if (fDynamicChoices || (fChoices == null)) {
         
         // Construct new list of choice names
         ChoiceData[] choiceData = getChoiceData();
         if (choiceData == null) {
            return null;
         }
         
         ArrayList<String> choices = new ArrayList<String>();
         for (int index=0; index<choiceData.length; index++) {
            fDynamicChoices = fDynamicChoices || choiceData[index].isDynamic();
            if (!choiceData[index].isEnabled(getProvider())) {
               continue;
            }
            choices.add(choiceData[index].getName());
         }
         fChoices = choices.toArray(new String[choices.size()]);
      }
      return fChoices;
   }

   /**
    * @return the choice data currently available in GUI.<br>
    *         This is affected by enabled choices
    * 
    * @throws Exception
    */
   public ArrayList<ChoiceData> getVisibleChoiceData() {

      // TODO - memorise?

      ChoiceData[] choiceData = getChoiceData();
      if (choiceData == null) {
         return null;
      }

      ArrayList<ChoiceData> choices = new ArrayList<ChoiceData>();
      for (int index=0; index<choiceData.length; index++) {
         if (!choiceData[index].isEnabled(getProvider())) {
            continue;
         }
         choices.add(choiceData[index]);
      }
      return choices;
   }

   /**
    * Set value (selection) by choice index
    * 
    * @param index Index into available currently available choices
    */
   abstract void setIndex(int index);
   
   /**
    * Set value by name
    * Only the visible choices are available.
    * 
    * @param name Name of choice to select<br>
    * 
    * @return True => value changed
    */
   public boolean setValueByName(String name) {
      
      String[] choiceData = getVisibleChoiceNames();
      if (choiceData == null) {
         return false;
      }
      
      int index = 0;
      for (String choice:choiceData) {
         if (choice.equalsIgnoreCase(name)) {
            break;
         }
         index++;
      }
      return setValue(index);
   }

   /**
    * Get index of value in choice entries
    * Only the visible choices are available.
    * 
    * @return index or -1 if not found
    */
   protected int getChoiceIndex(String name) {
      ChoiceData[] data = getChoiceData();
      if (data == null) {
         return -1;
      }
      for (int index=0; index<data.length; index++) {
         if (data[index].getName().equalsIgnoreCase(name)) {
            return index;
         }
      }
      return -1;
   }
   
   public ChoiceData getSelectedItemData() {
      int index = getChoiceIndex(getValueAsString());
      if (index<0) {
         index = (int)getValueAsLong();
      }
      return getChoiceData()[index];
   }
   
   final Pattern fFieldPattern = Pattern.compile("^(\\w+)(\\[(\\d+)?\\])?$");

   public int getChoiceIndex() {
      // Use index of current selected item
      return getChoiceIndex(getValueAsString());
   }
   
   /**
    * Get data for currently selected choice
    * 
    * @return Choice data
    */
   public ChoiceData getCurrentChoice() {
      int index = getChoiceIndex();
      if (index<0) {
         return null;
      }
      return getVisibleChoiceData().get(index);
   }
   
   /**
    * {@inheritDoc}<br>
    * 
    * Modified by allowing an index e.g. code[3] to select the code information associated with the 3rd choice.
    */
   @Override
   public String getField(String field) {
      Matcher m = fFieldPattern.matcher(field);
      if (!m.matches()) {
         return "Field "+field+" not matched";
      }

      if (m.group(2) == null) {
         // No index - get field directly from variable
         return super.getField(field);
      }
      
      // Return data from choice ([] present)
      String fieldName = m.group(1);
      int index;
      if (m.group(3) != null) {
         // Parse required index
         index = Integer.parseInt(m.group(3));
         if (index>= getChoiceData().length) {
            return "Index "+index+" out of range for variable "+getName() + ", field ="+field;
         }
      }
      else {
         // Use index of current selected item
         index = getChoiceIndex();
         if (index<0) {
            index = getChoiceIndex();
            return "No current choice value to retrieve data, -"+getValueAsString()+"- not found for field "+field;
         }
      }
      ChoiceData fData = getChoiceData()[index];
      if ("code".equals(fieldName)) {
         return fData.getCodeValue();
      } else if ("enum".equals(fieldName)) {
         String enumname = makeEnum(fData.getEnumName());
         if (enumname != null) {
            return enumname;
         }
         return getUsageValue();
      } else if ("name".equals(fieldName)) {
         return fData.getName();
      } else if ("value".equals(fieldName)) {
         return fData.getValue();
      }
      return "Field "+field+" not matched in choice";
   }

   /**
    * Get value as enum e.g. PmcLowVoltageDetect_Disabled
    * 
    * @return String for text substitutions (in C code)
    */
   public String getEnumValue() {
      // Use index of current selected item
      int index = getChoiceIndex(getValueAsString());
      if (index<0) {
         return "No current value for" + getName();
      }
      ChoiceData fData = getChoiceData()[index];
      return makeEnum(fData.getEnumName());
   }

   @Override
   public String getUsageValue() {
      String rv = getEnumValue();
      if (rv == null) {
         rv = getSubstitutionValue();
      }
      return rv;
   }

   @Override
   public void addInternalListeners() throws Exception {
      super.addInternalListeners();
//      System.err.println("Processing "+ getName());

      // Listen to choice expressions
      ChoiceData[] data = getChoiceData();
      for (ChoiceData choiceData:data) {
         choiceData.addListener(this);
      }
//      updateTargets(getCurrentChoice());
   }

   /**
    * Update targets affected by this choice selection
    * 
    * @param choiceData
    */
   void updateTargets(ChoiceData choiceData) {
      
      if (getDeviceInfo().getInitialisationPhase() == InitPhase.VariablePropagationSuspended) {
         return;
      }
//      if (getName().contains("ics_c2_bdiv")) {
//         System.err.println("Found it "+getName());
//      }
      String target = getTarget();

      if (target == null) {
         return;
      }
      
      String multiRef = choiceData.getMultiValueReference();
      if (multiRef != null) {
         String refs[]    = multiRef.split(";");
         String targets[] = getTarget().split(";");
         try {
            if (refs.length != targets.length) {
               throw new Exception("unmatched");
            }
            for (int index=0; index<refs.length; index++) {
               Variable targetVar = getProvider().getVariable(targets[index]);
               Object   value     = SimpleExpressionParser.evaluate(refs[index], getProvider(), Mode.EvaluateFully);
               targetVar.setValue(value);
            }
            return;
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
      try {
         // choice.ref => target
         Variable targetVar = getProvider().getVariable(target);

         VariableUpdateInfo info = new VariableUpdateInfo();

         // Assume enabled (may be later disabled by enabledBy etc.)
         info.enable = true;
         determineReferenceUpdate(info, choiceData.getReference());
         targetVar.enable(info.enable);
         
         if (info.value != null) {
            targetVar.setValue(info.value);
         }
         if (info.origin != null) {
            targetVar.setOrigin(info.origin+"\nSelected by "+getDescription());
         }
         targetVar.setStatus(info.status);
         
      } catch (Exception e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
   }
}
