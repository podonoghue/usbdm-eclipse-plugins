package net.sourceforge.usbdm.annotationEditor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

/**
 * Used to represent a model created from the Wizard description in a C file
 * 
 * @author podonoghue
 *
 */
public class AnnotationModel {

   /**
    * The root of the model
    */
   private AnnotationModelNode                   modelRoot  = null;
   private DocumentReferences                    references = null;
   private IDocument                             document   = null;
   private HashMap<String, AnnotationModelNode>  nameMap    = null;

   public AnnotationModel(IDocument document) {
      resetModel(document);
   }
   
   public void resetModel(IDocument document) {
      this.references = new DocumentReferences();
      this.document   = document;
      this.modelRoot  = null;
      this.nameMap    = new HashMap<String, AnnotationModelNode>();
   }
   
   public IDocument getDocument() {
      return document;
   }
   
   public AnnotationModelNode getModelRoot() {
      return modelRoot;
   }

   public void setModelRoot(AnnotationModelNode modelRoot) {
      this.modelRoot = modelRoot;
   }
   
   public AnnotationModelNode getModelNode(String name) {
      AnnotationModelNode t = nameMap.get(name);
      return t;
   }

   public void addNamedModelNode(String name, AnnotationModelNode node) {
      this.nameMap.put(name, node);
   }
   
   public HashMap<String, AnnotationModelNode> getNameMap() {
      return this.nameMap;
   }
   
   public void clearNameMap() {
      this.nameMap = new HashMap<String, AnnotationModel.AnnotationModelNode>();
   }
   
   /**
    * Get the list of references 
    * 
    * @return the references
    */
   public DocumentReferences getReferences() {
      return references;
   }

   public class DocumentReference {
      private final int     offset;
      private final int     length;
      private       boolean useHex;
      private       String  name = null;

      DocumentReference(int offset, int length) {
         this.offset = offset;
         this.length = length;
         this.useHex = false;
      }

      private void setValue(String value) throws Exception {
         try {
            document.replace(offset, length, value);
//            AnnotationParser.refreshPartitions(AnnotationModel.this);
         } catch (BadLocationException e) {
         }
      }

      private String getValue() {
         try {
            return document.get(offset, length);
         } catch (BadLocationException e) {
            return "Invalid location";
         }
      }
      
      public String getStringValue() {
         return getValue().substring(1,getValue().length()-1);
      }

      public long getIntegerValue() {
         try {
            String t = getValue();
            if (t.startsWith("0x")) {
               setUseHex(true);
            }
            return Long.decode(t);
         } catch (NumberFormatException e) {
            return 0;
         }
      }

      public void setStringValue(String value) throws Exception {
         setValue("\""+value+"\"");
      }
      
      public void setIntegerValue(long value) throws Exception {
         if (isUseHex()) {
            setValue(String.format("0x%X", value));
         }
         else {
            setValue(String.format("%d", value));
         }
      }
      
      public boolean isUseHex() {
         return useHex;
      }

      public void setUseHex(boolean useHex) {
         this.useHex = useHex;
      }
      
      public String toString() {
         return String.format("Ref[%d,%d]", this.offset, this.length);
      }

      public void setName(String name) {
         this.name = name;
      }
      
      public String getName() {
         return this.name;
      }
   }

   public class DocumentReferences {
      
      private Vector<DocumentReference> references = null;
      private HashMap<String, DocumentReference> map;
      
      public DocumentReferences() {
         references = new Vector<DocumentReference>();
      }
      
      /**
       * Clears the list of references 
       * 
       * @return the references
       */
      public void clearReferences() {
//         System.err.println("DocumentReferences.clearReferences()");
         references.clear();
      }

      /**
       * Adds a reference to a string or number in the C file
       * 
       * @param Reference to add
       */
      public void addReference(DocumentReference reference) {
//         System.err.println("DocumentReferences.addReference("+reference.toString()+")");
         this.references.add(reference);
         if (reference.getName() != null) {
            this.map.put(reference.getName(), reference);
         }
      }
      
      public int getReferenceCount() {
//         System.err.println("DocumentReferences.getReferenceCount() => " + references.size());
         return references.size();
      }

      public DocumentReference get(int index) {
         return references.get(index);
      }
   }
   
   public static class EnumValue {
      private String name;
      private long   value;

      EnumValue(String name, long value) {
         this.setName(name);
         this.setValue(value);
      }

      public String getName() {
         return name;
      }

