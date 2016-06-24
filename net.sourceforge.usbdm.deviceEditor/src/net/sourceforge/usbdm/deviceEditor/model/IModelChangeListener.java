package net.sourceforge.usbdm.deviceEditor.model;

/**
 * Used to communicate changes in the model element.
 *
 */
public interface IModelChangeListener {
   
   /**
    * Called when the model changes.
    * 
    * @param observableModel - The model element that has changed
    */
   void modelElementChanged(ObservableModel observableModel);
   
   /**
    * Called when the model changes structure.
    * 
    * @param observableModel - The model element that has changed
    */
   void modelStructureChanged(ObservableModel observableModel);

   /**
    * Called when the model changes status.
    * 
    * @param observableModel - The model element that has changed
    */
   void elementStatusChanged(ObservableModel observableModel);
   
}
