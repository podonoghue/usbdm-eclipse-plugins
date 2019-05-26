package net.sourceforge.usbdm.jni;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
//import org.eclipse.swt.SWT;
//import org.eclipse.swt.widgets.Display;
//import org.eclipse.swt.widgets.MessageBox;
//import org.eclipse.swt.widgets.Shell;

/**
 * @author podonoghue
 * @since 4.12
 */
public class Usbdm {
   private Usbdm(int deviceNum) throws UsbdmException {
      Usbdm.open(deviceNum);
   }
   
   /**
    *  Class describing the USBDM interface version & capabilities
    */
   public static class BdmInformation {
      public int  BDMsoftwareVersion;   //!< BDM Firmware version
      public int  BDMhardwareVersion;   //!< Hardware version reported by BDM firmware
      public int  ICPsoftwareVersion;   //!< ICP Firmware version
      public int  ICPhardwareVersion;   //!< Hardware version reported by ICP firmware
      public int  capabilities;         //!< BDM Capabilities
      public int  commandBufferSize;    //!< Size of BDM Communication buffer
      public int  jtagBufferSize;       //!< Size of JTAG buffer (if supported)

      public BdmInformation() {
         super();
      }

      public BdmInformation(int bdmSV, int bdmHV, int icpSV, int icpHV, int cap, int comm, int jtag) {
         BDMsoftwareVersion = bdmSV;
         BDMhardwareVersion = bdmHV;
         ICPsoftwareVersion = icpSV;
         ICPhardwareVersion = icpHV;
         capabilities       = cap;
         commandBufferSize  = comm;
         jtagBufferSize     = jtag;
      }
      
      public String toString() {
         return 
         String.format("  bdmSWVer     = 0x%02X,\n", BDMsoftwareVersion)+
         String.format("  bdmHWVer     = 0x%02X,\n", BDMhardwareVersion)+
         String.format("  icpSWVer     = 0x%02X,\n", ICPsoftwareVersion)+
         String.format("  icpHWVer     = 0x%02X,\n", ICPhardwareVersion)+
         String.format("  capabilities = 0x%04X,\n", capabilities)+
         String.format("  commBSize    = %d,\n",     commandBufferSize)+
         String.format("  jtagBSize    = %d",        jtagBufferSize);
      }
   };
   
   /**
    *  Target processor type (interface types)
    */
   public static enum TargetType {
      T_HCS12     (0),      //!< HC12 or HCS12 target
      T_HCS08     (1),      //!< HCS08 target
      T_RS08      (2),      //!< RS08 target
      T_CFV1      (3),      //!< Coldfire Version 1 target
      T_CFVx      (4),      //!< Coldfire Version 2,3,4 target
      T_JTAG      (5),      //!< JTAG target - TAP is set to \b RUN-TEST/IDLE
      T_EZFLASH   (6),      //!< EzPort Flash interface (SPI?)
      T_MC56F80xx (7),      //!< JTAG target with MC56F80xx optimised subroutines
      T_ARM_JTAG  (8),      //!< ARM target using JTAG
      T_ARM_SWD   (9),      //!< ARM target using SWD
      T_ARM       (10),     //!< ARM target using either SWD (preferred) or JTAG as supported
      T_OFF       (0xFF),
      T_UNKNOWN   (0xFF),
      ;
      private int               mask;
      
      TargetType(int mask) {
         this.mask                 = mask;
      }
      public int getMask() {
         return mask;
      }
      public static TargetType valueOf(int mask) {
         if ((mask == T_OFF.mask) || (mask > T_ARM.ordinal())) {
            return T_OFF;
         }
         else {
            return values()[mask];
         }
      }
   };

   /**
    * Selects target Vdd on BDM supporting this function
    */
   public static enum TargetVddSelect {
      BDM_TARGET_VDD_OFF     (0x00, "Off",     "Do not supply power to the target.  An external target supply is required."),
      BDM_TARGET_VDD_3V3     (0x01, "3V3",     "Supply 3.3V to the target through the BDM connection."),
      BDM_TARGET_VDD_5V      (0x02, "5V",      "Supply 5V to the target through the BDM connection."),
      BDM_TARGET_VDD_ENABLE  (0x10, "enable" , "Enable target supply at last used value"),
      BDM_TARGET_VDD_DISABLE (0x11, "disable", "Disable target supply but previously used level unchanged"),
      ;
      private final int    mask;
      private final String legibleName;
      private final String hint;
      
      TargetVddSelect(int mask, String legibleName, String hint) {
         this.mask         = mask;
         this.legibleName  = legibleName;
         this.hint         = hint;
      }
      public int getMask() {
         return mask;
      }
      public static TargetVddSelect valueOf(int mask) {
         for (TargetVddSelect type:values()) {
            if (type.mask == mask) {
               return type;
            }
         }
         return BDM_TARGET_VDD_OFF;
      }
      public String toString() {
         return legibleName;
      }
      public String getHint() {
         return hint;
      }
   };

   /**
    * Selects reset method
    */
   public static enum ResetMethod {
      RESET_TARGETDEFAULT  (0x00, "TargetDefault",  "Default for the target"),      //!< Depends on target
      RESET_HARDWARE       (0x01, "Hardware",       "Hardware, usually reset pin"), //!< Hardware = reset pin
      RESET_SOFTWARE       (0x02, "Software",       "Software method"),             //!< Software = e.g. BKGD reset command or ARM software
      RESET_VENDOR         (0x03, "Vendor",         "Vendor custom"),               //!< Vendor specific e.g. Kinetsi MDM_AP
      RESET_NONE           (0x04, "None",           "None"),                        //!< None
      ;
      private final int    mask;          // Value used by USBDM interface
      private final String optionName;    // Name of option - also used in XML
      private final String legibleName;   // Visible name of option 

      /**
       * @param mask          // Value used by USBDM JNI interface
       * @param optionName    // Name of option. Used in XML and parameters for GDB server  e.g. "Hardware"
       * @param legibleName   // Visible name of option e.g. "Hardware, usually reset pin"
       */
      ResetMethod(int mask, String optionName, String legibleName) {
         this.mask        = mask;
         this.legibleName = legibleName;
         this.optionName  = optionName;
      }
      /**
       * Get value used by USBDM JNI Interface
       * 
       * @return USBDM JNI Interface  value
       */
      public int getMask() {
         return mask;
      }
      /**
       * Get EraseMethod from option name
       * 
       * @param  name Name to look up e.g. "RESET_VENDOR"
       * 
       * @return Reset method or RESET_TARGETDEFAULT if not found
       * 
       * @note Only differs from valueOf(String) in that it does not throw exceptions.
       */
      public static ResetMethod getResetMethod(String name) {
         try {
            return valueOf(name);
         } catch (Exception e) {
            return ResetMethod.RESET_TARGETDEFAULT;
         }
      }
      /**
       * Get EraseMethod from option name
       * 
       * @param  name Name to look up e.g. "Software"
       * 
       * @return Reset method or RESET_TARGETDEFAULT if not found
       */
      public static ResetMethod getResetMethodFromName(String name) {
         for (ResetMethod em:ResetMethod.values()) {
            if (em.optionName.equalsIgnoreCase(name)) {
               return em;
            }
         }
         return ResetMethod.RESET_TARGETDEFAULT;
      }
      /**
       * Get reset method from USBDM JNI interface value
       * 
       * @param  mask Mask to look up
       * 
       * @return Reset method or RESET_TARGETDEFAULT if not found
       */
      public static ResetMethod valueOf(int mask) {
         for (ResetMethod type:values()) {
            if (type.mask == mask) {
               return type;
            }
         }
         return RESET_TARGETDEFAULT;
      }
      /**
       * Get name of method e.g. "Hardware"
       * 
       * @return
       */
      public String getOptionName() {
         return optionName;
      }
      /**
       * Get legible name of method e.g. "Hardware, usually reset pin"
       * 
       * @return
       */
      public String getLegibleName() {
         return legibleName;
      }
      /**
       * Get Legible name for Erase method
       */
      public String toString() {
         return legibleName;
      }
   };

   /**
    * Represents the allowed reset methods and the preferred method
    * 
    * @author podonoghue
    */
   static public class ResetMethods {
      ArrayList<ResetMethod> methods         = new ArrayList<ResetMethod>();
      ResetMethod            preferredMethod = ResetMethod.RESET_TARGETDEFAULT;
      
      public ResetMethods() {
         methods.add(ResetMethod.RESET_TARGETDEFAULT);
         methods.add(ResetMethod.RESET_NONE);
      }
      /**
       * Add reset method
       * 
       * @param method Method to add
       */
      public void addMethod(ResetMethod method) {
         methods.add(method);
      }
      
      /**
       * Get reset methods available for this memory
       * 
       * @return List of reset methods
       */
      public List<ResetMethod> getMethods() {
         return methods;
      }

      /**
       * Get preferred reset method
       * 
       * @return Preferred method
       */
      public ResetMethod getPreferredMethod() {
         return preferredMethod;
      }

      /**
       * Set preferred reset method
       * 
       * @param method Preferred method
       */
      public void setPreferredMethod(ResetMethod preferredMethod) {
         this.preferredMethod = preferredMethod;
      }

   }
   
   /**
    * Reresents erase method
    */
   public static enum EraseMethod {
      ERASE_TARGETDEFAULT    (0x00, "TargetDefault",  "Default for the target"),       //!< Depends on target
      ERASE_NONE             (0x01, "None",           "No erase done"),                //!< No erase is done
      ERASE_MASS             (0x02, "Mass",           "Mass erase device"),            //!< A Mass erase operation is done
      ERASE_ALL              (0x03, "All",            "Block erase entire device"),    //!< All Flash is erased (by Flash Block)
      ERASE_SELECTIVE        (0x04, "Selective",      "Selective erase (by sector)"),  //!< A selective erase (by sector) is done
      ;
      private final int    mask;          // Value used by USBDM interface
      private final String optionName;    // Name of option - also used in XML
      private final String legibleName;   // Visible name of option
      
