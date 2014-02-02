package net.sourceforge.usbdm.annotationEditor;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;

public class PartitionScanner extends RuleBasedPartitionScanner {
   public final static String C_COMMENT         = "__c_comment";
   public final static String C_IGNORED_COMMENT = "__c_ignored_comment";
   public final static String C_STRING          = "__c_string";
   public final static String C_NUMBER          = "__c_number";
   public final static String C_IDENTIFIER      = "__c_identifier";
 
	public PartitionScanner() {
	   setDefaultReturnToken(new Token(Document.DEFAULT_PARTITIONING));
	   
      IToken commentToken        = new Token(C_COMMENT);
      IToken ignoredCommentToken = new Token(C_IGNORED_COMMENT);
      IToken stringToken         = new Token(C_STRING);
      IToken numberToken         = new Token(C_NUMBER);
      IToken identifierToken     = new Token(C_IDENTIFIER);

		IPredicateRule[] rules = new IPredicateRule[] {
         new MultiLineRule( "/*!", "*/",  ignoredCommentToken), // Multi-line ignored comment
         new EndOfLineRule( "//!",        ignoredCommentToken), // Single-line ignored comment
         new MultiLineRule( "/*",  "*/",  commentToken),        // Multi-line comment
         new EndOfLineRule( "//",         commentToken),        // Single-line comment
         new SingleLineRule("\"",  "\"",  stringToken, '\\'),   // Rule for double quoted string
         new SingleLineRule("'",   "'",   stringToken, '\\'),   // Rule for single quoted char
         new IdentifierRule(identifierToken),                   // Rule for C identifier
         new NumberRule(numberToken),                           // Rule for C numbers
		};
		setPredicateRules(rules);
	}
}
