package net.sourceforge.usbdm.annotationEditor;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.graphics.Color;

public class ViewConfiguration extends SourceViewerConfiguration {
   
   /* (non-Javadoc)
    * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getConfiguredContentTypes(org.eclipse.jface.text.source.ISourceViewer)
    */
   @Override
   public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
      return new String[] {
            IDocument.DEFAULT_CONTENT_TYPE,
            PartitionScanner.C_IGNORED_COMMENT,
            PartitionScanner.C_COMMENT,
            PartitionScanner.C_STRING,
            PartitionScanner.C_IDENTIFIER,
            PartitionScanner.C_NUMBER,
      };
   }

   private RuleBasedScanner  codeScanner;
   private RuleBasedScanner  commentScanner;
   private RuleBasedScanner  ruleScanner;
   private ColorManager      colorManager;

   public ViewConfiguration(ColorManager colorManager) {
      this.colorManager = colorManager;
   }
   
   protected RuleBasedScanner getCommentScanner() {
      if (commentScanner == null) {
         commentScanner = new CommentScanner(colorManager);
         commentScanner.setDefaultReturnToken(new Token(new TextAttribute(colorManager.getColor(ColorConstants.COMMENT))));
      }
      return commentScanner;
   }

   protected RuleBasedScanner getCodeScanner() {
      if (codeScanner == null) {
         codeScanner = new CodeScanner(colorManager);
         codeScanner.setDefaultReturnToken(new Token(new TextAttribute(colorManager.getColor(ColorConstants.DEFAULT))));
      }
      return codeScanner;
   }

   protected RuleBasedScanner getDummyScanner(Color color) {
//      if (ruleScanner == null) {
         ruleScanner = new RuleBasedScanner();
         ruleScanner.setDefaultReturnToken(new Token(new TextAttribute(color)));
//      }
      return ruleScanner;
   }

   public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
      
      PresentationReconciler reconciler = new PresentationReconciler();

      DefaultDamagerRepairer dr;
      dr = new DefaultDamagerRepairer(getCommentScanner());
      reconciler.setDamager(dr, PartitionScanner.C_COMMENT);
      reconciler.setRepairer(dr, PartitionScanner.C_COMMENT);

      dr = new DefaultDamagerRepairer(getDummyScanner(colorManager.getColor(ColorConstants.DEFAULT)));
      reconciler.setDamager(dr, PartitionScanner.C_IGNORED_COMMENT);
      reconciler.setRepairer(dr, PartitionScanner.C_IGNORED_COMMENT);

      dr = new DefaultDamagerRepairer(getDummyScanner(colorManager.getColor(ColorConstants.NUMBER)));
      reconciler.setDamager(dr, PartitionScanner.C_NUMBER);
      reconciler.setRepairer(dr, PartitionScanner.C_NUMBER);

      dr = new DefaultDamagerRepairer(getDummyScanner(colorManager.getColor(ColorConstants.STRING)));
      reconciler.setDamager(dr, PartitionScanner.C_STRING);
      reconciler.setRepairer(dr, PartitionScanner.C_STRING);

      dr = new DefaultDamagerRepairer(getCodeScanner());
      reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
      reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

//      NonRuleBasedDamagerRepairer ndr;
//      ndr = new NonRuleBasedDamagerRepairer(new TextAttribute(colorManager.getColor(ColorConstants.COMMENT)));
//      reconciler.setDamager(ndr, PartitionScanner.C_COMMENT);
//      reconciler.setRepairer(ndr, PartitionScanner.C_COMMENT);

      return reconciler;
   }

 
}