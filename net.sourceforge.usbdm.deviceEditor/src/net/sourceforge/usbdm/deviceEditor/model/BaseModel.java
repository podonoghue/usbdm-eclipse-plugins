package net.sourceforge.usbdm.deviceEditor.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.StructuredViewer;

import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.peripherals.model.MemoryException;

/**
 * Base Model for tree item
 */
public abstract class BaseModel {
   
   /** Factory owning these models */
   static private ModelFactory fFactory = null;

   /** 
    * Set the Factory using these models 
    * 
    * @param factory
    */
   static void setFactory(ModelFactory factory) {
      fFactory = factory;
   }
   
   /**
    * Check for mapping conflicts<br>
    * This is done on a delayed thread for efficiency
    */
   static void checkConflicts() {
      if (fFactory != null) {
         fFactory.checkConflicts();
      }
   }
   
   /**
    * Get viewer associated with this model
    * 
    * @return
    */
   protected StructuredViewer getViewer() {
      if (fParent != null) {
         return fParent.getViewer();
      }
      return null;
   }
   
   /**
    * Refresh the viewer for this model
    */
   protected void refresh() {
      StructuredViewer viewer = getViewer();
      if ((viewer != null) && (!viewer.getControl().isDisposed())) {
         viewer.refresh();
      }
   }
   
   /**
    * Updates the model's presentation when one or more of its properties change
    */
   protected void update() {
      StructuredViewer viewer = getViewer();
      if (viewer != null) {
         viewer.update(this, null);
      }
   }
   
   /**
    * Updates the given element's and all ancestor's presentation when one or more of its properties change
    */
   protected void updateAncestors() {
      StructuredViewer viewer = getViewer();
      if (viewer != null) {
         BaseModel element = this;
         while (element != null) {
            viewer.update(element, null);
            element = element.getParent();
         }
      }
   }
   
   /** Name of model */
   protected       String            fName;
   
   /** Description of node */
   protected       String            fDescription;
   
   /** Parent node */
   protected BaseModel               fParent;
   
   /** Child nodes */
   protected       ArrayList<Object> fChildren = null;
   
   /** Tool-tip */
   protected       String            fToolTip = "";
   
   /** Information/Warning/Error message */
   protected Message fMessage = null;

   /** Controls logging */
   protected boolean fLogging = false;

   /**
    * Constructor
    * 
    * @param parent        Parent model
    * @param name          Display name
    * @param description   Display description
    * 
    * @note Added as child of parent if not null
    */
   public BaseModel(BaseModel parent, String name, String description) {
      if (name == null) {
         name = "No name";
      }
      fParent      = parent;
      fName        = name;
      fDescription = description;
      if (parent != null) {
         parent.addChild(this);
      }
//      fLogging = (fName.equalsIgnoreCase("FTM0") || ((fParent != null)&&fParent.fName.equalsIgnoreCase("FTM0")));
      if (fLogging) {
         System.err.println("Creating model "+name);
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
    * Sets the name of the tree item
    * 
    * @param name
    */
   public void setName(String name) {
      fName = name;
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
      return "BaseModel("+fName+", "+fDescription+")";
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
    * Set description text
    * 
    * @param description
    */
   void setDescription(String description) {
      fDescription = description;
   }
   
   /**
    * Gets description of element
    * 
    * @return string
    */
   public String getDescription() {
      String description = fDescription;
      Message message = getMessage();
      if ((message != null) && (message.greaterThan(Message.Severity.OK))) {
         description = message.getMessage();

         // Truncate to single line
         int eolIndex = description.indexOf('\n');
         if (eolIndex>0) {
            description = description.substring(0, eolIndex);
         }
      }
      if (fLogging) {
         System.err.println("Getting description "+fName+" => "+description);
      }
      return description;
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
    * Set node message as error
    * 
    * @param message Message to set (may be null)
    */
   public void setMessage(String message) {
      Message msg = null;
      if ((message != null) && !message.isEmpty()) {
         msg = new Message(message, Message.Severity.ERROR);
      }
      setMessage(msg);
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
      Message msg = getMessage();
      return (msg != null) && (msg.greaterThan(Message.Severity.INFO));
   }

   /** 
    * Indicates that element is 'reset'
    * 
    * @return
    */
   public boolean isReset() {
      return false;
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
      if (tip == null) {
         tip = "";
      }
      Message message = getMessage();
      if ((message != null) && (message.greaterThan(Message.Severity.WARNING))) {
         tip += (tip.isEmpty()?"":"\n")+message.getMessage();
      }
      else if (message != null) {
         tip += (tip.isEmpty()?"":"\n")+message.getRawMessage();
      }
      return (tip.isEmpty())?null:tip;
   }

   /**
    * Set tool tip
    * 
    * @param toolTip
    */
   public void setToolTip(String toolTip) {
      fToolTip = toolTip;
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

//   /**
//    * Update the node given
//    * 
//    * @param element
//    * @param properties
//    */
//   protected void viewerUpdate(BaseModel element, String[] properties) {
//      if (element != null) {
//         BaseModel root = getRoot();
//         if (root != null) {
//            root.viewerUpdate(element, properties);
//         }
//         if (getParent() != null) {
//            root.viewerUpdate(getParent(), null);
//         }
//      }
//   }

//   /**
//    * Refresh all views from the model from the root node
//    */
//   protected void refresh() {
//      BaseModel root = getRoot();
//      if (root != null) {
//         getRoot().refresh();
//      }
//   }

   /**
    * Remove any listeners created by this model<br>
    * Doesn't include listeners created by children
    */
   protected abstract void removeMyListeners();
   
   /**
    * Remove all listeners created by this model including by children
    */
   public final void removeListeners() {
      if (fChildren != null) {
         for (Object child:fChildren) {
            ((BaseModel) child).removeListeners();
         }
      }
      removeMyListeners();
   }

   public void setParent(BaseModel parent) {
      fParent = parent;
      if (parent != null) {
         parent.addChild(this);
      }
   }
}