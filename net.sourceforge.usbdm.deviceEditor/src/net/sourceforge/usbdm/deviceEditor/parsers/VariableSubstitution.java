package net.sourceforge.usbdm.deviceEditor.parsers;

import java.util.Collection;
import java.util.HashMap;
import java.util.function.BiConsumer;

import javax.management.RuntimeErrorException;

import net.sourceforge.usbdm.deviceEditor.information.StringVariable;
import net.sourceforge.usbdm.packageParser.IKeyMaker;
import net.sourceforge.usbdm.packageParser.ISubstitutionMap;
import net.sourceforge.usbdm.packageParser.ReplacementParser;

public class VariableSubstitution implements ISubstitutionMap {
   
   HashMap<String, StringVariable> fMap = new HashMap<String, StringVariable>();
   
   @Override
   public boolean isEmpty() {
      return fMap.isEmpty();
   }
   
   @Override
   public String getSubstitutionValue(String key) {
      StringVariable var = fMap.get(key);
      if (var == null) {
         return null;
      }
      return var.getSubstitutionValue();
      }
   
   @Override
   public String getDisplayValue(String key) {
      StringVariable var = fMap.get(key);
      if (var == null) {
         return null;
      }
      return var.getValueAsString();
      }
   
   public void addValue(String name, String key, String value, boolean isDerived) {
      StringVariable var = fMap.get(key);
      if (var != null) {
         throw new RuntimeErrorException(null, "Variable already exists");
      }
      var = new StringVariable(name, key);
      var.setValue(value);
      var.setDerived(isDerived);
      fMap.put(key, var);
   }

   @Override
   public void addValue(String key, String value) {
      addValue(null, key, value, false);
   }

   @Override
   public void forEach(BiConsumer<String, String> action) {
      fMap.forEach(new BiConsumer<String, StringVariable>() {

         @Override
         public void accept(String key, StringVariable value) {
            action.accept(key, value.getValueAsString());
         }
      });
   }

   @Override
   public Collection<String> keySet() {
      return fMap.keySet();
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
            addValue(key, value);
         }
      });
   }
}
