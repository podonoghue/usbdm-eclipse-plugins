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
      this.nameMap = new HashMap<String, AnnotationModel.AnnotationModelNode>();
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
      private void setValue(String value) throws Exception {
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
      private String getValue() {
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
   public static class EnumValue {
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
       * @param name    Name e.g. <b><i>&lt0=> No setup (Reset default)</b></i> => <b><i>"No setup (Reset default)"</b></i>
       * @param value   Value e.g. <b><i>&lt0=> No setup (Reset default)</b></i> => <b><i>0</b></i>
       */
      EnumValue(String name, long value) {
         this.setName(name);
         this.setValue(value);
      }

      /**
       * Get name of enumeration
       * 
       * @return Name e.g. <b><i>&lt0=> No setup (Reset default)</b></i> => <b><i>"No setup (Reset default)"</b></i>
       */
      public String getName() {
         return name;
      }

      /**
       * Set name of enumeration
       * 
       * @param name Name e.g. <b><i>No setup (Reset default)</b></i>
       */
      public void setName(String name) {
         this.name = name;
      }

      /**
       * Get value of enumeration
       * 
       * @return Value e.g. <b><i>&lt0=> No setup (Reset default)</b></i> => <b><i>0</b></i>
       */
      public long getValue() {
         return value;
      }

      /**
       * Set value of enumeration
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
      private String operation;
      
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
      public final EnumeratedOptionModelNode controllingNode;
      
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
      SelectionTag(EnumeratedOptionModelNode controllingNode, long selectionValue, String signalName, String signalValue) {
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

   /**
    * Generic node in the model
    */
   public class AnnotationModelNode {
      private String                            description    = null;
      private String                            toolTip        = null;
      private String                            choiceTip      = null;
      private AnnotationModelNode               parent         = null;
      private ArrayList<AnnotationModelNode>    children       = new ArrayList<AnnotationModelNode>();
      private boolean                           enabled        = true;
      private boolean                           modifiable     = false;
      private String                            name           = null;
      private Message                           errorMessage   = null;
      
      /**
       * Create AnnotationModelNode
       * 
       * @param description Description of the node<br>
       *                    This appears in the editor to describe the entry 
       */
      public AnnotationModelNode(String description) {
         this.setDescription(description);
      }

      /**
       * Copy constructor
       * 
       * @param other
       */
      public AnnotationModelNode(AnnotationModelNode other) {
         this.description  = other.description;
         this.toolTip      = other.toolTip;
         this.choiceTip    = other.choiceTip;
         this.parent       = other.parent;
         this.children     = other.children;
         this.enabled      = other.enabled;
         this.modifiable   = other.modifiable;
         this.name         = other.name;
         this.errorMessage = other.errorMessage;
      }

      /**
       * Copy values from another AnnotationModelNode
       * 
       * @param other      Node to copy
       * 
       * @throws Exception if other is not of expected type
       */
      public void copyFrom(Object other) throws Exception {
         if (!(other instanceof AnnotationModelNode)) {
            throw new Exception("Incompatible nodes in copyFrom()");
         }
         this.description = ((AnnotationModelNode)other).description;
         this.toolTip     = ((AnnotationModelNode)other).toolTip;
         this.choiceTip   = ((AnnotationModelNode)other).choiceTip;
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
               ((choiceTip == otherNode.choiceTip)     || ((choiceTip != null)   && (choiceTip.equals(otherNode.choiceTip)))) &&
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
         if (children.size() != otherNode.children.size()) {
            return false;
         }
         for (int index = 0; index< children.size(); index++) {
            if (!children.get(index).equals(otherNode.children.get(index))) {
               return false;
            }
         }
         return true;
      }
      
      /**
       * Get node description
       * 
       * @return
       */
      public String getDescription() {
         return description;
      }

      /**
       * Set Node description
       * 
       * @param description
       */
      public void setDescription(String description) {
         this.description = description;
      }

      /**
       * Get child nodes
       * 
       * @return List of children
       */
      public ArrayList<AnnotationModelNode> getChildren() {
         return children;
      }

      /**
       * Set child nodes
       * 
       * @param children List of children
       */
      public void setChildren(ArrayList<AnnotationModelNode> children) {
         this.children = children;
      }

      /**
       * Add a child node
       * 
       * @param child node to add
       */
      public void addChild(AnnotationModelNode child) {
         children.add(child);
         child.setParent(this);
      }

      /**
       * Remove all child nodes
       */
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

      /**
       * Set parent node
       * 
       * @param parent
       */
      public void setParent(AnnotationModelNode parent) {
         this.parent = parent; 
      }

      /**
       * Get tool tip 
       * 
       * @return Tool tip as string
       * 
       * @note May return error message if it exists instead
       */
      public String getToolTip() {
         if (errorMessage == null) {
            return toolTip;
         }         
         if (errorMessage.greaterThan(Severity.WARNING)) {
            // Just return the error message
            return errorMessage.getMessage();
         }
         // Append the error message
         return toolTip + "\n" + errorMessage.getMessage();
      }

      /**
       * Set too tip
       * 
       * @param toolTip Tool tip as string
       */
      public void addToolTip(String toolTip) {
         if (this.toolTip == null) {
            this.toolTip = toolTip;
         }
         else {
            this.toolTip = this.toolTip + "\n" + toolTip;
         }
      }

      /**
       * Get choice tip 
       * 
       * @return Choice tip as string
       */
      public String getChoiceTip() {
         return choiceTip;
      }

      /**
       * Set choice tip
       * 
       * @param choiceTip Choice tip as string
       */
      public void addInformation(String choiceTip) {
         if (this.choiceTip == null) {
            this.choiceTip = choiceTip;
         }
         else {
            this.choiceTip = this.choiceTip + "\n" + choiceTip;
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

      /**
       * Indicates node can be modified in the editor
       * 
       * @return true if not a constant node and node is enabled
       */
      public boolean canModify() {
         return isModifiable() && isEnabled();
      }

      /**
       * Indicates node can be modified
       * 
       * @return true if not a constant node
       */
      public boolean isModifiable() {
         return modifiable;
      }

      /**
       * Sets modifiable state of node
       * 
       * @return true if not a constant node
       */
      public void setModifiable(boolean modifiable) {
         this.modifiable = modifiable;
      }

      /**
       * Indicates if the node is enabled
       * 
       * @return true if enabled
       */
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

      /**
       * Lists the node to System.err
       * 
       * @param indent
       */
      public void listNode(int indent) {         
         System.err.println(getIndent(indent)+getDescription());
         String toolTip = getToolTip();
         if (toolTip != null) {
            System.err.println(getIndent(indent+3)+": "+ toolTip);
         }
      }

      /**
       * Lists the node and children to System.err
       * 
       * @param indent
       */
      public void listNodes(int indent) {
         listNode(indent);
         for (AnnotationModelNode child : children) {
            child.listNodes(indent+3);
         }
      }

      /**
       * Check if the value supplied is compatible with the node
       * 
       * @param value to test
       * 
       * @return
       */
      public boolean isOKValue(Object value) {
         return false;
      }

      /**
       * Set node name
       * 
       * @param name
       */
      public void setName(String name) {
         this.name = name;
      }

      /**
       * Get node name
       * 
       * @return
       */
      public String getName() {
         return name;
      }

      /**
       * Checks if node is valid (not in error state)
       * Note: This will recurse through child nodes and propagate <b>error</b> severity
       * 
       * @return Severity level.
       */
      public Severity getErrorState() {
         if (errorMessage != null) {
            return errorMessage.getSeverity();
         }
         for (AnnotationModelNode child:children) {
            if (child.getErrorState() == Severity.ERROR) {
               return Severity.ERROR;
            }
         }
         return Severity.OK;
      }

      /**
       * Set error message<br>
       * If non-null then node will be invalid
       * 
       * @param errorMessage
       */
      public void setErrorMessage(Message errorMessage) {
         this.errorMessage = errorMessage;
      }
      
      /**
       * Get error message as string.<br>
       * May be null
       * 
       * @return
       */
      public String getErrorMessage() {
         return (errorMessage==null)?null:errorMessage.getMessage();
      }
      /**
       * Get message<br>
       * May be null
       * 
       * @return
       */
      public Message getMessage() {
         return errorMessage;
      }

   }

   /**
    * A visible node used to represent an error
    */
   class ErrorNode extends AnnotationModelNode {

      /**
       * Constructor
       * 
       * @param description Description of error to display
       */
      public ErrorNode(String description) {
         super(description);
      }
   }
   
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
      
      @Override
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
   public class OptionModelNode extends AnnotationModelNode {
      private int referenceIndex;

      /**
       * Create OptionModelNode
       * 
       * @param description Description
       * @param offset
       */
      public OptionModelNode(String description, int offset) {
         super(description);
         this.referenceIndex = references.getReferenceCount()+offset;
         setModifiable(true);
      }

      /**
       * Create OptionModelNode from another
       * 
       * @param other
       */
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
      
      @Override
      public boolean equals(Object other) {
         if (!(other instanceof OptionModelNode)) {
            return false;
         }
         OptionModelNode otherNode = (OptionModelNode) other;
         return (referenceIndex == otherNode.referenceIndex) && super.equals(otherNode);
      }

      /**
       * Obtains the reference index of the associated item in the file being edited
       * 
       * @return Index
       */
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
      
      /**
       * Obtains the document reference of this node in the associated document
       * 
       * @return
       * @throws Exception
       */
      public DocumentReference getReference() throws Exception {
         try {
            return references.get(getReferenceIndex());
         } catch (Exception e) {
            throw new Exception("Illegal Reference @"+getReferenceIndex(), e);
         }
      }
      /**
       * Obtains the document reference of the node offset from this node in the associated document
       * 
       * @param offset Offset from this node to required node
       *  
       * @return node if found
       * 
       * @throws Exception if node cannot be located
       */
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
      public NumericOptionModelNode(String description, int offset, BitField bitField) {
         super(description, offset);
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
      public String getValueAsString() throws Exception {
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

      /**
       * Set if number is to be represented in hex with 0x prefix
       * 
       * @param useHex
       */
      public void setUseHex(boolean useHex) {
         this.useHex = useHex;
      }
      
   }

   /**
    * Currently unused?
    */
   public class PllConfigurationModelNode extends NumericOptionModelNode {
      public PllConfigurationModelNode(String description, int offset, BitField bitField) {
         super(description, offset, null);
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
   public class EnumeratedOptionModelNode extends NumericOptionModelNode {
      /** List of enumerated values applied to this node e.g. &lt0=> No setup (Reset default) */
      private ArrayList<EnumValue>     enumerationValues = null;
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
      public EnumeratedOptionModelNode(String description, int offset, BitField bitField) {
         super(description, offset, bitField);
         setModifiable(true);
      }

      /**
       * Constructor from another node<br>
       * Represents a enumeration node in the document
       * 
       * @param other      
       */
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
      
      /**
       * Constructor from another node<br>
       * Represents a numeric node in the document
       * 
       * @param other      
       */
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

      /**
       * Gets the text associated with the currently selected enumerated value
       * 
       * @return The selected value
       */
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
      

      /**
       * Get index of matching enumerated value
       * 
       * @param name Name to match
       * 
       * @return index or -1 if not found
       */
      public int getEnumIndex(String name) {
         for (int index=0; index<enumerationValues.size(); index++) {
            EnumValue enumValue = enumerationValues.get(index);
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
      public void setValueAsString(String name) throws Exception {
         int index = getEnumIndex(name);
         if (index>=0) {
            super.setValue(enumerationValues.get(index).getValue());
            return;
         }
         System.err.println("Failed to locate enumerated value n = '" + getName() + "', v = '" + name +"'");
      }

      /**
       * Get list of enumerated values applied to this node e.g. &lt0=> No setup (Reset default)
       * 
       * @return List of enumerated values
       */
      public ArrayList<EnumValue> getEnumerationValues() {
         return enumerationValues;
      }

      /**
       * Add enumerated value applied to this node e.g. &lt0=> No setup (Reset default)
       * 
       * @param enumerationValue Enumerated value to add
       */
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

      public String safeGetValueAsString() {
         try {
            return getValueAsString();
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
      public String getValueAsString() throws Exception {
         return ((Boolean)getValue())?trueValueText:falseValueText;
      }

      /**
       * Add enumerated value applied to this node e.g. &lt0=> No setup (Reset default)
       * 
       * @param enumerationValue Enumerated value to add
       */
      public void addEnumerationValue(EnumValue enumValue) {
         if (enumValue.getValue() == 0) {
            falseValueText = enumValue.getName();
         }
         else {
            trueValueText = enumValue.getName();
         }
      }

      @Override
      public Object getValue() throws Exception {
         return new Boolean(((Long)super.getValue()) != 0);
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
      public OptionHeadingModelNode(String description, int offset, BitField bitField) throws Exception {
         super(description, offset, bitField);
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

   /**
    * Describes an option that modifies a string value in the C file
    * <pre>
    * //  &lt;s> String Option Description
    * //  &lt;i> Tool-tip text
    * //  #define STRING_OPTION "Value of string"
    * <pre>
    */
   public class StringOptionModelNode extends OptionModelNode {

      int characterLimit;

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
      public StringOptionModelNode(String description, int offset, int characterLimit) {
         super(description, offset);
         this.characterLimit = characterLimit;
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

   public void setInverted(boolean inverted) {
      // TODO Auto-generated method stub
      
   }

}

