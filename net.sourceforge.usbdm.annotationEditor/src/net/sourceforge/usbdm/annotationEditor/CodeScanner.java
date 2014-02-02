package net.sourceforge.usbdm.annotationEditor;

import org.eclipse.jface.text.*;
import org.eclipse.jface.text.rules.*;

public class CodeScanner extends RuleBasedScanner {

   public CodeScanner(ColorManager manager) {
      IToken stringToken = new Token(new TextAttribute(manager.getColor(ColorConstants.STRING2)));

      IRule[] rules = new IRule[]{      
            new SingleLineRule("\"", "\"", stringToken, '\\'),  // Rule for double quotes
            new SingleLineRule("'",  "'",  stringToken, '\\'),  // Rule for single quotes
            new WhitespaceRule(new WhitespaceDetector()),       // Rule for generic whitespace rule.
      };
      setRules(rules);
   }
}
