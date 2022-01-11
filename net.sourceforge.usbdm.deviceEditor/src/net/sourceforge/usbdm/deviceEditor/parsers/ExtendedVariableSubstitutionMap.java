package net.sourceforge.usbdm.deviceEditor.parsers;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.information.VariableMap;
import net.sourceforge.usbdm.packageParser.IKeyMaker;
import net.sourceforge.usbdm.packageParser.ISubstitutionMap;
import net.sourceforge.usbdm.packageParser.ReplacementParser;

/**
 * Wraps a VariableMap and allows addition of simple substitution values
 */
public class ExtendedVariableSubstitutionMap implements ISubstitutionMap {

   VariableMap          fVariableMap;
   Map <String, String> fExtraSymbols = null;
   
   /**
    * Constructor<br>
    * Wraps a VariableMap while allowing addition of extra substitution values 
    * without affecting the underlying VariableMap
    * 
    * @param variableMap Underlying variable map
    */
   public ExtendedVariableSubstitutionMap(VariableMap variableMap) {
      fVariableMap = variableMap;
   }
   
   @Override
   public String getDisplayValue(String key) {
      String value = null;
      if (fExtraSymbols != null) {
         value = fExtraSymbols.get(key);
         if (value != null) {
            if (value.startsWith("Don't")) {
               System.err.print("Found " + value);
            }
            return value;
         }
      }
      return fVariableMap.getValueAsString(key);
   }

   @Override
   public String getSubstitutionValue(String key) {
      String value = null;
      if (fExtraSymbols != null) {
         value = fExtraSymbols.get(key);
         if (value != null) {
            if (value.startsWith("Don't")) {
               System.err.print("Found " + value);
            }
            return value;
         }
      }
      return fVariableMap.getSubstitutionValue(key);
   }

   @Override
   public void addValue(String key, String value) {
      if (fExtraSymbols == null) {
         fExtraSymbols = new HashMap<String, String>();
      }
      fExtraSymbols.put(key, value);
   }

   @Override
   public boolean isEmpty() {
      return fExtraSymbols.isEmpty() && fVariableMap.isEmpty();
   }

   @Override
   public void forEach(BiConsumer<String, String> action) {
      
      fVariableMap.forEach(new BiConsumer<String, Variable>() {
         @Override
         public void accept(String key, Variable value) {
            action.accept(key, value.getValueAsString());
         }
      });
      
      if (fExtraSymbols != null) {
         fExtraSymbols.forEach(action);
      }
   }

   @Override
   public Collection<String> keySet() {
      Set<String> keySet = new HashSet<String>(fExtraSymbols.keySet());
      keySet.addAll(fVariableMap.keySet());
      return keySet;
   }
   
   @Override
   public String substitute(String inputText, IKeyMaker keyMaker) {
      return ReplacementParser.substitute(inputText, this, keyMaker);
   }

   @Override
   public String substituteFinal(String inputText) {
      return ReplacementParser.substituteFinal(inputText, this);
   }

   @Override
   public String substitute(String inputText) {
      return ReplacementParser.substitute(inputText, this);
   }

   @Override
   public String substituteIgnoreUnknowns(String inputText) {
      return ReplacementParser.substituteIgnoreUnknowns(inputText, this);
   }

   @Override
   public void addAll(ISubstitutionMap newData) {
      newData.forEach(new BiConsumer<String, String>() {
         @Override
         public void accept(String key, String value) {
            fExtraSymbols.put(key, value);
         }
      });
   }
}
