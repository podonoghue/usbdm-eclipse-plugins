package net.sourceforge.usbdm.annotationEditor;

import java.util.ArrayList;

import net.sourceforge.usbdm.annotationEditor.AnnotationModel.BitField;
import net.sourceforge.usbdm.annotationEditor.AnnotationModel.Modifier;
import net.sourceforge.usbdm.annotationEditor.AnnotationModel.Range;

/**
 * Describes an option that modifies a numeric value in the C file
 * <pre>
 * //   &lt;o> External Reference Clock (Hz) <constant> <name=system_erc_clock>
 * //   &lt;i> Derived from the OSCCLK0 (external crystal or clock source on XTAL/EXTAL) or RTC_CLOCK(XTAL32/EXTAL32)
 * #define SYSTEM_ERC_CLOCK (8000000UL)
 * </pre>
 */
public class NumericOptionModelNode extends OptionModelNode {
   /** Range constraint on the node e.g. <b><i>&lt;64-32768:8></b></i> */
   private Range                    range       = null;
   
   /** List the modifiers applied e.g. <b><i>&lt;#/4></b></i>, <b><i>&lt;#*6></b></i>  */
   private ArrayList<Modifier>      modifiers   = null;
   
   /** Indicates the value should use hex notation e.g. 0x123 **/
   private boolean                  useHex      = false;
   
   /** Bit-field constraint applied to this node e.g. <b><i>&lt;o.4..5></b></i> */
   private BitField                 bitField    = null;
   
   /**
    * Constructor<br>
    * Represents a numeric node in the document
    * <pre>
    * //   &lt;o> External Reference Clock (Hz) <constant> <name=system_erc_clock>
    * //   &lt;i> Derived from the OSCCLK0 (external crystal or clock source on XTAL/EXTAL) or RTC_CLOCK(XTAL32/EXTAL32)
    * #define SYSTEM_ERC_CLOCK (8000000UL)
    * </pre>
    * 
    * @param description   Description
    * @param offset        Offset of numeric value 
    * @param bitField      
    */
   public NumericOptionModelNode(AnnotationModel annotationModel, String description, int offset, BitField bitField) {
      super(annotationModel, description, offset);
      this.bitField = bitField;
   }

   /**
    * Constructor from another node<br>
    * Represents a numeric node in the document
    * 
    * @param other      
    */
   public NumericOptionModelNode(NumericOptionModelNode other) {
      super(other);
      this.range              = other.range;
      this.modifiers          = other.modifiers;
      this.useHex             = other.useHex;
      this.bitField           = other.bitField;
   }

   @Override
   public void copyFrom(Object other) throws Exception {
      if (!(other instanceof NumericOptionModelNode)) {
         throw new Exception("Incompatible nodes in copyFrom()");
      }
      super.copyFrom(other);
      this.range              = ((NumericOptionModelNode)other).range;
      this.modifiers          = ((NumericOptionModelNode)other).modifiers;
      this.useHex             = ((NumericOptionModelNode)other).useHex;
      this.bitField           = ((NumericOptionModelNode)other).bitField;
   }
   
   @Override
   public boolean equals(Object other) {
      if (!(other instanceof NumericOptionModelNode)) {
         return false;
      }
      NumericOptionModelNode otherNode = (NumericOptionModelNode) other;
      return 
            ((range == otherNode.range)         || ((range != null)     && (range.equals(otherNode.range)))) &&
            ((modifiers == otherNode.modifiers) || ((modifiers != null) && (modifiers.equals(otherNode.modifiers)))) &&
            ((bitField == otherNode.bitField)   || ((bitField != null)  && (bitField.equals(otherNode.bitField)))) &&
            super.equals(otherNode);
   }

   @Override
   public String getToolTip() {
      String toolTip = super.getToolTip();
      if (getRange()!= null) {
         if (toolTip == null) {
            toolTip = "Range ";
         }
         else {
            toolTip += " ";
         }
         if (useHex) {
            toolTip += String.format("[0x%X..0x%X", getRange().getStart(), getRange().getEnd());
            if (getRange().getStep() != 1) {
               toolTip += String.format(", step 0x%X", getRange().getStep());
            }
            toolTip += ']';
         }
         else {
            toolTip += String.format("[%d..%d", getRange().getStart(), getRange().getEnd());
            if (getRange().getStep() != 1) {
               toolTip += String.format(", step %d", getRange().getStep());
            }
            toolTip += ']';
         }
      }
      return toolTip;
   }

   @Override
   public void setValue(Object value) throws Exception {
      if (value instanceof Boolean) {
         setValue(new Long(((Boolean)value)?1:0));
      }
      else {
         setValue((long)(Long)value);
      }
   }

   /**
    * Round value to step constraint
    * 
    * @param value Value to round
    * @return  Rounded value
    */
   private long roundValue(long value) {
      if (range != null) {
         value = value & ~(range.getStep()-1);
      }
      return value;
   }

   /**
    * Get mask for bit-field
    * 
    * @return mask
    */
   long getMask() {
      int width = bitField.getEnd()-bitField.getStart()+1;
      return ((1L<<width)-1);
   }
   
