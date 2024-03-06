package net.sourceforge.usbdm.deviceEditor.information;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo.InitPhase;
import net.sourceforge.usbdm.deviceEditor.model.IModelChangeListener;
import net.sourceforge.usbdm.deviceEditor.parsers.Expression;
import net.sourceforge.usbdm.deviceEditor.parsers.Expression.VariableUpdateInfo;
import net.sourceforge.usbdm.deviceEditor.parsers.XML_BaseParser;

public abstract class VariableWithChoices extends Variable {

   /** List of choices are to be re-created each time */
   boolean fDynamicChoices = false;
   
   /** List of choice names */
   private String[] fChoices = null;

   /** Name of table to produce in C code */
   private String fTableName;
   
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
   
   /**
    * Get number of choices available
    * 
    * @return
    */
   public int getChoiceCount() {
      return getVisibleChoiceData().size();
   }
   
   void clearCachedChoiceInformation() {
      fChoices = null;
   }
   
   /**
    * 
    */
   public void updateChoicesAvailable() {
      String[] choices = getVisibleChoiceNames();
      if (getValueAsLong()>=choices.length) {
         setValue(0);
      }
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

      // TODO - memorise?

      ChoiceData[] choiceData = getChoiceData();
      if (choiceData == null) {
         return null;
      }

      ArrayList<ChoiceData> choices = new ArrayList<ChoiceData>();
      for (int index=0; index<choiceData.length; index++) {
         if (fDeviceInfo.getInitialisationPhase().isLaterThan(InitPhase.VariablePropagationSuspended) &&
               !choiceData[index].isEnabled(getProvider())) {
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
   public abstract void setIndex(int index);
   
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
    * Get default choice entry
    * 
    * @return choice or null if none
    */
   protected abstract ChoiceData getdefaultChoice();
   
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
      for (ChoiceData choiceData:data) {
         try {
            choiceData.addListener(this);
         } catch (Exception e) {
            System.err.println("Failed to add internal listener to choice variable '" + this.getName() + "'");
            System.err.println("Choice '" + choiceData.getName() + "', ref= '" + choiceData.getReference() + "'");
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
      if (fDeviceInfo.getInitialisationPhase().isEarlierThan(InitPhase.VariablePropagationAllowed)) {
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
      updateChoicesAvailable();
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
         updateChoicesAvailable();
      }
      return changed;
   }
   
   @Override
   public boolean enable(boolean enabled) {
      boolean changed = enableQuietly(enabled);
      if (changed) {
         updateChoicesAvailable();
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

}
