package net.sourceforge.usbdm.deviceEditor.information;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo.InitPhase;
import net.sourceforge.usbdm.deviceEditor.model.IModelChangeListener;
import net.sourceforge.usbdm.deviceEditor.parsers.Expression;
import net.sourceforge.usbdm.deviceEditor.parsers.Expression.VariableUpdateInfo;
import net.sourceforge.usbdm.deviceEditor.parsers.XML_BaseParser;
import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;

public abstract class VariableWithChoices extends Variable {

   /** List of choices are to be re-created each time */
   boolean fDynamicChoices = false;
   
   /** List of <b>available</b> choice names */
   private String[] fChoices = null;

   /** Name of table to produce in C code */
   private String fTableName;
   
   public VariableWithChoices(VariableProvider provider, String name, String key) {
      super(provider, name, key);
   }
   
   /**
    * Get choice data (includes disabled choices)
    * 
    * @return Visible choices (enable and disabled)
    */
   public abstract ChoiceData[] getChoiceData();

   /**
    * Get choice data for hidden choices
    * 
    * @return Visible choices (enable and disabled)
    */
   public abstract ChoiceData[] getHiddenChoiceData();

   /**
    * Set choice by index
    * 
    * @param index Index of choice to select
    * 
    * @return true if index actually changes
    */
   public abstract boolean setChoiceIndex(int index);
   
//   /**
//    * Get the index of the currently selected choice
//    *
//    * @return Index
//    */
//   public abstract int getChoiceIndex();
   
   /**
    * Get number of choices available (including disabled choices)
    * 
    * @return
    */
   public int getChoiceCount() {
      return getChoiceData().length;
   }
   
   void clearCachedChoiceInformation() {
      fChoices = null;
   }
   
   /**
    * Find first enabled choice
    * 
    * @return Index of choice or -1 if none are available!!
    */
   int findFirstAvailableIndex() {
      ChoiceData[] choices = getChoiceData();
      for (int index=0; index<choices.length; index++) {
         if (choices[index].isEnabled(getProvider())) {
            return index;
         }
      }
      return -1;
   }
   
   /**
    * Checks and updates the choice index if no longer available
    * 
    * @return true if index changes due to check
    */
   public boolean checkChoiceIndex() {
      // Check if current choice valid
      ChoiceData currentChoice = getCurrentChoice();
      if ((currentChoice != null)&&(currentChoice.isEnabled(getProvider()))) {
         return false;
      }
      // Set 'safe' value
      return setChoiceIndex(findFirstAvailableIndex());
   }
   
