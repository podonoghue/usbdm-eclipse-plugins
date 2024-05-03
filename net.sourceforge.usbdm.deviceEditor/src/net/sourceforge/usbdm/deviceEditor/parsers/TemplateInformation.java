package net.sourceforge.usbdm.deviceEditor.parsers;

import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;

/**
 * Used to represent a template for code in a project
 */
public class TemplateInformation {
   
   /** Key used to index template */
   private final String fKey;

   /** Namespace for template (info, usbdm, class) */
   private final String fNameSpace;
   
   /** Buffer to accumulate text contents for template */
   private StringBuilder fStringBuilder;
   
   /** Processed text contents for template - cached */
   private String fText = null;
   
   /** Condition to evaluate during code generation */
   private String fCodeGenerationCondition;

   /**
    * Builder for enum templates
    */
   private TemplateContentBuilder fContentBuilder = null;

   /**
    * Construct template
    * 
    * @param key                       Key used to index template
    * @param codeGenerationCondition
    * @param namespace                 Namespace for template (info, usbdm, class)
    */
   public TemplateInformation(String key, String nameSpace, String codeGenerationCondition) {
      fContentBuilder = null;
      fKey           = key;
      fNameSpace     = nameSpace;
      fStringBuilder = new StringBuilder(100);
      fText          = null;
      fCodeGenerationCondition = codeGenerationCondition;
   }
   
   enum State { Text, DiscardAfterNewline, Escape, InString, InCharacter };

   /**
    * Append text to template.
    * The text is processed \n => newline char etc.
    * 
    * @param contents Text to append
    * 
    * @throws Exception
    */
   public void addText(String contents) throws Exception {
      if (fContentBuilder != null) {
         throw new Exception("Adding text to template with builder, key='"+fKey+"'");
      }
      contents = contents.trim();  // Discard leading and trailing white space

      StringBuilder sb = new StringBuilder();
      fStringBuilder.append(addText(sb, contents));
   }
   
   /**
    * Append text to template.
    * The text is processed \n => newline char etc.
    * 
    * @param contents Text to append
    * @return
    * 
    * @throws Exception
    */
   public static String addText(StringBuilder sb, String contents) throws Exception {
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
               state = State.DiscardAfterNewline;
            }
            else {
               sb.append('\\');
               sb.append(ch);
            }
            break;
         }
      }
      return sb.toString();
   }
   
   /**
    * @return Key used to index template
    */
   public String getKey() {
      return fKey;
   }

   
   /**
    * Get expanded text contents of template.
    * 
    * @throws Exception
    */
   public String getExpandedText(VariableProvider varProvider) {
      
      String text = fText;
      
      if (fContentBuilder != null) {
         try {
            String contents = fContentBuilder.build().trim();
            StringBuilder sb = new StringBuilder();
            fStringBuilder.append(addText(sb, contents));
            text = sb.toString();
         } catch (Exception e) {
            e.printStackTrace();
            return "Content builder failed, reason: " + e.getMessage() + "\n";
         }
      }
      else {
         if (fText == null) {
            // Convert to string on first use
            fText = fStringBuilder.toString();
            fStringBuilder = null;
         }
         text = fText;
      }
      if (fCodeGenerationCondition != null) {
         try {
            Boolean condition = Expression.getValueAsBoolean(fCodeGenerationCondition, varProvider);
            if (!condition) {
               return "";
            }
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
      return text;
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
   @Override
   public String toString() {
      String text = fText;
      if (fStringBuilder != null) {
         text = fStringBuilder.toString();
      }
      return "T["+text+"]";
   }

   public void setBuilder(TemplateContentBuilder enumBuilder) {
      fContentBuilder = enumBuilder;
   }
}