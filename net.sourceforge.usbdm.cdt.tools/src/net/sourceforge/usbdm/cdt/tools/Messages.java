package net.sourceforge.usbdm.cdt.tools;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
   private static final String BUNDLE_NAME = "net.sourceforge.usbdm.cdt.wizard.messages"; //$NON-NLS-1$
   public static String ERR_GCC_PATH_OR_PREFIX_INVALID;
   public static String ERR_LINKER_SCRIPT_PATH_INVALID;
   public static String ERR_DEVICE_DATABASE_INVALID;
   public static String ERR_DEVICE_DATABASE_NOT_FOUND;
   public static String NAME_USBDM_PARAMETERS;
   public static String TOOL_TIP_USBDM_SPRITE;
   public static String NAME_AUTO_LINKER_SCRIPT;
   public static String TOOL_TIP_AUTO_LINKER_SCRIPT;
   public static String TOOL_TIP_EXTERNAL_LINKER_SCRIPT;
   public static String NAME_BROWSE;
   public static String TOOL_TIP_BROWSE_LINKER_SCRIPT;
   public static String USBDM_INTERFACE;
   public static String USBDM_PARAMETERS;
   public static String NAME_LINKER_PARAMETERS;
   public static String NAME_OPEN;
   static {
      // Initialise resource bundle
      NLS.initializeMessages(BUNDLE_NAME, Messages.class);
   }

   private Messages() {
   }
}
