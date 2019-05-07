package net.sourceforge.usbdm.annotationEditor;

/**
 * Describes an option that modifies a string value in the C file
 * <pre>
 * //  &lt;s> String Option Description
 * //  &lt;i> Tool-tip text
 * //  #define STRING_OPTION "Value of string"
 * <pre>
 */
public class StringOptionModelNode extends OptionModelNode {

   protected int characterLimit;

   /**
    * Constructor<br>
    * Represents an option that modifies a string value in the C file
    * <pre>
    * //  &lt;s> String Option Description
    * //  &lt;i> Tool-tip text
    * //  #define STRING_OPTION "Value of string"
    * <pre>
    * 
    * @param description   Description
    * @param offset        Offset of numeric value 
    * @param bitField      
    */
   public StringOptionModelNode(AnnotationModel annotationModel, String description, int offset, int characterLimit) {
      super(annotationModel, description, offset);
      this.characterLimit = characterLimit;
   }

   public StringOptionModelNode(StringOptionModelNode other) {
      super(other);
      this.characterLimit = other.characterLimit;
   }

   @Override
   public boolean equals(Object other) {
      if (!(other instanceof StringOptionModelNode)) {
         return false;
      }
      StringOptionModelNode otherNode = (StringOptionModelNode) other;
      return
            (characterLimit == otherNode.characterLimit) &&
            super.equals(otherNode);
   }

   @Override
   public void copyFrom(Object other) throws Exception {
      if (!(other instanceof StringOptionModelNode)) {
         throw new Exception("Incompatible nodes in copyFrom()");
      }
      super.copyFrom(other);
      this.characterLimit = ((StringOptionModelNode)other).characterLimit;
   }
   
   @Override
   public String getDialogueValueAsString() {
      return (String)getValue();
   }

   @Override
   public Object getValue() {
      try {
         return annotationModel.getReferences().get(getReferenceIndex()).getStringValue();
      } catch (Exception e) {
         return e.getMessage();
      }
   }

   @Override
   public void setValue(Object value) {
      try {
         annotationModel.getReferences().get(getReferenceIndex()).setValue((String) value);
//         AnnotationParser.refreshPartitions(AnnotationModel.this);
      } catch (Exception e) {
      }
   }

   @Override
   public void setValueFromDialogueString(String value) {
      try {
         annotationModel.getReferences().get(getReferenceIndex()).setStringValue(value);
//         AnnotationParser.refreshPartitions(AnnotationModel.this);
      } catch (Exception e) {
      }
   }

   @Override
   public void listNode(int indent) {
      System.err.print(AnnotationModel.getIndent(indent)+String.format("o=%d", getReferenceIndex()));
      if (characterLimit > 0) {
         System.err.print(String.format("[%d]", characterLimit));
      }
      System.err.println(String.format(": %s", getDescription()));
      String toolTip = getToolTip();
      if (toolTip != null) {
         System.err.println(AnnotationModel.getIndent(indent+3)+": "+ toolTip);
      }
   }

}

