package net.sourceforge.usbdm.annotationEditor;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

public class IdentifierRule implements IPredicateRule {

   IToken okToken = Token.UNDEFINED;
   
   IdentifierRule(IToken token) {
      this.okToken = token;
   }
   
   @Override
   public IToken evaluate(ICharacterScanner scanner) {
      int firstChar = scanner.read();
      if (!Character.isJavaIdentifierStart(firstChar)) {
         scanner.unread();
         return Token.UNDEFINED;
      }
      int c = scanner.read();
      while (Character.isJavaIdentifierPart(c)) {
         c = scanner.read();
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
