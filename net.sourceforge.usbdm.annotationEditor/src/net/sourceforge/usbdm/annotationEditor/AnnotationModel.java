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
 */
public class AnnotationModel {

   public enum Severity {
      OK, INFORMATION, WARNING, ERROR;

      /**
       * Checks if the level is less than the given level
       * 
       * @param other
       * 
       * @return this&lt;other?
       */
      boolean lessThan(Severity other) {
         return this.ordinal() < other.ordinal();
      }

      /**
       * Checks if the level is greater than the given level
       * 
       * @param other
       * 
       * @return this&gt;other?
       */
      boolean greaterThan(Severity other) {
         return this.ordinal() > other.ordinal();
      }

   };

   /** The root of the model */
   private AnnotationModelNode                   modelRoot  = null;
   
   /** Index of references to strings or numbers in the associated document */
   private DocumentReferences                    references = null;
   
   /** The associated document*/
   private IDocument                             document   = null;
   
   /** Map from node names to node */
   private HashMap<String, AnnotationModelNode>  nameMap    = null;
   
   /** List of selection tags in document e.g. <b><i>&lt;selection=GPIOA_1_PIN_SEL,PTA1></b></i> **/
   private ArrayList<SelectionTag>               selectionTags = null;

   /**
    * Constructs a empty model to represent the Wizard description in a C file
    * 
    * @param document Associated document
    */
   public AnnotationModel(IDocument document) {
      resetModel(document);
   }
   
   public void resetModel(IDocument document) {
//      System.err.println("resetModel() resetting model");

      this.references      = new DocumentReferences();
      this.document        = document;
      this.modelRoot       = null;
      this.nameMap         = new HashMap<String, AnnotationModelNode>();
      this.selectionTags   = new ArrayList<SelectionTag>();
   }
   
   /**
    * Get selections applied to this node 
    * 
    * @return List of selections applied e.g. <b><i>&lt;selection=i2c0_sda,2></b></i>
    */
   public ArrayList<SelectionTag> getSelections() {
      return selectionTags;
   }

