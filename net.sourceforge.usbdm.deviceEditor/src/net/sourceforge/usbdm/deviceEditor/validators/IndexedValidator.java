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
      int index = -1;
      if (variable != null) {
         index = variable.getIndex();
      }
      if (index == -1) {
         for (fIndex=0; fIndex<fDimension; fIndex++) {
            validate(variable, fIndex);
         }
      }
      else {
         fIndex = index;
         validate(variable, fIndex);
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
      for(fIndex=0; fIndex<Math.max(1,fDimension); fIndex++) {
         super.addToWatchedVariables(variablesToWatch);
      }
      fIndex = 0;
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
      for(fIndex=0; fIndex<Math.max(1,fDimension); fIndex++) {
         super.addToWatchedVariables(variablesToWatch);
      }
      fIndex = 0;
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
      for(fIndex=0; fIndex<Math.max(1,fDimension); fIndex++) {
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
      for(fIndex=0; fIndex<Math.max(1,fDimension); fIndex++) {
         super.addSpecificWatchedVariables(variablesToWatch);
      }
   }
   

}
