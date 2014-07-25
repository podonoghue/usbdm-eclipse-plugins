package net.sourceforge.usbdm.jni;

/**
 * @since 4.10
 */
public class UsbdmException extends Exception {

   private static final long serialVersionUID = -3089219531225345831L;
   private int errorNo;
   

   /** 
    * Constructor
    * 
    * @param reason_ String describing reason, errorNo is set to -1
    */
   public UsbdmException(String reason_) {
      super(reason_);
      errorNo = -1;
   }
   /**
    * Constructor
    * 
    * @param rc USBDM rc describing the error
    */
   public UsbdmException(int rc) {
      super(Usbdm.getErrorString(rc));
      errorNo = rc;
   }
   /**
    * Get USDBM Error code
    * 
    * @return USBDM Error code - (-1) if not USBDM error
    */
   public int getErrorNo() {
      return errorNo;
   }
}
