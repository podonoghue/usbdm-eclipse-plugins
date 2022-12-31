package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

public abstract class IndexedValidator extends PeripheralValidator {

   protected final int fDimension;

   protected IndexedValidator(PeripheralWithState peripheral, int dimension) {
      super(peripheral);
      fDimension = dimension;
   }

   @Override
   final protected void validate(Variable variable) throws Exception {
      super.validate(variable);
      int varIndex = -1;
      if (variable != null) {
         varIndex = variable.getIndex();
      }
      if (varIndex < 0) {
         for (int clockIndex=0; clockIndex<fDimension; clockIndex++) {
            setClockIndex(clockIndex);
            validate(variable, clockIndex);
         }
      }
      else {
         setClockIndex(varIndex);
         validate(variable, varIndex);
      }
   }
   
   protected abstract void validate(Variable variable, int index) throws Exception;
   
   /**
    * Add to watched variables
    * 
    * @param variablesToWatch Variables to add
    */
   @Override
   protected void addToWatchedVariables(String[] variablesToWatch) {
      if (variablesToWatch == null) {
         return;
      }
      for(int fIndex=0; fIndex<Math.max(1,fDimension); fIndex++) {
         setClockIndex(fIndex);
         super.addToWatchedVariables(variablesToWatch);
      }
      setClockIndex(0);
   }

   /**
    * Add to watched variables
    * 
    * @param externalVariables Variables to add
    */
   @Override
   protected void addToWatchedVariables(ArrayList<String> variablesToWatch) {
      if (variablesToWatch == null) {
         return;
      }
      for(int fIndex=0; fIndex<Math.max(1,fDimension); fIndex++) {
         setClockIndex(fIndex);
         super.addToWatchedVariables(variablesToWatch);
      }
      setClockIndex(0);
   }

   /**
    * Add this validator as a listener on each variable in list
    * 
    * @param variablesToWatch
    */
   @Override
   protected void addSpecificWatchedVariables(String[] variablesToWatch) {
      if (variablesToWatch == null) {
         return;
      }
      for(int fIndex=0; fIndex<Math.max(1,fDimension); fIndex++) {
         setClockIndex(fIndex);
         super.addSpecificWatchedVariables(variablesToWatch);
      }
   }
   
   /**
    * Add this validator as a listener on each variable in list
    * 
    * @param variablesToWatch
    */
   @Override
   protected void addSpecificWatchedVariables(ArrayList<String> variablesToWatch) {
      if (variablesToWatch == null) {
         return;
      }
      for(int fIndex=0; fIndex<Math.max(1,fDimension); fIndex++) {
         setClockIndex(fIndex);
         super.addSpecificWatchedVariables(variablesToWatch);
      }
   }

}
