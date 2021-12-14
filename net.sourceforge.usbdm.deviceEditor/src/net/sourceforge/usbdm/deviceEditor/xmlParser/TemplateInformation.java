package net.sourceforge.usbdm.deviceEditor.xmlParser;

import java.util.HashMap;
import java.util.Map;
import net.sourceforge.usbdm.cdt.utilties.ReplacementParser;

/**
 * Used to represent a template for code in a project
 */
public class TemplateInformation {
   /** Key used to index template */
   private final String fKey;
   /** Namespace for template (info, usbdm, class) */
   private final String fNameSpace;
   /** Buffer to accumulate text contents for template */
   private StringBuilder fBuilder;
   /** Dimension for array template */
   private final int fDimension;
   /** Processed text contents for template - cached */
   private String fText = null;
   /** Iteration has been applied to this template */
   private boolean fIterationDone = false;
   /** Variable for iteration */
   private String fVariable = null;
   /** Enumeration for iteration */
   private String fEnumeration;

   /**
    * Construct template
    * 
    * @param key        Key used to index template
    * @param namespace  Namespace for template (info, usbdm, class)
    * @param dimension  Dimension for array template
    */
   public TemplateInformation(String key, String nameSpace, int dimension) {
      fKey        = key;
      fNameSpace  = nameSpace;
      fDimension  = dimension;
      fBuilder    = new StringBuilder(100); 
   }
   
   /**
    * Set enumeration for this template.
    *  
    * @param variable      Variable being enumerated
    * @param enumeration   Enumerate value for variable
    * 
    * @throws Exception 
    */
   public void setIteration(String variable, String enumeration) throws Exception {
      if (fIterationDone) {
         throw new Exception("Template already has iteration");
      }
      fIterationDone = true;
      fVariable    = variable.trim();
      fEnumeration = enumeration.trim();
   }
   
   enum State { Text, DiscardAfterNewline, Escape, InString, InCharacter };

   /**
    * Append text to template.
    * The text is processed \n => newline char etc.
    *  
    * @param contents Text to append
    */
   public void addText(String contents) {
      
      contents = contents.trim();  // Discard leading and trailing white space

      StringBuilder sb = new StringBuilder();
      State state = State.Text;
      
      for(int index = 0; index<contents.length(); index++) {
         char ch = contents.charAt(index);
         
         switch (state) {
         case DiscardAfterNewline:
            if (" \t".indexOf(ch)>=0) {
               break;
            }
            state = State.Text;
         case Text:
            if (ch == '"') {
               sb.append(ch);
               state = State.InString;
            }
            else if (ch == '\'') {
               sb.append(ch);
               state = State.InCharacter;
            }
            else if (ch == '\n') {
               sb.append(ch);
               state = State.DiscardAfterNewline;
            }
            else if (ch == '\\') {
               state = State.Escape;
            }
            else {
               sb.append(ch);
            }
            break;
         case InString:
            sb.append(ch);
            if (ch == '"') {
               state = State.Text;
            }
            break;
         case InCharacter:
            sb.append(ch);
            if (ch == '\'') {
               state = State.Text;
            }
            break;

         case Escape:
            state = State.Text;
            if (ch == 't') {
               sb.append("   ");
            }
            else if (ch == 'n') {
               sb.append("\n");
            }
            else {
               sb.append('\\');
               sb.append(ch);
            }
            break;
         }
      }
      fBuilder.append(sb.toString());
      fText = null;
   }

   /**
    * Add child template
    * 
    * @param template Template to add
    * @throws Exception 
    */
   public void addChild(TemplateInformation template) throws Exception {
      fBuilder.append(template.getExpandedText());
   }
   
   /**
    * @return Key used to index template
    */
   public String getKey() {
      return fKey;
   }

   /**
    * Get expanded text contents of template.
    * Any template iterations will be expanded.
    * 
    * @return Expanded text contents for template
    * @throws Exception 
    */
   public String getExpandedText() {
      if (fText != null) {
         return fText;
      }
      String text = fBuilder.toString();
      if (fEnumeration == null) {
         fText = text;
         return text;
      }
      StringBuilder       sb   = new StringBuilder();
      Map<String, String> map  = new HashMap<String, String>();
      fBuilder = null;
      String[] variables = fVariable.split("\\s*:\\s*");
      for(String s:fEnumeration.split("\\s*,\\s*")) {
         String[] enums = s.split("\\s*:\\s*");
         if (enums.length != variables.length) {
            sb.append("Variable and enumeration do not match, enum='"+s+"', var='"+fVariable+"'");
            break;
         }
         for (int index=0; index<enums.length; index++) {
//            System.err.println("Adding '" + variables[index] + "' => '" + enums[index] + "'");
            map.put(variables[index], enums[index]);
         }
         sb.append(ReplacementParser.substituteIgnoreUnknowns(text, map));
      }
      fText = sb.toString();
      return fText;
   }

   /**
    * @return Dimension for array template
    */
   public int getDimension() {
      return fDimension;
   }

   /**
    * @return Namespace for template (info, usbdm, class)
    */
   public String getNamespace() {
      return fNameSpace;
   }
   
   /**
    * Return a String representation of the template
    */
   public String toString() {
      return "T["+fBuilder.toString()+"]";
   }
}