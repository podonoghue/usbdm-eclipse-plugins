package net.sourceforge.usbdm.packageParser;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class SubstitutionMap implements ISubstitutionMap {
   
   HashMap<String, String> fMap = new HashMap<String, String>();
   
   public SubstitutionMap(Map<String, String> baseMap) {
      fMap = new HashMap<String, String>(baseMap);
   }

   public SubstitutionMap() {
      fMap = new HashMap<String, String>();
   }

   @Override
   public boolean isEmpty() {
      return fMap.isEmpty();
   }
   
   @Override
   public String getSubstitutionValue(String key) {
      String s =  fMap.get(key);
//      if (s == null) {
//         System.err.println("Symbol not found '"+key+"'");
//      }
      return s;
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
