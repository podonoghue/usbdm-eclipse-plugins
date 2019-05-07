package net.sourceforge.usbdm.annotationEditor;

import java.util.ArrayList;

import net.sourceforge.usbdm.annotationEditor.AnnotationModel.Severity;

/**
 * Generic node in the model
 */
public class AnnotationModelNode {
   private String                            description     = null;
   private String                            toolTip         = null;
   private String                            choiceTip       = null;
   private AnnotationModelNode               parent          = null;
   private ArrayList<AnnotationModelNode>    children        = new ArrayList<AnnotationModelNode>();
   private boolean                           enabled         = true;
   private boolean                           modifiable      = false;
   private String                            name            = null;
   private Message                           errorMessage    = null;
   
   protected AnnotationModel                 annotationModel;
   
   /**
    * Create AnnotationModelNode
    * 
    * @param description Description of the node<br>
    *                    This appears in the editor to describe the entry 
    */
   public AnnotationModelNode(AnnotationModel annotationModel, String description) {
      this.annotationModel = annotationModel;
      this.setDescription(description);
   }

   /**
    * Copy constructor
    * 
    * @param other
    */
   public AnnotationModelNode(AnnotationModelNode other) {
      this.annotationModel   = other.annotationModel;
      this.description       = other.description;
      this.toolTip           = other.toolTip;
      this.choiceTip         = other.choiceTip;
      this.parent            = other.parent;
      this.children          = other.children;
      this.enabled           = other.enabled;
      this.modifiable        = other.modifiable;
      this.name              = other.name;
      this.errorMessage      = other.errorMessage;
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
   public String getDialogueValueAsString() throws Exception {
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
   public void setValueFromDialogueString(String value) throws Exception {
   }

   /**
    * Returns the value of a node in a node dependent form. <br>
    * The value is retrieved from the document.<br>
    * This might be:
    *    <li> a number as an integer
    *    <li> an enumerated type name e.g. <em>high</em>
    *    <li> a boolean value
    *    <li> a string that is the value of the node (quotes stripped)
    * 
    * @return value as above or null if no value is associated with the node
    * 
    * @throws Exception
    */
   public Object getValue() throws Exception {
      return null;
   }

   /**
    * Sets the value of a node in a node dependent form. <br>
    * The value is written to the document.<br>
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
      System.err.println(AnnotationModel.getIndent(indent)+getDescription());
      String toolTip = getToolTip();
      if (toolTip != null) {
         System.err.println(AnnotationModel.getIndent(indent+3)+": "+ toolTip);
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
