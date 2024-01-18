package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

public abstract class IndexedValidator extends PeripheralValidator {

   protected final int fDimension;
   protected int       fClockIndex=0;

   protected IndexedValidator(PeripheralWithState peripheral, int dimension) {
      super(peripheral);
      fDimension = dimension;
   }

   @Override
   final protected void validate(Variable variable, int properties) throws Exception {
      super.validate(variable, properties);
      int varIndex = -1;
      if (variable != null) {
         varIndex = variable.getIndex();
      }
      if ((variable!=null) && variable.isLogging()) {
         System.err.println("validate("+variable+")");
      }
      if (varIndex < 0) {
         for (int clockIndex=0; clockIndex<fDimension; clockIndex++) {
            setClockIndex(clockIndex);
            validate(variable, properties, clockIndex);
         }
      }
      else {
         setClockIndex(varIndex);
         validate(variable, properties, varIndex);
      }
   }
   
   protected abstract void validate(Variable variable, int properties, int index) throws Exception;
   
   /**
    * Get Variable from associated peripheral. <br>
    * Tries to obtain an indexed variable or failing that an unindexed one.
    * 
    * @param key  Key to lookup variable
    * 
    * @return Variable or null if not found
    */
   @Override
   protected Variable safeGetVariable(String key) {
      
      // Make absolute relative to peripheral
      key = getPeripheral().makeKey(key);
      
      if (key.endsWith("[]")) {
         // Make specific to current clock index
         key = key.replace("[]", "["+fClockIndex+"]");
      }
      else {
         // XXX Remove eventually
         if (getPeripheral().safeGetVariable(key+"[1]") != null) {
            throw new RuntimeException("Use of indexed var '"+key+"' without index");
         }
      }
      return getPeripheral().safeGetVariable(key);
   }
   
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

   protected int getClockIndex() {
      return fClockIndex;
   }

   protected void setClockIndex(int fIndex) {
      this.fClockIndex = fIndex;
   }

}
