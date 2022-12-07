package net.sourceforge.usbdm.deviceEditor.peripherals;

public interface Customiser {

   /**
    * Customise the peripheral<br>
    * This is done after all settings have been loaded so values are available.
    * 
    * @throws Exception
    */
   public void modifyPeripheral() throws Exception;
}
