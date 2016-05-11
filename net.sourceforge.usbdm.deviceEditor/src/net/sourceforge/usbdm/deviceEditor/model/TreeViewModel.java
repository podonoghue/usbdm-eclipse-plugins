package net.sourceforge.usbdm.deviceEditor.model;

import org.eclipse.jface.viewers.TreeViewer;

/**
 *  Represents the top of a Tree view
 */
public abstract class TreeViewModel extends PeripheralPageModel {

   /** Labels for view columns */
   private final String[] fColumnLabels;
   
   /** Viewer associated with this model */
   private TreeViewer fViewer = null;
   
   /**
    * Constructor
    * 
    * @param columnLabels  Labels to use for columns
    * @param title         Title
    * @param toolTip       Tool tip
    */
   public TreeViewModel(String[] columnLabels, String title, String toolTip) {
      super(null, title, "");
      super.setToolTip(toolTip);
      
      fColumnLabels  = columnLabels;
   }

   /**
    * Get array of titles to use on editor
    * 
    * @return
    */
   public String[] getColumnLabels() {
      return fColumnLabels;
   }
   
   @Override
   protected BaseModel getRoot() {
      return this;
   }

   @Override
   protected void viewerUpdate(BaseModel element, String[] properties) {
      System.err.println("BaseModel("+fName+").viewerUpdate("+element.getName()+")");
      if ((fViewer != null) && !fViewer.getTree().isDisposed()) {
         fViewer.update(element,  properties);
      }
   }

   /**
    * Refresh the tree
    */
   protected void refresh() {
      if ((fViewer != null) && !fViewer.getTree().isDisposed()) {
         fViewer.refresh();
      }
   }
   
   /**
    * Add a view to the model
    * 
    * @param viewer
    */
   public void addViewer(TreeViewer viewer) {
      if ((fViewer != null) && (fViewer != viewer)) {
         throw new RuntimeException("Viewer already assigned");
      }
      fViewer = viewer;
   }
}
