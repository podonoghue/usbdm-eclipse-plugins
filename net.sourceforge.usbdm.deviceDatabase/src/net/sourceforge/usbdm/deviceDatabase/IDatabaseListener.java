package net.sourceforge.usbdm.deviceDatabase;


/**
 * Used to communicate changes in the model element.
 *
 */
public interface IDatabaseListener {
   
   /**
    * Called when the database completes loading
    * 
    * @param observableModel - The model element that has changed
    */
   void databaseLoaded(DeviceDatabase database);
}
