package net.sourceforge.usbdm.deviceEditor.model;

import org.eclipse.jface.viewers.TreeViewer;

public class RootModel extends BaseModel {

   /** Labels for view columns */
   private final String[] fColumnLabels;
   
   /** Viewer associated with this model */
   private TreeViewer fViewer = null;
   
   /** Factory responsible for this model */
   private final ModelFactory fModelFactory;

   /** Device variant name */
   private final String fDeviceVariant;

   /**
    * @return the Title
    */
   public String getDeviceName() {
      return fDeviceVariant;
   }

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
      fDeviceVariant = modelFactory.getDeviceInfo().getDeviceVariantName();
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
   public String getValueAsString() {
      return fDeviceVariant;
   }

   @Override
   protected BaseModel getRoot() {
      return this;
   }

   @Override
   protected void viewerUpdate(BaseModel element, String[] properties) {
      if (fViewer != null) {
         fViewer.update(element,  properties);
         if (element.getParent() != null) {
            fViewer.update(element.getParent(),  properties);
         }
      }
   }

   /**
    * Refresh the tree
    */
   protected void refresh() {
      if (fViewer != null) {
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