      /**
       * @param mask          // Value used by USBDM JNI interface
       * @param optionName    // Name of option - also used in XML
       * @param legibleName   // Visible name of option
       */
      EraseMethod(int mask, String optionName, String legibleName) {
         this.mask        = mask;
         this.optionName  = optionName;
         this.legibleName = legibleName;
      }
      
      /**
       * Get value used by USBDM JNI Interface
       * 
       * @return USBDM JNI Interface  value
       */
      public int getMask() {
         return mask;
      }
      /**
       * Get erase method from USBDM JNI interface value
       * 
       * @param mask Mask to look up
       * 
       * @return erase method or ERASE_TARGETDEFAULT if not found
       */
      public static EraseMethod valueOf(int mask) {
         for (EraseMethod type:values()) {
            if (type.mask == mask) {
               return type;
            }
         }
         return ERASE_TARGETDEFAULT;
      }
      /**
       * Get EraseMethod from option name
       * 
       * @param   name Name to look up e.g. "ERASE_SELECTIVE"
       * 
       * @return erase method or ERASE_TARGETDEFAULT if not found
       * 
       * @note Only differs from valueOf(String) in that it does not throw exceptions.
       */
      public static EraseMethod getEraseMethod(String name) {
         try {
            return valueOf(name);
         } catch (Exception e) {
            return ERASE_TARGETDEFAULT;
         }
      }
      /**
       * Get EraseMethod from option name
       * 
       * @param  name Name to look up e.g. "Software"
       * 
       * @return erase method or ERASE_TARGETDEFAULT if not found
       */
      public static EraseMethod getEraseMethodFromName(String name) {
         for (EraseMethod em:EraseMethod.values()) {
            if (em.optionName.equalsIgnoreCase(name)) {
               return em;
            }
         }
         return ERASE_TARGETDEFAULT;
      }
      /**
       * Get name of method e.g. "Selective"
       * 
       * @return
       */
      public String getOptionName() {
         return optionName;
      }
      /**
       * Get legible name of method e.g. "Selective erase (by sector)"
       * 
       * @return
       */
      public String getLegibleName() {
         return legibleName;
      }
      /**
       * Get Legible name for Erase method
       */
      public String toString() {
         return legibleName;
      }
   };

   /**
    * Represents the allowed erase methods and the preferred method
    * 
    * @author podonoghue
    */
   static public class EraseMethods {
      ArrayList<EraseMethod> methods         = new ArrayList<EraseMethod>();
      EraseMethod            preferredMethod = EraseMethod.ERASE_TARGETDEFAULT;
      
      public EraseMethods() {
         methods.add(EraseMethod.ERASE_TARGETDEFAULT);
         methods.add(EraseMethod.ERASE_NONE);
      }
      /**
       * Add erase method
       * 
       * @param method Method to add
       */
      public void addMethod(EraseMethod method) {
         methods.add(method);
      }
      
      /**
       * Get erase methods available for this memory
       * 
       * @return List of erase methods
       */
      public List<EraseMethod> getMethods() {
         return methods;
      }

      /**
       * Get preferred erase method
       * 
       * @return Preferred method
       */
      public EraseMethod getPreferredMethod() {
         return preferredMethod;
      }

      /**
       * Set preferred erase method
       * 
       * @param method Preferred method
       */
      public void setPreferredMethod(EraseMethod preferredMethod) {
         this.preferredMethod = preferredMethod;
      }

   }
   
   /**
    * Selects erase method
    */
   public static enum SecurityOptions {
      SECURITY_IMAGE     (0x00, "Image",     "Security field from flash image"),               //!< Security region is not modified by programmer
      SECURITY_UNSECURED (0x02, "Unsecured", "Default unsecured configuration"),               //!< Security region is forced to a default secured state
      SECURITY_SMART     (0x03, "Smart",     "Unsecured if no security field in flash image"), //!< Default unsecured is used if no security region in flash image
      SECURITY_SECURED   (0x01, "Secured",   "Default secured configuration"),                 //!< Security region is forced to a default unsecured state
      SECURITY_CUSTOM    (0x04, "Custom",    "Custom Settings"),                               //!< Custom settings used (not implemented)
      ;
      private final int    mask;          // Value used by USBDM JNI interface
      private final String legibleName;   // Visible name
      private final String optionName;    // Name of option - also used in XML

      /**
       * @param mask          // Value used by USBDM JNI interface
       * @param optionName    // Name of option - also used in XML e.g. "Unsecured"
       * @param legibleName   // Visible name of option e.g. "Default unsecured configuration"
       */
      SecurityOptions(int mask, String optionName, String legibleName) {
         this.mask        = mask;
         this.legibleName = legibleName;
         this.optionName  = optionName;
      }

      /**
       * Get value used by USBDM JNI Interface
       * 
       * @return USBDM JNI Interface  value
       */
      public int getMask() {
         return mask;
      }
      /**
       * Get security option from USBDM JNI interface value
       * 
       * @param mask
       * 
       * @return erase method or ERASE_TARGETDEFAULT if not found
       */
      public static SecurityOptions valueOf(int mask) {
         for (SecurityOptions type:values()) {
            if (type.mask == mask) {
               return type;
            }
         }
         return SECURITY_UNSECURED;
      }
      /**
       * Get name of option e.g. "Unsecured"
       * 
       * @return
       */
      public String getOptionName() {
         return optionName;
      }
      /**
       * Get legible name of option e.g. "Default unsecured configuration"
       * 
       * @return
       */
      public String getLegibleName() {
         return legibleName;
      }
      public String toString() {
         return legibleName;
      }
   };

   /**
    * Selects target Vpp on BDM supporting this function (RS08 programming)
    */
   public static enum TargetVppSelect {
      BDM_TARGET_VPP_OFF       (0),   //!< Target Vpp Off
      BDM_TARGET_VPP_STANDBY   (1),   //!< Target Vpp Standby (Inverter on, Vpp off)
      BDM_TARGET_VPP_ON        (2),   //!< Target Vpp On
      BDM_TARGET_VPP_ERROR     (3),   //!< Target Vpp ??
      ;
      private int mask;
      TargetVppSelect(int mask) {
         this.mask = mask;
      }
      public int getMask() {
         return mask;
      }
   };
   
   /**
    * Used to report target Vdd
    */
   public static enum TargetVddState {
      BDM_TARGET_VDD_NONE,   //!< Target Vdd not detected
      BDM_TARGET_VDD_EXT ,   //!< Target Vdd external
      BDM_TARGET_VDD_INT ,   //!< Target Vdd internal
      BDM_TARGET_VDD_ERR ,   //!< Target Vdd error
      ;
      public static TargetVddState valueOf(int mask) {
         if (mask > BDM_TARGET_VDD_ERR.ordinal()) {
            return BDM_TARGET_VDD_ERR;
         }
         else {
            return values()[mask];
         }
      }
   };
   
   /**
    * Used to report connection status 
    */
   public static enum ConnectMode {
      SPEED_NO_INFO      ,   //!< Not connected
      SPEED_SYNC         ,   //!< Speed determined by SYNC
      SPEED_GUESSED      ,   //!< Speed determined by trial & error
      SPEED_USER_SUPPLIED    //!< User has specified the speed to use
      ;
      public static ConnectMode valueOf(int mask) {
         if (mask > SPEED_USER_SUPPLIED.ordinal()) {
            return SPEED_NO_INFO;
         }
         else {
            return values()[mask];
         }
      }
   };
   
   /**
    * Determines when automatic re-connection will be done 
    */
   public static enum AutoConnect {
      AUTOCONNECT_NEVER    ("Never", 0),  // Only connect explicitly
      AUTOCONNECT_STATUS   ("Status", 1),  // Reconnect on ReadStatusReg()
      AUTOCONNECT_ALWAYS   ("Always", 2),  // Reconnect before every command
      ;
      private int mask;
      private String name;
      
      AutoConnect(String name, int mask) {
         this.name = name;
         this.mask = mask;
      }
      public int getMask() {
         return mask;
      }
      /**
       * @since 4.11
       */
      public String getOptionName() {
         return name;
      }
      public static AutoConnect valueOf(int mask) {
         if (mask > AUTOCONNECT_ALWAYS.ordinal()) {
            return AUTOCONNECT_STATUS;
         }
         else {
            return values()[mask];
         }
      }
   };
   /**
    * Determines BDM Clock source of HCS08/HCS12 etc devices
    */
   public static enum ClkSwValues {
      CLKSW_DEFAULT (255),  //!< CLKSW has default value (varies by reset type)
      CLKSW_BUS     (1),    //!< CLKSW selects Bus Clock for BDM
      CLKSW_ALT     (2),    //!< CLKSW selects Alt Clock for BDM
      ;
      private int mask;
      
      ClkSwValues(int mask) {
         this.mask = mask;
      }
      public int getMask() {
         return mask;
      }
      public static ClkSwValues valueOf(int mask) {
         for (ClkSwValues clkSel:values()) {
            if (clkSel.mask == mask) {
               return clkSel;
            }
         }
         return CLKSW_DEFAULT;
      }
   };
   
   /** 
    * Identifies the memory space and access size
    */
   public static class MemorySpace {
      private int mask;
      
      // Masks for above
      public static final int SIZE      = (0x7<<0);   // Size
      public static final int SPACE     = (0x7<<4);   // Memory space

      public MemorySpace(MemorySpace size, int space, int modifiers) {
         mask = (size.mask&SIZE)|(space&SPACE)|modifiers;
      };
      public MemorySpace(MemorySpace size, int space) {
         mask = (size.mask&SIZE)|(space&SPACE);
      };
      private MemorySpace(int size) {
         mask = (size&SIZE)|MemorySpace.None;
      };
      
