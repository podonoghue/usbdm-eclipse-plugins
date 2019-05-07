package net.sourceforge.usbdm.annotationEditor;

import net.sourceforge.usbdm.annotationEditor.AnnotationModel.DocumentReference;

/**
 * Describes an option that modifies a value in the C file
 * The node has
 *    <li> a value (untyped represented as a string)
 *    <li> an reference index allowing the location in the C file to be accessed.
 */
public class OptionModelNode extends AnnotationModelNode {
   private int referenceIndex;
   
   /**
    * Create OptionModelNode
    * 
    * @param description Description
    * @param offset
    */
   public OptionModelNode(AnnotationModel annotationModel, String description, int offset) {
      super(annotationModel, description);
      if (annotationModel != null) {
         // For debug only
         this.referenceIndex  = annotationModel.getReferences().getReferenceCount()+offset;
      }
      setModifiable(true);
   }

   /**
    * Create OptionModelNode from another
    * 
    * @param other
    */
   public OptionModelNode(OptionModelNode other) {
      super(other);
      this.referenceIndex  = other.referenceIndex;
   }

   @Override
   public void copyFrom(Object other) throws Exception {
      if (!(other instanceof OptionModelNode)) {
         throw new Exception("Incompatible nodes in copyFrom()");
      }
      super.copyFrom(other);
      this.referenceIndex  = ((OptionModelNode)other).referenceIndex;
   }
   
   @Override
   public boolean equals(Object other) {
      if (!(other instanceof OptionModelNode)) {
         return false;
      }
      OptionModelNode otherNode = (OptionModelNode) other;
      return (referenceIndex == otherNode.referenceIndex) && super.equals(otherNode);
   }

   /**
    * Obtains the reference index of the associated item in the file being edited
    * 
    * @return Index
    */
   int getReferenceIndex() {
      return referenceIndex;
   }

   @Override
   public void listNode(int indent) {
      System.err.print(AnnotationModel.getIndent(indent)+String.format("r=%d:", getReferenceIndex()));
      System.err.println(String.format("%s", getDescription()));
      String toolTip = getToolTip();
      if (toolTip != null) {
         System.err.println(AnnotationModel.getIndent(indent+3)+": "+ toolTip);
      }
   }
   
   @Override
   public Object getValue() throws Exception {
      return getReference().getValue();
   }
   
   @Override
   public String getDialogueValueAsString() throws Exception {
         return getReference().getStringValue();
   }
   
   /**
    * Obtains the document reference of this node in the associated document
    * 
    * @return
    * @throws Exception
    */
   public DocumentReference getReference() throws Exception {
      try {
         return annotationModel.getReferences().get(getReferenceIndex());
      } catch (Exception e) {
         throw new Exception("Illegal Reference @"+getReferenceIndex(), e);
      }
   }
   /**
    * Obtains the document reference of the node offset from this node in the associated document
    * 
    * @param offset Offset from this node to required node
    *  
    * @return node if found
    * 
    * @throws Exception if node cannot be located
    */
   public DocumentReference getReference(int offset) throws Exception {
      try {
         return annotationModel.getReferences().get(getReferenceIndex()+offset);
      } catch (Exception e) {
         throw new Exception("Illegal Reference @"+getReferenceIndex(), e);
      }
   }
}
