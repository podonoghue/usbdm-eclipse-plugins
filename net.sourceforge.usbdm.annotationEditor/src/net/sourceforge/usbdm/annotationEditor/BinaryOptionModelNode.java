package net.sourceforge.usbdm.annotationEditor;

import net.sourceforge.usbdm.annotationEditor.AnnotationModel.BitField;
import net.sourceforge.usbdm.annotationEditor.AnnotationModel.EnumNumericValue;
import net.sourceforge.usbdm.annotationEditor.AnnotationModel.Polarity;

/**
 * Represents a two value option e.g. true/false, enabled/disabled
 * <pre>
 * // &lt;q> Description
 * // &lt;i> Tool-tip
 * //     &lt;0=> False option name
 * //     &lt;1=> True option name
 * </pre>
 */
public class BinaryOptionModelNode extends NumericOptionModelNode {

   /** Text to display of false */
   String   falseValueText = "false";
   
   /** Text to display of true */
   String   trueValueText  = "true";
   
   /** Indicates node is active-high or active-low */
   private Polarity  fPolarity = Polarity.ACTIVE_HIGH;

   /**
    * Constructor<br>
    * Represents a binary (two value) node in the document
    * <pre>
    * // &lt;q> Description
    * // &lt;i> Tool-tip
    * //     &lt;0=> False option name
    * //     &lt;1=> True option name
    * </pre>
    * 
    * @param description   Description
    * @param offset        Offset of numeric value 
    * @param bitField      
    */
   public BinaryOptionModelNode(AnnotationModel annotationModel, String description, int offset, BitField bitField) throws Exception {
      super(annotationModel, description, offset, bitField);
      if ((bitField != null) && (bitField.getStart() != bitField.getEnd())) {
         throw new Exception("Binary option must have a width of 1");
      }
      setModifiable(true);
   }

   @Override
   public void copyFrom(Object other) throws Exception {
      if (!(other instanceof BinaryOptionModelNode)) {
         throw new Exception("Incompatible nodes in copyFrom()");
      }
      super.copyFrom(other);
      this.falseValueText = ((BinaryOptionModelNode)other).falseValueText;
      this.trueValueText  = ((BinaryOptionModelNode)other).trueValueText;
      this.fPolarity      = ((BinaryOptionModelNode)other).getPolarity();
   }
   
   @Override
   public boolean equals(Object other) {
      if (!(other instanceof BinaryOptionModelNode)) {
         return false;
      }
      BinaryOptionModelNode otherNode = (BinaryOptionModelNode) other;
      return 
            falseValueText.equals(otherNode.falseValueText) &&
            trueValueText.equals(otherNode.trueValueText) &&
            super.equals(otherNode);
   }

   @Override
   public String getDialogueValueAsString() throws Exception {
      return ((Boolean)getValue())?trueValueText:falseValueText;
   }

   /**
    * Add enumerated value applied to this node e.g. &lt0=> No setup (Reset default)
    * 
    * @param enumerationValue Enumerated value to add
    */
   public void addEnumerationValue(EnumNumericValue enumValue) {
      if (enumValue.getValue() == 0) {
         falseValueText = enumValue.getName();
      }
      else {
         trueValueText = enumValue.getName();
      }
   }

   @Override
   public Object getValue() throws Exception {
      return Boolean.valueOf(((Long)super.getValue()) != 0);
   }

   /**
    * Gets value but captures any exceptions<br>
    * Uses polarity to determine active value
    * 
    * @return
    */
   public Boolean safeGetActiveValue() {
      try {
         return getPolarity().apply((Boolean)getValue());
      } catch (Exception e) {
      }
      return false;
   }
   
   /**
    * Gets value but captures any exceptions
    * 
    * @return
    */
   public Boolean safeGetValue() {
      try {
         return (Boolean)getValue();
      } catch (Exception e) {
         return false;
      }
   }
   
   /**
    * Sets polarity of node<br>
    * Only applies to boolean nodes
    * 
    * @param polarity
    */
   public void setPolarity(Polarity polarity) {
      fPolarity = polarity;
   }
   
   /**
    * Indicates polarity of node<br>
    * Only applies to boolean nodes
    * 
    * @return polarity
    */
   public Polarity getPolarity() {
      return fPolarity;
   }
}