      // One of the following
      public static final MemorySpace Byte = new MemorySpace(1);  // Byte (8-bit) access
      public static final MemorySpace Word = new MemorySpace(2);  // Word (16-bit) access
      public static final MemorySpace Long = new MemorySpace(4);  // Long (32-bit) access

      // One of the following
      public static final int None      = (0<<4);     // Memory space unused/undifferentiated
      public static final int Program   = (1<<4);     // Program memory space (e.g. P: on DSC)
      public static final int Data      = (2<<4);     // Data memory space (e.g. X: on DSC)
      public static final int Global    = (3<<4);     // HCS12 Global addresses (Using BDMPPR register)
      
      // Fast memory access for HCS08/HCS12 (stopped target), regs. are modified
      public static final int Fast      = (1<<7);
      
      // For convenience (DSC)
      public static final MemorySpace PWord     = new MemorySpace(Word,Program);
      public static final MemorySpace PLong     = new MemorySpace(Long,Program);
      public static final MemorySpace XByte     = new MemorySpace(Byte,Data);
      public static final MemorySpace XWord     = new MemorySpace(Word,Data);
      public static final MemorySpace XLong     = new MemorySpace(Long,Data);

      public int getMask() {
         return mask;
      }
   };
   
   public static final MemorySpace BYTE = MemorySpace.Byte;
   public static final MemorySpace WORD = MemorySpace.Word;
   public static final MemorySpace LONG = MemorySpace.Long;
   
   /**
    *  Describes Reset mode and method
    */
   public static class ResetType {
      enum Mode {
         // Modes
         SPECIAL   (0<<0), //!< Special mode [BDM active, Target halted]
         NORMAL    (1<<0), //!< Normal mode [usual reset, Target executes]
         ;
         int mask;
         Mode(int mode) {
            this.mask = mode;
         } 
      };
      enum Method {
         // Methods
         ALL       (0<<2), //!< Use all reset strategies as appropriate
         HARDWARE  (1<<2), //!< Use hardware RESET pin reset
         SOFTWARE  (2<<2), //!< Use software (BDM commands) reset
         POWER     (3<<2), //!< Cycle power
         DEFAULT   (7<<2), //!< Use target specific default method
         ;
         int mask;
         Method(int mode) {
            this.mask = mode;
         }
      };
      public static final int MODE_MASK   = (3<<0);
      public static final int METHOD_MASK = (7<<2);

      public static final ResetType RESET_SPECIAL = new ResetType(ResetType.Mode.SPECIAL, ResetType.Method.DEFAULT);
      public static final ResetType RESET_NORMAL  = new ResetType(ResetType.Mode.NORMAL,  ResetType.Method.DEFAULT);

      private int mask;
      public ResetType(Mode mode, Method method) {
         this.mask = mode.mask|method.mask;
      }
      public ResetType(Mode mode) {
         this.mask = mode.mask|Method.DEFAULT.mask;
      }
      public int getMask() {
         return mask;
      }
      private static final String modes[]   = {"Special", "Normal"}; 
      private static final String methods[] = {"All", "Hardware", "Software", "Power", "Invalid", "Invalid", "Invalid", "Default"}; 
      public String toString() {
         if ((mask&~(MODE_MASK|METHOD_MASK)) != 0) {
            return "Invalid";
         }
         return (methods[mask&MODE_MASK]+"|"+modes[mask&METHOD_MASK]);
      }
   };

   public static final ResetType RESET_SPECIAL = ResetType.RESET_SPECIAL;
   public static final ResetType RESET_NORMAL  = ResetType.RESET_NORMAL;
   
   /**
    *  Register accesses use this to identify the register
    */
   public static interface regInterface {
      public enum RegSpace {
         NO_REG_SPACE,
         C_REG_SPACE,
         D_REG_SPACE,
         ;
         public int getmask() {
            return ordinal();
         }
      }
      public int      getNo();
      public RegSpace getSpace();
      public String   name();
   };
   
   /**
    *  This allows access to an unanticipated register
    */
   public static class CustomReg implements regInterface {
      private int      regNo;
      private RegSpace space;
      private String   name;
      
      public CustomReg(regInterface reg) {
         this.regNo = reg.getNo();
         this.space = reg.getSpace();
         this.name  = reg.name();
      }
      public CustomReg(int regNo, RegSpace space, String name) {
         this.regNo = regNo;
         this.space = space;
         this.name  = name;
      }
      public CustomReg(int regNo, RegSpace space) {
         this.regNo = regNo;
         this.space = space;
         this.name  = "";
      }
      public CustomReg(int regNo) {
         this.regNo = regNo;
         this.space = RegSpace.NO_REG_SPACE;
         this.name  = "";
      }
      public int getNo() {
         return regNo;
      }
      public RegSpace getSpace() {
         return space;
      }
      public String name() {
         return name;
      }
      public String toString() {
         return "("+name+":"+space+"."+regNo+")";
      }
   }
   
   /**
    *  Standard registers (predefined)
    */
   public static enum Reg implements regInterface {
      // HCS12
      HCS12_RegPC        (3),      //!< PC reg
      HCS12_RegD         (4),      //!< D reg
      HCS12_RegX         (5),      //!< X reg
      HCS12_RegY         (6),      //!< Y reg
      HCS12_RegSP        (7),      //!< SP reg
                         
      HCS12_RegBDMSTS    (0xFF01,RegSpace.D_REG_SPACE), //!< BDMSTS (debug status/control) register
      HCS12_RegCCR       (0xFF06,RegSpace.D_REG_SPACE), //!< Saved Target CCR
      HCS12_RegBDMINR    (0xFF07,RegSpace.D_REG_SPACE), //!< BDM Internal Register Position Register
                         
      // HCS08           
      HCS08_RegPC        (0xB),    //!< PC  reg
      HCS08_RegSP        (0xF),    //!< SP  reg
      HCS08_RegHX        (0xC),    //!< HX  reg
      HCS08_RegA         (8),      //!< A   reg
      HCS08_RegCCR       (9),      //!< CCR reg
                         
      HCS08_RegBKPT      (0x0,RegSpace.D_REG_SPACE), //!< Breakpoint register
                         
      //RS08             
      RS08_RegCCR_PC     (0xB),    //!< Combined CCR/PC register
      RS08_RegSPC        (0xF),    //!< Shadow PC
      RS08_RegA          (8),      //!< A reg
                         
      // CFV1            
      CFV1_RegD0         (0),               //!< D0
      CFV1_RegD1         (1),               //!< D1
      CFV1_RegD2         (2),               //!< D2
      CFV1_RegD3         (3),               //!< D3
      CFV1_RegD4         (4),               //!< D4
      CFV1_RegD5         (5),               //!< D5
      CFV1_RegD6         (6),               //!< D6
      CFV1_RegD7         (7),               //!< D7
      CFV1_RegA0         (8),               //!< A0
      CFV1_RegA1         (9),               //!< A1
      CFV1_RegA2         (10),              //!< A2
      CFV1_RegA3         (11),              //!< A3
      CFV1_RegA4         (12),              //!< A4
      CFV1_RegA5         (13),              //!< A5
      CFV1_RegA6         (14),              //!< A6
      CFV1_RegA7         (15),              //!< A7
      CFV1_PSTBASE       (16),              //!< Start of PST registers, access as CFV1_PSTBASE+n
                         
      CFV1_RegOTHER_A7   (0 ,RegSpace.C_REG_SPACE),  //!< Other A7 (not active in target)
      CFV1_RegVBR        (1 ,RegSpace.C_REG_SPACE),  //!< Vector Base register
      CFV1_RegCPUCR      (2 ,RegSpace.C_REG_SPACE),  //!< CPUCR
      CFV1_RegSR         (14,RegSpace.C_REG_SPACE),  //!< Status register
      CFV1_RegPC         (15,RegSpace.C_REG_SPACE),  //!< Program Counter

      CFV1_RegCSR        (0x00,RegSpace.D_REG_SPACE),   //!< CSR
      CFV1_RegXCSR       (0x01,RegSpace.D_REG_SPACE),   //!< XCSR
      CFV1_RegCSR2       (0x02,RegSpace.D_REG_SPACE),   //!< CSR2
      CFV1_RegCSR3       (0x03,RegSpace.D_REG_SPACE),   //!< CSR3
      CFV1_RegBAAR       (0x05,RegSpace.D_REG_SPACE),   //!< BAAR
      CFV1_RegAATR       (0x06,RegSpace.D_REG_SPACE),   //!< AATR
      CFV1_RegTDR        (0x07,RegSpace.D_REG_SPACE),   //!< TDR
      CFV1_RegPBR0       (0x08,RegSpace.D_REG_SPACE),   //!< PBR0
      CFV1_RegPBMR       (0x09,RegSpace.D_REG_SPACE),   //!< PBMR - mask for PBR0
      CFV1_RegABHR       (0x0C,RegSpace.D_REG_SPACE),   //!< ABHR
      CFV1_RegABLR       (0x0D,RegSpace.D_REG_SPACE),   //!< ABLR
      CFV1_RegDBR        (0x0E,RegSpace.D_REG_SPACE),   //!< DBR
      CFV1_RegBDMR       (0x0F,RegSpace.D_REG_SPACE),   //!< DBMR - mask for DBR
      CFV1_RegPBR1       (0x18,RegSpace.D_REG_SPACE),   //!< PBR1
      CFV1_RegPBR2       (0x1A,RegSpace.D_REG_SPACE),   //!< PBR2
      CFV1_RegPBR3       (0x1B,RegSpace.D_REG_SPACE),   //!< PBR3

