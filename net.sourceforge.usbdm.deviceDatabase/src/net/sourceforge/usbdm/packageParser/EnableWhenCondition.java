package net.sourceforge.usbdm.packageParser;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.swt.widgets.Button;

public class EnableWhenCondition {

   public enum Type {
      preclusion, requirement,
      or, and, not, emptyTrue };

   private Type                           fType;
   private ArrayList<EnableWhenCondition> fOperands     = null;
   private String                         fVariableName = null;
   
   public static EnableWhenCondition      trueCondition = new EnableWhenCondition();

   public EnableWhenCondition() {
      fType = Type.emptyTrue;
   }

   public EnableWhenCondition(EnableWhenCondition.Type operator, ArrayList<EnableWhenCondition> operands) 
         throws Exception {
      fType     = operator;
      fOperands = operands;
      if (operator == Type.not) {
         if (operands.size() > 1) {
            throw new Exception("ApplyWhenCondition(operator, operands) - Too many operands for NOT operation");
         }
      }
      else if ((operator != Type.and) && (operator != Type.or)) {
         throw new Exception("ApplyWhenCondition(operator, operands) - Must be NOT, OR or AND operator");
      }
   }

   public EnableWhenCondition(EnableWhenCondition.Type operator, String variableName) {
      fType         = operator;
      fVariableName = variableName;
   }
   
   public boolean enabled(HashMap<String, Button> fButtonMap) throws Exception {
      Button button = null;
      switch (fType) {
      case and:
         // Short-circuit evaluation
         for (EnableWhenCondition operand:fOperands) {
            if (!operand.enabled(fButtonMap)) {
               return false;
            };
         }
         return true;
      case or:
         // Short-circuit evaluation
         for (EnableWhenCondition operand:fOperands) {
            if (operand.enabled(fButtonMap)) {
               return true;
            }
         }
         return false;
      case not:
         return !fOperands.get(0).enabled(fButtonMap);
      case preclusion:
         button = fButtonMap.get(fVariableName);
         return (button == null) || !button.isEnabled() || !button.getSelection();
      case requirement:
         button = fButtonMap.get(fVariableName);
         return (button != null) && button.isEnabled() && button.getSelection();
      case emptyTrue:
         return true;
      }
      throw new Exception("Invalid condition : " + fType);
   }

}
