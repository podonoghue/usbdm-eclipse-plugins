package net.sourceforge.usbdm.deviceEditor.xmlParser;

import java.util.Map;

import net.sourceforge.usbdm.deviceEditor.information.FileUtility.IKeyMaker;

public class ReplacementParser {

   /** Map of symbols for substitution */
   final Map<String, String> fSymbols;
   /** Key-maker to translate a symbol before lookup */
   final IKeyMaker           fKeyMaker;
   
   /**
    * Create replacement parser
    * 
    * @param symbols    Map of symbols for substitution
    * @param keyMaker   Key-maker to translate a symbol before lookup in symbols
    */
   public ReplacementParser(Map<String, String> symbols, IKeyMaker keyMaker) {
      fSymbols  = symbols;
      fKeyMaker = keyMaker;
   }

   /**
    * 
    * @param inputText  Text to do substitution within
    * @param index      How far progressed through the inputText
    * @param sb         Buffer to assemble output
    * 
    * @return  Current position in inputText
    * 
    * @throws Exception
    */
   private int parse(String inputText, int index, StringBuilder sb) {
      StringBuilder pattern = new StringBuilder(20);
      boolean inPattern = true;
      boolean escape    = false;
      while(index<inputText.length()) {
         char c = inputText.charAt(index);
         if (escape) {
            escape = false;
            if (c == '(') {
               index++;
               index = parse(inputText, index, pattern);
            }
            else {
               index++;
               pattern.append('$');
               pattern.append(c);
            }
         }
         else if (c == '$') {
            escape = true;
            index++;
         }
         else if (c == ')') {
            index++;
            inPattern = false;
            break;
         }
         else {
            index++;
            pattern.append(c);
         }
      }
      if (escape) {
         sb.append('$');
      }
      if (inPattern) {
         sb.append("Unterminated replacement pattern");
      }
      String result = pattern.toString();

      String key          = null;
      String defaultValue = null;
      String modifier     = null;

      String replaceWith = null;
      String[] parts = result.split("\\s*:\\s*");
      if (parts.length>0) {
         key = parts[0];
      }
      if (parts.length>1) {
         defaultValue = parts[1];
      }
      if (parts.length>2) {
         modifier = parts[2];
      }
      key = fKeyMaker.makeKey(key);
      replaceWith = fSymbols.get(key);
      if (replaceWith == null) {
         replaceWith = defaultValue;
      }
      if (modifier != null) {
         if (modifier.equalsIgnoreCase("toupper")) {
            replaceWith = replaceWith.toUpperCase();
         }
         else if (modifier.equalsIgnoreCase("tolower")) {
            replaceWith = replaceWith.toLowerCase();
         }
         else if (modifier.equalsIgnoreCase("defer")) {
            // Don't expand unknown symbol (yet)
            replaceWith = "$(" + key + ")";
         }
         else {
            // force error expansion for unknown modifier
            replaceWith = null; 
         }
      }
      if (replaceWith == null) {
         replaceWith = 
               "---Symbol not found or format incorrect for substitution '"+pattern+
               "' => key='" + key +
               "', def='" + defaultValue + 
               "', mod='" + modifier;
      }
      result = replaceWith;
      sb.append(result);
      return index;
   }

   public String parse(String inputText) {
      StringBuilder sb     = new StringBuilder(100);
      int           index  = 0;
      boolean       escape = false;
      while (index < inputText.length()) {
         char c = inputText.charAt(index);
         if (escape) {
            escape = false;
            if (c == '(') {
               index++;
               index = parse(inputText, index, sb);
            }
            else {
               index++;
               sb.append('$');
               sb.append(c);
            }
         }
         else if (c == '$') {
            index++;
            escape = true;
         }
         else {
            index++;
            sb.append(c);
         }
      };
      if (escape) {
         sb.append("Unterminated escape character");
      }
      return sb.toString();
   }
}