      CFV1_ByteRegs      (0x1000,RegSpace.D_REG_SPACE),                                 // Special access to msb
      CFV1_RegXCSRbyte   (CFV1_RegXCSR.regNo+CFV1_ByteRegs.regNo,RegSpace.D_REG_SPACE), //!< XCSR.msb
      CFV1_RegCSR2byte   (CFV1_RegCSR2.regNo+CFV1_ByteRegs.regNo,RegSpace.D_REG_SPACE), //!< CSR2.msb
      CFV1_RegCSR3byte   (CFV1_RegCSR3.regNo+CFV1_ByteRegs.regNo,RegSpace.D_REG_SPACE), //!< CSR3.msb
      
      // CFVx
      CFVx_RegD0         (0),      //!< D0
      CFVx_RegD1         (1),      //!< D1
      CFVx_RegD2         (2),      //!< D2
      CFVx_RegD3         (3),      //!< D3
      CFVx_RegD4         (4),      //!< D4
      CFVx_RegD5         (5),      //!< D5
      CFVx_RegD6         (6),      //!< D6
      CFVx_RegD7         (7),      //!< D7
      CFVx_RegA0         (8),      //!< A0
      CFVx_RegA1         (9),      //!< A1
      CFVx_RegA2         (10),     //!< A2
      CFVx_RegA3         (11),     //!< A3
      CFVx_RegA4         (12),     //!< A4
      CFVx_RegA5         (13),     //!< A5
      CFVx_RegA6         (14),     //!< A6
      CFVx_RegA7         (15),     //!< A7

      CFVx_CRegD0        (0x80,RegSpace.C_REG_SPACE),   //!< D0-D7
      CFVx_CRegD1        (0x81,RegSpace.C_REG_SPACE),
      CFVx_CRegD2        (0x82,RegSpace.C_REG_SPACE),
      CFVx_CRegD3        (0x83,RegSpace.C_REG_SPACE),
      CFVx_CRegD4        (0x84,RegSpace.C_REG_SPACE),
      CFVx_CRegD5        (0x85,RegSpace.C_REG_SPACE),
      CFVx_CRegD6        (0x86,RegSpace.C_REG_SPACE),
      CFVx_CRegD7        (0x87,RegSpace.C_REG_SPACE),
      CFVx_CRegA0        (0x88,RegSpace.C_REG_SPACE),   //!< A0-A7
      CFVx_CRegA1        (0x89,RegSpace.C_REG_SPACE),
      CFVx_CRegA2        (0x8A,RegSpace.C_REG_SPACE),
      CFVx_CRegA3        (0x8B,RegSpace.C_REG_SPACE),
      CFVx_CRegA4        (0x8C,RegSpace.C_REG_SPACE),
      CFVx_CRegA5        (0x8D,RegSpace.C_REG_SPACE),
      CFVx_CRegA6        (0x8E,RegSpace.C_REG_SPACE),
      CFVx_RegUSER_SP    (0x8F),
      CFVx_RegOTHER_SP   (0x800,RegSpace.C_REG_SPACE), //!< Other A7 (not active in target)
      CFVx_RegVBR        (0x801,RegSpace.C_REG_SPACE), //!< Vector Base register
      CFVx_RegSR         (0x80E,RegSpace.C_REG_SPACE), //!< Status Register
      CFVx_RegPC         (0x80F,RegSpace.C_REG_SPACE), //!< Program Counter
      CFV1_RegFLASHBAR   (0xC04,RegSpace.C_REG_SPACE), //!< Flash Base register
      CFV1_RegRAMBAR     (0xC05,RegSpace.C_REG_SPACE), //!< RAM Base register
      
      CFVx_RegCSR        (0x00,RegSpace.D_REG_SPACE),  //!< CSR                        
      CFVx_RegBAAR       (0x05,RegSpace.D_REG_SPACE),  //!< BAAR   
      CFVx_RegAATR       (0x06,RegSpace.D_REG_SPACE),  //!< AATR
      CFVx_RegTDR        (0x07,RegSpace.D_REG_SPACE),  //!< TDR 
      CFVx_RegPBR0       (0x08,RegSpace.D_REG_SPACE),  //!< PBR0
      CFVx_RegPBMR       (0x09,RegSpace.D_REG_SPACE),  //!< PBMR
      CFVx_RegABHR       (0x0C,RegSpace.D_REG_SPACE),  //!< ABHR        
      CFVx_RegABLR       (0x0D,RegSpace.D_REG_SPACE),  //!< ABLR        
      CFVx_RegDBR        (0x0E,RegSpace.D_REG_SPACE),  //!< DBR         
      CFVx_RegBDMR       (0x0F,RegSpace.D_REG_SPACE),  //!< DBMR
      CFVx_RegPBR1       (0x18,RegSpace.D_REG_SPACE),  //!< PBR1       
      CFVx_RegPBR2       (0x1A,RegSpace.D_REG_SPACE),  //!< PBR2       
      CFVx_RegPBR3       (0x1B,RegSpace.D_REG_SPACE),  //!< PBR3       
      
      // Kinetis ARM
      ARM_RegR0          (0),      //!< R0
      ARM_RegR1          (1),      //!< R1
      ARM_RegR2          (2),      //!< R2
      ARM_RegR3          (3),      //!< R3
      ARM_RegR4          (4),      //!< R4
      ARM_RegR5          (5),      //!< R5
      ARM_RegR6          (6),      //!< R6
      ARM_RegR7          (7),      //!< R7
      ARM_RegR8          (8),      //!< R8
      ARM_RegR9          (9),      //!< R9
      ARM_RegR10         (10),     //!< R10
      ARM_RegR11         (11),     //!< R11
      ARM_RegR12         (12),     //!< R12
      ARM_RegSP          (13),     //!< SP
      ARM_RegLR          (14),     //!< LR
      ARM_RegPC          (15),     //!< PC (Debug return address)
      ARM_RegxPSR        (16),     //!< xPSR
      ARM_RegMSP         (17),     //!< Main Stack Ptr
      ARM_RegPSP         (18),     //!< Process Stack Ptr
      ARM_RegMISC        (20),     // [31:24]=CONTROL,[23:16]=FAULTMASK,[15:8]=BASEPRI,[7:0]=PRIMASK.
      //                           
      ARM_RegFPSCR       (0x21),   //!<
      ARM_RegFPS0        (0x40),   //!<

      // AP#0 - Common ARM AHB-AP
      ARM_RegAHB_AP_CSW       (0x00000000,RegSpace.C_REG_SPACE),   //!< AHB-AP Control/Status Word register
      ARM_RegAHB_AP_TAR       (0x00000004,RegSpace.C_REG_SPACE),   //!< AHB-AP Transfer Address register
      ARM_RegAHB_AP_DRW       (0x0000000C,RegSpace.C_REG_SPACE),   //!< AHB-AP Data Read/Write register

      ARM_RegAHB_AP_CFG       (0x000000F4,RegSpace.C_REG_SPACE),   //!< AHB-AP Config register
      ARM_RegAHB_AP_Base      (0x000000F8,RegSpace.C_REG_SPACE),   //!< AHB-AP IDebug base address register
      ARM_RegAHB_AP_Id        (0x000000FC,RegSpace.C_REG_SPACE),   //!< AHB-AP ID Register

      // AP#1 - Kinetis MDM-AP registers
      ARM_RegMDM_AP_Status    (0x01000000,RegSpace.C_REG_SPACE),   //!< Status register
      ARM_RegMDM_AP_Control   (0x01000004,RegSpace.C_REG_SPACE),   //!< Control register
      ARM_RegMDM_AP_Ident     (0x010000FC,RegSpace.C_REG_SPACE),   //!< Identifier register (should read 0x001C_0000)

      ARM_RegIDCODE           (0,RegSpace.D_REG_SPACE),    //!< IDCODE  reg - read, SWD-AP only
      ARM_RegABORT            (0,RegSpace.D_REG_SPACE),    //!< ABORT   reg - write only
      ARM_RegSTATUS           (1,RegSpace.D_REG_SPACE),    //!< STATUS  reg - read only
      ARM_RegCONTROL          (1,RegSpace.D_REG_SPACE),    //!< CONTROL reg - write only
      ARM_RegRESEND           (2,RegSpace.D_REG_SPACE),    //!< RESEND  reg - read only
      ARM_RegSELECT           (2,RegSpace.D_REG_SPACE),    //!< SELECT  reg - write only
      ARM_RegRDBUFF           (3,RegSpace.D_REG_SPACE),    //!< RDBUFF  reg - read only

      ARM_RegAPReg0           (4,RegSpace.D_REG_SPACE),    //!< AP reg #0
      ARM_RegAPReg1           (5,RegSpace.D_REG_SPACE),    //!< AP reg #1
      ARM_RegAPReg2           (6,RegSpace.D_REG_SPACE),    //!< AP reg #2
      ARM_RegAPReg3           (7,RegSpace.D_REG_SPACE),    //!< AP reg #3
      ;
      private int      regNo;
      private RegSpace space;

      Reg(int regNo, RegSpace space) {
         this.regNo = regNo;
         this.space = space;
      }
      Reg(int regNo) {
         this.regNo = regNo;
         this.space = RegSpace.NO_REG_SPACE;
      }
      public int getNo() {
         return regNo;
      }
      public RegSpace getSpace() {
         return space;
      }
      public String toString() {
         return "("+this.name()+":"+space+"."+regNo+")";
      }
   }
   
   /**
    *   Pin Control masks
    */
   public static final int PIN_BKGD_OFFS      = (0);
   public static final int PIN_BKGD           = (3<<PIN_BKGD_OFFS);  //!< Mask for BKGD values (PIN_BKGD_LOW, PIN_BKGD_HIGH & PIN_BKGD_3STATE)
   public static final int PIN_BKGD_NC        = (0<<PIN_BKGD_OFFS);  //!< No change
   public static final int PIN_BKGD_3STATE    = (1<<PIN_BKGD_OFFS);  //!< Set BKGD 3-state
   public static final int PIN_BKGD_LOW       = (2<<PIN_BKGD_OFFS);  //!< Set BKGD low
   public static final int PIN_BKGD_HIGH      = (3<<PIN_BKGD_OFFS);  //!< Set BKGD high

