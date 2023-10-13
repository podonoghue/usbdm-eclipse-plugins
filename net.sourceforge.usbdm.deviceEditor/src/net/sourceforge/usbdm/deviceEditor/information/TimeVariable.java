package net.sourceforge.usbdm.deviceEditor.information;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.EngineeringNotation;
import net.sourceforge.usbdm.deviceEditor.model.TimeVariableModel;
import net.sourceforge.usbdm.deviceEditor.model.VariableModel;
import net.sourceforge.usbdm.deviceEditor.parsers.Expression;

public class TimeVariable extends LongVariable {

   Expression fPeriodExpression = null;
   
   public TimeVariable(String name, String key) {
      super(name, key);
   }

   public TimeVariable(String name, String key, Object value) {
      super(name, key, value);
   }
   
   @Override
   public void addInternalListeners() throws Exception {
      if (fPeriodExpression != null) {
         fPeriodExpression.addListener(this);
      }
      super.addInternalListeners();
   }

   @Override
   public boolean update(Expression expression) throws Exception {
      
      boolean changed = super.update(expression);
      
      changed = changed || (expression == fPeriodExpression);
      
      return changed;
   }
   
   @Override
   public String formatValueAsString(long value) {
      double frequency;
      double period;
      StringBuilder sb = new StringBuilder();

      switch(getUnits()) {
      case Hz:
         // Primary value is frequency
         frequency = value;
         period = 1/frequency;
         sb.append(DoubleVariable.formatValueAsString(frequency, Units.Hz) + ", (");
         sb.append(DoubleVariable.formatValueAsString(period, Units.s) + ")");
         break;
      case s:
         // Primary value is period
         period = value;
         frequency = 1/period;
         sb.append(DoubleVariable.formatValueAsString(period, Units.s) + ", (");
         sb.append(DoubleVariable.formatValueAsString(frequency, Units.Hz) +")");
         break;
      case ticks:
         // Primary value is in ticks and needs to be converted to seconds
         try {
            period = fPeriodExpression.getValueAsDouble();
         } catch (Exception e) {
            return "Period calculation failed" + e.getMessage();
         }
         frequency = 1/period;
         sb.append(DoubleVariable.formatValueAsString(value, Units.ticks) + ", (");
         sb.append(DoubleVariable.formatValueAsString(frequency, Units.Hz) + ", ");
         sb.append(DoubleVariable.formatValueAsString(period, Units.s) + ")");
         break;
      case None:
         break;
      }
      return sb.toString();
   }

   @Override
   protected VariableModel privateCreateModel(BaseModel parent) {
      return new TimeVariableModel(parent, this);
   }

   @Override
   public Object getEditValueAsString() {
      return super.formatValueAsString(getValueAsLong());
   }

   @Override
   public boolean isLocked() {
      return true;
   }

   @Override
   public boolean isDerived() {
      return true;
   }

   /**
    * Set expression for calculating period from tick value
    * 
    * @param expression Expression to use
    * 
    * @throws Exception
    */
   public void setPeriodExpression(String expression) throws Exception {
      if ((expression == null)||expression.isBlank()) {
         fPeriodExpression = null;
         return;
      }
      fPeriodExpression = new Expression(expression, getProvider());
   }
   
   /**
    * Get expression for calculating period from tick value
    * 
    * @return Expression to use or null if none
    */
   public Expression getPeriodExpression() throws Exception {
      return fPeriodExpression;
   }

   /**
    * Convert object to suitable type for this variable
    * 
    * @param value
    * 
    * @return Converted object
    */
   private Long translate(Object value) {
      
      if (value instanceof Double) {
         Double res = (Double) value;
         return res.longValue();
      }
      if (value instanceof Long) {
         return (Long) value;
      }
      if (value instanceof Integer) {
         Integer res = (Integer) value;
         return res.longValue();
      }
      if (value instanceof String) {
         Double numericValue = EngineeringNotation.parse((String) value);
         if (numericValue != null) {
            Units fromUnits = EngineeringNotation.parseUnits((String) value);
            Units toUnits = getUnits();
            if ((fromUnits == toUnits)||(fromUnits == Units.None)) {
               // No translation needed
               return Math.round(numericValue);
            }
            Double translatedValue = null;
            switch(toUnits) {
            case Hz:
               if (fromUnits == Units.s) {
                  translatedValue = 1/numericValue;
               }
               break;
            case s:
               if (fromUnits == Units.Hz) {
                  translatedValue = 1/numericValue;
               }
               break;
            case ticks:
               // Wrong units and no translation
               break;
            case None:
               // No translation
               translatedValue = numericValue;
               break;
            }
            if (translatedValue != null) {
               return Math.round(translatedValue);
            }
         }
      }
      System.err.println("Object '"+ value + "' (" + value.getClass()+") is not compatible with DoubleVariable or "+getUnits());
      return null;
   }
   
   @Override
   public boolean setValueQuietly(Object value) {
      return setValueQuietly(translate(value));
   }
   

}
