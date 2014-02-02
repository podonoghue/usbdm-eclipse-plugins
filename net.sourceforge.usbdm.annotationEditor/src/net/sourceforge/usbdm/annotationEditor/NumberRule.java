package net.sourceforge.usbdm.annotationEditor;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

public class NumberRule implements IPredicateRule {

   IToken okToken = Token.UNDEFINED;
   
   NumberRule(IToken token) {
      this.okToken = token;
   }
   
   @Override
   public IToken evaluate(ICharacterScanner scanner) {
      int firstChar = scanner.read();
      boolean hexNum = false;
      if (firstChar == '-') {
         firstChar = scanner.read();
         if (!Character.isDigit(firstChar)) {
            scanner.unread();
            scanner.unread();
            return Token.UNDEFINED;
         }
      }
      else if (!Character.isDigit(firstChar)) {
         scanner.unread();
         return Token.UNDEFINED;
      }
      int c = scanner.read();
      if ((firstChar == '0') && ((c == 'x') || (c == 'X'))) {
         c = scanner.read();
         hexNum = true;
      }
      while (Character.isDigit(c) || (hexNum & ((c >= 'a') && (c <= 'f'))||(c >= 'A') && (c <= 'F'))) {
         c = scanner.read();
      }
      if ((c == 'e')||(c == 'E')) {
         do {
            c = scanner.read();
         } while (Character.isDigit(c));
      }
      scanner.unread();
      return okToken;
   }

   @Override
   public IToken getSuccessToken() {
      return okToken;
   }

   @Override
   public IToken evaluate(ICharacterScanner scanner, boolean resume) {
      return evaluate(scanner);
   }

}