   public static final int PIN_RESET_OFFS     = (2);
   public static final int PIN_RESET          = (3<<PIN_RESET_OFFS); //!< Mask for RESET values (PIN_RESET_LOW & PIN_RESET_3STATE)
   public static final int PIN_RESET_NC       = (0<<PIN_RESET_OFFS); //!< No change
   public static final int PIN_RESET_3STATE   = (1<<PIN_RESET_OFFS); //!< Set Reset 3-state
   public static final int PIN_RESET_LOW      = (2<<PIN_RESET_OFFS); //!< Set Reset low

   public static final int PIN_TA_OFFS        = (4);
   public static final int PIN_TA             = (3<<PIN_TA_OFFS);    //!< Mask for TA signal
   public static final int PIN_TA_NC          = (0<<PIN_TA_OFFS);    //!< No change
   public static final int PIN_TA_3STATE      = (1<<PIN_TA_OFFS);    //!< Set TA 3-state
   public static final int PIN_TA_LOW         = (2<<PIN_TA_OFFS);    //!< Set TA low

   public static final int PIN_DE_OFFS        = (4);
   public static final int PIN_DE             = (3<<PIN_DE_OFFS);    //!< Mask for DE signal
   public static final int PIN_DE_NC          = (0<<PIN_DE_OFFS);    //!< No change
   public static final int PIN_DE_3STATE      = (1<<PIN_DE_OFFS);    //!< Set DE 3-state
   public static final int PIN_DE_LOW         = (2<<PIN_DE_OFFS);    //!< Set DE low

   public static final int PIN_TRST_OFFS      = (6);
   public static final int PIN_TRST           = (3<<PIN_TRST_OFFS);  //!< Mask for TRST signal (not implemented)
   public static final int PIN_TRST_NC        = (0<<PIN_TRST_OFFS);  //!< No change
   public static final int PIN_TRST_3STATE    = (1<<PIN_TRST_OFFS);  //!< Set TRST 3-state
   public static final int PIN_TRST_LOW       = (2<<PIN_TRST_OFFS);  //!< Set TRST low

   public static final int PIN_BKPT_OFFS      = (8);
   public static final int PIN_BKPT           = (3<<PIN_BKPT_OFFS);  //!< Mask for BKPT signal
   public static final int PIN_BKPT_NC        = (0<<PIN_BKPT_OFFS);  //!< No change
   public static final int PIN_BKPT_3STATE    = (1<<PIN_BKPT_OFFS);  //!< Set BKPT 3-state
   public static final int PIN_BKPT_LOW       = (2<<PIN_BKPT_OFFS);  //!< Set BKPT low

   public static final int PIN_SWD_OFFS       = (10);
   public static final int PIN_SWD            = (3<<PIN_SWD_OFFS);   //!< Mask for SWD values (PIN_SWD_LOW; PIN_SWD_HIGH & PIN_SWD_3STATE)
   public static final int PIN_SWD_NC         = (0<<PIN_SWD_OFFS);   //!< No change
   public static final int PIN_SWD_3STATE     = (1<<PIN_SWD_OFFS);   //!< Set SWD 3-state
   public static final int PIN_SWD_LOW        = (2<<PIN_SWD_OFFS);   //!< Set SWD low
   public static final int PIN_SWD_HIGH       = (3<<PIN_SWD_OFFS);   //!< Set SWD high

   public static final int PIN_NOCHANGE       = 0;    //!< No change to pins (used to get pin status)
   public static final int PIN_RELEASE        = -1;   //!< Release all pins (go to default for current target)

   /**
    *  Internal class to JNI layer
    */
   private static class ExtendedOptions_ {
      // Options passed to the BDM
      public int               targetType;                 //!< Target type
      public int               targetVdd;                  //!< Target Vdd (off, 3.3V or 5V)
      public boolean           cycleVddOnReset;            //!< Cycle target Power  when resetting
      public boolean           cycleVddOnConnect;          //!< Cycle target Power if connection problems)
      public boolean           leaveTargetPowered;         //!< Leave target power on exit
      public int               autoReconnect;              //!< Automatically re-connect to target (for speed change)
      public boolean           guessSpeed;                 //!< Guess speed for target w/o ACKN
      public int               bdmClockSource;             //!< BDM clock source in target
      public boolean           useResetSignal;             //!< Whether to use RESET signal on BDM interface
      public boolean           maskInterrupts;             //!< Whether to mask interrupts when  stepping
      public int               interfaceFrequency;         //!< CFVx/JTAG etc - Interface speed (kHz)
      public boolean           usePSTSignals;              //!< CFVx, PST Signal monitors
      public int               powerOffDuration;           //!< How long to remove power (ms)
      public int               powerOnRecoveryInterval;    //!< How long to wait after power enabled (ms)
      public int               resetDuration;              //!< How long to assert reset (ms)
      public int               resetReleaseInterval;       //!< How long to wait after reset release to release other signals (ms)
      public int               resetRecoveryInterval;      //!< How long to wait after reset sequence completes (ms)
      
      public ExtendedOptions_(TargetType targetType) {
         this.targetType = targetType.getMask();
      };
         
      public ExtendedOptions_(ExtendedOptions options) {
         targetType               = options.targetType.getMask();           
         targetVdd                = options.targetVdd.getMask();  
         cycleVddOnReset          = options.cycleVddOnReset;  
         cycleVddOnConnect        = options.cycleVddOnConnect;  
         leaveTargetPowered       = options.leaveTargetPowered;  
         autoReconnect            = options.autoReconnect.getMask();  
         guessSpeed               = options.guessSpeed;  
         bdmClockSource           = options.bdmClockSource.getMask();  
         useResetSignal           = options.useResetSignal;  
         maskInterrupts           = options.maskInterrupts;  
         interfaceFrequency       = options.interfaceFrequency;  
         usePSTSignals            = options.usePstSignals;  
         powerOffDuration         = options.powerOffDuration;  
         powerOnRecoveryInterval  = options.powerOnRecoveryInterval;  
         resetDuration            = options.resetDuration;  
         resetReleaseInterval     = options.resetReleaseInterval ;  
         resetRecoveryInterval    = options.resetRecoveryInterval;  
      }
   };

   /**
    *  Describes interface options
    */
   public static class ExtendedOptions {
      // Options passed to the BDM
      public TargetType        targetType;                 //!< Target type
      public TargetVddSelect   targetVdd;                  //!< Target Vdd (off, 3.3V or 5V)
      public boolean           cycleVddOnReset;            //!< Cycle target Power  when resetting
      public boolean           cycleVddOnConnect;          //!< Cycle target Power if connection problems)
      public boolean           leaveTargetPowered;         //!< Leave target power on exit
      public AutoConnect       autoReconnect;              //!< Automatically re-connect to target (for speed change)
      public boolean           guessSpeed;                 //!< Guess speed for target w/o ACKN
      public ClkSwValues       bdmClockSource;             //!< BDM clock source in target
      public boolean           useResetSignal;             //!< Whether to use RESET signal on BDM interface
      public boolean           maskInterrupts;             //!< Whether to mask interrupts when  stepping
      public int               interfaceFrequency;         //!< CFVx/JTAG etc - Interface speed (kHz)
      public boolean           usePstSignals;              //!< CFVx, PST Signal monitors
      public int               powerOffDuration;           //!< How long to remove power (ms)
      public int               powerOnRecoveryInterval;    //!< How long to wait after power enabled (ms)
      public int               resetDuration;              //!< How long to assert reset (ms)
      public int               resetReleaseInterval;       //!< How long to wait after reset release to release other signals (ms)
      public int               resetRecoveryInterval;      //!< How long to wait after reset sequence completes (ms)
      
      public ExtendedOptions(TargetType targetType) {
         this.targetType = targetType;
         targetVdd       = TargetVddSelect.BDM_TARGET_VDD_OFF;
         autoReconnect   = AutoConnect.AUTOCONNECT_STATUS;
         bdmClockSource  = ClkSwValues.CLKSW_DEFAULT;
      }
      public ExtendedOptions(ExtendedOptions_ options_) {
         targetType               = TargetType.valueOf(options_.targetType);           
         targetVdd                = TargetVddSelect.valueOf(options_.targetVdd);  
         cycleVddOnReset          = options_.cycleVddOnReset;  
         cycleVddOnConnect        = options_.cycleVddOnConnect;  
         leaveTargetPowered       = options_.leaveTargetPowered;  
         autoReconnect            = AutoConnect.valueOf(options_.autoReconnect);  
         guessSpeed               = options_.guessSpeed;  
         bdmClockSource           = ClkSwValues.valueOf(options_.bdmClockSource);  
         useResetSignal           = options_.useResetSignal;  
         maskInterrupts           = options_.maskInterrupts;  
         interfaceFrequency       = options_.interfaceFrequency;  
         usePstSignals            = options_.usePSTSignals;  
         powerOffDuration         = options_.powerOffDuration;  
         powerOnRecoveryInterval  = options_.powerOnRecoveryInterval;  
         resetDuration            = options_.resetDuration;  
         resetReleaseInterval     = options_.resetReleaseInterval ;  
         resetRecoveryInterval    = options_.resetRecoveryInterval;
      }
      public String toString() {
         return 
         "\n targetType              = " + targetType +             
         "\n targetVdd               = " + targetVdd +              
         "\n cycleVddOnReset         = " + cycleVddOnReset +        
         "\n cycleVddOnConnect       = " + cycleVddOnConnect +      
         "\n leaveTargetPowered      = " + leaveTargetPowered +     
         "\n autoReconnect           = " + autoReconnect +          
         "\n guessSpeed              = " + guessSpeed +             
         "\n bdmClockSource          = " + bdmClockSource +         
         "\n useResetSignal          = " + useResetSignal +         
         "\n maskInterrupts          = " + maskInterrupts +         
         "\n interfaceFrequency      = " + interfaceFrequency +     
         "\n usePSTSignals           = " + usePstSignals +          
         "\n powerOffDuration        = " + powerOffDuration +       
         "\n powerOnRecoveryInterval = " + powerOnRecoveryInterval +
         "\n resetDuration           = " + resetDuration +          
         "\n resetReleaseInterval    = " + resetReleaseInterval +   
         "\n resetRecoveryInterval   = " + resetRecoveryInterval +  
         "\n";
      }
   };

