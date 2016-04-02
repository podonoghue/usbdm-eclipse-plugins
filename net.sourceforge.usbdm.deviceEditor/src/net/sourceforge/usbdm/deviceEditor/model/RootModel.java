package net.sourceforge.usbdm.deviceEditor.model;

import java.util.ArrayList;

import org.eclipse.jface.viewers.TreeViewer;

public class RootModel extends BaseModel {

   /** Labels for view columns */
   private final String[] fColumnLabels;
   
   /** List of views associated with this model */
   private final ArrayList<TreeViewer> fViewers = new ArrayList<TreeViewer>();
   
   /** Factory responsible for this model */
   private final ModelFactory fModelFactory;

   /** Device name */
   private final String fDeviceName;

   /**
    * @return the Title
    */
   public String getDeviceName() {
      return fDeviceName;
   }

   /*
    * =============================================================================================
    */
   /**
    * Constructor
    * 
    * @param modelFactory  Factory owning model
    * @param columnLabels  Labels to use for columns
    * @param string2 
    * @param string 
    */
   public RootModel(ModelFactory modelFactory, String[] columnLabels, String title, String toolTip) {
      
      super(null, title, modelFactory.getDeviceInfo().getSourceFilename());
      
      fModelFactory = modelFactory;
      fColumnLabels = columnLabels;
      fDeviceName   = modelFactory.getDeviceInfo().getDeviceName();
      super.setToolTip(toolTip);
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
      return fDeviceName;
   }

   @Override
   protected BaseModel getRoot() {
      return this;
   }

   @Override
   protected void viewerUpdate(BaseModel element, String[] properties) {
      for (TreeViewer viewer:fViewers) {
         viewer.update(element,  properties);
         if (element.getParent() != null) {
            viewer.update(element.getParent(),  properties);
         }
      }
   }

   protected void refresh() {
      for (TreeViewer viewer:fViewers) {
         viewer.refresh();
      }
   }
   
   /**
    * Add a view to the model
    * 
    * @param viewer
    */
   public void addViewer(TreeViewer viewer) {
      fViewers.add(viewer);
   }

   /**
    * @return the fModelFactory
    */
   public ModelFactory getModelFactory() {
      return fModelFactory;
   }

}
