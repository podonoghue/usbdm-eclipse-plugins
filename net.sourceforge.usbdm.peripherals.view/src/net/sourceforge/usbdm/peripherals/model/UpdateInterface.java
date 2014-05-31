package net.sourceforge.usbdm.peripherals.model;

/**
 * Interface that allows triggering updates
 */
public interface UpdateInterface {
   /**
    * Forces the model to update from target
    */
   void forceUpdate();
}