      public void setName(String name) {
         this.name = name;
      }

      public long getValue() {
         return value;
      }

      public void setValue(long value) {
         this.value = value;
      }
   }

   public static class Modifier {
      private String operation;
      private long   factor;
      Modifier(String operation, long factor) {
         this.setOperation(operation);
         this.setFactor(factor);
      }
      public String getOperation() {
         return operation;
      }
      public void setOperation(String operation) {
         this.operation = operation;
      }
      public long getFactor() {
         return factor;
      }
      public void setFactor(long factor) {
         this.factor = factor;
      }
   }

   public static class BitField {
      private final int start;
      private final int end;
      
      BitField(int start, int end) {
         this.start = start;
         this.end   = end;
      }

      public int getStart() {
         return start;
      }

      public int getEnd() {
         return end;
      }
   }
   
   public static class Range {
      private long start;
      private long end;
      private long step;

      Range(long start, long end, long step) {
         this.start = start;
         this.end   = end;
         this.step  = step;
      }

      public long getStart() {
         return start;
      }

      public void setStart(long start) {
         this.start = start;
      }

      public long getEnd() {
         return end;
      }

      public void setEnd(long end) {
         this.end = end;
      }

      public long getStep() {
         return step;
      }

      public void setStep(long step) {
         this.step = step;
      }
   }

   /**
    * Generic node in the model
    */
   public class AnnotationModelNode {
      private String                            description    = null;
      private String                            toolTip        = null;
      private AnnotationModelNode               parent         = null;
      private ArrayList<AnnotationModelNode>    children       = new ArrayList<AnnotationModelNode>();
      private boolean                           enabled        = false;
      private boolean                           modifiable     = true;
      private String                            name           = null;
//      private ArrayList<MyValidator>            listeners   = new ArrayList<MyValidator>();
      private String                            errorMessage   = null;
      
      public AnnotationModelNode(String description) {
         this.setDescription(description);
         this.toolTip    = null;
         this.parent     = null;
         this.enabled    = true;
         this.modifiable = false;
      }

      public AnnotationModelNode(AnnotationModelNode other) {
         this.description = other.description;
         this.toolTip     = other.toolTip;
         this.parent      = other.parent;
         this.children    = other.children;
         this.enabled     = other.enabled;
         this.modifiable  = other.modifiable;
         this.name        = other.name;
      }

      public void copyFrom(Object other) throws Exception {
         if (!(other instanceof AnnotationModelNode)) {
            throw new Exception("Incompatible nodes in copyFrom()");
         }
         this.description = ((AnnotationModelNode)other).description;
         this.toolTip     = ((AnnotationModelNode)other).toolTip;
         this.enabled     = ((AnnotationModelNode)other).enabled;
         this.modifiable  = ((AnnotationModelNode)other).modifiable;
         this.name        = ((AnnotationModelNode)other).name;
      }
      
      @Override
      public boolean equals(Object other) {
         if (!(other instanceof AnnotationModelNode)) {
            return false;
         }
         AnnotationModelNode otherNode = (AnnotationModelNode) other;
         boolean res = 
               (other instanceof AnnotationModelNode) &&
               ((description == otherNode.description) || ((description != null) && (description.equals(otherNode.description)))) &&
               ((toolTip == otherNode.toolTip)         || ((toolTip != null)     && (toolTip.equals(otherNode.toolTip)))) &&
               ((name == otherNode.name)               || ((name != null)        && name.equals(otherNode.name))) &&
               (enabled == otherNode.enabled) &&
               (modifiable == otherNode.modifiable);
         if (!res) {
            return false;
         }
         if (children == otherNode.children) {
            return true;
         }
         if ((children == null) || (otherNode.children == null)) {
            return false;
         }
         for (int index = 0; index< children.size(); index++) {
            if (!children.get(index).equals(otherNode.children.get(index))) {
               return false;
            }
         }
         return true;
      }
      
      public String getDescription() {
         return description;
      }

      public void setDescription(String description) {
         this.description = description;
      }

      public ArrayList<AnnotationModelNode> getChildren() {
         return children;
      }

      public void setChildren(ArrayList<AnnotationModelNode> children) {
         this.children = children;
      }

      public void addChild(AnnotationModelNode child) {
         children.add(child);
         child.setParent(this);
      }

      public void removeAllChildren() {
         children = new ArrayList<AnnotationModelNode>();
      }

      public void removeChild(AnnotationModelNode child) {
         child.setParent(null);
         children.remove(child);
      }

