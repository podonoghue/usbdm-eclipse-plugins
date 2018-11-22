package net.sourceforge.usbdm.deviceEditor.xmlParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.sourceforge.usbdm.deviceEditor.xmlParser.ParseMenuXML.TemplateIteration;
import net.sourceforge.usbdm.cdt.utilties.ReplacementParser;
import net.sourceforge.usbdm.cdt.utilties.ReplacementParser.IKeyMaker;

/**
 * Used to represent a template for code in a project
 */
public class TemplateInformation {
   /** Key used to index template */
   private final String            fKey;
   /** Namespace for template (info, usbdm, class) */
   private final String            fNameSpace;
   /** Text contents for template */
   private final StringBuilder     fBuilder;
   /** Dimension for array template */
   private final int               fDimension;
   /** Raw text contents for template - cached */
   private String            fText = null;
   /** Expanded text contents for template - cached */
   private String fExpandedText = null;
   /** Iterations applied to template */
   private ArrayList<TemplateIteration> fIterations = null;

   /**
    * Construct template
    * 
    * @param key        Key used to index template
    * @param namespace  Namespace for template (info, usbdm, class)
    * @param dimension  Dimension for array template
    * @param contents   Text contents for template
    */
   public TemplateInformation(String key, String nameSpace, String contents, int dimension) {
      fKey        = key;
      fNameSpace  = nameSpace;
      fBuilder    = new StringBuilder(contents); 
      fDimension  = dimension;
   }
   
   /**
    * Add enumeration to this template
    * 
    * @param variable
    * @param enumeration
    * 
    * @return Iteration added
    */
   public TemplateIteration addIteration(String variable, String enumeration) {
      if (fIterations == null) {
         fIterations = new ArrayList<TemplateIteration>();
      }
      TemplateIteration interation = new TemplateIteration(variable, enumeration.trim());
      fIterations.add(interation);
      return interation;
   }
   
   /**
    * @return Iterations to apply to this template
    */
   public ArrayList<TemplateIteration> getIterations() {
      return fIterations;
   }

   /**
    * @return Key used to index template
    */
   public String getKey() {
      return fKey;
   }

   /**
    * Get text contents of template<br>
    * 
    * @return Text contents for template
    */
   public String getRawText() {
      if (fText == null) {
         fText = fBuilder.toString();
      }
      return fText;
   }

   /**
    * @return Text contents for template
    */
   public String getExpandedText() {
      if (fExpandedText == null) {
         StringBuffer sb = new StringBuffer();
         if (fIterations != null) {
            for (TemplateIteration i:fIterations) {
               Map<String, String> map = new HashMap<String, String>();
               for(String s:i.getEnumeration()) {
                  map.put(i.getVariable(), s);
                  IKeyMaker fKeyMaker = new IKeyMaker() {
                     @Override
                     public String makeKey(String name) {
                        return name;
                     }
                  };
                  sb.append(ReplacementParser.substitute(getRawText(), map, fKeyMaker));
               }
            }
            fExpandedText = sb.toString();
         }
         else {
            fExpandedText = getRawText();
         }
      }
      return fExpandedText;
   }

   /**
    * Append text to template
    *  
    * @param contents Text to append
    */
   public void addToContents(String contents) {
      fBuilder.append(contents);
      fText = null;
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
}