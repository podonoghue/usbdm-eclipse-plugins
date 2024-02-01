package net.sourceforge.usbdm.deviceEditor.model;

import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;

import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;

/**
 *  Represents the top of a Tree view
 */
public class TreeViewModel extends BaseModel {

   /** Viewer associated with this model */
   private TreeViewer fViewer = null;
   
   /**
    * Constructor
    * @param parent        Parent model
    * @param title         Title
    * @param toolTip       Tool tip
    */
   public TreeViewModel(BaseModel parent, String title, String toolTip) {
      super(parent, title);
      super.setToolTip(toolTip);
   }

   /**
    * Sets the viewer of this model
    * 
    * @param viewer
    */
   public void setViewer(TreeViewer viewer) {
      fViewer = viewer;
   }

   @Override
   protected StructuredViewer getViewer() {
      if (fViewer != null) {
         return fViewer;
      }
      if (fParent != null) {
         return fParent.getViewer();
      }
      return null;
   }

   @Override
   protected void removeMyListeners() {
   }

   @Override
   public TreeViewModel clone(BaseModel parentModel, VariableProvider provider, int index) throws CloneNotSupportedException {
      return (TreeViewModel) super.clone(parentModel, provider, index);
   }
   
   
}
