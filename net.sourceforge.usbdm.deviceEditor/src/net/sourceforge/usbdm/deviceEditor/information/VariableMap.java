package net.sourceforge.usbdm.deviceEditor.information;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import net.sourceforge.usbdm.deviceEditor.parsers.ExtendedVariableSubstitutionMap;
import net.sourceforge.usbdm.packageParser.ISubstitutionMap;

public class VariableMap {

   HashMap<String, Variable> fMap = new HashMap<String, Variable>();
   
   public boolean isEmpty() {
      return fMap.isEmpty();
   }

   public void clear() {
      fMap.clear();
   }

   /**
    * Removes a variable<br>
    * It is not an error to remove a non-existent variable
    * 
    * @param key Key of variable to remove
    * 
    * @return Removed variable (if existing)
    */
   public Variable remove(String key) {
      return fMap.remove(key);
   }

   /**
    * Get variable with key
    * 
    * @param key
    * 
    * @return Variable found or null
    */
   public Variable safeGet(String key) {
      return fMap.get(key);
   }

   /**
    * Get variable with key
    * 
    * @param key
    * 
    * @return Variable found or null
    */
   public Variable get(String key) {
      Variable var = fMap.get(key);
      if (var == null) {
         System.err.println("Error - Variable '" + key + "' not found");
      }
      return var;
   }

   /**
    * Get variable value as a string suitable for user display
    * 
    * @return String for display or null if variable not found
    */
   public String getValueAsString(String key) {
      String field = null;
      int hashIndex = key.indexOf('.');
      if ((hashIndex>=0)&&(hashIndex<key.length()-1)) {
         // '.' not at end of name 
         field = key.substring(hashIndex+1);
         key   = key.substring(0, hashIndex);
      }
      if (key.endsWith(".")) {
         key = key.substring(0, key.length()-1)+"[0]";
      }
      if (key.endsWith("[]")) {
         key = key.substring(0, key.length()-2)+"[0]";
      }
      Variable var = fMap.get(key);
      if (var == null) {
         return null;
      }
      if (field != null) {
         return var.getField(field);
      }
      return var.getValueAsString();
   }

   /**
    * Get the variable value as a string for use in substitutions
    * 
    * @return String for text substitutions (in C code) or null if variable not found
    */
   public String getSubstitutionValue(String key) {
      String field = null;
      int hashIndex = key.indexOf('.');
      if ((hashIndex>=0)&&(hashIndex<key.length()-1)) {
         // '.' not at end of name 
         field = key.substring(hashIndex+1);
         key   = key.substring(0, hashIndex);
      }
      if (key.endsWith(".")) {
         key = key.substring(0, key.length()-1)+"[0]";
      }
      if (key.endsWith("[]")) {
         key = key.substring(0, key.length()-2)+"[0]";
      }
      Variable var = fMap.get(key);
      if (var == null) {
         return null;
      }
      if (field != null) {
         return var.getField(field);
      }
      return var.getSubstitutionValue();
   }

   /**
    * Add or replace a variable
    * 
    * @param key  Key to use
    * @param var  Variable to add
    * 
    * @return  Replaced variable or null if not already present.
    */
   public Variable put(String key, Variable var) {
      return fMap.put(key, var);
   }

   public Set<String> keySet() {
      return fMap.keySet();
   }

   public Set<Map.Entry<String, Variable>> entrySet() {
      return fMap.entrySet();
   }

   public void putAll(Map<String, Variable> map) {
      fMap.putAll(map);
   }

   /**
    * Adds or updates a Variable<br>
    * If there is already a variable with the same key then it is preserved and its value modified,
    *  otherwise a new StringVariable is created.
    *  
    * @param name    Name for a new variable if created.
    * @param key     Key for variable
    * @param value   Value for variable
    * 
    * @return The existing or newly added variable
    */
   public Variable addOrUpdateVariable(String name, String key, String value, boolean isDerived) {
      Variable var = fMap.get(key);
      if (var == null) {
         var = new StringVariable(name, key);
         var.setDerived(isDerived);
         fMap.put(key, var);
      }
      var.setValue(value);
      return var;
   }
   
   /**
    * Adds or updates a Variable<br>
    * 
    * If there is already a variable with the same key then it is preserved and its value modified,
    *  otherwise a new StringVariable is created.<br>
    * The name for a new variable is created from the key.
    *  
    * @param key     Key for variable. Also used to create name.
    * @param value   Value for variable
    * 
    * @return The existing or newly added variable
    */
   public Variable addOrUpdateVariable(String key, String value, boolean isDerived) {
      return addOrUpdateVariable(null, key, value, isDerived);
   }

   /**
    * Performs the given action for each entry in this map until all entries 
    * have been processed or the action throws an exception.
    * 
    * @param action
    */
   public void forEach(BiConsumer<String, Variable> action) {
      fMap.forEach(action);
   }

   /**
    * Get simple map of variable->substitution values<br>
    * The map is backed by the VariableMap but allows addition of 
    * substitution values without affecting the VariableMap. 
    * 
    * @return Map
    */
   public ISubstitutionMap getSubstitutionMap() {
      return new ExtendedVariableSubstitutionMap(this);
   }
}
