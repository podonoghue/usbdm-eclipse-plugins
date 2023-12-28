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
    * @param properties      - Indicates properties changed "Value", "Structure" or "Status"
    */
   void modelElementChanged(ObservableModelInterface observableModel, String[] properties);
   
}
