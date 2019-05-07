package net.sourceforge.usbdm.annotationEditor;

import net.sourceforge.usbdm.annotationEditor.AnnotationModel.BitField;

/**
 * Currently unused?
 */
public class PllConfigurationModelNode extends NumericOptionModelNode {
   public PllConfigurationModelNode(AnnotationModel annotationModel, String description, int offset, BitField bitField) {
      super(annotationModel, description, offset, null);
   }

   public PllConfigurationModelNode(PllConfigurationModelNode other) {
      super(other);
   }

   @Override
   public void copyFrom(Object other) throws Exception {
      if (!(other instanceof PllConfigurationModelNode)) {
         throw new Exception("Incompatible nodes in copyFrom()");
      }
      super.copyFrom(other);
   }
   
   @Override
   public boolean equals(Object other) {
      if (!(other instanceof PllConfigurationModelNode)) {
         return false;
      }
      PllConfigurationModelNode otherNode = (PllConfigurationModelNode) other;
      return super.equals(otherNode);
   }

   @Override
   public void setValue(Long value) throws Exception {
      super.setValue(value);
   }

}