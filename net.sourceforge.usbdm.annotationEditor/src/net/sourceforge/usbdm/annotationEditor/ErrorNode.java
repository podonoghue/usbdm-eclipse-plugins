package net.sourceforge.usbdm.annotationEditor;


/**
 * A visible node used to represent an error
 */
public class ErrorNode extends AnnotationModelNode {

   /**
    * Constructor
    * 
    * @param description Description of error to display
    */
   public ErrorNode(AnnotationModel annotationModel, String description) {
      super(annotationModel, description);
   }
}

