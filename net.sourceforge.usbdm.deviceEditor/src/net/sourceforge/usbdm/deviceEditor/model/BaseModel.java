package net.sourceforge.usbdm.deviceEditor.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.TreeViewer;

import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;

/**
 * Base Model for tree item
 */
public class BaseModel {
   /** Name of model */
   protected final String            fName;
   
   /** Description of node */
   protected final String            fDescription;
   
   /** Parent node */
   protected final BaseModel         fParent;
   
   /** Child nodes */
   protected       ArrayList<Object> fChildren = null;
   
   /** Tool-tip */
   protected       String            fToolTip  = null;
   
   /** Information/Warning/Error message */
   protected Message fMessage = null;
   
   /**
    * Constructor
    * 
    * @param parent        Parent of this element in tree
    * @param name          Name of this element in tree
    * @param description   Description of element
    * 
    * @note Added as child of parent if not null
    */
   public BaseModel(BaseModel parent, String name, String description) {
      if (name == null) {
         name = "No name";
      }
      if (description == null) {
         description = "No description";
      }
      fParent      = parent;
      fName        = name;
      fDescription = description;
      if (parent != null) {
         parent.addChild(this);
      }
   }

   /**
    * @return the parent
    */
   public BaseModel getParent() {
      return fParent;
   }

   /**
    * Add child node
    * 
    * @param model
    */
   public void addChild(BaseModel model) {
      if (fChildren == null) {
         fChildren = new ArrayList<Object>();
      }
      fChildren.add(model);
   }

   /**
    * Indicates if the node has children
    * 
    * @return
    */
   public boolean hasChildren() {
      return fChildren != null;
   }

   /**
    * Get child nodes
    * 
    * @return the children
    */
   public ArrayList<Object> getChildren() {
      return fChildren;
   }

   /**
    * Gets the name of the tree item
    * 
    * @return String name
    */
   public String getName() {
      return fName;
   }

   /**
    * Returns a string representing the value in an appropriate form for model
    * 
    * @return String representation e.g. "PTA3"
    * @throws MemoryException 
    */
   public String getValueAsString() {
      return "";
   }

   /**
    * Gets a string representation of the name suitable for tree view  
    *
    * @return string
    */
   public String toString() {
      if (fName != null) {
         return fName;
      }
      if (fDescription != null) {
         return fDescription;
      }
      return super.toString();
   }

   /**
    * Indicates if the value has changed compared to the reference value  
    *
    * @return true/false
    */
   public boolean isChanged() throws Exception {
      return false;
   }

   /**
    * 
    * Gets description of element
    * 
    * @return string
    */
   public String getDescription() {
      Message message = getMessage();
      if ((message != null) && (message.greaterThan(Message.Severity.OK))) {
         return message.getMessage();
      }
      return fDescription;
   }

   /**
    * 
    * Gets description of element
    * 
    * @return string
    */
   public String getShortDescription() {
      return fDescription;
   }

   /**
    * Indicates if the value needs to be updated from target
    *
    * @return true/false
    */
   public boolean isUpdateNeeded() {
      return false;
   }

   /** 
    * Indicates that model value may be changed
    * 
    * @return
    */
   public boolean canEdit() {
      return false;
   }

   /** 
    * Indicates that model value is locked and a icon should be displayed
    * 
    * @return
    */
   public boolean isLocked() {
      return false;
   }


   /**
    * Set node message as error
    * 
    * @param message Message to set (may be null)
    */
   protected void setMessage(String message) {
      if ((message == null) || message.isEmpty()) {
         fMessage = null;
         return;
      }
      setMessage(new Message(message, Message.Severity.ERROR));
   }

   /**
    * Set node message
    * 
    * @param message Message to set (may be null)
    */
   void setMessage(Message message) {
      fMessage = message;
   }
   
   /**
    * Get message that applies to this node<br>
    * Note - Error messages are propagated from children
    * 
    * @return
    */
   Message getMessage() {
      // Search children for error
      Message rv = fMessage;
      if ((rv != null) && rv.greaterThan(Message.Severity.WARNING)) {
         return rv;
      }
      if (fChildren == null) {
         return rv;
      }
      for (Object node:fChildren) {
         BaseModel child = (BaseModel) node;
         Message m = child.getMessage();
         if ((m != null) && m.greaterThan(Message.Severity.WARNING)) {
            return m;
         }
      }
      return rv;
   }
   
   /** 
    * Indicates that model value is in error and an error icon to be displayed
    * Note - Error are propagated from children
    * 
    * @return
    */
   public boolean isError() {
      Message msg = getMessage();
      return (msg != null) && (msg.greaterThan(Message.Severity.WARNING));
   }

   /** 
    * Indicates that model value is in warning and an error icon to be displayed
    * 
    * @return
    */
   public boolean isWarning() {
      return (fMessage != null) && (fMessage.greaterThan(Message.Severity.INFORMATION));
   }

   /** 
    * Indicates that element is enabled
    * 
    * @return
    */
   public boolean isEnabled() {
      return true;
   }

   /**
    * Get tool tip
    * 
    * @return
    */
   public String getToolTip() {
      String tip = fToolTip;
      Message message = getMessage();
      if ((message != null) && (message.greaterThan(Message.Severity.WARNING))) {
         tip += fMessage.getMessage();
      }
      else if ((message != null) && (message.greaterThan(Message.Severity.OK))) {
         tip += "\n"+fMessage.getMessage();
      }
      return tip;
   }

   /**
    * Set tool tip
    * 
    * @param toolTip
    */
   protected void setToolTip(String toolTip) {
      fToolTip = toolTip;
   }

   /**
    * Get root node of tree
    * 
    * @return
    */
   protected BaseModel getRoot() {
      if (fParent != null) {
         return fParent.getRoot();
      }
      return null;
   }
   
   /**
    * Get viewers associated with this model
    * 
    * @return
    */
   protected ArrayList<TreeViewer> getViewers() {
      return getRoot().getViewers();
   }
   
   /**
    * Check if this node's function mapping conflicts with set of already mapped nodes
    * 
    * @param mappedNodes
    * @return
    */
   protected Message checkConflicts(Map<String, List<MappingInfo>> mappedNodes) {
      return null;
   }

}