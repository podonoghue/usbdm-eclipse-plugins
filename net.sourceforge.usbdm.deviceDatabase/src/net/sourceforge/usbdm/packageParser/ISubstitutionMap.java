package net.sourceforge.usbdm.packageParser;

import java.util.Collection;
import java.util.function.BiConsumer;

public interface ISubstitutionMap {

   /**
    * Get display value for key
    * 
    * @param key Key to use
    * 
    * @return DIsplay value found or null if not present
    */
   public String getDisplayValue(String key);
   
   /**
    * Get substitution value for key
    * 
    * @param key Key to use
    * 
    * @return Substitution value found or null if not present
    */
   public String getSubstitutionValue(String key);
   
   /**
    * Checks if there are no mapping values available
    * 
    * @return True if non available
    */
   public boolean isEmpty();
   
   /**
    * Add substitution value
    * 
    * @param key     Key for value
    * @param value   Substitution value
    */
   public void addValue(String key, String value);

   /**
    * Apply the action to each element
    * 
    * @param action
    */
   public void forEach(BiConsumer<String, String> action);
   
    /**
    * Returns a Set view of the keys contained in this map. The set <em><b>may be</b></em> backed by ISimpleSubstitution.
    * 
    * @return
    */
   public Collection<String> keySet();

   /**
    * Replaces macros e.g. $(key:defaultValue) with values from this map
    * <li>Default values are used if present and key not found, otherwise
    * <li>Unknown macros without a default generate an error
    * <li>Escape sequences e.g. '\n' are left unexpanded
    * 
    * @param inputText     String to replace macros in
    * @param keyMaker      Interface providing a method to create a key from a variable name
    * 
    * @return      String with substitutions (or original if map is empty)
    */
   public String substitute(String inputText, IKeyMaker keyMaker); 
   
   /**
    * Replaces macros e.g. $(key:defaultValue) with values from a map
    * <li>Default values are used if present and key not found, otherwise
    * <li>Unknown macros without a default generate an error
    * <li>Escape sequences e.g. '\n' are expanded
    * <li>NULL keymaker is used
    * 
    * @param input         String to replace macros in
    * 
    * @return      String with substitutions (or original if map is empty)
    */
   public String substituteFinal(String inputText);
   
   /**
    * Replaces macros e.g. $(key:defaultValue) with values from a map
    * <li>Default values are used if present and key not found, otherwise
    * <li>Unknown macros without a default generate an error
    * <li>Escape sequences e.g. '\n' are left unexpanded
    * <li>NULL keyMaker is used
    * 
    * @param input         String to replace macros in
    * @param symbolMap   Map of key->value pairs for substitution
    * 
    * @return      String with substitutions (or original if map is empty)
    */
   public String substitute(String inputText);
   
   /**
    * Replaces macros e.g. $(key:defaultValue) with values from a map
    * <li>Default values are used if present and key not found, otherwise
    * <li>Unknown macros are retained
    * <li>Escape sequences e.g. '\n' are left unexpanded
    * <li>NULL keyMaker is used
    * 
    * @param inputText    String to replace macros in
    * 
    * @return      String with substitutions (or original if map is empty)
    */
   public String substituteIgnoreUnknowns(String inputText);

   /**
    * Add add elements of newData
    * 
    * @param newData Data to add
    */
   public void addAll(ISubstitutionMap newData);

}
