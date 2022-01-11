package net.sourceforge.usbdm.packageParser;

import java.util.Collection;
import java.util.HashMap;
import java.util.function.BiConsumer;

public class SubstitutionMap implements ISubstitutionMap {
   
   HashMap<String, String> fMap = new HashMap<String, String>();
   
   @Override
   public boolean isEmpty() {
      return fMap.isEmpty();
   }
   
   @Override
   public String getSubstitutionValue(String key) {
      return fMap.get(key);
      }
   
   @Override
   public String getDisplayValue(String key) {
      return getSubstitutionValue(key);
      }

   @Override
   public void addValue(String key, String value) {
      fMap.put(key, value);
   }

   @Override
   public void forEach(BiConsumer<String, String> action) {
      fMap.forEach(action);
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
}
