package net.sourceforge.usbdm.deviceEditor.parsers;

import net.sourceforge.usbdm.deviceEditor.information.Variable;
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
   
   /** Condition to evaluate during code generation */
   private String fCodeGenerationCondition;

   /**
    * Builder for enum templates
    */
   private TemplateContentBuilder fContentBuilder = null;

   /// Priority used to order template generation
   private int priority = 50;

   /// Variable provider
   private VariableProvider fProvider;
   
   /**
    * Construct template
    * @param fProvider
    * 
    * @param key                       Key used to index template
    * @param codeGenerationCondition
    * @param namespace                 Namespace for template (info, usbdm, class)
    * @throws Exception
    */
   public TemplateInformation(VariableProvider provider, String key, String nameSpace, String codeGenerationCondition) throws Exception {
      fProvider       = provider;
      fContentBuilder = null;
      fKey            = key;
      fNameSpace      = nameSpace;
      fStringBuilder  = new StringBuilder(100);
      fCodeGenerationCondition = Expression.expandImmediateSubexpressions(codeGenerationCondition, provider);
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
      fStringBuilder.append(contents);
   }
   
   /**
    * The text is processed \n => newline char etc.
    * 
    * @param contents Text to process
    * 
    * @return Processed text
    * 
    * @throws Exception
    */
   public static String processText(String contents) {
      StringBuilder sb   = new StringBuilder();
      int column = 0;
      
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
               column++;
               state = State.InString;
            }
            else if (ch == '\'') {
               sb.append(ch);
               column++;
               state = State.InCharacter;
            }
            else if (ch == '\n') {
               sb.append(ch);
               column = 0;
               state = State.DiscardAfterNewline;
            }
            else if (ch == '\\') {
               state = State.Escape;
            }
            else {
               sb.append(ch);
               column++;
            }
            break;
         case InString:
            sb.append(ch);
            column++;
            if (ch == '"') {
               state = State.Text;
            }
            break;
         case InCharacter:
            sb.append(ch);
            column++;
            if (ch == '\'') {
               state = State.Text;
            }
            break;

         case Escape:
            state = State.Text;
            if (ch == 't') {
               for (int c=(3-(column%3)); c>0; c--) {
                  sb.append(" ");
               }
            }
            else if (ch == 'n') {
               sb.append("\n");
               column = 0;
               state = State.DiscardAfterNewline;
            }
            else {
               sb.append('\\');
               sb.append(ch);
               column++;
               column++;
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

      // Can't cache as substitutions may change
      
      String text = null;
      
      // Need to create text from fContentBuilder or fStringBuilder
      
      if (fContentBuilder != null) {
         // Use content builder to build text now
         try {
            text = fContentBuilder.build();
         } catch (Exception e) {
            e.printStackTrace();
            return "Content builder failed, reason: " + e.getMessage() + "\n";
         }
      }
      else {
         text = fStringBuilder.toString();
      }
      text = processText(text);

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
      if (fProvider != null) {
         Variable errorFlagVar = fProvider.safeGetVariable("/HARDWARE/generateDebugDefinitions");
         if ((errorFlagVar != null) && errorFlagVar.getValueAsBoolean()) {
            text = "// ns = '"+fNameSpace+"', k = '"+fKey+"'\n" + text;
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
      
      String text = null;
      
      if (fStringBuilder != null) {
         text = fStringBuilder.toString();
      }
      else if (fContentBuilder != null) {
         try {
            text = fContentBuilder.build();
         } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
         }
      }
      else {
         text = "Broken template";
      }
      return "T["+text+"]";
   }

   public void setBuilder(TemplateContentBuilder builder) throws Exception {
      
      if ((fStringBuilder != null)&&(!fStringBuilder.isEmpty())) {
         throw new Exception("Template already contains text before setting content builder");
      }
      fStringBuilder = null;
      fContentBuilder = builder;
   }

   /**
    * Get priority used to order template generation
    * 
    * @return the priority
    */
   public int getPriority() {
      return priority;
   }

   /**
    * Set priority used to order template generation
    * 
    * @param priority the priority to set
    */
   public void setPriority(int priority) {
      this.priority = priority;
   }
}