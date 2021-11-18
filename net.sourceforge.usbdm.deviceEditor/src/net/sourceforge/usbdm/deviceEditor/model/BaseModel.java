package net.sourceforge.usbdm.deviceEditor.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.widgets.Display;

import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;
import net.sourceforge.usbdm.deviceEditor.xmlParser.ListModel;

/**
 * Base Model for tree item
 */
public abstract class BaseModel implements Cloneable {
   
   static final String watchedName = "OSC"; 

   /** Factory owning these models */
   static private ModelFactory fFactory = null;

   private String fDescription = null;
   
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
      final StructuredViewer viewer = getViewer();
      Display.getDefault().asyncExec(new Runnable() {
         public void run() {
            if ((viewer != null) && (!viewer.getControl().isDisposed())) {
               viewer.refresh();
            }
         }
     });
   }

   final List<BaseModel> fModelsToRefresh = Collections.synchronizedList(new ArrayList<BaseModel>());

   /**
    * Updates the model's presentation when one or more of its properties change
    */
   public synchronized void update() {
      BaseModel        origin = this;
      //          XXX Delete OK
//      if (origin.getName().contains(watchedName)) {
//         System.err.println("BaseModel.update Scheduling : " + origin + ":" + origin.hashCode() + ", " + origin.getDescription());
//         System.err.flush();
//      }         
      StructuredViewer viewer = getViewer();

      if (!fModelsToRefresh.contains(origin)) {
         //          XXX Delete OK
//         if (origin.getName().contains(watchedName)) {
//            System.err.println("BaseModel.update Scheduling : " + origin + ":" + origin.hashCode() + ", " + origin.getDescription());
//            System.err.flush();
//         }         
         fModelsToRefresh.add(origin);

         Display.getDefault().asyncExec(new Runnable() {
            public void run() {
               // XXX Delete OK
//               if (origin.getName().contains(watchedName)) {
//                  System.err.println("BaseModel.update.run()     : " + origin + ":" + origin.hashCode() + ", " + origin.getDescription());
//                  System.err.flush();
//               }
               fModelsToRefresh.remove(origin);
               if ((viewer != null) && (!viewer.getControl().isDisposed())) {
                  //                  viewer.refresh();
                  BaseModel model = origin;
                  while (model != null) {
                     viewer.update(model, null);
                     model = model.getParent();
                  }
               }
            }
         });
      }
      //      else {
      //         if (origin.getName().contains(watchedName)) {
      //            // XXX Delete OK
      //            System.err.println("BaseModel.update.Discarding : " + origin + ":" + origin.hashCode() + ", " + origin.getDescription());
      //            System.err.flush();
      //         }
      //      }
   }
   
   /**
    * Updates the given element's and all ancestor's presentation when one or more of its properties change
    */
   protected void updateAncestors() {
      final StructuredViewer viewer         = getViewer();
      final BaseModel        currentElement = this;
      
      Display.getDefault().asyncExec(new Runnable() {
         public void run() {
            if ((viewer != null) && (!viewer.getControl().isDisposed())) {
               BaseModel element = currentElement;
               while (element != null) {
                  viewer.update(element, null);
                  element = element.getParent();
               }
            }
         }
     });
   }
   
   /** Name of model */
   protected       String            fName;
   
   /** Parent node */
   protected BaseModel               fParent;
   
   /** Child nodes */
   protected       ArrayList<Object> fChildren = null;
   
   /** Tool-tip */
   protected       String            fToolTip = "";
   
   /** Information/Warning/Error message */
   protected Status fMessage = null;

   /** Controls logging */
   protected final boolean fLogging = false;

   /** Index for indexed models */
   private int  fIndex;

   /**
    * Constructor
    * 
    * @param parent        Parent model
    * @param name          Display name
    * @param description   Display description
    * 
    * @note Added as child of parent if not null
    */
   public BaseModel(BaseModel parent, String name) {
      if (name == null) {
         name = "No name";
      }
      fParent      = parent;
      fName        = name;
      if (parent != null) {
         parent.addChild(this);
      }
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
      if (model instanceof ListModel) {
         ListModel listModel = (ListModel) model;
         listModel.addChildrenToParent(this);
      }
      else {
         if (fChildren == null) {
            fChildren = new ArrayList<Object>();
         }
         fChildren.add(model);
      }
   }

   public void removeChild(BaseModel model) throws Exception {
      if (!fChildren.remove(model)) {
         throw new Exception("Model not present "+ model.getName());
      }
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
    * Remove child nodes
    */
   public void removeChildren() {
      if (fChildren == null) {
         return;
      }
      for (Object child:fChildren) {
         BaseModel childModel = (BaseModel) child;
         childModel.removeListeners();
      }
      fChildren = null;
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
      return getClass().getSimpleName()+"("+fName+", "+getDescription()+")";
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
    * Gets description of element
    * 
    * @return string
    */
   public String getSimpleDescription() {
      if (fDescription == null) {
         return "";
      }
      return fDescription;
   }
   
   /**
    * Sets simple description of element
    * 
    * @return string
    */
   public void setSimpleDescription(String description) {
      fDescription = description;
   }
   
   /**
    * Gets description of element.<br>
    * If the status is not <b>null</b> or more severe than <b>Status.Severity.OK</b> then the status text is returned instead.
    * 
    * @return string Description
    */
   public final String getDescription() {
      String description = getSimpleDescription();
      Status status = getStatus();
      if ((status != null) && (status.greaterThan(Status.Severity.WARNING))) {
         if (status.greaterThan(Status.Severity.INFO)) {
            description = status.getText();
         }
         else {
            description = status.getSimpleText();
         }
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
    * Set node message
    * 
    * @param message Message to set (may be null)
    */
   public void setStatus(Status message) {
      fMessage = message;
   }
   
   /**
    * Get status that applies to this node<br>
    * Note - Error status is propagated from children
    * 
    * @return
    */
   Status getStatus() {
      // Search children for status
      Status returnStatus = fMessage;
      if (((returnStatus == null) || returnStatus.lessThan(Status.Severity.ERROR)) && (fChildren != null)) {
         for (Object node:fChildren) {
            BaseModel child = (BaseModel) node;
            Status status = child.getStatus();
            if ((status != null) && status.greaterThan(Status.Severity.WARNING)) {
               returnStatus = status;
               break;
            }
         }
      }
      return returnStatus;
   }
   
   /** 
    * Indicates that model value is in error and an error icon to be displayed
    * Note - Error are propagated from children
    * 
    * @return
    */
   public boolean isError() {
      Status msg = getStatus();
      return (msg != null) && (msg.greaterThan(Status.Severity.WARNING));
   }

   /** 
    * Indicates that model value is in warning and an error icon to be displayed
    * 
    * @return
    */
   public boolean isWarning() {
      Status msg = getStatus();
      return (msg != null) && (msg.greaterThan(Status.Severity.INFO));
   }

   /** 
    * Indicates that element is inactive and should be displayed dimmed.
    * 
    * @return
    */
   public boolean isInactive() {
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
      Status message = getStatus();
      if (message != null) {
         String hint = message.getHint();
         if (hint != null) {
            tip+= hint;
         }
         if (message.greaterThan(Status.Severity.WARNING)) {
            tip += (tip.isEmpty()?"":"\n")+message.getText();
         }
         else {
            tip += (tip.isEmpty()?"":"\n")+message.getSimpleText();
         }
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
   protected Status checkConflicts(Map<String, List<MappingInfo>> mappedNodes) {
      return null;
   }

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

   /**
    * Set parent model<br>
    * Also adds the model to the parent
    * 
    * @param parent
    */
   public void setParent(BaseModel parent) {
      fParent = parent;
      if (parent != null) {
         parent.addChild(this);
      }
   }

   /**
    * Set parent model<br>
    * Does not add the model to the parent
    * 
    * @param parent
    */
   public void setParentOnly(BaseModel parent) {
      fParent = parent;
   }

   /**
    * 
    * @param parentModel   Parent for cloned node
    * @param provider      Variable provider to register variables with
    * @param index         Index for the clone
    * 
    * @return Clone of the object
    * 
    * @throws CloneNotSupportedException if cannot be cloned
    */
   public BaseModel clone(BaseModel parentModel, VariableProvider provider, int index) throws CloneNotSupportedException {
      
      // Clone simple members
      BaseModel model = (BaseModel) super.clone();
      
      // Change to correct parent
      model.setParent(parentModel);
      
      // Set unique index
      model.fIndex = index;
      model.fName  = fName.replaceAll("\\[\\d+\\]$", "["+index+"]");

      // Remove cross-linked children
      model.fChildren = null;
      
      // Add cloned children
      if (fChildren != null) {
         for (Object obj : fChildren) {
            BaseModel child = (BaseModel) obj;
            child.clone(model, provider, index);
         }
      }
      return model;
   }
         
   /**
    * Get index for indexed models
    * 
    * @return
    */
   public int getIndex() {
      return fIndex;
   }

}