   /**
    *  Interface status (really only for BDM type interfaces)
    *  Internal JNI use
    */
   private static class Status_ {
      public int  acknState;      
      public int  connectionState;
      public int  resetState;     
      public int  resetRecent;    
      public int  haltState;      
      public int  powerState;     
      @SuppressWarnings("unused")
      public int  flashState;     
   };

   /**
    *  Interface status (really only for BDM type interfaces)
    */
   public static class Status {
      public Boolean            acknState;        //!< Supports ACKN ?
      public ConnectMode        connectionState;  //!< Connection status & speed determination method
      public Boolean            resetState;       //!< Current target RST0 state
      public Boolean            resetRecent;      //!< Target reset recently?
      public Boolean            halted;           //!< CFVx halted (from ALLPST)?
      public TargetVddState     powerState;       //!< Target has power?
      public Status(Status_ status) {
        acknState       = status.acknState   != 0;
        connectionState = ConnectMode.valueOf(status.connectionState);
        resetState      = status.resetState  != 0;
        resetRecent     = status.resetRecent != 0;
        halted          = status.haltState   != 0;
        powerState      = TargetVddState.valueOf(status.powerState);
      }
      public String toString() {
         return "  acknState       = " + acknState +   
                "\n  connectionState = " + connectionState +
                "\n  resetState      = " + resetState +
                "\n  resetRecent     = " + resetRecent +
                "\n  halted          = " + halted +
                "\n  powerState      = " + powerState + "\n";
      }
   };

   private static boolean        libraryLoaded     = false;
   private static boolean        libraryLoadFailed = false;

   private static native int     usbdmGetUsbdmResourcePath(byte[] description);
   private static native int     usbdmGetUsbdmApplicationPath(byte[] description);
   private static native int     usbdmGetUsbdmDataPath(byte[] description);

   private static native int     usbdmInit();
   private static native int     usbdmExit();
   private static native int     usbdmGetBDMInformation(BdmInformation bdmInfo);
   private static native String  usbdmGetErrorString(int errorNum);
   private static native int     usbdmFindDevices(int[] deviceCount);
   private static native int     usbdmReleaseDevices();
   private static native int     usbdmGetBDMSerialNumber(byte[] serialNumber);
   private static native int     usbdmGetBDMDescription(byte[] description);
   private static native int     usbdmGetBDMStatus(Status_ description);
   private static native int     usbdmOpen(int deviceNum);
   private static native int     usbdmClose();

   private static native int     usbdmGetDefaultExtendedOptions(ExtendedOptions_ options);
   private static native int     usbdmSetExtendedOptions(ExtendedOptions_ options);
   private static native int     usbdmGetExtendedOptions(ExtendedOptions_ options);

   private static native int     usbdmSetTargetType(int type);
   private static native int     usbdmSetTargetVdd(int type);
   private static native int     usbdmSetTargetVpp(int type);
   private static native int     usbdmTargetReset(int type);
   private static native int     usbdmTargetConnect();
   private static native int     usbdmTargetGo();
   private static native int     usbdmTargetHalt();
   private static native int     usbdmTargetStep();

   private static native int     usbdmReadMemory(int memorySpace, int byteCount, int address, byte data[]);
   private static native int     usbdmWriteMemory(int memorySpace, int byteCount, int address, byte data[]);

   private static native int     usbdmReadReg(int regSpace, int address, int data[]);
   private static native int     usbdmWriteReg(int regSpace, int address, int data);
   private static native int     usbdmControlPins(int value, int status[]);

   private static native int     usbdmReadStatusReg(int data[]);
   private static native int     usbdmWriteControlReg(int data);
   
   private static native int     usbdmSetSpeed(int speed);
   private static native int     usbdmGetSpeed(int speed[]);
   
   private static native int     getVersion(int[] version);
   
   private static final  int     BDM_RC_OK                 = 0;
   private static final  int     BDM_RC_NO_USBDM_DEVICE    = 36;    //!< No usbdm device was located

   /**
    * Obtains a default set of options appropriate to the target type
    * 
   * @param targetType  Use to customise options for a particular target
   * 
   * @throws UsbdmException
   */
   public static ExtendedOptions getDefaultExtendedOptions(TargetType targetType) throws UsbdmException {
      ExtendedOptions_ options_ = new ExtendedOptions_(targetType);
      int rc = usbdmGetDefaultExtendedOptions(options_);
      if (rc != BDM_RC_OK) {
         throw new UsbdmException(rc);
      }
      return new ExtendedOptions(options_);
   }

   /**
   * Obtains currently set options
   * 
   * @param options USBDM options retrieved
   * 
   * @throws UsbdmException
   */
   public static ExtendedOptions getExtendedOptions() throws UsbdmException {
      ExtendedOptions_ options_ = new ExtendedOptions_(TargetType.T_OFF);
      int rc = usbdmGetExtendedOptions(options_);
      if (rc != BDM_RC_OK) {
         throw new UsbdmException(rc);
      }
      return new ExtendedOptions(options_);
   }
   
   /**
   * Set current options
   * 
   * @param options USBDM options to set
   * 
   * @throws UsbdmException
   */
   public static void setExtendedOptions(ExtendedOptions options) throws UsbdmException {
      ExtendedOptions_ options_ = new ExtendedOptions_(options);
      int rc = usbdmSetExtendedOptions(options_);
      if (rc != BDM_RC_OK) {
         throw new UsbdmException(rc);
      }
   }

  /**
   * Set target type (opens target)
   * 
   * @param type Target type to set
   * 
   * @throws UsbdmException
   */
   public static void setTargetType(TargetType type) throws UsbdmException {
      int rc = usbdmSetTargetType(type.getMask());
      if (rc != BDM_RC_OK) {
         throw new UsbdmException(rc);
      }
   }

   /**
    * Set target Vdd
    * 
    * @param vddSelect Target Vdd to set
    * 
    * @throws UsbdmException
    */
    public static void setTargetVdd(TargetVddSelect vddSelect) throws UsbdmException {
       int rc = usbdmSetTargetVdd(vddSelect.getMask());
       if (rc != BDM_RC_OK) {
          throw new UsbdmException(rc);
       }
    }

    /**
     * Set target Vpp 
     * 
     * @param vppSelect Target Vpp to set
     * 
     * @throws UsbdmException
     */
     public static void setTargetVpp(TargetVppSelect vppSelect) throws UsbdmException {
        int rc = usbdmSetTargetVpp(vppSelect.getMask());
        if (rc != BDM_RC_OK) {
           throw new UsbdmException(rc);
        }
     }

   /**
   * Reset target
   * 
   * @param resetType Reset mode & method to use
   * 
   * @throws UsbdmException
   */
   public static void targetReset(ResetType resetType) throws UsbdmException {
      int rc = usbdmTargetReset(resetType.getMask());
      if (rc != BDM_RC_OK) {
         throw new UsbdmException(rc);
      }
   }

   /**
   *  Connect to target
   *  
   * @throws UsbdmException
   */
   public static void targetConnect() throws UsbdmException {
      int rc = usbdmTargetConnect();
      if (rc != BDM_RC_OK) {
         throw new UsbdmException(rc);
      }
   }

   /**
   *  Start target execution
   *  
   * @throws UsbdmException
   */
   public static void targetGo() throws UsbdmException {
      int rc = usbdmTargetGo();
      if (rc != BDM_RC_OK) {
         throw new UsbdmException(rc);
      }
   }

   /**
   *  Step target
   *  
   * @throws UsbdmException
   */
   public static void targetStep() throws UsbdmException {
      int rc = usbdmTargetStep();
      if (rc != BDM_RC_OK) {
         throw new UsbdmException(rc);
      }
   }

   /**
   *  Halt target
   *  
   * @throws UsbdmException
   */
   public static void targetHalt() throws UsbdmException {
      int rc = usbdmTargetHalt();
      if (rc != BDM_RC_OK) {
         throw new UsbdmException(rc);
      }
   }

   /**
   *  Control interface pins
   *  
   * @throws UsbdmException
   */
   public static int controlPins(int value) throws UsbdmException {
      int status[] = new int[1];
      int rc = usbdmControlPins(value, status);
      if (rc != BDM_RC_OK) {
         throw new UsbdmException(rc);
      }
      return status[0];
   }
   
   /**
   *  Control interface pins
   *  
   * @throws UsbdmException
   */
   public static void setSpeed(int speed) throws UsbdmException {
      int rc = usbdmSetSpeed(speed);
      if (rc != BDM_RC_OK) {
         throw new UsbdmException(rc);
      }
   }
   
   /**
   *  Control interface pins
   *  
   * @throws UsbdmException
   */
   public static int getSpeed() throws UsbdmException {
      int speed[] = new int[1];
      int rc = usbdmGetSpeed(speed);
      if (rc != BDM_RC_OK) {
         throw new UsbdmException(rc);
      }
      return speed[0];
   }
   
   /**
    * @param memorySpace   Memory space and element size
    * @param byteCount     Number of bytes to read
    * @param address       Memory address to read from
    * @param data          Data read
    * 
    * @throws UsbdmException
    */
   public static void readMemory(MemorySpace memorySpace, int byteCount, int address, byte data[]) throws UsbdmException {
      int rc = usbdmReadMemory(memorySpace.mask, byteCount, address, data);
      if (rc != BDM_RC_OK) {
         throw new UsbdmException(rc);
      }
   }

