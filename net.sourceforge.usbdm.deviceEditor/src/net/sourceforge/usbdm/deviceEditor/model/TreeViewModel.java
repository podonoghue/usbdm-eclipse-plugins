package net.sourceforge.usbdm.deviceEditor.model;

import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;

/**
 *  Represents the top of a Tree view
 */
public class TreeViewModel extends BaseModel {

   /** Viewer associated with this model */
   private TreeViewer fViewer = null;
   
   /**
    * Constructor
    * @param columnLabels  Labels to use for columns
    * @param title         Title
    * @param toolTip       Tool tip
    */
   public TreeViewModel(BaseModel parent, String title, String toolTip) {
      super(parent, title, "");
      super.setToolTip(toolTip);
   }

   /**
    * Sets the viewer of this model
    * 
    * @param viewer
    */
   public void setViewer(TreeViewer viewer) {
      if ((fViewer != null) && (fViewer != viewer)) {
         throw new RuntimeException("Viewer already assigned");
      }
      fViewer = viewer;
   }

   @Override
   protected StructuredViewer getViewer() {
      return fViewer;
   }

   @Override
   protected void removeMyListeners() {
   }
}
