package net.sourceforge.usbdm.deviceEditor.information;

import net.sourceforge.usbdm.deviceEditor.parsers.Expression;
import net.sourceforge.usbdm.deviceEditor.parsers.IExpressionChangeListener;
import net.sourceforge.usbdm.deviceEditor.parsers.XML_BaseParser;
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

   /** Pin mapping associated with this choice */
   private String fPinMap;

   /** Tool-tip to use with choice */
   private String fTooltip;

   /** Hardware associated with this choice */
   private String fAssociatedHardware;
   
   /**
    * 
    * @param name       Name used by GUI/model
    * @param value      Value used by data
    * @param enumName   Suffix for code enum generation e.g. yyy => ENUM_yyy
    * @param codeValue  Code fragment for this enum e.g. getPeripheralClock()
    * @param reference  Reference to another variable associated with this choice or multi-value constants
    * @param enabledBy  Expression enabling this choice
    * @param pinMap     PinMap associated with this choice
    * @param provider   Provider (for expressions)
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
         String pinMap,
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
      if ((enumName != null) && !enumName.isBlank()) {
         if ("*".equals(enumName)) {
            enumName = name;
         }
         enumName = enumName.trim();
         if (!XML_BaseParser.isValidCIdentifier("X_"+enumName)) {
            throw new Exception("Enum name must be simple name, name="+enumName);
         }
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
      if ((pinMap != null) && (!pinMap.isBlank())) {
         // Ignore empty pin maps
         fPinMap = pinMap;
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
      if (fName != null) {
         return fName;
      }
      try {
         String name = fNameExpression.getValueAsString();
//         System.err.println("Dynamic name = '" + name + "'");
         return name;
      } catch (Exception e) {
         e.printStackTrace();
         return "Error in evaluation";
      }
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
    *    <li>enableBy
    *    <li>nameExpression
    *    <li>reference
    * 
    * @param listener
    * @throws Exception
    */
   public void addListener(IExpressionChangeListener listener) throws Exception {
      if (fEnabledBy != null) {
         fEnabledBy.addListener(listener);
      }
      if (fNameExpression != null) {
         fNameExpression.addListener(listener);
      }
      if (fReference != null) {
         fReference.addListener(listener);
      }
   }
   
   /**
    * Get enabledBy expression associated with this choice
    * 
    * @return Expression or null if none
    */
   public Expression getEnabledBy() {
      return fEnabledBy;
   }

   /**
    * Get pin mapping associated with this choice
    * 
    * @return Pin mapping or null if none
    */
   public String getPinMap() {
      return fPinMap;
   }

   /**
    * Set tool-tip
    * 
    * @param toolTip Tool-tip to use with choice
    */
   public void setToolTip(String toolTip) {
      fTooltip = toolTip;
   }
   
   /**
    * Get tool-tip
    * 
    * @return Tool-tip to use with choice
    */
   public String getToolTip() {
      return fTooltip;
   }

   /**
    * Checks if choice is dependent on the given expression
    * 
    * @param expression
    * @return
    */
   public boolean isDependentOn(Expression expression) {
      if ((fNameExpression == expression) || (fReference == expression) || (fEnabledBy == expression)) {
         return true;
      }
      return false;
   }

   /**
    * Set name of hardware associated with this choice (either signal or peripheral)
    * 
    * @param associatedHardware Name of hardware to associate
    */
   public void setAssociatedHardware(String associatedHardware) {
      fAssociatedHardware = associatedHardware;
   }

   /**
    * Get name of hardware associated with this choice
    * 
    * @return Name or null if none
    */
   public String getAssociatedHardware() {
      return fAssociatedHardware;
   }

}