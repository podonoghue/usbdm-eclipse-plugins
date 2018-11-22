package net.sourceforge.usbdm.cdt.utilties;

import java.util.HashMap;
import java.util.Map;

import net.sourceforge.usbdm.cdt.utilties.ReplacementParser.IKeyMaker;

/**
 * Simple replacement parser to do substitutions in strings
 * 
 * Matches pattern $(key:defaultValue:modifiers)
 * 
 * Modifiers:
 *   toupper   Convert replacementText to upper-case
 *   tolower   Convert replacementText to lower-case
 *   defer     Defer replacement once i.e. replace whole pattern with "$(key:defaultValue)" 
 *             to allow later attempt at substitution
 */
public class ReplacementParser {

   public interface IKeyMaker {
      /**
       * Generate variable key from name
       * 
       * @param  name Name used to create key
       * @return Key generated from name
       */
      public String makeKey(String name);
   }
   
   /** Map of symbols for substitution */
   final Map<String, String> fSymbols;
   
   /** Key-maker to translate a symbol before lookup */
   final IKeyMaker           fKeyMaker;
   
   /**
    * Key maker for unadorned symbols
    */
   private static final IKeyMaker publicKeyMaker = new IKeyMaker() {

      @Override
      public String makeKey(String name) {
         return name;
      }
   };
   
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
    * Create replacement parser
    * 
    * @param symbols    Map of symbols for substitution
    */
   public ReplacementParser(Map<String, String> symbols) {
      fSymbols  = symbols;
      fKeyMaker = publicKeyMaker;
   }

   enum KeyState {KEY, DOLLAR};

   /**
    * Parses Key field of replacement pattern e.g.
    * <pre>
    *   $(keeeey:default:modifier)     
    *     ^....^
    * </pre>
    * Key may contain a replacement pattern!
    * 
    * @param inputText String containing entire input String 
    * @param index     Position of current character (just past open parenthesis)
    * @param sb        Builder to accumulate key in
    * 
    * @return  Updated position (at colon or closing parenthesis)
    * 
    * @throws Exception 
    */
   private int parseKey(String inputText, int index, StringBuilder sb) throws Exception {
      
      KeyState state    = KeyState.KEY;
      boolean  complete = false;
      while((index<inputText.length()) && (!complete)) {
         char c = inputText.charAt(index);
         switch(state) {
            case KEY:
               if (c == '$') {
                  index++;
                  state = KeyState.DOLLAR;
               }
               else if ((c == ':') ||  (c == ')')) {
                  complete = true;
               }
               else {
                  index++;
                  sb.append(c);
               }
               break;
            case DOLLAR:
               if (c == '(') {
                  index++;
                  index = parseSubstitution(inputText, index, sb);
               }
               else {
                  index++;
                  sb.append('$');
                  sb.append(c);
               }
               state = KeyState.KEY;
               break;
            default:
               break;
         }
      }
      if (state == KeyState.DOLLAR) {
         sb.append('$');
         state = KeyState.KEY;
      }
      if (state != KeyState.KEY) {
         throw new Exception("Error in replacement key = '" + inputText + "'");
      }
      return index;
   }
   
   enum DefaultState {CODE, STRING, CHAR, ESCAPE, DOLLAR};