   /**
    * @return The names of the choices currently available in GUI.<br>
    *         This is affected by enabled choices.
    * 
    * @throws Exception
    */
   public String[] getVisibleChoiceNames() {
      
      boolean previousDynamicChoices = false;
      
      ChoiceData[] choiceData = getChoiceData();
      
      // Create new list if already found dynamic or list not created yet
      if (fDynamicChoices || (fChoices == null)) {
         
         // Construct new list of choice names
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
      // Add listeners if newly found dynamic
      if (!previousDynamicChoices && fDynamicChoices) {
         for (ChoiceData choiceDatax:getChoiceData()) {
            if (choiceDatax.isDynamic()) {
               try {
                  choiceDatax.addListener(this);
               } catch (Exception e) {
                  e.printStackTrace();
               }
            }
         }
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

      ChoiceData[] choiceData = getChoiceData();
      if (choiceData == null) {
         return null;
      }

      ArrayList<ChoiceData> choices = new ArrayList<ChoiceData>();
      for (int index=0; index<choiceData.length; index++) {
         if (getDeviceInfo().getInitialisationPhase().isLaterThan(InitPhase.VariablePropagationSuspended) &&
               !choiceData[index].isEnabled(getProvider())) {
            continue;
         }
         choices.add(choiceData[index]);
      }
      return choices;
   }

   /**
    * Set value by name
    * 
    * @param name Name of choice to select<br>
    * 
    * @return True => value changed
    */
   public boolean setValueByName(String name) {
      
      ChoiceData[] choiceData = getChoiceData();
      if (choiceData == null) {
         return false;
      }
      int index = 0;
      for (ChoiceData choice:choiceData) {
         if (choice.getName().equalsIgnoreCase(name)) {
            break;
         }
         index++;
      }
      return setValue(index);
   }

   /**
    * Get default choice entry
    * 
    * @return choice or null if none
    */
   protected abstract ChoiceData getdefaultChoice();
   
   /**
    * Get index of value in choice entries (including disabled)
    * 
    * @param name Name of choice
    * 
    * @return index or -1 if not found
    */
   protected int getChoiceIndexByName(String name) {
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
   
   /**
    * Get data for currently selected choice (even if disabled)
    * 
    * @return Choice data or null if none selected or available
    */
   abstract ChoiceData getCurrentChoice();
   
   /**
    * Get data for selected choice if enabled or disabled choice if not
    * 
    * @return Choice data or null if none selected or available
    */
    public abstract ChoiceData getEffectiveChoice();
   
   /**
    * {@inheritDoc}<br>
    * 
    * May be modified by adding an index e.g. code[3] to select the code information associated with the 3rd choice.
    */
   @Override
   public String getField(String field) {
      final Pattern fFieldPattern = Pattern.compile("^(\\w+)(\\[(\\d+)?\\])?$");

      Matcher m = fFieldPattern.matcher(field);
      if (!m.matches()) {
         return "Field "+field+" not matched";
      }

      if (m.group(2) == null) {
         // No index - get field directly from variable
         if ("size".equalsIgnoreCase(field)) {
            return Integer.toString(getChoiceCount());
         }
         return super.getField(field);
      }
      
      // Return data from choice ([] present)
      String fieldName = m.group(1);
      ChoiceData choice = null;
      if (m.group(3) != null) {
         // Parse required index
         int index = Integer.parseInt(m.group(3));
         if (index>= getChoiceData().length) {
            return "Index "+index+" out of range for variable "+getName() + ", field ="+field;
         }
         choice = getChoiceData()[index];
      }
      else {
         // Use index of current selected item
         choice = getCurrentChoice();
      }
      if ("code".equals(fieldName)) {
         return choice.getCodeValue();
      } else if ("enum".equals(fieldName)) {
         String enumname = makeEnum(choice.getEnumName());
         if (enumname != null) {
            return enumname;
         }
         return getUsageValue();
      } else if ("name".equals(fieldName)) {
         return choice.getName();
      } else if ("value".equals(fieldName)) {
         return choice.getValue();
      }
      return "Field "+field+" not matched in choice";
   }

   /**
    * Get effective value as enum e.g. PmcLowVoltageDetect_Disabled
    * 
    * @return String for text substitutions (in C code)
    */
   public String getEnumValue() {
      
      ChoiceData choice = getEffectiveChoice();
      if (choice == null) {
         return "--no selection--";
      }
      return makeEnum(choice.getEnumName());
   }

//   /**
//    * Get value as enum e.g. PmcLowVoltageDetect_Disabled
//    * @param value
//    *
//    * @return String for text substitutions (in C code)
//    */
//   public String getEnumValue(String value) {
//      // Get index of value
//      int index = getChoiceIndexByName(value);
//      if (index<0) {
//         return null;
//      }
//      ChoiceData fData = getChoiceData()[index];
//      return makeEnum(fData.getEnumName());
//   }

   @Override
   public String formatUsageValue(String value) {
      return makeEnum(value);
   }
   
   @Override
   public String getUsageValue() {
      String rv = getEnumValue();
      if (rv == null) {
         rv = getSubstitutionValue();
      }
      return rv;
   }

   /**
    * {@inheritDoc}
    * 
    * <li>choice[].enableBy
    * <li>choice[].nameExpression
    * <li>choice[].reference
    */
   @Override
   public void addInternalListeners() throws Exception {
      // Listen to choice expressions
      ChoiceData[] data = getChoiceData();
      if (data != null) {
         for (ChoiceData choiceData:data) {
            try {
               choiceData.addListener(this);
            } catch (Exception e) {
               System.err.println("Failed to add internal listener to choice variable '" + this.getName() + "'");
               System.err.println("Choice '" + choiceData.getName() + "', ref= '" + choiceData.getReference() + "'");
            }
         }
      }
      super.addInternalListeners();

   }

   /**
    * Update targets affected by this choice selection
    * 
    * @param choiceData  Choice being examined
    * @param info        Updated if choice affects owner e.g. name changes
    * @param expression  Expression triggering update
    * 
    * @throws Exception
    */
   void updateChoice(ChoiceData choiceData, VariableUpdateInfo info, Expression expression) {

      if (choiceData == null) {
         return;
      }
      if (getDeviceInfo().getInitialisationPhase().isEarlierThan(InitPhase.VariablePropagationAllowed)) {
         return;
      }
      if (fLogging) {
         System.err.println(getName()+".updateTargets("+choiceData.getName()+")");
      }
      try {
         // Update pin mapping from choice (or disabled)
         String     disabledPinMap = getDisabledPinMap();
         Expression pinMapEnable   = getPinMapEnable();

         if ((pinMapEnable != null) && !pinMapEnable.getValueAsBoolean()) {
            if ((disabledPinMap == null)||(disabledPinMap.isBlank())) {
               // Use map from default choice
               ChoiceData defaultChoice = getdefaultChoice();
               disabledPinMap = defaultChoice.getPinMap();
            }
            // Release pin mappings
            releaseActivePinMappings(disabledPinMap);
         }
         else if (!isEnabled() && (disabledPinMap != null) && (!disabledPinMap.isBlank())) {
            // Disabled and special mapping provided
            setActivePinMappings(disabledPinMap);
         }
         else {
            // Use mapping from choice
            setActivePinMappings(choiceData.getPinMap());
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

      // Target affected?
      String target = getTarget();
      if (target == null) {
         return;
      }
      
      String multiRef = choiceData.getMultiValueReference();
      try {
         if (multiRef != null) {
            String refs[]    = multiRef.split(";",-1);
            String targets[] = getTarget().split(";",-1);
            if (refs.length != targets.length) {
               throw new Exception("length of refs does not match target");
            }
            for (int index=0; index<refs.length; index++) {
               if (refs[index].isBlank()) {
                  // Blank ignore
                  continue;
               }
               Variable targetVar = getProvider().getVariable(targets[index]);
               Object   value     = Expression.getValue(refs[index], getProvider());
               targetVar.setValue(value);
            }
            return;
         }
      } catch (Exception e) {
         Exception t = new Exception("Failed to update from Expression '"+multiRef+"'", e);
         t.printStackTrace();
      }
      
      Expression referenceExpression = choiceData.getReference();
      try {
         // Update choice.ref => target
         Variable targetVar = getProvider().getVariable(target);
         
         VariableUpdateInfo info1 = new VariableUpdateInfo();
         targetVar.determineUpdateInformation(info1, referenceExpression);
         targetVar.update(info1);
         if (info1.properties != 0) {
            targetVar.notifyListeners(info1.properties);
         }
      } catch (Exception e) {
         Exception t = new Exception("Failed to update from Expression '"+referenceExpression+"' to target '"+target+"'", e);
         t.printStackTrace();
      }
      checkChoiceIndex();
   }

   /**
    * {@inheritDoc}<br>
    * <br>
    * Adds handling changes in:
    * <li> fRef from choices<br><br>
    */
   @Override
   public void update(VariableUpdateInfo info, Expression expression) {
      if (fLogging) {
         System.err.println(getName()+".update(VWC:"+expression+")");
      }
      
      super.update(info, expression);
      
      // Check if expression is from active choice and process if so
      ChoiceData choice = getCurrentChoice();

      if (choice != null) {
         // If change or choice expression changed
         if (info.doFullUpdate ||
             (choice.isDependentOn(expression) ||
             (info.properties&IModelChangeListener.PROPERTY_VALUE)!=0) ) {
            try {
               updateChoice(choice, info, expression);
               info.properties |= IModelChangeListener.PROPERTY_VALUE;
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
      }
   }

   @Override
   public
   boolean enableQuietly(boolean enabled) {
      boolean changed = super.enableQuietly(enabled);
      if (changed) {
         checkChoiceIndex();
      }
      return changed;
   }
   
   @Override
   public boolean enable(boolean enabled) {
      boolean changed = enableQuietly(enabled);
      if (changed) {
         checkChoiceIndex();
         notifyListeners();
      }
      return changed;
   }

   @Override
   public String getToolTip() {
      String tooltip = super.getToolTip();
      
      ChoiceData choiceData = getCurrentChoice();
      if (choiceData != null) {
         String addionalTooltip = choiceData.getToolTip();
         if (addionalTooltip != null) {
            addionalTooltip = addionalTooltip.replace("\n", "\n\t");
            if (tooltip == null) {
               tooltip = addionalTooltip;
            }
            else {
               tooltip = tooltip + "\n\n" + addionalTooltip;
            }
         }
      }
      return tooltip;
   }

   @Override
   public String getToolTipAsCode(String padding) {
      String tooltip = XML_BaseParser.escapeString(super.getToolTip());
      if (tooltip == null) {
         return "";
      }
      tooltip = tooltip.replace("\n", "\n"+padding);
      return tooltip;
   }
   
   /**
    * Set name of table to produce in C code
    * 
    * @param tableName
    */
   public void setTableName(String tableName) {
      fTableName = tableName;
   }

   /**
    * Get name of table to produce in C code
    * 
    * @param tableName (may be null)
    * @return
    */
   public String getTableName() {
      return fTableName;
   }
   
   @Override
   public String fieldExtractFromRegister(String registerValue) {
      String valueFormat = getValueFormat();
      String typeName = getTypeName();
      if (valueFormat != null) {
         String parts[] = valueFormat.split(",");
         StringBuilder sb = new StringBuilder();
         boolean needBracketForMask = false;
         for(String format:parts) {
            Pattern p = Pattern.compile("^([a-zA-Z0-9_]*)\\(?\\%s\\)?");
            Matcher m = p.matcher(format);
            if (!m.matches()) {
               return "Illegal use of formatParam - unexpected pattern '"+valueFormat+"'";
            }
            String macro = m.group(1);
            if (!sb.isEmpty()) {
               sb.append("|");
               needBracketForMask = true;
            }
            sb.append(macro+"_MASK");
         }
         String mask = sb.toString();
         if (needBracketForMask) {
            mask = "("+mask+")";
         }
         registerValue = String.format("%s&%s", registerValue, mask);
         if (typeName == null) {
            registerValue = "("+registerValue+")";
         }
      }
      if (typeName != null) {
         registerValue = String.format("%s(%s)", typeName, registerValue);
      }
      return registerValue;
   }

   /**
    * Get definition value for selection choice
    * 
    * @param index Index of selected choice
    * 
    * @return Formatted string suitable for enum definition
    */
   public String getDefinitionValue(ChoiceData choice) {
      
      String[] valueFormats = getValueFormat().split(",");
      String[] vals         = choice.getValue().split(",");
      if (valueFormats.length != vals.length) {
         return ("valueFormat '"+getValueFormat()+"' does not match value '"+choice.getValue()+"'" );
      }
      StringBuilder sb = new StringBuilder();
      for(int valIndex=0; valIndex<valueFormats.length; valIndex++) {
         if (valIndex>0) {
            sb.append('|');
         }
         sb.append(String.format(valueFormats[valIndex], vals[valIndex]));
      }
      return sb.toString();
   }

   @Override
   public String getDefinitionValue() {
      
      int index = getChoiceIndexByName(getValueAsString());
      ChoiceData choice = getChoiceData()[index];
      return getDefinitionValue(choice);
   }
   
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
   
}