   /**
    * Add selection tag e.g. <b><i>&lt;selection=i2c0_sda,2></b></i>
    * 
    * @param selectionTag Tag to add
    */
   public void addSelection(SelectionTag selectionTag) {
      selectionTags.add(selectionTag); 
//      System.err.println("addSelection() - Adding " + selectionTag);
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
   
   public void clearScannedInformation() {
      this.nameMap = new HashMap<String, AnnotationModelNode>();
      this.selectionTags   = new ArrayList<SelectionTag>();
   }
   
   /**
    * Get the list of references to strings or numbers in the associated document
    * 
    * @return the references
    */
   public DocumentReferences getReferences() {
      return references;
   }

   /**
    * Represents a reference to a string or number in the associated document
    */
   public class DocumentReference {
      /** Offset in document */
      private final int     offset;
      /** Length in document */
      private final int     length;
      /** Indicates that numeric values should be represented in hex in the document e.g. 0x23 */
      private       boolean useHex;
      /** Name of this reference */
      private       String  name = null;

      /**
       * Create Document reference
       * 
       * @param offset  Offset in document
       * @param length  Length in document
       */
      DocumentReference(int offset, int length) {
         this.offset = offset;
         this.length = length;
         this.useHex = false;
      }

      /**
       * Sets the value of this reference in the associated document i.e. <br>
       * It changes the text in the document
       * 
       * @param value  Text to set
       * 
       * @throws Exception
       */
      void setValue(String value) throws Exception {
         try {
            document.replace(offset, length, value);
//            AnnotationParser.refreshPartitions(AnnotationModel.this);
         } catch (BadLocationException e) {
         }
      }

      /**
       * Gets the value of this reference in the associated document i.e.
       * it obtains the text from the document
       * 
       * @return Text from document
       * 
       * @throws Exception
       */
      String getValue() {
         try {
            return document.get(offset, length);
         } catch (BadLocationException e) {
            return "Invalid location";
         }
      }
      
      /**
       * Gets the value of this reference in the associated document i.e. 
       * it obtains the text from the document<br>
       * Assumes the text is a quoted string and trims the quotes e.g. "Hello" => Hello
       * 
       * @return Contents quoted string from document
       * 
       * @throws Exception
       */
      public String getStringValue() {
         return getValue().substring(1,getValue().length()-1);
      }

      /**
       * Gets the value of this reference in the associated document i.e. 
       * it obtains the text from the document<br>
       * Assumes the text is a number and returns it's numeric value e.g. "0x34" => 52
       * 
       * @return Value of number represented as a text in document
       * 
       * @note As a side-effect it sets hex format if the number is prefixed by "0x".  This is a persistent change
       * 
       * @throws Exception
       */
      public long getIntegerValue() {
         try {
            String t = getValue();
            if (t.startsWith("0x")) {
               setUseHex(true);
            }
            return Long.decode(t);
         } catch (NumberFormatException e) {
            System.err.println("");
            return 0;
         }
      }

      /**
       * Sets the value of this reference in the associated document i.e. 
       * it changes the text in the document<br>
       * Assumes the text is a quoted string and adds the quotes e.g. Hello => "Hello"
       * 
       * @param value   String to set
       * 
       * @throws Exception
       */
      public void setStringValue(String value) throws Exception {
         setValue("\""+value+"\"");
      }
      
      /**
       * Sets the value of this reference in the associated document i.e. 
       * it changes the text in the document<br>
       * Assumes the text is a number and writes it's numeric value as text<br>
       * If isUseHex() is true then it will be written as a hex number e.g. 53 => 0x34
       * 
       * @param value Number to set
       * 
       * @throws Exception
       */
      public void setIntegerValue(long value) throws Exception {
         if (isUseHex()) {
            setValue(String.format("0x%X", value));
         }
         else {
            setValue(String.format("%d", value));
         }
      }
      
      /**
       * Indicates if the number is to be represented in hex in the associated document
       * 
       * @return true indicates use hex prefix 0x
       */
      public boolean isUseHex() {
         return useHex;
      }

      /**
       * Indicates if the number is to be represented in hex in the associated document
       * 
       * @param useHex true to convert numbers to/from hex when modifying document
       */
      public void setUseHex(boolean useHex) {
         this.useHex = useHex;
      }
      
      @Override
      public String toString() {
         return String.format("Ref[%d,%d]", this.offset, this.length);
      }

      /**
       * Set name of reference for indexing
       * 
       * @param name
       */
      public void setName(String name) {
         this.name = name;
      }
      
      /**
       * Get name of reference
       * 
       * @return
       */
      public String getName() {
         return this.name;
      }
   }

   /**
    * Maintains index of references to strings or numbers in the associated document
    */
   public class DocumentReferences {
      
      /** Vector of all references */
      private Vector<DocumentReference> references = null;
      /** Map from name to reference */
      private HashMap<String, DocumentReference> map;
      
      /**
       * Constructs empty Reference list
       */
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
      
      /**
       * Obtain count of references to associated document
       * 
       * @return
       */
      public int getReferenceCount() {
//         System.err.println("DocumentReferences.getReferenceCount() => " + references.size());
         return references.size();
      }

      /**
       * Obtain reference at index to a string or number in the associated document
       * 
       * @param index
       * 
       * @return
       */
      public DocumentReference get(int index) {
         return references.get(index);
      }
   }
   
   /**
    * Represents an enumerated value in a document annotation e.g. <br>
    * <pre>
    * //     &lt0=> No setup (Reset default)
    * </pre>
    */
   public static interface EnumValue {
      public String getName();
      public void setName(String name);
   }
   
   /**
    * Represents an enumerated value in a document annotation e.g. <br>
    * <pre>
    * //     &lt0=> No setup (Reset default)
    * </pre>
    */
   public static class EnumTextValue implements EnumValue {
      /** Name e.g. <b><i>&lt"abc"=> No setup (Reset default)</b></i> => <b><i>"No setup (Reset default)"</b></i> */
      private String name;
      
      /** Value e.g. <b><i>&lt"abc"=> No setup (Reset default)</b></i> => <b><i>"abc"</b></i> */
      private String   value;

      /**
       * Constructor for an enumerated value in a document annotation e.g. <br>
       * <pre>
       * //     &lt"abc"=> No setup (Reset default)
       * </pre>
       * 
       * @param name e.g. <b><i>"No setup (Reset default)"</b></i>
       * @param value e.g. <b><i>"abc"</b></i> 
       */
      EnumTextValue(String name, String value) {
         this.setName(name);
         this.setValue(value);
      }

      /**
       * Get name of enumeration <br>
       * <pre>
       * //     &lt"abc"=> No setup (Reset default)
       * </pre>
       * 
       * @return Name e.g. <b><i>"No setup (Reset default)"</b></i>
       */
      @Override
      public String getName() {
         return name;
      }

      /**
       * Set name of enumeration <br>
       * <pre>
       * //     &lt"abc"=> No setup (Reset default)
       * </pre>
       * 
       * @param name e.g. <b><i>"No setup (Reset default)"</b></i>
       */
      public void setName(String name) {
         this.name = name;
      }

      /**
       * Get value of enumeration <br>
       * <pre>
       * //     &lt"abc"=> No setup (Reset default)
       * </pre>
       * 
       * @return Value e.g. <b><i>"abc"</b></i>
       */
      public String getValue() {
         return value;
      }

      /**
       * Set value of enumeration <br>
       * <pre>
       * //     &lt"abc"=> No setup (Reset default)
       * </pre>

       * 
       * @param value Value e.g. <b><i>"abc"</b></i>
       */
      public void setValue(String value) {
         this.value = value;
      }
   }

   /**
    * Represents an enumerated value in a document annotation e.g. <br>
    * <pre>
    * //     &lt0=> No setup (Reset default)
    * </pre>
    */
   public static class EnumNumericValue implements EnumValue {
      /** Name e.g. <b><i>&lt0=> No setup (Reset default)</b></i> => <b><i>"No setup (Reset default)"</b></i> */
      private String name;
      /** Value e.g. <b><i>&lt0=> No setup (Reset default)</b></i> => <b><i>0</b></i> */
      private long   value;

      /**
       * Constructor for an enumerated value in a document annotation e.g. <br>
       * <pre>
       * //     &lt0=> No setup (Reset default)
       * </pre>
       * 
       * @param name    Name e.g. <b><i>"No setup (Reset default)"</b></i>
       * @param value   Value e.g. <b><i>0</b></i>
       */
      EnumNumericValue(String name, long value) {
         this.setName(name);
         this.setValue(value);
      }

      /**
       * Get name of enumeration <br>
       * <pre>
       * //     &lt0=> No setup (Reset default)
       * </pre>
       * 
       * @return Name e.g. <b><i>"No setup (Reset default)"</b></i>
       */
      public String getName() {
         return name;
      }

      /**
       * Set name of enumeration <br>
       * <pre>
       * //     &lt0=> No setup (Reset default)
       * </pre>
       * 
       * @param name Name e.g. <b><i>No setup (Reset default)</b></i>
       */
      public void setName(String name) {
         this.name = name;
      }

      /**
       * Get value of enumeration <br>
       * <pre>
       * //     &lt0=> No setup (Reset default)
       * </pre>
       * 
       * @return Value e.g. <b><i>0</b></i>
       */
      public long getValue() {
         return value;
      }

      /**
       * Set value of enumeration <br>
       * <pre>
       * //     &lt0=> No setup (Reset default)
       * </pre>
       * 
       * @param value Value e.g. <b><i>0</b></i>
       */
      public void setValue(long value) {
         this.value = value;
      }
   }

   /**
    * Represents a modifier for a numeric value e.g. <b><i>&lt;#/4></b></i>, <b><i>&lt;#*6></b></i>
    */
   public static class Modifier {
      /** Operation to apply e.g. <b><i>&lt;#/4></b></i> => <b><i>/</b></i> */
      String operation;
      
      /** Factor to use with operation <b><i>&lt;#/4></b></i> => <b><i>4</b></i> */
      private long   factor;
      
      /**
       * Constructor for a modifier mark-up in the document e.g. <b><i>&lt;#/4></b></i> 
       * 
       * @param operation  Operation to apply e.g. <b><i>&lt;#/4></b></i> => <b><i>/</b></i>
       * @param factor     Factor to use with operation <b><i>&lt;#/4></b></i> => <b><i>4</b></i>
       */
      Modifier(String operation, long factor) {
         this.setOperation(operation);
         this.setFactor(factor);
      }
      /**
       * Get operation
       * 
       * @return Operation to apply e.g. <b><i>&lt;#/4></b></i> => <b><i>/</b></i>
       */
      public String getOperation() {
         return operation;
      }
      /**
       * Set operation 
       * 
       * @param operation Operation to apply e.g. <b><i>&lt;#/4></b></i> => <b><i>/</b></i>
       */
      public void setOperation(String operation) {
         this.operation = operation;
      }
      /**
       * Get factor
       * 
       * @return Factor to use with operation <b><i>&lt;#/4></b></i> => <b><i>4</b></i>
       */
      public long getFactor() {
         return factor;
      }
      /**
       * Set factor
       * 
       * @param factor Factor to use with operation <b><i>&lt;#/4><b><i> => <b><i>4</b></i>
       */
      public void setFactor(long factor) {
         this.factor = factor;
      }
   }

   /**
    * Represent a selection annotation in the document e.g. <b><i>&lt;selection=GPIOA_1_PIN_SEL,PTA1></b></i>
    */
   public static class SelectionTag {
      /** Associated node that represents the pin that can be mapped to this peripheral signal */ 
      public final EnumeratedNumericOptionModelNode controllingNode;
      
      /** Name of peripheral signal mapped to this node(pin) by this selection */
      public final String           signalName;
      
      /** The index of the option that enables this peripheral function on the associated node(pin) */
      public final long             selectionValue;

      /** Selection value for the signal node e.g. &lt;selection=GPIOA_1_PIN_SEL,PTA1> => PTA1<br> */
      public final String          signalValue;

      /**
       * Constructor<br>
       * 
       * Represent a selection annotation in the document e.g. <b><i>&lt;selection=GPIOA_1_PIN_SEL,PTA1></b></i>
       * 
       * @param controllingNode  The node associated with the SelectionTag (pin that is controlling node)
       * @param selectionValue   The value of the option that enables this signal on this pin
       * @param signalName       Name of signal being controller e.g. &lt;selection=GPIOA_1_PIN_SEL,PTA1> => GPIOA_1_PIN_SEL<br>
       *                         This is expected to be the name of another node representing the signal
       * @param signalValue      Selection value for the signal node e.g. &lt;selection=GPIOA_1_PIN_SEL,PTA1> => PTA1<br>
       *                         This is expected to be a valid selection for the signal node
       */
      SelectionTag(EnumeratedNumericOptionModelNode controllingNode, long selectionValue, String signalName, String signalValue) {
         this.controllingNode = controllingNode;
         this.selectionValue  = selectionValue;
         this.signalName      = signalName;
         this.signalValue     = signalValue;
      }
      
      @Override
      public String toString() {
         return String.format("SelectionTag(controllingNode=%s, sel=%d, signalName=%s, value=%s)", 
               controllingNode.getName(), selectionValue, signalName, signalValue);
      }
   };

   /**
    * Represents a Bit-field part of mark-up in the document e.g. <b><i>&lt;o.4..5></b></i>
    */
   public static class BitField {
      /** Start of bit-field range */
      private final int start;
      /** End of bit-field range */
      private final int end;
      
      /**
       * Constructor
       * 
       * @param start Start of bit-field span e.g. <b><i>&lt;o.4..5></b></i> => <b><i>4</b></i>
       * @param end   End of bit-field span e.g. <b><i>&lt;o.4..5></b></i> => <b><i>5</b></i>
       */
      BitField(int start, int end) {
         this.start = start;
         this.end   = end;
      }

      /**
       * Get start of span
       * 
       * @return Start of bit-field span e.g. <b><i>&lt;o.4..5></b></i> => <b><i>4</b></i>
       */
      public int getStart() {
         return start;
      }

      /**
       * Get end of span
       * 
       * @return Start of bit-field span e.g. <b><i>&lt;o.4..5></b></i> => <b><i>5</b></i>
       */
      public int getEnd() {
         return end;
      }
   }
   
   /**
    * Represents a range constraint on the node e.g. <b><i>&lt;64-32768:8></b></i>
    */
   public static class Range {
      /** Inclusive start of permitted range <b><i>&lt;64-32768:8></b></i> => <b><i>64</b></i> */
      private long start;
      /** Inclusive end of permitted range <b><i>&lt;64-32768:8></b></i> => <b><i>32768</b></i> */
      private long end;
      /** Value must be a multiple of this step size <b><i>&lt;64-32768:8></b></i> => <b><i>8</b></i> */
      private long step;

      /**
       * Constructor
       * 
       * @param start   Inclusive start of permitted range <b><i>&lt;64-32768:8></b></i> => <b><i>64</b></i>
       * @param end     Inclusive end of permitted range <b><i>&lt;64-32768:8></b></i> => <b><i>32768</b></i>
       * @param step    Value must be a multiple of this step size <b><i>&lt;64-32768:8></b></i> => <b><i>8</b></i>
       */
      Range(long start, long end, long step) {
         this.start = start;
         this.end   = end;
         this.step  = step;
      }

      /**
       * Get start of range 
       * 
       * @return Inclusive start of permitted range <b><i>&lt;64-32768:8></b></i> => <b><i>64</b></i>
       */
      public long getStart() {
         return start;
      }

      /**
       * Set start of range 
       * 
       * @param start Inclusive start of permitted range <b><i>&lt;64-32768:8></b></i> => <b><i>64</b></i>
       */
      public void setStart(long start) {
         this.start = start;
      }

      /**
       * Get end of range
       * 
       * @return Inclusive end of permitted range <b><i>&lt;64-32768:8></b></i> => <b><i>32768</b></i>
       */
      public long getEnd() {
         return end;
      }

      /**
       * Set end of range
       * 
       * @param end Inclusive end of permitted range <b><i>&lt;64-32768:8></b></i> => <b><i>32768</b></i>
       */
      public void setEnd(long end) {
         this.end = end;
      }

      /**
       * Get step size
       * 
       * @return Value must be a multiple of this step size <b><i>&lt;64-32768:8></b></i> => <b><i>8</b></i>
       */
      public long getStep() {
         return step;
      }

      /**
       * Set step size
       * 
       * @param step    Value must be a multiple of this step size <b><i>&lt;64-32768:8></b></i> => <b><i>8</b></i>
       */
      public void setStep(long step) {
         this.step = step;
      }
   }



   


   static enum Polarity {
      ACTIVE_HIGH,
      ACTIVE_LOW;
      
      /**
       * Interprets the value taking into account the polarity
       * 
       * @param value
       * 
       * @return true/false interpretation
       */
      boolean apply(boolean value) {
         switch (this) {
         case ACTIVE_HIGH:
            return value;
         case ACTIVE_LOW:
            return !value;
         }
         return false;
      }
      /**
       * Interprets the value taking into account the polarity
       * 
       * @param value
       * 
       * @return true/false interpretation
       */
      boolean apply(int value) {
         switch (this) {
         case ACTIVE_HIGH:
            return value != 0;
         case ACTIVE_LOW:
            return value == 0;
         }
         return false;
      }
   };
   

   /**
    * Describes a simple heading used to group options in the model<br>
    * The option has a binary value that can be used to enable/disable the group
    * <pre>
    *  // &lt;e> Configuration of FTM0
    *  // &lt;0=> Disabled
    *  // &lt;1=> Enabled
    * </pre>
    */
   public class OptionHeadingModelNode extends BinaryOptionModelNode {

      /**
       * Constructor<br>
       * Represent a simple heading used to group options in the model
       * <pre>
       *  // &lt;e> Configuration of FTM0
       *  // &lt;0=> Disabled
       *  // &lt;1=> Enabled
       * </pre>
       * @param description   Description
       * @param offset        Offset of numeric value 
       * @param bitField      
       */
      public OptionHeadingModelNode(AnnotationModel annotationModel, String description, int offset, BitField bitField) throws Exception {
         super(annotationModel, description, offset, bitField);
      }
      
      @Override
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
         child.setEnabled(isEnabled() && getPolarity().apply(safeGetValue()));
      }

      @Override
      public void setValue(Object value) throws Exception {
         super.setValue(value);
      }
      
      @Override
      public Object getValue() throws Exception {
         Boolean value = (Boolean) super.getValue();
         setChildrenEnabled(isEnabled() && getPolarity().apply(value));
         return value;
      }
   }


   private final static String INDENT_STRING = "                                                                         ";

   /**
    * Obtain a string of spaces of the given length
    * 
    * @param indent
    * @return
    */
   public static String getIndent(int indent) {
      if (indent>INDENT_STRING.length()) {
         indent = INDENT_STRING.length();
      }
      return INDENT_STRING.substring(0, indent);
   }
}