   /**
    * Returns the value rounded & limited to the acceptable range
    * 
    * @param value
    * @return modified value
    */
   public long limitedValue(long value) {
      value = roundValue(value);
      if (range != null) {
         if (value < range.getStart()) {
            value = range.getStart();
         }
         if (value > range.getEnd()) {
            value = range.getEnd();
         }
      }
      return value;
   }

   /**
    * Sets the value of the option
    * 
    * @param value
    * @throws Exception
    * 
    * @note The value is modified by modifying factors before being applied to the file
    */
   public void setValue(Long value) throws Exception {
      value = limitedValue(value);
      if (getModifiers() != null) {
         for (int index = 0; index<getModifiers().size(); index++) {
            Modifier m = getModifiers().get(index);
            if (m.operation.equals("/")) {
               value /= m.getFactor();
            }
            if (m.operation.equals("*")) {
               value *= m.getFactor();
            }
            if (m.operation.equals("+")) {
               value += m.getFactor();
            }
            if (m.operation.equals("-")) {
               value -= m.getFactor();
            }
         }
      }
      if (getBitField() != null) {
         long originalValue = 0;
         try {
            originalValue = getReference().getIntegerValue() & ~(getMask()<<getBitField().getStart());
         } catch (Exception e) {
            e.printStackTrace();
            return;
         }
         value = originalValue | ((value & getMask()) << getBitField().getStart());
      }
      annotationModel.getReferences().get(getReferenceIndex()).setIntegerValue(value);
//      notifyListeners();
   }

   @Override
   public void setValueFromDialogueString(String value) throws Exception {
//      System.err.println("NumericOptionModelNode.setValueAsString() value = "+ value + ", " + value.getClass());
      setValue((Long.decode(value) & 0xFFFFFFFFL));
   }

   /**
    * Gets the option value from the file
    * @throws Exception 
    * 
    * @note modifiers are applied when retrieving the value
    */
   public Object getValue() throws Exception {
      long value = getReference().getIntegerValue();
//      System.err.println(String.format("NumericOptionModelNode.getValue() => 0x%X", value));
      if (bitField != null) {
         value = (value>>bitField.getStart())&getMask();
//         System.err.println(String.format("NumericOptionModelNode.getValue() Masked=> 0x%X", value));
      }
      if (getModifiers() != null) {
         for (int index=getModifiers().size()-1; index>=0; index--) {
            Modifier m = getModifiers().get(index);
            if (m.operation.equals("/")) {
               value *= m.getFactor();
            }
            if (m.operation.equals("*")) {
               value /= m.getFactor();
            }
            if (m.operation.equals("+")) {
               value -= m.getFactor();
            }
            if (m.operation.equals("-")) {
               value += m.getFactor();
            }
         }
//         System.err.println(String.format("NumericOptionModelNode.getValue() Modified=> 0x%X", value));
      }
      return new Long(value);
   }

   /**
    * Get value as Long
    * 
    * @return
    */
   public long getValueAsLong() {
      try {
         Object value = getValue();
         if (value instanceof Boolean) {
            return (Boolean) value?1:0;
         }
         return (Long) value;
      } catch (Exception e) {
         e.printStackTrace();
      }
      return 0;
   }
   
   @Override
   public String getDialogueValueAsString() throws Exception {
      long value = safeGetValueAsLong();
      if ((bitField != null) && (bitField.getStart() == bitField.getEnd())) {
         return String.format("%d", value);
      }
      try {
         if (getReference().isUseHex()) {
            return String.format("0x%X", value);
         }
         else {
            return String.format("%d", value);
         }
      } catch (Exception e) {
         return "Illegal reference";
      }
   }

   public long safeGetValueAsLong() {
      try {
         Object value = getValue();
         if (value instanceof Boolean) {
            return ((Boolean)value)?1:0;
         }
         return (Long)value;
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      return 0;
   }

   /**
    * Get bitfield
    * 
    * @return
    */
   public BitField getBitField() {
      return bitField;
   }

   /**
    * Get range of node
    * 
    * @return Range that has been set
    */
   public Range getRange() {
      return range;
   }

   /**
    * Set range of node
    * 
    * @param range to set
    */
   public void setRange(Range range) {
      this.range = range;
   }

   /**
    * Get modifiers applied to this node
    * 
    * @return  List of modifiers
    */
   public ArrayList<Modifier> getModifiers() {
      return modifiers;
   }

   /**
    * Add modifier for this node
    * 
    * @param modifier Modifier to add
    */
   public void addModifier(Modifier modifier) {
      if (modifiers == null) {
         modifiers = new ArrayList<Modifier>();
      }
      modifiers.add(modifier);
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
      System.err.println(String.format("%s", getDescription()));
      String toolTip = getToolTip();
      if (toolTip != null) {
         System.err.println(AnnotationModel.getIndent(indent+3)+": "+ toolTip);
      }
   }

   /**
    * Set if number is to be represented in hex with 0x prefix
    * 
    * @param useHex
    */
   public void setUseHex(boolean useHex) {
      this.useHex = useHex;
   }
   
}
