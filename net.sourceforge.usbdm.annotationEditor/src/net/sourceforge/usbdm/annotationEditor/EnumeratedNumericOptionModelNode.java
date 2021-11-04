package net.sourceforge.usbdm.annotationEditor;

import java.util.ArrayList;

import net.sourceforge.usbdm.annotationEditor.AnnotationModel.BitField;
import net.sourceforge.usbdm.annotationEditor.AnnotationModel.EnumNumericValue;
import net.sourceforge.usbdm.annotationEditor.AnnotationModel.Modifier;
import net.sourceforge.usbdm.annotationEditor.AnnotationModel.SelectionTag;

/**
 * Describes an option that modifies a numeric value in the C file from an enumerated set of values
 * <pre>
 * //   <o> Description
 * //   <i> Tool-tip
 * //     <0=> Option with value 0
 * //     <1=> Option with value 1
 * //     <12=> Option with value 12
 * //     <1=> Default Option value
 * </pre>
 */
public class EnumeratedNumericOptionModelNode extends NumericOptionModelNode {
   /** List of enumerated values applied to this node e.g. &lt0=> No setup (Reset default) */
   private ArrayList<EnumNumericValue>     enumerationValues = null;
   private ArrayList<SelectionTag>  selectionTags = null;

   /**
    * Constructor<br>
    * Represents a enumeration node in the document
    * <pre>
    * //   <o> Description
    * //   <i> Tool-tip
    * //     <0=> Option with value 0
    * //     <1=> Option with value 1
    * //     <12=> Option with value 12
    * //     <1=> Default Option value
    * </pre>
    * 
    * @param description   Description
    * @param offset        Offset of numeric value 
    * @param bitField      
    */
   public EnumeratedNumericOptionModelNode(AnnotationModel annotationModel, String description, int offset, BitField bitField) {
      super(annotationModel, description, offset, bitField);
      setModifiable(true);
   }

   /**
    * Constructor from another node<br>
    * Represents a enumeration node in the document
    * 
    * @param other      
    */
   public EnumeratedNumericOptionModelNode(NumericOptionModelNode other) {
      super(other);
   }

   @Override
   public void copyFrom(Object other) throws Exception {
      if (!(other instanceof EnumeratedNumericOptionModelNode)) {
         throw new Exception("Incompatible nodes in copyFrom()");
      }
      super.copyFrom(other);
      this.enumerationValues = ((EnumeratedNumericOptionModelNode)other).enumerationValues;
   }
   
   /**
    * Constructor from another node<br>
    * Represents a numeric node in the document
    * 
    * @param other      
    */
   public boolean equals(Object other) {
      if (!(other instanceof EnumeratedNumericOptionModelNode)) {
         return false;
      }
      EnumeratedNumericOptionModelNode otherNode = (EnumeratedNumericOptionModelNode) other;
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
   public String getDialogueValueAsString() throws Exception {
      long value = (Long) getValue();
      for (int index=0; index<enumerationValues.size(); index++) {
         EnumNumericValue enumValue = enumerationValues.get(index);
         if (enumValue.getValue() == value) {
//            System.err.println(String.format("getValueAsString() Found (0x%X => %s) ", value, enumValue.getName()));
            return enumValue.getName();
         }
      }
//      System.err.println(String.format("getValueAsString() Didn't find (0x%X => %s) ", value, enumerationValues.get(0).getName()));
      return enumerationValues.get(0).getName();
   }
   

   /**
    * Get index of matching enumerated value
    * 
    * @param name Name to match
    * 
    * @return index or -1 if not found
    */
   public int getEnumIndex(String name) {
      for (int index=0; index<enumerationValues.size(); index++) {
         EnumNumericValue enumValue = enumerationValues.get(index);
         if (enumValue.getName().equals(name)) {
            return index;
         }
      }
      return -1;
   }
   
   /**
    * Sets the currently selected value based on the text provided
    * 
    * @param The name of a enumerated value
    * 
    * @note If the value doesn't match any of the permitted enumerated values then it is ignored.
    */
   @Override
   public void setValueFromDialogueString(String name) throws Exception {
      int index = getEnumIndex(name);
      if (index>=0) {
         super.setValue(enumerationValues.get(index).getValue());
         return;
      }
//      System.err.println("Failed to locate enumerated value n = '" + getName() + "', v = '" + name +"'");
   }

   /**
    * Get list of enumerated values applied to this node e.g. &lt0=> No setup (Reset default)
    * 
    * @return List of enumerated values
    */
   public ArrayList<EnumNumericValue> getEnumerationValues() {
      return enumerationValues;
   }

   /**
    * Add enumerated value applied to this node e.g. &lt0=> No setup (Reset default)
    * 
    * @param enumerationValue Enumerated value to add
    */
   public void addEnumerationValue(EnumNumericValue enumerationValue) {
      if (enumerationValues == null) {
         enumerationValues = new ArrayList<EnumNumericValue>();
      }
      this.enumerationValues.add(enumerationValue);
   }

   @Override
   public void listNode(int indent) {
      System.err.print(AnnotationModel.getIndent(indent)+String.format("o=%d", getReferenceIndex()));
      if (getBitField() != null) {
         System.err.print(String.format("[%d..%d]", getBitField().getStart(), getBitField().getEnd()));
      }
      if (getRange() != null) {
         System.err.print(String.format("(0x%X..0x%X:0x%X)", getRange().getStart(), getRange().getEnd(), getRange().getStep()));
      }
      if (getModifiers() != null) {
         for (Modifier m : getModifiers()) {
            System.err.print(String.format("(#%s0x%X)", m.getOperation(), m.getFactor()));
         }
      }
      if (enumerationValues != null) {
         for (EnumNumericValue e : enumerationValues) {
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

   /**
    * Add a selection tag to the node
    * 
    * @param selectionTag
    */
   public void addSelectionTag(SelectionTag selectionTag) {
      if (selectionTags == null) {
         selectionTags = new ArrayList<SelectionTag>();
      }
      selectionTags.add(selectionTag);
   }
   
   public ArrayList<SelectionTag> getSelectionTags() {
      return selectionTags;
   }
}
