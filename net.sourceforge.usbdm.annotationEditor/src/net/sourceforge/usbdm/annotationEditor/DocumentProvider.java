package net.sourceforge.usbdm.annotationEditor;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;

public class DocumentProvider extends TextFileDocumentProvider {

   @Override
   public IDocument getDocument(Object element) {
      
      IDocument document = super.getDocument(element);
      
      if (document != null) {
         IDocumentPartitioner partitioner = new Partitioner (
               new PartitionScanner(), 
               new String[] { 
                  PartitionScanner.C_IGNORED_COMMENT,
                  PartitionScanner.C_COMMENT,
                  PartitionScanner.C_IDENTIFIER,
                  PartitionScanner.C_STRING,
                  PartitionScanner.C_NUMBER,
                  });
         partitioner.connect(document);
         document.setDocumentPartitioner(partitioner);
      }
      return document;
   }
}