   /**
    * Parses Default field of replacement pattern e.g.
    * <pre>
    *   $(key:default:modifier)     
    *         ^.....^
    * </pre>
    * Default may contain a replacement pattern!
    * This is designed to process C code so it also watches levels of parenthesis and string and character constants and 
    * does not terminate the replacement pattern prematurely.
    * 
    * @param inputText     String containing entire input String 
    * @param index         Position of current character (just past second colon)
    * @param defaultValue  Builder to accumulate default value in
    * 
    * @return  Updated position (at colon or closing parenthesis)
    * 
    * @throws Exception 
    */
   private int parseDefault(String inputText, int index, StringBuilder defaultValue) throws Exception {
      boolean  complete = false;
      DefaultState state     = DefaultState.CODE;
      DefaultState pushState = DefaultState.CODE;
      int bracketLevel = 0;
      while((index<inputText.length()) && (!complete)) {
         char c = inputText.charAt(index);
         switch (state) {
            case CODE:
               pushState = state;
               if ((bracketLevel == 0) && ((c==':') || (c==')'))) {
                  complete = true;
               }
               else {
                  index++;
                  if (c == '$') {
                     state = DefaultState.DOLLAR;
                  } 
                  else {
                     defaultValue.append(c);
                     if(c=='\\') {
                        state     = DefaultState.ESCAPE;
                     }
                     else if(c=='\'') {
                        state = DefaultState.CHAR;
                     }
                     else if(c=='\"') {
                        state = DefaultState.STRING;
                     }
                     else if(c=='(') {
                        bracketLevel++;
                     }
                     else if(c==')') {
                        bracketLevel--;
                     }
                  }
               }
               break;
            case DOLLAR:
               if (c == '(') {
                  index++;
                  index = parseSubstitution(inputText, index, defaultValue);
               }
               else {
                  index++;
                  defaultValue.append('$');
                  defaultValue.append(c);
               }
               state = pushState;
               break;
            case CHAR:
               index++;
               pushState = state;
               if (c == '$') {
                  state = DefaultState.DOLLAR;
               } 
               else {
                  defaultValue.append(c);
                  if(c=='\\') {
                     state = DefaultState.ESCAPE;
                  }
                  else if(c=='\'') {
                     state = DefaultState.CODE;
                  }
               }
               break;
            case STRING:
               index++;
               pushState = state;
               if (c == '$') {
                  state = DefaultState.DOLLAR;
               } 
               else {
                  defaultValue.append(c);
                  if(c=='\\') {
                     state = DefaultState.ESCAPE;
                  }
                  else if(c=='\"') {
                     state = DefaultState.CODE;
                  }
               }
               break;
            case ESCAPE:
               defaultValue.append(c);
               index++;
               state = pushState;
               break;
            default:
               break;
         }
      }
      if (!complete) {
         throw new Exception("Missing ':' or ')' in '" + inputText + "'");
      }
      return index;
   }