   /**
    * @param memorySpace   Memory space and element size
    * @param byteCount     Number of bytes to write
    * @param address       Memory address to write from
    * @param data          Data to write
    * 
    * @throws UsbdmException
    */
   public static void writeMemory(MemorySpace memorySpace, int byteCount, int address, byte data[]) throws UsbdmException {
      int rc = usbdmWriteMemory(memorySpace.mask, byteCount, address, data);
      if (rc != BDM_RC_OK) {
         throw new UsbdmException(rc);
      }
   }

   /**
    * @param regId   Identifies register to access
    * 
    * @return  register value
    * 
    * @throws UsbdmException
    */
   public static int readReg(regInterface regId) throws UsbdmException {
      int data[]  = new int[1];
      int rc = usbdmReadReg(regId.getSpace().getmask(), regId.getNo(), data);
      if (rc != BDM_RC_OK) {
         throw new UsbdmException(rc);
      }
      return data[0];
   }
   
   /**
    * @param regId   Identifies register to access
    * @param value   Value to write to register
    * 
    * @throws UsbdmException
    */
   public static void writeReg(regInterface regId, int value) throws UsbdmException {
      int rc = usbdmWriteReg(regId.getSpace().getmask(), regId.getNo(), value);
      if (rc != BDM_RC_OK) {
         throw new UsbdmException(rc);
      }
   }
   
   /**
    * @return  status register value
    * 
    * @throws UsbdmException
    */
   public static int readStatusReg() throws UsbdmException {
      int data[]  = new int[1];
      int rc = usbdmReadStatusReg(data);
      if (rc != BDM_RC_OK) {
         throw new UsbdmException(rc);
      }
      return data[0];
   }
   
   /**
    * @param value   Value to write to control register
    * 
    * @throws UsbdmException
    */
   public static void writeControlReg(int value) throws UsbdmException {
      int rc = usbdmWriteControlReg(value);
      if (rc != BDM_RC_OK) {
         throw new UsbdmException(rc);
      }
   }
   
   /**
    * Opens given device & release USBDM internal device list
    * 
    * @param deviceNum number of USBDM device to open
    * 
    * @throws UsbdmException
    * 
    * @info findDevices() must be called first to obtain permitted device numbers and create device list
    */
   public static void open(int deviceNum) throws UsbdmException {
      int rc = Usbdm.usbdmOpen(deviceNum);
      Usbdm.releaseDevices();
      if (rc != BDM_RC_OK) {
         throw new UsbdmException(rc);
      }
   }
   
   /**
    * Opens given device and optionally release USBDM internal device list
    * 
    * @param deviceNum number of USBDM device to open
    * 
    * @throws UsbdmException
    * 
    * @info findDevices() must be called first to obtain permitted device numbers and create device list
    */
   public static void open(int deviceNum, boolean releaseDevices) throws UsbdmException {
      int rc = Usbdm.usbdmOpen(deviceNum);
      if (rc != BDM_RC_OK) {
         throw new UsbdmException(rc);
      }
      if (releaseDevices) {
         Usbdm.releaseDevices();
      }
   }
   
   /**
   * Closes current open device.
   * 
   * @throws UsbdmException
   */
  public static void close() throws UsbdmException {
     int rc = Usbdm.usbdmClose();
     if (rc != BDM_RC_OK) {
        throw new UsbdmException(rc);
     }
  }
  
  /**
   * Close down the USBDM JNI interface
   */
  public static void exit() {
     usbdmExit();
  }
  
   /**
    * Get error string for given erro number
    * 
    * @param errorNum Error number to determine message
    *  
    * @return String describing the error
    */
   public static String getErrorString(int errorNum) {
      return usbdmGetErrorString(errorNum);
   }
   
   /**
    * Enumerate USBDM devices connected
    * 
    * @return Count of USBDM devices found
    * 
    * @throws UsbdmException
    * 
    * @note No devices being present is not considered an error
    * @note An internal device list is created and not released until 
    *       open(deviceNum,true) or releaseDevices() is called.
    */
   public static int findDevices() throws UsbdmException {
      int deviceCount_[] = new int[1];
      int rc = usbdmFindDevices(deviceCount_);
      if (rc == BDM_RC_NO_USBDM_DEVICE) {
         return 0;
      }
      if (rc != BDM_RC_OK) {
         throw new UsbdmException(rc);
      }
      return deviceCount_[0];
   }
   
   /**
    * Releases internal device list created by findDevices()
    * 
    * @throws UsbdmException
    */
   public static void releaseDevices() throws UsbdmException {
      int rc =  usbdmReleaseDevices();
      if (rc != BDM_RC_OK) {
         throw new UsbdmException(rc);
      }
   }
   
   /**
    * @return BDM status
    * 
    * @throws UsbdmException
    */
   public static Status getBDMStatus() throws UsbdmException {
      Status_ statusInt = new Status_();
      int rc = Usbdm.usbdmGetBDMStatus(statusInt);
      if (rc != BDM_RC_OK) {
         throw new UsbdmException(rc);
      }
      Status status = new Status(statusInt);
      return status;
   }
   
   /**
    * Queries device for description
    * 
    * @return Device description
    * 
    * @throws UsbdmException
    */
   public static String getBDMDescription() throws UsbdmException {
      Charset utf16leCharset = Charset.forName("UTF-16LE");
      CharsetDecoder utf16leCharsetDecoder = utf16leCharset.newDecoder();
      byte[] desc   = new byte[200];
      int rc = Usbdm.usbdmGetBDMDescription(desc);
      if (rc != BDM_RC_OK) {
         throw new UsbdmException(rc);
      }
      String serial = null;
      try {
         if (rc == BDM_RC_OK) {
            int len = (desc[0]<<8)+(((int)desc[1])&0xFF);
            ByteBuffer buff = ByteBuffer.allocate(len);
            buff.put(desc, 2, len);
            buff.rewind();
            serial = utf16leCharsetDecoder.decode(buff).toString();
         }
      } catch (CharacterCodingException e) {
         Activator.logError("Description contains illegal characters", e);
         return "Description contains illegal characters";
      }
      return serial;
   }

   /**
    * Queries device for serial number
    * 
    * @return Device serial number as string
    * 
    * @throws UsbdmException
    */
   public static String getBDMSerialNumber() throws UsbdmException {
      Charset utf16leCharset = Charset.forName("UTF-16LE");
      CharsetDecoder utf16leCharsetDecoder = utf16leCharset.newDecoder();
      byte[] serialNum   = new byte[200];
      int rc = Usbdm.usbdmGetBDMSerialNumber(serialNum);
      if (rc != BDM_RC_OK) {
         throw new UsbdmException(rc);
      }
      String serial = null;
      try {
         if (rc == BDM_RC_OK) {
            int len = (serialNum[0]<<8)+(((int)serialNum[1])&0xFF);
            ByteBuffer buff = ByteBuffer.allocate(len);
            buff.put(serialNum, 2, len);
            buff.rewind();
            serial = utf16leCharsetDecoder.decode(buff).toString();
         }
      } catch (CharacterCodingException e) {
         return "Serial number contains illegal characters";
      }
      return serial;
   }
   
   /**
    * @return Queries device for information describing capabilities etc.
    * 
    * @throws UsbdmException
    */
   public static BdmInformation getBDMInformation() throws UsbdmException {
      BdmInformation bdmInfo = new BdmInformation();
      int rc = Usbdm.usbdmGetBDMInformation(bdmInfo);
      if (rc != BDM_RC_OK) {
         throw new UsbdmException(rc);
      }
      return bdmInfo;
   }
   
   /**
    *  Class describing the BDM
    */
   public static class USBDMDeviceInfo {
      public static final USBDMDeviceInfo nullDevice = new USBDMDeviceInfo("Generic BDM", "[Any connected USBDM]", new BdmInformation());
      public String         deviceDescription;
      public String         deviceSerialNumber;
      public BdmInformation bdmInfo;
      public USBDMDeviceInfo(String desc, String ser, BdmInformation bdmI) {
         deviceDescription  = desc;
         deviceSerialNumber = ser;
         bdmInfo            = bdmI;
//         Activator.logError("BdmInformation.USBDMDeviceInfo()");
      }
      public String toString() {
         return "  Description   = "+deviceDescription+";\n" +
         	    "  Serial Number = "+deviceSerialNumber+";\n" +
         		 "  Information   = \n"+bdmInfo.toString();
      }
      public boolean isNullDevice() {
         return this == nullDevice; 
      }
   };

   // Note - Changed to static linked usbdm DLLs because of trap when MINGW C++ DLLs unloaded
   final static String win_i386_libraries_debug[] = {
      "i386/usbdm-debug.4",
      "i386/usbdm-jni-debug.4",
   };
   final static String win_i386_libraries[] = {
      "i386/usbdm.4",
      "i386/usbdm-jni.4",
   };
   final static String win_x86_64_libraries_debug[] = {
      "x86_64/libwinpthread-1",
      "x86_64/libgcc_s_seh-1",
      "x86_64/libstdc++-6",
      "x86_64/usbdm-debug.4",
      "x86_64/usbdm-jni-debug.4",
   };
   final static String win_x86_64_libraries[] = {
      "x86_64/libwinpthread-1",
      "x86_64/libgcc_s_seh-1",
      "x86_64/libstdc++-6",
      "x86_64/usbdm.4",
      "x86_64/usbdm-jni.4",
   };
   // Note: Linux name used below should not have 'lib' prefix or '.so' suffix
   final static String linux_i386_libraries_debug[] = {
      "usbdm-jni-debug", // Note - loads libusbdm-jni-debug
   };
   final static String linux_i386_libraries[] = {
      "usbdm-jni", // Note - loads libusbdm-jni
   };
   final static String linux_x86_64_libraries_debug[] = {
      "usbdm-jni-debug", // Note - loads libusbdm-jni-debug
   };
   final static String linux_x86_64_libraries[] = {
      "usbdm-jni", // Note - loads libusbdm-jni
   };
   
