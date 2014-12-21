package net.sourceforge.usbdm.deviceDatabase;


public class Condition {
   private final ProjectVariable  variable;
   private final String  value;
   private final boolean negated;
   
   public Condition(ProjectVariable variable, String value, boolean negated) {
      this.variable     = variable;
      this.value        = value;
      this.negated      = negated;
   }
   public ProjectVariable getVariable() {
      return variable;
   }
   public String getValue() {
      return value;
   }
   public boolean isNegated() {
      return negated;
   }
}