      public AnnotationModelNode getParent() {
         return parent;
      }

      public void setParent(AnnotationModelNode parent) {
         this.parent = parent; 
      }

      public String getToolTip() {
         if (errorMessage != null) {
            return errorMessage;
         }
         return toolTip;
      }

      public void addToolTip(String toolTip) {
         if (this.toolTip == null) {
            this.toolTip = toolTip;
         }
         else {
            this.toolTip = this.toolTip + "\n" + toolTip;
         }
      }

      /**
       * Returns the value of this node as a string suitable for display in a dialogue. <br>
       * This might be:
       *    <li> a number as an integer in a preferred form e.g. <em>"0x1234"</em>
       *    <li> an enumerated type name e.g. <em>"high"</em>
       *    <li> a boolean value as <em>"true"<em>/</em>"false"</em> or other two-valued scheme
       *    <li> a string that is the value of the node
       *    
       * @return string as above
       * 
       * @throws Exception
       */
      public String getValueAsString() throws Exception {
         return "";
      }

      /**
       * Sets the value of this node from a string suitable for display in a dialogue<br>
       * This might be:
       *    <li> a number as an integer in a preferred form e.g. <em>"0x1234"</em>
       *    <li> an enumerated type name e.g. <em>"high"</em>
       *    <li> a boolean value as <em>"true"<em>/</em>"false"</em> or other two-valued scheme
       *    <li> a string that is the value of the node
       *    
       * @param value value as above
       * 
       * @throws Exception
       */
      public void setValueAsString(String value) throws Exception {
      }

      /**
       * Returns the value of a node in a node dependent form <br>
       * This might be:
       *    <li> a number as an integer
       *    <li> an enumerated type name e.g. <em>high</em>
       *    <li> a boolean value
       *    <li> a string that is the value of the node
       * 
       * @return value as above or null if no value is associated with the node
       * 
       * @throws Exception
       */
      public Object getValue() throws Exception {
         return null;
      }

      /**
       * Sets the value of a node in a node dependent form <br>
       * This might be:
       *    <li> a number as an integer
       *    <li> an number that corresponds to an enumerated type
       *    <li> a boolean value as true/false
       *    <li> a string that is the value of the node
       * 
       * @param obj The object used to set the node value
       * 
       * @throws Exception obj in incorrect type etc
       */
      public void setValue(Object obj) throws Exception {
      }

      public boolean canModify() {
         return isModifiable() && isEnabled();
      }

      public boolean isModifiable() {
         return modifiable;
      }

      public void setModifiable(boolean modifiable) {
         this.modifiable = modifiable;
      }

      public boolean isEnabled() {
         return enabled;
      }

      /**
       * Sets this node and children of this node as enabled or disabled
       * 
       * @param enabled
       */
      public void setEnabled(boolean enabled) {
         this.enabled = enabled;
         setChildrenEnabled(enabled);
      }

      /**
       * Sets children of this node as enabled or disabled
       * 
       * @param enabled
       */
      public void setChildrenEnabled(boolean enabled) {
         for (AnnotationModelNode child : children) {
            child.setEnabled(enabled);
         }
      }

      public void listNode(int indent) {         
         System.err.println(getIndent(indent)+getDescription());
         String toolTip = getToolTip();
         if (toolTip != null) {
            System.err.println(getIndent(indent+3)+": "+ toolTip);
         }
      }

      public void listNodes(int indent) {
         listNode(indent);
         for (AnnotationModelNode child : children) {
            child.listNodes(indent+3);
         }
      }

      public boolean isOKValue(Object value) {
         return false;
      }

//      public void clearListeners() {
//         listeners = new ArrayList<MyValidator>();
//      }
//
//      public void addListener(MyValidator function) {
//         listeners.add(function);
////         System.err.println("AnnotationModel.addListener()");
//      }
      
//      protected void notifyListeners() throws Exception {
//         for (MyValidator function : listeners) {
//            function.changed();
//         }
//      }

      public void setName(String name) {
         this.name = name;
      }

      public String getName() {
         return name;
      }

      public boolean isValid() {
         return errorMessage == null;
      }

      public void setErrorMessage(String errorMessage) {
         this.errorMessage = errorMessage;
      }
   }

   class ErrorNode extends AnnotationModelNode {

      public ErrorNode(String description) {
         super(description);
      }
   }
   
