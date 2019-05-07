package net.sourceforge.usbdm.annotationEditor;

import java.util.ArrayList;

import net.sourceforge.usbdm.annotationEditor.AnnotationModel.EnumTextValue;

/**
 * Describes an option that modifies a string value in the C file from an enumerated set of values
 * <pre>
 * //   &lts> Description
 * //   &lti> Tool-tip
 * //     &lt"aaa"=> Option with value aaa
 * //     &lt"bbb"=> Option with value bbb
 * //     &lt"ccc"=> Option with value ccc
 * //     &lt1=> Default Option value
 * </pre>
 */
public class EnumeratedStringOptionModelNode extends StringOptionModelNode {
   /** List of enumerated values applied to this node e.g. &lt0=> No setup (Reset default) */
   private ArrayList<EnumTextValue>    enumerationValues = null;

   /**
    * Constructor<br>
    * Represents a enumeration node in the document
    * <pre>
    * //   &lto> Description
    * //   &lti> Tool-tip
    * //     &lt0=> Option with value 0
    * //     &lt1=> Option with value 1
    * //     &lt12=> Option with value 12
    * //     &lt1=> Default Option value
    * </pre>
    * 
    * @param description   Description
    * @param offset        Offset of numeric value 
    * @param bitField      
    */
   public EnumeratedStringOptionModelNode(AnnotationModel annotationModel, String description, int offset, int characterLimit) {
      super(annotationModel, description, offset, characterLimit);
   }
   
   /**
    * Constructor from another node<br>
    * Represents a enumeration node in the document
    * 
    * @param other      
    */
   public EnumeratedStringOptionModelNode(StringOptionModelNode other) {
      super(other);
   }

   @Override
   public void copyFrom(Object other) throws Exception {
      if (!(other instanceof EnumeratedStringOptionModelNode)) {
         throw new Exception("Incompatible nodes in copyFrom()");
      }
      super.copyFrom(other);
      this.enumerationValues = ((EnumeratedStringOptionModelNode)other).enumerationValues;
   }
   
   /**
    * Constructor from another node<br>
    * Represents a numeric node in the document
    * 
    * @param other      
    */
   public boolean equals(Object other) {
      if (!(other instanceof EnumeratedStringOptionModelNode)) {
         return false;
      }
      EnumeratedStringOptionModelNode otherNode = (EnumeratedStringOptionModelNode) other;
      if (super.equals(otherNode) && (enumerationValues == otherNode.enumerationValues)) {
         return true; 
      }
      if ((enumerationValues == null) || (otherNode.enumerationValues == null)) {
         return false; 
      }
      for (int index = 0; index<enumerationValues.size(); index++) {
         if (!enumerationValues.get(index).equals(otherNode.enumerationValues.get(index))) {
            return false;
         }
      }
      return true;
   }

   @Override
   public String getDialogueValueAsString() {
      // Get value from document
      String value = (String) super.getValue();
      for (int index=0; index<enumerationValues.size(); index++) {
         EnumTextValue enumValue = enumerationValues.get(index);
//         System.err.println(String.format("getDialogueValueAsString() checking (%s)", enumValue.getValue()));
         if (enumValue.getValue().equals(value)) {
//            System.err.println(String.format("getValueAsString() Found (%s => %s) ", value, enumValue.getName()));
            return enumValue.getName();
         }
      }
//      System.err.println(String.format("getDialogueValueAsString() Didn't find (%s) defaulting to  (%s)", value, enumerationValues.get(0).getName()));
      return enumerationValues.get(0).getName();
   }
   

   /**
    * Get index of matching enumerated value
    * 
    * @param name Name to match e.g. for &lt"value"=> name 
    * 
    * @return index or -1 if not found
    */
   public int getEnumIndex(String name) {
      for (int index=0; index<enumerationValues.size(); index++) {
         EnumTextValue enumValue = enumerationValues.get(index);
         if (enumValue.getName().equals(name)) {
            return index;
         }
      }
      return -1;
   }
   
   /**
    * Sets the currently selected value based on the name provided
    * 
    * @param name The name of a enumerated value e.g. for &lt"value"=> name 
    * 
    * @note If the value doesn't match any of the permitted enumerated values then it is ignored.
    */
   @Override
   public void setValueFromDialogueString(String name) {
      int index = getEnumIndex(name);
      if (index>=0) {
         super.setValueFromDialogueString(enumerationValues.get(index).getValue());
         return;
      }
//      System.err.println("setValueAsString() - Failed to locate enumerated value node_name = '" + getName() + "', name = '" + name +"'");
   }

   /**
    * Get list of enumerated values applied to this node e.g. &lt0=> No setup (Reset default)
    * 
    * @return List of enumerated values
    */
   public ArrayList<EnumTextValue> getEnumerationValues() {
      return enumerationValues;
   }

   /**
    * Add enumerated value applied to this node e.g. &lt0=> No setup (Reset default)
    * 
    * @param enumerationValue Enumerated value to add
    */
   public void addEnumerationValue(EnumTextValue enumerationValue) {
      if (enumerationValues == null) {
         enumerationValues = new ArrayList<EnumTextValue>();
      }
      this.enumerationValues.add(enumerationValue);
   }

   @Override
   public void listNode(int indent) {
      System.err.print(AnnotationModel.getIndent(indent)+String.format("o=%d", getReferenceIndex()));
      if (enumerationValues != null) {
         for (EnumTextValue e : enumerationValues) {
            System.err.print(String.format("(%s=>0x%X)", e.getName(), e.getValue()));
         }
      }
      System.err.println(String.format("%s", getDescription()));
      String toolTip = getToolTip();
      if (toolTip != null) {
         System.err.println(AnnotationModel.getIndent(indent+3)+": "+ toolTip);
      }
   }

   public String safeGetValueAsString() {
      try {
         return getDialogueValueAsString();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
   return "";
   }

}