   /**
    * Parses modifier field of replacement pattern e.g.
    * <pre>
    *   $(key:default:modifier)     
    *                 ^......^
    * </pre>
    * @param inputText String containing entire input String 
    * @param index     Position of current character (just past second colon)
    * @param modifier  Builder to accumulate modifier in
    * 
    * @return  Updated position (at closing parenthesis)
    * 
    * @throws Exception 
    */
   private int parseModifier(String inputText, int index, StringBuilder modifier) throws Exception {
      boolean  complete = false;
      while((index<inputText.length()) && (!complete)) {
         char c = inputText.charAt(index);
         if (c==')') {
            complete = true;
         }
         else {
            index++;
            modifier.append(c);
         }  
      }
      if (!complete) {
         throw new Exception("Missing ')' in '" + inputText + "'");
      }
      return index;
   }

/**
    * Parses replacement pattern e.g. 
    * <pre>
    *   $(key:default:modifier)     
    *     ^...................^
    * </pre>
    * 
    * @param inputText String containing entire input String 
    * @param index     Position of current character (just past open parenthesis)
    * @param sb        Builder to accumulate key in
    * 
    * @return  Updated position (just past closing parenthesis)
    * 
    * @throws Exception 
    */
   private int parseSubstitution(String inputText, int index, StringBuilder sb) throws Exception {
      
      String key = null;
      {
      StringBuilder keyBuffer = new StringBuilder(20);
      index = parseKey(inputText, index, keyBuffer);
      key = keyBuffer.toString();
      }
      
      String defaultValue = "Symbol '" + key + "' not found";
      char c = inputText.charAt(index);
      if (c == ':') {
         index++;
         StringBuilder defaultValueBuffer = new StringBuilder(20);
         index = parseDefault(inputText, index, defaultValueBuffer);
         if (defaultValueBuffer.length() != 0) {
            defaultValue = defaultValueBuffer.toString().trim();
         }
         c = inputText.charAt(index);
      }
      
      String modifier = null;
      if (c == ':') {
         index++;
         StringBuilder modifierBuffer = new StringBuilder(20);
         index = parseModifier(inputText, index, modifierBuffer);
         if (modifierBuffer.length() != 0) {
            modifier = modifierBuffer.toString().trim();
         }
      }
      c = inputText.charAt(index);
      if (c != ')') {
         throw new Exception("Missing ')' in '" + inputText + "'");
      }
      index++;
      
      String replaceWith = null;
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
            if (defaultValue != null) {
               replaceWith = "$(" + key + ":" + defaultValue + ")";
            }
            else {
               replaceWith = "$(" + key + ")";
            }
         }
         else {
            // force error expansion for unknown modifier
            replaceWith = null; 
         }
      }
      if (replaceWith == null) {
         replaceWith = 
               "---Symbol not found or format incorrect for substitution '"+inputText+
               "' => key='" + key +
               "', def='" + defaultValue + 
               "', mod='" + modifier + "'";
      }
      sb.append(replaceWith);
      return index;
   }

   /**
    * Replaces all macros in text
    * 
    * @param inputText  Text to process
    * 
    * @return Replaced text or original if unchanged
    * @throws Exception 
    */
   public String replaceAll(String inputText) throws Exception {
      StringBuilder sb          = new StringBuilder(100);
      int           index       = 0;
      boolean       foundDollar = false;
      boolean       isChanged   = false;
      
      while (index < inputText.length()) {
         char c = inputText.charAt(index);
         if (foundDollar) {
            foundDollar = false;
            if (c == '(') {
               index++;
               isChanged = true;
               index     = parseSubstitution(inputText, index, sb);
            }
            else {
               index++;
               sb.append('$');
               sb.append(c);
            }
         }
         else if (c == '$') {
            index++;
            foundDollar = true;
         }
         else {
            index++;
            sb.append(c);
         }
      };
      if (!isChanged) {
         return inputText;
      }
      if (foundDollar) {
         sb.append('$');
      }
      return sb.toString();
   }
   
  /**
    * Replaces macros e.g. $(name:defaultValue) with values from a map or default if not found
    * 
    * @param inputText    String to replace macros in
    * @param map          Map of key->value pairs for substitution
    * @param keyMaker     Interface providing a method to create a key from a variable name
    * 
    * @return      String with substitutions (or original if none)
    */
   public static String substitute(String inputText, Map<String, String> map, IKeyMaker keyMaker) {
      if (inputText == null) {
         return null;
      }
      if (map == null) {
         return inputText;
      }
      ReplacementParser replacementParser = new ReplacementParser(map, keyMaker);
      try {
         return replacementParser.replaceAll(inputText);
      } catch (Exception e) {
         e.printStackTrace();
         return e.getMessage();
      }
   }

   /**
    * Replaces macros e.g. $(key:defaultValue) with values from a map or default if not found
    * 
    * @param input        String to replace macros in
    * @param variableMap  Map of key->value pairs for substitution
    * 
    * @return      String with substitutions (or original if none)
    */
   public static String substitute(String input, Map<String,String> variableMap) {
      return substitute(input, variableMap, publicKeyMaker);
   }
   final static HashMap<String, String> exampleSymbols = new HashMap<String, String>();
// final static String                  TestPattern    = "hello th$ere $(aaa::toupper) $(b$(ccc)bb) $(dd:234)";
// final static String                  TestPattern    = "hello th$ere $(aaa::toupper)";
// final static String  TestPattern    = "hello th$ere $(aaa::toupper) $(b$(ccc)bb::tolower) $(dd:234) bye$bye";
 final static String  TestPattern    = "<start> $(aba:func(int x)  { x = \"$(bbb)\"})  <end>";

 static IKeyMaker keyMaker = new IKeyMaker() {

    @Override
    public String makeKey(String name) {
       return name;
    }
 };
 
 public static void main(String[] args) throws Exception {
    exampleSymbols.put("aaa",    "a-a");
    exampleSymbols.put("bbb",    "b-b");
    exampleSymbols.put("ccc",    "ccc");
    exampleSymbols.put("dd",     "Dd");
    exampleSymbols.put("bcccbb", "bcccBB");
    
    ReplacementParser parser = new ReplacementParser(exampleSymbols, keyMaker);
    
    System.err.println("'" + TestPattern + "' => '" + parser.replaceAll(TestPattern) + "'");
 }


}