   /**
    * Describes a simple heading used to group options in the model
    * The node has no value associated with it.
    */
   class HeadingModelNode extends AnnotationModelNode {
      
      public HeadingModelNode(String description) {
         super(description);
      }
      
      @Override
      public void copyFrom(Object other) throws Exception {
         if (!(other instanceof HeadingModelNode)) {
            throw new Exception("Incompatible nodes in copyFrom()");
         }
         super.copyFrom(other);
      }
      
      public boolean equals(Object other) {
         return (other instanceof HeadingModelNode) && super.equals(other);
      }
   }

   /**
    * Describes an option that modifies a value in the C file
    * The node has
    *    <li> a value (untyped represented as a string)
    *    <li> an reference index allowing the location in the C file to be accessed.
    */
   class OptionModelNode extends AnnotationModelNode {
      private int    referenceIndex;

      public OptionModelNode(String description, int offset) {
         super(description);
         this.referenceIndex = references.getReferenceCount()+offset;
         setModifiable(true);
      }

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
      
      public boolean equals(Object other) {
         if (!(other instanceof OptionModelNode)) {
            return false;
         }
         OptionModelNode otherNode = (OptionModelNode) other;
         return (referenceIndex == otherNode.referenceIndex) && super.equals(otherNode);
      }

      int getReferenceIndex() {
         return referenceIndex;
      }

      @Override
      public void listNode(int indent) {
         System.err.print(getIndent(indent)+String.format("r=%d:", getReferenceIndex()));
         System.err.println(String.format("%s", getDescription()));
         String toolTip = getToolTip();
         if (toolTip != null) {
            System.err.println(getIndent(indent+3)+": "+ toolTip);
         }
      }
      
      @Override
      public Object getValue() throws Exception {
         return getReference().getValue();
      }
      
      @Override
      public String getValueAsString() throws Exception {
            return getReference().getStringValue();
      }
      
      public DocumentReference getReference() throws Exception {
         try {
            return references.get(getReferenceIndex());
         } catch (Exception e) {
            throw new Exception("Illegal Reference @"+getReferenceIndex(), e);
         }
      }
      public DocumentReference getReference(int offset) throws Exception {
         try {
            return references.get(getReferenceIndex()+offset);
         } catch (Exception e) {
            throw new Exception("Illegal Reference @"+getReferenceIndex(), e);
         }
      }
   }

   /**
    * Describes an option that modifies a numeric value in the C file
    *
    */
   public class NumericOptionModelNode extends OptionModelNode {
      private Range                    range       = null;
      private ArrayList<Modifier>      modifiers   = null;
      private boolean                  useHex      = false;
      private BitField                 bitField    = null;

      public NumericOptionModelNode(String description, int offset, BitField bitField) {
         super(description, offset);
         this.bitField = bitField;
      }

      public NumericOptionModelNode(NumericOptionModelNode other) {
         super(other);
         this.range              = other.range;
         this.modifiers          = other.modifiers;
         this.useHex             = other.useHex;
         this.bitField           = other.bitField;
      }

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
         setValue((long)(Long)value);
      }