   /**
    * Load the USBDM JNI library
    * 
    * @param debug True to load debug version of DLLs
    */
   public static void loadUsbdmLibraries(final Boolean debug) {
      if (libraryLoaded) {
         return;
      }
      StringBuilder sb = new StringBuilder();
      try {
         //String property = System.getProperty("java.library.path");
         //StringTokenizer parser = new StringTokenizer(property, ":");
         //while (parser.hasMoreTokens()) {
         //    Activator.logError(parser.nextToken());
         //    }
         String os    = System.getProperty("os.name");            
         String arch  = System.getProperty("os.arch");            
         String jvm   = System.getProperty("java.vm.name");
         sb.append("os.name      => " + os   + "\n");
         sb.append("java.vm.name => " + jvm  + "\n" );
         sb.append("os.arch      => " + arch + "\n" );
         String libraryList[];
         if ((os != null) && os.toUpperCase().contains("LINUX")) {
            if (arch.toLowerCase().contains("amd64")) {
               if (debug) {
                  libraryList = linux_x86_64_libraries_debug;
               }
               else {
                  libraryList = linux_x86_64_libraries;
               }
            }
            else {
               // Assume running 32-bit VM
               if (debug) {
                  libraryList = linux_i386_libraries_debug;
               }
               else {
                  libraryList = linux_i386_libraries;
               }
            }
         }
         else {
            if (arch.toLowerCase().contains("amd64")) {
               if (debug) {
                  libraryList = win_x86_64_libraries_debug;
               }
               else {
                  libraryList = win_x86_64_libraries;
               }
            }
            else { // amd64
               // Assume running 32-bit VM
               if (debug) {
                  libraryList = win_i386_libraries_debug;
               }
               else {
                  libraryList = win_i386_libraries;
               }
            }
         }
         for (String libraryName : libraryList) {
            sb.append("Loading library name = " + libraryName + "\n");
            System.loadLibrary(libraryName);
         }
         usbdmInit();
         libraryLoaded = true;
         //            Activator.logError("Loaded Library: "+UsbdmJniConstants.UsbdmJniLibraryName);

         //            Activator.logError("Libraries successfully loaded");
         //            Shell shell;
         //            // Find the default display and get the active shell
         //            final Display disp = Display.getDefault();
         //            if (disp == null) {
         //               shell = new Shell(new Display());
         //            }
         //            else {
         //               shell = new Shell(disp);
         //            }
         //            MessageBox msgbox = new MessageBox(shell, SWT.OK);
         //            msgbox.setText("USBDM Notice");
         //            msgbox.setMessage("Loading of USBDM native library OK.");
         //            msgbox.open();
         Activator.log(sb.toString());
      } catch (Exception e) {
         Activator.log(sb.toString());
         Activator.logError(e.getMessage(), e);
//         String reason = e.getMessage();
//         // Report first failure only
//         if (!libraryLoadFailed) {
//            Shell shell;
//            // Find the default display and get the active shell
//            final Display disp = Display.getDefault();
//            if (disp == null) {
//               shell = new Shell(new Display());
//            }
//            else {
//               shell = new Shell(disp);
//            }
//            Activator.logError("Libary Path = "+System.getProperty("java.library.path"));
//            libraryLoadFailed = true;
//            MessageBox msgbox = new MessageBox(shell, SWT.OK);
//            msgbox.setText("USBDM Error");
//            msgbox.setMessage("Loading of USBDM native library failed.\n" + reason);
//            msgbox.open();
//            e.printStackTrace();
//            try {
//               throw new UsbdmException("USBDM JNI Library failure: "+e.getMessage());
//            } catch (UsbdmException e1) {
//               e1.printStackTrace();
//            }
//         }
//         Activator.logError("USBDM Libraries failed to load");
//         return;
      }
   }
   
   /*
    * Load USBDM DLLs 
    */
   static {
      // TO DO - change to non-debug version
      loadUsbdmLibraries(false);
   }
   
   /**
    * Get list of devices
    * 
    * @return empty list if no devices found
    * 
    * @throws UsbdmException 
    * @throws CharacterCodingException 
    */
   public static ArrayList<USBDMDeviceInfo> getDeviceList() {
      //      Activator.logError("Usbdm.getDeviceList()");

      ArrayList<USBDMDeviceInfo> deviceList = new ArrayList<USBDMDeviceInfo>();

      int deviceCount = 0;
      BdmInformation   bdmInfo = new Usbdm.BdmInformation();
      USBDMDeviceInfo  deviceInfo;

      // Get count of devices (and create USBDM internal device list)
      try {
         deviceCount = findDevices();
         if (deviceCount == 0) {
            return deviceList;
         }
         //         Activator.logError("Usbdm.findDevices(): Found  " + deviceCount + " devices");
         for (int deviceNum=0; deviceNum < deviceCount; deviceNum++) {
            String description = new String("Unresponsive device");
            String serialNum   = new String("Unknown");
            try {
               // Open device without releasing device list
               open(deviceNum, false);
               description = getBDMDescription();
               serialNum   = getBDMSerialNumber();
               bdmInfo     = getBDMInformation();
               deviceInfo  = new USBDMDeviceInfo(description, serialNum, bdmInfo);
               deviceList.add(deviceInfo);
            } catch (UsbdmException e1) {
               Activator.logError(e1.getMessage(), e1);
            }
            close();
         }
         // Release device list
         releaseDevices(); 
      } catch (UsbdmException e) {
      }
      return deviceList;
   } 

   /**
    *  Obtain USBDM Application path
    * 
    *  @return Path to USBDM Application directory (executable, data etc)
    * 
    *  @throws UsbdmException
    */
   public static String getUsbdmApplicationPath() throws UsbdmException {
      Charset utf8Charset = Charset.forName("UTF-8");
      CharsetDecoder utf8CharsetDecoder = utf8Charset.newDecoder();
      byte[] pathArray   = new byte[2000];
      String path = "";
      try {
         int rc = Usbdm.usbdmGetUsbdmApplicationPath(pathArray);
         if (rc == BDM_RC_OK) {
            int len = (pathArray[0]<<8)+(((int)pathArray[1])&0xFF);
            ByteBuffer buff = ByteBuffer.allocate(len);
            buff.put(pathArray, 2, len);
            buff.rewind();
            path = utf8CharsetDecoder.decode(buff).toString();
         }
      } catch (CharacterCodingException e) {
         Activator.logError(e.getMessage(), e);
      }
      return path;
   }

   /**
    *  Obtain USBDM Application path
    * 
    *  @return Path to USBDM Application directory (executable)
    */
   public static IPath getApplicationPath() {
      Path path = null;
      try {
         path = new Path(getUsbdmApplicationPath());
      } catch (UsbdmException e) {
         Activator.logError(e.getMessage(), e);
      }
      return path;
   }
   
   /**
    *  Obtain USBDM Resource path
    * 
    *  @return Path to USBDM Resource directory (data etc)
    * 
    *  @throws UsbdmException
    */
   public static String getUsbdmResourcePath() throws UsbdmException {
      Charset utf8Charset = Charset.forName("UTF-8");
      CharsetDecoder utf8CharsetDecoder = utf8Charset.newDecoder();
      byte[] pathArray   = new byte[2000];
      String path = "";
      try {
         int rc = Usbdm.usbdmGetUsbdmResourcePath(pathArray);
         if (rc == BDM_RC_OK) {
            int len = (pathArray[0]<<8)+(((int)pathArray[1])&0xFF);
            ByteBuffer buff = ByteBuffer.allocate(len);
            buff.put(pathArray, 2, len);
            buff.rewind();
            path = utf8CharsetDecoder.decode(buff).toString();
         }
      } catch (CharacterCodingException e) {
         Activator.logError(e.getMessage(), e);
      }
      return path;
   }

   /**
    *  Obtain USBDM Resource path
    * 
    *  @return Path to USBDM Resource directory (data etc)
    */
   public static IPath getResourcePath() {
      Path path = null;
      try {
         path = new Path(getUsbdmResourcePath());
      } catch (UsbdmException e) {
         Activator.logError(e.getMessage(), e);
      }
      return path;
   }
   
   /**
    *  Obtain USBDM Data path
    * 
    *  @return Path to USBDM Data directory (user data etc)
    * 
    *  @throws UsbdmException
    */
   public static String getUsbdmDataPath() throws UsbdmException {
      Charset utf8Charset = Charset.forName("UTF-8");
      CharsetDecoder utf8CharsetDecoder = utf8Charset.newDecoder();
      byte[] pathArray   = new byte[2000];
      String path = "";
      try {
         int rc = Usbdm.usbdmGetUsbdmDataPath(pathArray);
         if (rc == BDM_RC_OK) {
            int len = (pathArray[0]<<8)+(((int)pathArray[1])&0xFF);
            ByteBuffer buff = ByteBuffer.allocate(len);
            buff.put(pathArray, 2, len);
            buff.rewind();
            path = utf8CharsetDecoder.decode(buff).toString();
         }
      } catch (CharacterCodingException e) {
         Activator.logError(e.getMessage(), e);
      }
      return path.replace('\\', '/');
   }
   
   /**
    *  Obtain USBDM Data path
    * 
    *  @return Path to USBDM Data directory (user data etc)
    */
   public static IPath getDataPath() {
      Path path = null;
      try {
         path = new Path(getUsbdmDataPath());
      } catch (UsbdmException e) {
         Activator.logError(e.getMessage(), e);
      }
      return path;
   }
   
   
}
