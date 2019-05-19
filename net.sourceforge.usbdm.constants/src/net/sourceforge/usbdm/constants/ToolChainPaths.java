package net.sourceforge.usbdm.constants;

import java.nio.file.Path;
import java.nio.file.Paths;
import com.registry.RegStringValue;
import com.registry.RegistryKey;

public class ToolChainPaths {

   static class Info {
      final String key;
      final String name;
      final String pathExtra;

      public Info(String key, String name, String pathExtra) {
         super();
         this.key = key;
         this.name = name;
         this.pathExtra = pathExtra;
      }
   }

   // Where to look for tool-chain
   static final Info infos[] = {
         new Info("SOFTWARE\\WOW6432Node\\Eclipse Foundation\\Eclipse CDT", "InstallationDirectory", "/GNU Tools ARM Embedded/8 2018-q4-major/bin/arm-none-eabi-gcc.exe"),
         new Info("SOFTWARE\\WOW6432Node\\ARM\\GNU Tools for ARM Embedded Processors", "InstallFolder", "/bin/arm-none-eabi-gcc.exe"),
         new Info("SOFTWARE\\WOW6432Node\\NXP\\S32 Design Studio\\Product Versions\\S32 Design Studio for ARM Version 2018.R1", "Path", "/Cross_Tools/gcc-arm-none-eabi-4_9/bin/arm-none-eabi-gcc.exe"),
         new Info("SOFTWARE\\Freescale\\Kinetis Design Studio 3.2.0", "Path", "/Toolchain/bin/arm-none-eabi-gcc.exe"),
         new Info("SOFTWARE\\Freescale\\Kinetis Design Studio 3", "Path", "/Toolchain/bin/arm-none-eabi-gcc.exe"),
   };

   /**
    * Obtain path from registry and check it exists
    * 
    * @param key        Registry key
    * @param name       Name of registry value
    * @param lastPart   Extra path to append to value form registry
    * @return
    */
   static private String getPath(String key, String name, String lastPart) {

      String rv = null;

      RegistryKey software = new RegistryKey(RegistryKey.listRoots()[RegistryKey.HKEY_LOCAL_MACHINE_INDEX], key);
      software.view64BitRegistry(true);
      RegStringValue regValue = (RegStringValue)software.getValue(name);
      if (regValue != null) {
         System.err.println("regValue = " +regValue);
         rv = regValue.getValue();
         System.err.println("rv = " +rv);
         if (rv != null) {
            rv = rv.replaceAll("\\\\", "/");
         }
         if (rv.endsWith("/")) {
            rv = rv.substring(0, rv.lastIndexOf('/'));
         }
         Path p = Paths.get(rv, lastPart);
         rv += lastPart;
         if (p.toFile().exists()) {
            return rv;
         }
      }
      return null;
   }

   /**
    * Get Default Tool-chain Bin Directory
    * 
    * @return Path as string
    */
   static String getDefaultToolchainBinDirectory() {
      for (Info info:infos) {
         String rv = getPath(info.key, info.name, info.pathExtra);
         if (rv != null) {
            System.err.print("Path = " +rv);
            return rv;
         }
      }
      return null;
   }

   /**
    * Set default USBDM tool-paths if not already set
    * 
    * @param settings Shared settings object
    */
   public static void setDefaultToolPaths(UsbdmSharedSettings settings) {

      String toolPath = null;
      String prefix   = null;

      if (settings != null) {
         toolPath = settings.get(UsbdmSharedConstants.ARMLTD_ARM_PATH_VAR, "");
         prefix   = settings.get(UsbdmSharedConstants.ARMLTD_ARM_PREFIX_VAR, "");
      }

      if ((toolPath==null) || toolPath.isEmpty() || toolPath.equals(UsbdmSharedConstants.PATH_NOT_SET) ||
            (prefix==null) ||  prefix.isEmpty() || prefix.equals(UsbdmSharedConstants.PREFIX_NOT_SET)) {

         String gccCommand    = "gcc.exe";

         toolPath = null;
         prefix = null;

         String path = getDefaultToolchainBinDirectory();
         path = path.replaceAll("\\\\", "/");

         int index = path.lastIndexOf("/");
         if (index>=0)  {
            toolPath = path.substring(0,  index);
            prefix   = path.substring(index+1);

            index = prefix.lastIndexOf(gccCommand);
            if (index>=0)  {
               prefix = prefix.substring(0,  index);
            }
         }
         if (settings != null) {
            settings.put(UsbdmSharedConstants.ARMLTD_ARM_PATH_VAR, toolPath);
            settings.put(UsbdmSharedConstants.ARMLTD_ARM_PREFIX_VAR, prefix);
         }
      }
   }

   /**
    * Set default make and rm commands
    * 
    * @param settings Shared settings object
    */
   public static void setDefaultCommands(UsbdmSharedSettings settings) {
      if (settings != null) {
         String makeCommand    = settings.get(UsbdmSharedConstants.USBDM_MAKE_COMMAND_VAR, "");
         String removeCommand  = settings.get(UsbdmSharedConstants.USBDM_RM_COMMAND_VAR,   "");

         if (makeCommand.isEmpty()) {
            settings.put(UsbdmSharedConstants.USBDM_MAKE_COMMAND_VAR, UsbdmSharedConstants.USBDM_MAKE_COMMAND_DEFAULT);
         }         
         if (removeCommand.isEmpty() ) {
            settings.put(UsbdmSharedConstants.USBDM_RM_COMMAND_VAR, UsbdmSharedConstants.USBDM_RM_COMMAND_DFAULT);
         }         
      }
   }

   /**
    * Set default settings
    * 
    * @param settings Shared settings object
    */
   public static void setDefaults(UsbdmSharedSettings settings) {
      setDefaultToolPaths(settings);
      setDefaultCommands(settings);
      if (settings != null) {
         settings.flush();
      }
   }

   public static void main(String[] args) {
      setDefaults(null);
   }
}