      private long roundValue(long value) {
         if (range != null) {
            value = value & ~(range.getStep()-1);
         }
         return value;
      }

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
       * 
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
         references.get(getReferenceIndex()).setIntegerValue(value);
//         notifyListeners();
      }

      @Override
      public void setValueAsString(String value) throws Exception {
//         System.err.println("NumericOptionModelNode.setValueAsString() value = "+ value + ", " + value.getClass());
         setValue((Long.decode(value) & 0xFFFFFFFFL));
      }

      /**
       * Gets the option value from the file
       * 
       * @note modifiers are applied when retrieving the value
       */
      public Object getValue() throws Exception {
         long value = getReference().getIntegerValue();
//         System.err.println(String.format("NumericOptionModelNode.getValue() => 0x%X", value));
         if (bitField != null) {
            value = (value>>bitField.getStart())&getMask();
//            System.err.println(String.format("NumericOptionModelNode.getValue() Masked=> 0x%X", value));
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
//            System.err.println(String.format("NumericOptionModelNode.getValue() Modified=> 0x%X", value));
         }
         return new Long(value);
      }

      public long getValueAsLong() {
         try {
            return (Long) getValue();
         } catch (Exception e) {
            e.printStackTrace();
         }
         return 0;
      }
      
      @Override
      public String getValueAsString() throws Exception {
         long value = (Long)getValue();
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

      public BitField getBitField() {
         return bitField;
      }

      public Range getRange() {
         return range;
      }

      public void setRange(Range range) {
         this.range = range;
      }

      public ArrayList<Modifier> getModifiers() {
         return modifiers;
      }

      public void addModifier(Modifier modifier) {
         if (modifiers == null) {
            modifiers = new ArrayList<Modifier>();
         }
         modifiers.add(modifier);
      }

      @Override
      public void listNode(int indent) {
         System.err.print(getIndent(indent)+String.format("o=%d", getReferenceIndex()));
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
            System.err.println(getIndent(indent+3)+": "+ toolTip);
         }
      }

      public void setUseHex(boolean useHex) {
         this.useHex = useHex;
      }
   }

   public class PllConfigurationModelNode extends NumericOptionModelNode {
      public PllConfigurationModelNode(String description, int offset, BitField bitField) {
         super(description, offset, null);
      }

      public PllConfigurationModelNode(PllConfigurationModelNode other) {
         super(other);
      }

      public void copyFrom(Object other) throws Exception {
         if (!(other instanceof PllConfigurationModelNode)) {
            throw new Exception("Incompatible nodes in copyFrom()");
         }
         super.copyFrom(other);
      }
      
      public boolean equals(Object other) {
         if (!(other instanceof PllConfigurationModelNode)) {
            return false;
         }
         PllConfigurationModelNode otherNode = (PllConfigurationModelNode) other;
         return super.equals(otherNode);
      }

      /* (non-Javadoc)
       * @see net.sourceforge.usbdm.annotationEditor.AnnotationModel.NumericOptionModelNode#setValue(java.lang.Long)
       */
      @Override
      public void setValue(Long value) throws Exception {
         super.setValue(value);
      }

   }
   
   /**
    * Describes an option that modifies a numeric value in the C file with an enumerated set of values
    */
   public class EnumeratedOptionModelNode extends NumericOptionModelNode {
      private ArrayList<EnumValue>     enumerationValues;

      public EnumeratedOptionModelNode(String description, int offset, BitField bitField) {
         super(description, offset, bitField);
         setModifiable(true);
      }

      public EnumeratedOptionModelNode(NumericOptionModelNode other) {
         super(other);
      }

      @Override
      public void copyFrom(Object other) throws Exception {
         if (!(other instanceof EnumeratedOptionModelNode)) {
            throw new Exception("Incompatible nodes in copyFrom()");
         }
         super.copyFrom(other);
         this.enumerationValues = ((EnumeratedOptionModelNode)other).enumerationValues;
      }
      
      public boolean equals(Object other) {
         if (!(other instanceof EnumeratedOptionModelNode)) {
            return false;
         }
         EnumeratedOptionModelNode otherNode = (EnumeratedOptionModelNode) other;
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
      public String getValueAsString() throws Exception {
         long value = (Long) getValue();
         for (int index=0; index<enumerationValues.size(); index++) {
            EnumValue enumValue = enumerationValues.get(index);
            if (enumValue.getValue() == value) {
//               System.err.println(String.format("getValueAsString() Found (0x%X => %s) ", value, enumValue.getName()));
               return enumValue.getName();
            }
         }
         System.err.println(String.format("getValueAsString() Didn't find (0x%X => %s) ", value, enumerationValues.get(0).getName()));
         return enumerationValues.get(0).getName();
      }
      
      @Override
      public void setValueAsString(String name) throws Exception {
         for (int index=0; index<enumerationValues.size(); index++) {
            EnumValue enumValue = enumerationValues.get(index);
            if (enumValue.getName() == name) {
               super.setValue(enumValue.getValue());
               return;
            }
         }
      }

      public ArrayList<EnumValue> getEnumerationValues() {
         return enumerationValues;
      }

      public void addEnumerationValue(EnumValue enumerationValue) {
         if (enumerationValues == null) {
            enumerationValues = new ArrayList<EnumValue>();
         }
         this.enumerationValues.add(enumerationValue);
      }

      @Override
      public void listNode(int indent) {
         System.err.print(getIndent(indent)+String.format("o=%d", getReferenceIndex()));
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
            for (EnumValue e : enumerationValues) {
               System.err.print(String.format("(%s=>0x%X)", e.getName(), e.getValue()));
            }
         }
         System.err.println(String.format("%s", getDescription()));
         String toolTip = getToolTip();
         if (toolTip != null) {
            System.err.println(getIndent(indent+3)+": "+ toolTip);
         }
      }
   }

   /**
    * Represents a two value option
    */
   public class BinaryOptionModelNode extends NumericOptionModelNode {

      String   falseValueText = "false";
      String   trueValueText  = "true";
      
      public BinaryOptionModelNode(String description, int offset, BitField bitField) throws Exception {
         super(description, offset, bitField);
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
      }
      
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
      public String getValueAsString() throws Exception {
         return ((Boolean)getValue())?trueValueText:falseValueText;
      }

      public void addEnumerationValue(EnumValue enumValue) {
         if (enumValue.getValue() == 0) {
            falseValueText = enumValue.getName();
         }
         else {
            trueValueText = enumValue.getName();
         }
      }

      @Override
      public void setValue(Object value) throws Exception {
         if (!(value instanceof Boolean)) {
            throw new RuntimeException("Illegal type in setValue(): " + value.getClass());
         }
         super.setValue(new Long(((Boolean)value)?1:0));
      }

      @Override
      public Object getValue() throws Exception {
         return new Boolean(((Long)super.getValue()) != 0);
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
      
   }

   /**
    * Describes a simple heading used to group options in the model
    * 
    * The option has a binary value that can be used to enable/disable the group
    */
   public class OptionHeadingModelNode extends BinaryOptionModelNode {

      public OptionHeadingModelNode(String description, int offset, BitField bitField) throws Exception {
         super(description, offset, bitField);
      }
      
      public boolean equals(Object other) {
         if (!(other instanceof OptionHeadingModelNode)) {
            return false;
         }
         return super.equals(other);
      }

      @Override
      public void copyFrom(Object other) throws Exception {
         if (!(other instanceof OptionHeadingModelNode)) {
            throw new Exception("Incompatible nodes in copyFrom()");
         }
         super.copyFrom(other);
      }

      @Override
      public void addChild(AnnotationModelNode child) {
         super.addChild(child);
         child.setEnabled(isEnabled() && safeGetValue());
      }

      @Override
      public void setValue(Object value) throws Exception {
         super.setValue(value);
      }
      
      @Override
      public Object getValue() throws Exception {
         Boolean value = (Boolean) super.getValue();
         setChildrenEnabled(isEnabled() && value);
         return value;
      }
   }

   /**
    * Describes an option that modifies a numeric value in the C file with an enumerated set of values
    */
   public class StringOptionModelNode extends OptionModelNode {

      int characterLimit;

      public StringOptionModelNode(String description, int offset, int characterLimit) {
         super(description, offset);
         this.characterLimit = characterLimit;
      }

      public boolean equals(Object other) {
         if (!(other instanceof StringOptionModelNode)) {
            return false;
         }
         StringOptionModelNode otherNode = (StringOptionModelNode) other;
         return
               (characterLimit == otherNode.characterLimit) &&
               super.equals(otherNode);
      }

      public void copyFrom(StringOptionModelNode other) throws Exception {
         if (!(other instanceof StringOptionModelNode)) {
            throw new Exception("Incompatible nodes in copyFrom()");
         }
         super.copyFrom(other);
         this.characterLimit = other.characterLimit;
      }
      
      @Override
      public String getValueAsString() {
         try {
            return references.get(getReferenceIndex()).getStringValue();
         } catch (Exception e) {
            return e.getMessage();
         }
      }

      @Override
      public Object getValue() {
         return getValueAsString();
      }

      @Override
      public void setValue(Object value) {
         try {
            references.get(getReferenceIndex()).setValue((String) value);
//            AnnotationParser.refreshPartitions(AnnotationModel.this);
         } catch (Exception e) {
         }
      }

      @Override
      public void setValueAsString(String value) {
         try {
            references.get(getReferenceIndex()).setStringValue(value);
//            AnnotationParser.refreshPartitions(AnnotationModel.this);
         } catch (Exception e) {
         }
      }

      @Override
      public void listNode(int indent) {
         System.err.print(getIndent(indent)+String.format("o=%d", getReferenceIndex()));
         if (characterLimit > 0) {
            System.err.print(String.format("[%d]", characterLimit));
         }
         System.err.println(String.format(": %s", getDescription()));
         String toolTip = getToolTip();
         if (toolTip != null) {
            System.err.println(getIndent(indent+3)+": "+ toolTip);
         }
      }
   }

   private final static String indentString = "                                                                         ";

   public static String getIndent(int indent) {
      return indentString.substring(0, indent);
   }
}

