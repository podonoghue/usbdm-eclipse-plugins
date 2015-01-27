package net.sourceforge.usbdm.deviceDatabase;

import java.util.ArrayList;


public class Block {
   private ApplyWhenCondition applyWhen;

   public Block() {
      this.applyWhen = null;
   }
   
   /**
    * @return the applyWhen
    */
   public ApplyWhenCondition getApplyWhen() {
      return applyWhen;
   }

   /**
    * @param applyWhen the applyWhen to set
    * @throws Exception 
    */
   public void setApplyWhen(ApplyWhenCondition applyWhen) throws Exception {
      if (this.applyWhen != null) {
         throw new Exception("Condition already has ApplyWhen clause");
      }
      this.applyWhen = applyWhen;
   }

   public ArrayList<ProjectVariable> getVariables() {
      if (applyWhen == null) {
         return null;
      }
      return applyWhen.getVariables();
   }
}