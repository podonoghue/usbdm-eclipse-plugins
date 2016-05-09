package net.sourceforge.usbdm.deviceEditor.model;

import org.eclipse.jface.viewers.TreeViewer;

public abstract class RootModel extends BaseModel {

   /** Labels for view columns */
   private final String[] fColumnLabels;
   
   /** Viewer associated with this model */
   private TreeViewer fViewer = null;
   
   /** Factory responsible for this model */
   protected final ModelFactory fModelFactory;

   /*
    * =============================================================================================
    */
   /**
    * Constructor
    * 
    * @param modelFactory  Factory owning model
    * @param columnLabels  Labels to use for columns
    * @param title         Title
    * @param toolTip       Tool tip
    */
   public RootModel(ModelFactory modelFactory, String[] columnLabels, String title, String toolTip) {
      
      super(null, title, modelFactory.getDeviceInfo().getSourceFilename());
      super.setToolTip(toolTip);
      
      fModelFactory  = modelFactory;
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
//         if (element.getParent() != null) {
//            fViewer.update(element.getParent(),  properties);
//         }
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
      if (fViewer != null) {
         throw new RuntimeException("Viewer already assigned");
      }
      fViewer = viewer;
   }

   /**
    * @return the ModelFactory
    */
   public ModelFactory getModelFactory() {
      return fModelFactory;
   }

}
