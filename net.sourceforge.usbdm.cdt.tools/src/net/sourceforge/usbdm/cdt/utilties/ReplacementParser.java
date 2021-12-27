package net.sourceforge.usbdm.cdt.utilties;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple replacement parser to do substitutions in strings
 * 
 * Matches pattern $(key:defaultValue:modifiers)
 * 
 * Modifiers:
 *   toupper   Convert replacementText to upper-case
 *   tolower   Convert replacementText to lower-case
 */
public class ReplacementParser {

   private class Pair<K, V> {

      public final K first;
      public final V second;
    
      public Pair(K first, V second){
          this.first = first;
          this.second = second;
      }
  };
  
   public interface IKeyMaker {
      /**
       * Generate variable key from name
       * 
       * @param  name Name used to create key
       * 
       * @return Key generated from name
       */
      public String makeKey(String name);
   }

   /** Map of symbols for substitution */
   private final Map<String, String> fSymbols;
   
   /** Key-maker to translate a symbol before lookup */
   private final IKeyMaker           fKeyMaker;
   
   /** True to ignore (preserve) unknown symbols */
   private boolean fIgnoreUnknowns = false;

   /** Prefix to use for symbol mode */
   private String fPrefix = "";
   
   private boolean fExpandEscapes = false;

   public void setExpandEscapes(boolean expand) {
      fExpandEscapes = expand;
   }
   
