package net.sourceforge.usbdm.deviceEditor.model;

/**
 * Model describing the device pins organised by peripheral or pin category
 */
public final class DevicePinsModel extends TreeViewModel {

   /** Factory responsible for this model */
   private final ModelFactory fModelFactory;

   /**
    * Constructor
    * 
    * @param modelFactory  Factory owning model
    * @param columnLabels  Labels to use for columns
    * @param title 
    * @param toolTip 
    */
   public DevicePinsModel(ModelFactory modelFactory, String[] columnLabels, String title, String toolTip) {
      super(columnLabels, title, toolTip);
      fModelFactory = modelFactory;
   }

   @Override
   public String getValueAsString() {
      return "";
   }

   protected Message checkConflicts() {
      getModelFactory().checkConflicts();
      return null;
   }

   @Override
   protected void removeMyListeners() {
   }

   /**
    * @return the ModelFactory
    */
   public ModelFactory getModelFactory() {
      return fModelFactory;
   }

   @Override
   public EditorPage createEditorPage() {
      return new TreeEditorPage();
   }
}
