package net.sourceforge.usbdm.deviceEditor.information;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
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
   public void expressionChanged(Expression expression) {
      super.expressionChanged(expression);
      if (expression == fPeriodExpression) {
         notifyListeners(null);
      }
   }
   
   /**
    * Converts the given string into a form appropriate for model
    * 
    * @param value Value to format
    * 
    * @return String in appropriate form e.g. 24.56MHz
    */
   @Override
   public String formatValueAsString(long value) {
      try {
         double frequency = 1;
         double period    = 1;
         StringBuilder sb = new StringBuilder();
         if (fPeriodExpression != null) {
            // Primary value is in ticks ands needs to be converted to seconds
            period = fPeriodExpression.getValueAsDouble();
            frequency = 1/period;
            sb.append(DoubleVariable.formatValueAsString(value, Units.ticks) + ", ");
         }
         else {
            switch(getUnits()) {
            case Hz:
               // Primary value is frequency
               frequency = value;
               period = 1/frequency;
               break;
            case s:
               // Primary value is period
               period = value;
               frequency = 1/period;
               break;
            case ticks:
            case None:
               throw new Exception("ticks used withoud period equation");
            }
         }
         sb.append(DoubleVariable.formatValueAsString(frequency, Units.Hz) + ", ");
         sb.append(DoubleVariable.formatValueAsString(period, Units.s));
         return sb.toString();
         
      } catch (Exception e) {
         e.printStackTrace();
         return e.getMessage();
      }
   }

   @Override
   protected VariableModel privateCreateModel(BaseModel parent) {
      return new TimeVariableModel(parent, this);
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
   
}
