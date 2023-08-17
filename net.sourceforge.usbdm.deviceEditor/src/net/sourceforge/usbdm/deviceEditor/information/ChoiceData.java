package net.sourceforge.usbdm.deviceEditor.information;

import net.sourceforge.usbdm.deviceEditor.parsers.Expression;
import net.sourceforge.usbdm.deviceEditor.parsers.IExpressionChangeListener;
import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;

/**
 * Class to hold the data for choices
 */
public class ChoiceData {
   
   /** Name used by GUI/model */
   private String     fName = null;
   
   /** Used if name is dynamic i.e. calculated */
   private final Expression fNameExpression;
   
   /** Value used by substitution */
   private final String fValue;

   /** Suffix for code enum generation e.g. yyy => ENUM_yyy */
   private final String fEnumName;
   
   /** Code fragment for this enum e.g. getPeripheralClock() */
   private final String fCodeValue;
   
   /** Reference to expression associated with this choice and forwarded to owning choiceOption target */
   private final Expression fReference;
   
   /** A set of constant values associated with this choice and forwarded to owning choiceOption target */
   private final String fMultiValue;
   
   /** Reference to expression enabling this choice */
   private final Expression fEnabledBy;
   
   /**
    * 
    * @param name       Name used by GUI/model
    * @param value      Value used by data
    * @param enumName   Suffix for code enum generation e.g. yyy => ENUM_yyy
    * @param codeValue  Code fragment for this enum e.g. getPeripheralClock()
    * @param reference  Reference to another variable associated with this choice or multi-value constants
    * 
    * @throws Exception
    */
   public ChoiceData(
         String name,
         String value,
         String enumName,
         String codeValue,
         String reference,
         String enabledBy,
         VariableProvider provider) throws Exception {
      
      if (name.startsWith("@")) {
         // Calculated name
         fNameExpression = new Expression(name.substring(1), provider);
         fName           = null;
      }
      else {
         // Simple name
         fNameExpression = null;
         fName           = name;
      }
      fValue       = value;
      if ("*".equals(enumName)) {
         enumName = name;
      }
      fEnumName    = enumName;
      fCodeValue   = codeValue;
      if (reference != null) {
         if (reference.contains(";")) {
            // multi-choice
            fReference  = null;
            fMultiValue = reference;
         }
         else {
            // Dynamic expression
            fReference  = new Expression(reference, provider);
            fMultiValue = null;
         }
      }
      else {
         fReference = null;
         fMultiValue = null;
      }
      if (enabledBy != null) {
         fEnabledBy = new Expression(enabledBy, provider);
      }
      else {
         fEnabledBy = null;
      }
   }
   
   /**
    * Get multi-value references of form<br>
    *   constant...;constant
    * 
    * @return Value (may be null)
    */
   String getMultiValueReference() {
      return fMultiValue;
   }
   
   /**
    * 
    * @param name       Name used by GUI/model
    * @param value      Value used by data
    * @param enumName   Suffix for code enum generation e.g. yyy => ENUM_yyy
    * @param codeValue  Code fragment for this enum e.g. getPeripheralClock()
    * @param reference  Reference to another variable associated with this choice e.g. clock source selection
    */
   public ChoiceData(String name, String value, String enumName) {
      fName           = name;
      fNameExpression = null;
      fValue          = value;
      if ("*".equals(enumName)) {
         enumName = name;
      }
      fEnumName       = enumName;
      fCodeValue      = null;
      fReference      = null;
      fEnabledBy      = null;
      fMultiValue     = null;
   }
   
   /**
    * 
    * @param name       Name used by GUI/model
    * @param value      Value used by data
    * @param enumName   Suffix for code enum generation e.g. yyy => ENUM_yyy
    * @param codeValue  Code fragment for this enum e.g. getPeripheralClock()
    * @param reference  Reference to another variable associated with this choice e.g. clock source selection
    */
   public ChoiceData(String name, String value) {
      fName           = name;
      fNameExpression = null;
      fValue          = value;
      fEnumName       = null;
      fCodeValue      = null;
      fReference      = null;
      fEnabledBy      = null;
      fMultiValue     = null;
   }
   
   /**
    * Get name of this choice
    * 
    * @return data value or null if none
    */
   public String getName() {
      if (fName == null) {
         try {
            return fNameExpression.getValueAsString();
         } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
         }
      }
      return fName;
   }
   
   /**
    * Get value associated with this choice
    * 
    * @return value
    */
   public String getValue() {
      return fValue;
   }
   
   /**
    * Get value reference value associated with this choice
    * 
    * @return value of reference
    * @throws Exception
    */
   public Object getReferenceValue() throws Exception {
      if (fReference == null) {
         return null;
      }
      return fReference.getValue();
   }
   
   /**
    * Get suffix for code enum generation e.g. yyy => ENUM_yyy
    * 
    * @return suffix
    */
   public String getEnumName() {
      return fEnumName;
   }

   /**
    * Get code fragment for this enum e.g. getPeripheralClock()
    * 
    * @return code fragment
    */
   public String getCodeValue() {
      return fCodeValue;
   }

   /**
    * Get reference to another variable associated with this choice e.g. clock source selected
    * 
    * @return Variable name or null if no reference
    */
   public Expression getReference() {
      return fReference;
   }

   @Override
   public String toString() {
      return "ChoiceData("+fName+", "+fValue+", "+fEnumName+", "+fCodeValue+", "+fReference+")";
   }
   
   public boolean isEnabled(VariableProvider varProvider) {
      if (fEnabledBy == null) {
         return true;
      }
      try {
         return fEnabledBy.getValueAsBoolean();
      } catch (Exception e) {
         e.printStackTrace();
      }
      return true;
   }

   /**
    * Indicates if choice needs to be recreated on each use as dependent on external information
    * 
    * @return
    */
   public boolean isDynamic() {
      return (fEnabledBy != null) || (fNameExpression != null) || (fReference != null);
   }
   
   /**
    * Add listener to choice expressions:
    *    enableBy
    *    nameExpression
    *    reference
    * 
    * @param listener
    */
   public void addListener(IExpressionChangeListener listener) {
      if (fEnabledBy != null) {
         fEnabledBy.addListener(listener);
      }
      if (fNameExpression != null) {
         fNameExpression.addListener(listener);
         try {
            fNameExpression.getValue();
         } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
         }
      }
      if (fReference != null) {
         fReference.addListener(listener);
      }
   }
   
   public Expression getEnabledBy() {
      return fEnabledBy;
   }

}