package net.sourceforge.usbdm.annotationEditor;

/**
 * Describes a simple heading used to group options in the model<br>
 * The node has no value associated with it.
 * <pre>
 * // &lt;h> Description
 * </pre>
 */
public class HeadingModelNode extends AnnotationModelNode {
   
   /**
    * Constructor for a simple heading used to group options in the model
    * <pre>
    * // &lt;h> Description
    * </pre>
    * 
    * @param description Description to display
    */
   public HeadingModelNode(AnnotationModel annotationModel, String description) {
      super(annotationModel, description);
   }
   
   @Override
   public void copyFrom(Object other) throws Exception {
      if (!(other instanceof HeadingModelNode)) {
         throw new Exception("Incompatible nodes in copyFrom()");
      }
      super.copyFrom(other);
   }
   
   @Override
   public boolean equals(Object other) {
      return (other instanceof HeadingModelNode) && super.equals(other);
   }
}
