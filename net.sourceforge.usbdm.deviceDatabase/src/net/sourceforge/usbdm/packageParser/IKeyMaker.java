package net.sourceforge.usbdm.packageParser;

public interface IKeyMaker {
   /**
    * Generate variable key from name
    * 
    * @param  name Name used to create key
    * 
    * @return Key generated from name
    */
   public String makeKey(String name);
}