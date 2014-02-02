package net.sourceforge.usbdm.annotationEditor;

import org.eclipse.jface.text.rules.*;
import org.eclipse.jface.text.*;

/**
 * Scans a comment looking for annotations
 * 
 * @author podonoghue
 *
 */
public class CommentScanner extends RuleBasedScanner {

   /**
    * Scans a comment looking for annotations
    * 
    * @param manager
    */
   public CommentScanner(ColorManager manager) {
      IToken optionToken = new Token(new TextAttribute(manager.getColor(ColorConstants.OPTION_TAG)));
      
      IRule[] rules = new IRule[] {
         new SingleLineRule("<<<", ">>>", optionToken),  // Add rule for start/end
         new SingleLineRule("<", ">",     optionToken),  // Add rule for processing annotations
         new WhitespaceRule(new WhitespaceDetector()),   // Add generic whitespace rule.
      };
      setRules(rules);
   }
}