   /**
    * Key maker that just returns the unmodified key
    */
   private static final IKeyMaker NullKeyMaker = new IKeyMaker() {

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
   private ReplacementParser(Map<String, String> symbols, IKeyMaker keyMaker) {
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
      fKeyMaker = NullKeyMaker;
   }

   enum KeyState {KEY, DOLLAR, ESCAPE};

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
      Character pendingChar = null;

      while((index<inputText.length()) && (!complete)) {
         char c = inputText.charAt(index);
         pendingChar = null;
         switch(state) {
            case KEY:
               if (c == '$') {
                  index++;
                  state = KeyState.DOLLAR;
                  pendingChar = c;
               }
               else if (c == '\\') {
                  index++;
                  state = KeyState.ESCAPE;
                  pendingChar = c;
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
               
            case ESCAPE:
               index++;
               if (!fExpandEscapes) {
                  sb.append('\\');
               }
               sb.append(c);
               state = KeyState.KEY;
               
            default:
               break;
         }
      }
      if (pendingChar != null) {
         sb.append(pendingChar);
      }
      if (state != KeyState.KEY) {
         throw new Exception("Error in replacement key = '" + inputText + "'");
      }
      return index;
   }
   
   enum ReplaceState {CODE, STRING, CHAR, ESCAPE, DOLLAR, QUOTED_ESCAPE, SLASH, LINE_COMMENT, BLOCK_COMMENT, BLOCK_COM_STAR};

   /**
    * Parses Default field of replacement pattern e.g.
    * <pre>
    *   $(key:default:modifier)     
    *         ^.....^
    * </pre>
    * Default may contain a replacement pattern!
    * This is designed to process C code so it also watches levels of parenthesis, strings
    * and character constants, and does not terminate the replacement pattern prematurely.
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
      ReplaceState state     = ReplaceState.CODE;
      ReplaceState pushState = ReplaceState.CODE;
      int bracketLevel = 0;
      Character pendingChar = null;
      while((index<inputText.length()) && (!complete)) {
         char c = inputText.charAt(index);
         pendingChar = null;
         switch (state) {
            case CODE:
               if ((bracketLevel == 0) && ((c==':') || (c==')'))) {
                  complete = true;
               }
               else {
                  pushState = state;
                  index++;
                  if (c == '$') {
                     state = ReplaceState.DOLLAR;
                     pendingChar = c;
                  }
                  else if (c=='\\') {
                     state = ReplaceState.ESCAPE;
                     pendingChar = c;
                  }
                  else {
                     defaultValue.append(c);
                     if(c=='\'') {
                        state = ReplaceState.CHAR;
                     }
                     else if(c=='\"') {
                        state = ReplaceState.STRING;
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
                  state = ReplaceState.DOLLAR;
                  pendingChar = c;
               } 
               else {
                  defaultValue.append(c);
                  if(c=='\\') {
                     state = ReplaceState.QUOTED_ESCAPE;
                  }
                  else if(c=='\'') {
                     state = ReplaceState.CODE;
                  }
               }
               break;
               
            case STRING:
               index++;
               pushState = state;
               if (c == '$') {
                  state = ReplaceState.DOLLAR;
                  pendingChar = c;
               } 
               else {
                  defaultValue.append(c);
                  if(c=='\\') {
                     state = ReplaceState.QUOTED_ESCAPE;
                  }
                  else if(c=='\"') {
                     state = ReplaceState.CODE;
                  }
               }
               break;
               
            case QUOTED_ESCAPE:
               // Quotes a single char unchanged e.g. inside string or char
               defaultValue.append(c);
               index++;
               state = pushState;
               break;
               
            case ESCAPE:
               // Quotes a single char with simple substitutions for \n, \r, \t
               if (fExpandEscapes) {
                  if (c == 'n') {
                     c = '\n';
                  }
                  else if (c == 'r') {
                     c = '\r';
                  } 
                  else if (c == 't') {
                     c = '\t';
                  }     
                  else if (c == '\n') {
                     // Keep escape char as C line continuation
                     defaultValue.append('\\');
                  }     
               }
               else {
                  // Keep escape char
                  defaultValue.append('\\');
               }
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
      if (pendingChar != null) {
         defaultValue.append(pendingChar);
      }
      return index;
   }

   /**
    * Make key from bare name<br>
    * If there is no symbol map set it just adds the prefix from setSymbolPrefix().
    * 
    * @param key Bare key
    * 
    * @return Key with added path or prefix etc.
    */
   String makeKey(String key) {
      String  replaceWith = null;
      if (fSymbols == null) {
         replaceWith = fPrefix + "_" + key;
      }
      else {
         replaceWith = fSymbols.get(fKeyMaker.makeKey(key));
      }
      return replaceWith;
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

      boolean conditional = (inputText.charAt(index) == '?');

      if (conditional) {
         index++;
      }
      String key = null;
      {
         StringBuilder keyBuffer = new StringBuilder(20);
         index = parseKey(inputText, index, keyBuffer);
         key = keyBuffer.toString();
      }

      String arg1 = null;
      char c = inputText.charAt(index);
      if (c == ':') {
         index++;
         StringBuilder defaultValueBuffer = new StringBuilder(20);
         index = parseDefault(inputText, index, defaultValueBuffer);
         if (defaultValueBuffer.length() != 0) {
            arg1 = defaultValueBuffer.toString().trim();
         }
         c = inputText.charAt(index);
      }

      String arg2 = null;
      if (c == ':') {
         index++;
         StringBuilder modifierBuffer = new StringBuilder(20);
         index = parseDefault(inputText, index, modifierBuffer);
         if (modifierBuffer.length() != 0) {
            arg2 = modifierBuffer.toString().trim();
         }
      }
      c = inputText.charAt(index);
      if (c != ')') {
         throw new Exception("Missing ')' in '" + inputText + "'");
      }
      index++;

      String  replaceWith = null;
      if (conditional) {
         replaceWith = fSymbols.get(fKeyMaker.makeKey(key));
         if ((replaceWith == null) && fIgnoreUnknowns) {
            // Don't expand unknown symbol (yet)
            replaceWith = "$(?" + key;
            if (arg1 != null) {
               replaceWith += ":" + arg1;
            }
            if (arg2 != null) {
               if (arg1 == null) {
                  replaceWith += ":";
               }
               replaceWith += ":" + arg2;
            }
            replaceWith += ")";
         }
         else {
            boolean isFalse = replaceWith.equals("false") || replaceWith.equals("0");
            if (isFalse) {
               replaceWith = arg2;
            }
            else {
               replaceWith = arg1;
            }
         }
      }
      else {
         if (fSymbols == null) {
            replaceWith = fPrefix + "_" + key;
         }
         else {
            replaceWith = fSymbols.get(fKeyMaker.makeKey(key));
            if (replaceWith != null) {
               replaceWith = replaceAll(replaceWith);
            }
         }
         if ((replaceWith == null) && fIgnoreUnknowns) {
            // Don't expand unknown symbol (yet)
            replaceWith = "$(" + key;
            if (arg1 != null) {
               replaceWith += ":" + arg1;
            }
            if (arg2 != null) {
               if (arg1 == null) {
                  replaceWith += ":";
               }
               replaceWith += ":" + arg2;
            }
            replaceWith += ")";
         }
         else {
            if (arg1 == null) {
               arg1 = "Symbol '" + key + "' not found";
            }
            if (replaceWith == null) {
               replaceWith = arg1;
            }
            if (arg2 != null) {
               if (arg2.equalsIgnoreCase("toupper")) {
                  replaceWith = replaceWith.toUpperCase();
               }
               else if (arg2.equalsIgnoreCase("tolower")) {
                  replaceWith = replaceWith.toLowerCase();
               }
               else {
                  // force error expansion for unknown modifier
                  replaceWith = null; 
               }
            }
         }
      }
      if (replaceWith == null) {
         replaceWith = 
               "---Symbol not found or format incorrect for substitution '"+inputText+
               "' => key='" + key +
               "', def='" + arg1 + 
               "', mod='" + arg2 + "'";
      }
      sb.append(replaceWith);
      return index;
   }

  Pair<ReplaceState, Character> replaceCodeNextState(char c) {
      
      Character pendingChar = null;
      ReplaceState state = ReplaceState.CODE;
      if (c == '$') {
         state = ReplaceState.DOLLAR;
         pendingChar = c;
      }
      else if (c=='\\') {
         state = ReplaceState.ESCAPE;
         pendingChar = c;
      }
      else {
         if(c=='\'') {
            state = ReplaceState.CHAR;
         }
         else if(c=='\"') {
            state = ReplaceState.STRING;
         }
         else if(c=='/') {
            state = ReplaceState.SLASH;
         }
      }
      return new Pair<ReplaceState, Character>(state, pendingChar);
}
   
   /**
    * Replaces all macros in text
    * 
    * @param inputText  Text to process
    * 
    * @return Replaced text or original if unchanged
    * @throws Exception 
    */
   private String replaceAll(String inputText) throws Exception {
      ReplaceState state     = ReplaceState.CODE;
      ReplaceState pushState = ReplaceState.CODE;
      int index = 0;
      StringBuilder sb = new StringBuilder();
      Character pendingChar = null;
      Pair<ReplaceState, Character> t = null;
      
      while((index<inputText.length())) {
         char c = inputText.charAt(index);
         pendingChar = null;
         switch (state) {
         case CODE:
            pushState = state;
            index++;
            t = replaceCodeNextState(c);
            state       = t.first;
            pendingChar = t.second;
            if (pendingChar == null) {
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
            state = pushState;
            break;
            
         case CHAR:
            index++;
            pushState = state;
            if (c == '$') {
               state = ReplaceState.DOLLAR;
               pendingChar = c;
            } 
            else {
               sb.append(c);
               if(c=='\\') {
                  state = ReplaceState.QUOTED_ESCAPE;
               }
               else if(c=='\'') {
                  state = ReplaceState.CODE;
               }
            }
            break;
            
         case STRING:
            index++;
            pushState = state;
            if (c == '$') {
               state = ReplaceState.DOLLAR;
               pendingChar = c;
            } 
            else {
               sb.append(c);
               if(c=='\\') {
                  state = ReplaceState.QUOTED_ESCAPE;
               }
               else if(c=='\"') {
                  state = ReplaceState.CODE;
               }
            }
            break;
            
         case QUOTED_ESCAPE:
            // Quotes a single char unchanged e.g. inside string or char
            sb.append(c);
            index++;
            state = pushState;
            break;
            
         case SLASH:
            // Possible start of comment
            index++;
            if (c == '/') {
               state = ReplaceState.LINE_COMMENT;
            }
            else if (c == '*') {
               state = ReplaceState.BLOCK_COMMENT;
            }
            else {
               t = replaceCodeNextState(c);
               state       = t.first;
               pendingChar = t.second;
            }
            if (pendingChar == null) {
               sb.append(c);
            }
            break;
            
         case LINE_COMMENT:
            // Process line comment
            pushState = state;
            if (c == '$') {
               state = ReplaceState.DOLLAR;
               pendingChar = c;
            } 
            else {
               sb.append(c);
               if (c == '\n') {
                  state = ReplaceState.CODE;
               }
            }
            index++;
            break;
            
         case BLOCK_COMMENT:
            // Process block comment
            pushState = state;
            if (c == '$') {
               pendingChar = c;
               state = ReplaceState.DOLLAR;
            } 
            else { 
               sb.append(c);
               if (c == '*') {
                  state = ReplaceState.BLOCK_COM_STAR;
               }
            }
            index++;
            break;
            
         case BLOCK_COM_STAR:
            // Ignores line comment
            if (c == '/') {
               state = ReplaceState.CODE;
            }
            else if (c != '*') {
               state = ReplaceState.BLOCK_COMMENT;
            }
            sb.append(c);
            index++;
            break;
            
         case ESCAPE:
            // Quotes a single char with simple substitutions for \n, \r, \t
            // Only outside of strings, characters and comments
            if (fExpandEscapes) {
               if (c == 'n') {
                  c = '\n';
               }
               else if (c == 'r') {
                  c = '\r';
               } 
               else if (c == 't') {
                  c = '\t';
               }     
               else if (c == '\n') {
                  // Keep escape char as C line continuation
                  sb.append('\\');
               }     
            }
            else {
               // Keep escape char
               sb.append('\\');
            }
            sb.append(c);
            index++;
            state = pushState;
            break;
            
         default:
            break;
         }
      }
      if (pendingChar != null) {
         sb.append(pendingChar);
      }
      return sb.toString();
   }
   
   /** 
    * Controls whether unknown symbols are treated as an error or preserved.
    * This also means default values will not be used.
    *  
    * @param ignoreUnknowns True to ignore (preserved) unknown symbols.
    */
   private void setIgnoreUnknowns(boolean ignoreUnknowns) {
      fIgnoreUnknowns = ignoreUnknowns;
   }
   
   private void setSymbolPrefix(String prefix) {
      fPrefix = prefix;
   }

  /**
    * Replaces macros e.g. $(name:defaultValue) with values from a map or default if not found
    * 
    * @param inputText      String to replace macros in
    * @param map            Map of key->value pairs for substitution
    * @param keyMaker       Interface providing a method to create a key from a variable name
    * @param ignoreUnknowns True to ignore (preserved) unknown symbols.
    * @param expandEscapes  Whether to expand escapes e.g. '\n' or preserve escape sequence
    * @param prefix
    * 
    * @return      String with substitutions (or original if none)
    */
   private static String substitute(
         String               inputText, 
         Map<String, String>  map, 
         IKeyMaker            keyMaker,
         boolean              ignoreUnknowns,
         boolean              expandEscapes,
         String               prefix) {
      
      if (inputText == null) {
         return null;
      }
      ReplacementParser replacementParser = new ReplacementParser(map, keyMaker);
      replacementParser.setIgnoreUnknowns(ignoreUnknowns);
//      replacementParser.setSymbolPrefix("");
      replacementParser.setSymbolPrefix(prefix);
      replacementParser.setExpandEscapes(expandEscapes);
      try {
         return replacementParser.replaceAll(inputText);
      } catch (Exception e) {
         e.printStackTrace();
         return e.getMessage();
      }
   }

   /**
    * Replaces macros e.g. $(key:defaultValue) with values from a map
    * <li>Default values are used if present and key not found, otherwise
    * <li>Unknown macros without a default generate an error
    * <li>Escape sequences e.g. '\n' are left unexpanded
    * 
    * @param inputText     String to replace macros in
    * @param variableMap   Map of key->value pairs for substitution
    * @param keyMaker      Interface providing a method to create a key from a variable name
    * 
    * @return      String with substitutions (or original if none)
    */
   public static String substitute(
         String               inputText, 
         Map<String, String>  variableMap, 
         IKeyMaker            keyMaker) {
      
      if (variableMap == null) {
         return inputText;
      }
      return substitute(inputText, variableMap, keyMaker, false, false, "");
   }
   
   /**
    * Replaces macros e.g. $(key:defaultValue) with values from a map
    * <li>Default values are used if present and key not found, otherwise
    * <li>Unknown macros without a default generate an error
    * <li>Escape sequences e.g. '\n' are expanded
    * <li>NULL keymaker is used
    * 
    * @param input         String to replace macros in
    * @param variableMap   Map of key->value pairs for substitution
    * 
    * @return      String with substitutions (or original if variableMap is empty)
    */
   public static String substituteFinal(String inputText, Map<String,String> variableMap) {
      if (variableMap == null) {
         return inputText;
      }
      return substitute(inputText, variableMap, NullKeyMaker, false, true, "");
   }
   
   /**
    * Replaces macros e.g. $(key:defaultValue) with values from a map
    * <li>Default values are used if present and key not found, otherwise
    * <li>Unknown macros without a default generate an error
    * <li>Escape sequences e.g. '\n' are left unexpanded
    * <li>NULL keymaker is used
    * 
    * @param input         String to replace macros in
    * @param variableMap   Map of key->value pairs for substitution
    * 
    * @return      String with substitutions (or original if variableMap is empty)
    */
   public static String substitute(String inputText, Map<String,String> variableMap) {
      if (variableMap == null) {
         return inputText;
      }
      return substitute(inputText, variableMap, NullKeyMaker, false, false, "");
   }
   
   /**
    * Replaces macros e.g. $(key:defaultValue) with values from a map
    * <li>Default values are used if present and key not found, otherwise
    * <li>Unknown macros are retained
    * <li>Escape sequences e.g. '\n' are left unexpanded
    * <li>NULL keymaker is used
    * 
    * @param inputText    String to replace macros in
    * @param variableMap  Map of key->value pairs for substitution
    * 
    * @return      String with substitutions (or original if variableMap is empty)
    */
   public static String substituteIgnoreUnknowns(String inputText, Map<String,String> variableMap) {
      if (variableMap == null) {
         return inputText;
      }
      return substitute(inputText, variableMap, NullKeyMaker, true, false, "");
   }

   /**
    * Replaces macros e.g. $(key:defaultValue) with values prefixed symbol (for use in C code)<br>
    * Used for device header files to refer to symbols related to the struct e.g. dimensions<br>
    * <li>No symbol map so no substitutions
    * <li>Escape sequences e.g. '\n' are left unexpanded
    * <li>Prefix is added to keys
    * 
    * @param inputText    String to replace macros in
    * @param prefix       Prefix to add to front of relative symbols
    * 
    * @return      String with substitutions
    */
   public static String addPrefixToKeys(String inputText, String prefix) {
      return substitute(inputText, null, NullKeyMaker, true, false, prefix);
   }

   
   
   
   
   /*********************************************************
    * TESTING
    **********************************************************/
   
   final static HashMap<String, String> exampleSymbols = new HashMap<String, String>();
// final static String                  TestPattern    = "hello th$ere $(aaa::toupper) $(b$(ccc)bb) $(dd:234)";
// final static String                  TestPattern    = "hello th$ere $(aaa::toupper)";
// final static String  TestPattern    = "hello th$ere $(aaa::toupper) $(b$(ccc)bb::tolower) $(dd:234) bye$bye";
//   final static String  TestPattern    = "<start> $(aba:func(int x)  { x = \"$(bbb)\"})  <end>";
   final static String  TestPattern    = "<start> /* hello\\nthere\\t * */ <end>";

 static IKeyMaker keyMaker = new IKeyMaker() {

    @Override
    public String makeKey(String name) {
       return name;
    }
 };
 
 public static void main(String[] args) throws Exception {
    Pattern px = Pattern.compile("^PIT(\\d+)");
    Matcher mx = px.matcher("PIT0");
    System.err.println("px='" + px + "', mx='"+mx.groupCount()+"' m=" + mx.matches());
//    
//    exampleSymbols.put("aaa",    "a-a");
//    exampleSymbols.put("bbb",    "b-b");
//    exampleSymbols.put("ccc",    "ccc");
//    exampleSymbols.put("dd",     "Dd");
//    exampleSymbols.put("bcccbb", "bcccBB");
//    
//    ReplacementParser parser = new ReplacementParser(exampleSymbols, keyMaker);
//    
//    System.err.println("'" + TestPattern + "' => '" + parser.replaceAll(TestPattern) + "'");
 }

}
