package net.sourceforge.usbdm.deviceEditor.information;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class VariableWithChoices extends Variable {

   /** List of choices to be re-created each time */
   boolean fDynamicChoices = false;
   
   /** List of choices */
   private String[] fChoices = null;

   public VariableWithChoices(String name, String key) {
      super(name, key);
   }
   
   /**
    * @return the choices
    */
   public abstract ChoiceData[] getChoiceData();

   /**
    * COnvert an enum value into a complete enum for code use
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
   
   void clearChoices() {
      fChoices = null;
   }
   
   /**
    * @return the choices
    * @throws Exception
    */
   public String[] getChoices() {
      
      if (fDynamicChoices || (fChoices == null)) {
//         // XX Delete me
//         if (getName().contains("mcg_c1_frdiv[1]")) {
//            System.err.println("getChoices() - regenerating");
//         }

         // Construct new list
         ArrayList<String> choices = new ArrayList<String>();
         
         ChoiceData[] choiceData = getChoiceData();
         if (choiceData == null) {
            return null;
         }
         for (int index=0; index<choiceData.length; index++) {
            fDynamicChoices = fDynamicChoices || choiceData[index].isDynamic();
            if (choiceData[index].isHidden()) {
               continue;
            }
            if (!choiceData[index].isEnabled(getProvider())) {
               continue;
            }
            choices.add(choiceData[index].getDynamicName(getProvider()));
         }
         fChoices = choices.toArray(new String[choices.size()]);
//         // XX delete me
//         if (getName().contains("mcg_c1_frdiv[1]")) {
//            System.err.println("getChoices() - choices = " + choices);
//         }
      }
      return fChoices;
   }

   /**
    * Set value by name
    * 
    * @param name Name of choice to select
    * 
    * @return True => value changed
    */
   public boolean setValueByName(String name) {
      
      String[] choiceData = getChoices();
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
    * Get index of current value in choice entries
    * 
    * @return index or -1 if not found
    */
   protected int getIndex(String name) {
      ChoiceData[] data = getChoiceData();
      if (data == null) {
         return -1;
      }
      for (int index=0; index<data.length; index++) {
         if (data[index].getName().equalsIgnoreCase(name)) {
            return index;
         }
      }
      if (fDynamicChoices) {
         String choices[] = getChoices();
         for (int index=0; index<choices.length; index++) {
            if (choices[index].equalsIgnoreCase(name)) {
               return index;
            }
         }
      }
      
      return -1;
   }
   
   public ChoiceData getSelectedItemData() {
      int index = getIndex(getValueAsString());
      if (index<0) {
         index = (int)getValueAsLong();
      }
      return getChoiceData()[index];
   }
   
   final Pattern fFieldPattern = Pattern.compile("^(\\w+)(\\[(\\d+)?\\])?$");

   @Override
   public String getField(String field) {
      Matcher m = fFieldPattern.matcher(field);
      if (!m.matches()) {
         return "Field "+field+" not matched";
      }

      if (m.group(2) == null) {
         // No index - get field from variable
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
         index = getIndex(getValueAsString());
         if (index<0) {
            return "No current choice value to retrieve data "+getValueAsString()+" not found for field ="+field;
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
         return getFormattedValue();
      } else if ("name".equals(fieldName)) {
         return fData.getName();
      } else if ("value".equals(fieldName)) {
         return fData.getValue();
      }
      return "Field "+field+" not matched in choice";
   }

   public String getEnumValue() {
      // Use index of current selected item
      int index = getIndex(getValueAsString());
      if (index<0) {
         return "No current value for" + getName();
      }
      ChoiceData fData = getChoiceData()[index];
      return makeEnum(fData.getEnumName());
   }

   @Override
   public String getValueFormat() {
      String format = super.getValueFormat();
      if (format != null) {
         return format;
      }
      return  getBaseNameFromKey(getKey()).toUpperCase()+"(%s)";
   }
}
