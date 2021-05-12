package net.sourceforge.usbdm.constants;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import net.sourceforge.usbdm.jni.Usbdm;
import net.sourceforge.usbdm.jni.UsbdmException;

public class ToolChainPaths {

   static class WinRegistryInformation {
      final String key;
      final String name;
      final String pathExtra;

      public WinRegistryInformation(String key, String name, String pathExtra) {
         super();
         this.key = key;
         this.name = name;
         this.pathExtra = pathExtra;
      }
   }

   // Where to look for windows tool-chain in registry
   static final WinRegistryInformation winRegistryInformation[] = {
         new WinRegistryInformation("SOFTWARE\\WOW6432Node\\Eclipse Foundation\\Eclipse CDT", "InstallationDirectory", "/GNU Tools ARM Embedded/bin/arm-none-eabi-gcc.exe"),
         new WinRegistryInformation("SOFTWARE\\WOW6432Node\\ARM\\GNU Tools for ARM Embedded Processors", "InstallFolder", "/bin/arm-none-eabi-gcc.exe"),
         new WinRegistryInformation("SOFTWARE\\WOW6432Node\\NXP\\S32 Design Studio\\Product Versions\\S32 Design Studio for ARM Version 2018.R1", "Path", "/Cross_Tools/gcc-arm-none-eabi-4_9/bin/arm-none-eabi-gcc.exe"),
         new WinRegistryInformation("SOFTWARE\\Freescale\\Kinetis Design Studio 3.2.0", "Path", "/Toolchain/bin/arm-none-eabi-gcc.exe"),
         new WinRegistryInformation("SOFTWARE\\Freescale\\Kinetis Design Studio 3", "Path", "/Toolchain/bin/arm-none-eabi-gcc.exe"),
   };

   /**
    * Obtain path from registry and check it exists
    * 
    * @param key        Registry key
    * @param name       Name of registry value
    * @param lastPart   Extra path to append to value from registry
    * 
    * @return
    */
   static private String getPath(String key, String name, String lastPart) {
      try {
         String rv = Usbdm.readWindowsRegistry(key, name);
         Activator.log(String.format("getPath('%s', '%s', '%s') => ", key, name, lastPart) + rv);
         if (rv != null) {
            rv = rv.replaceAll("\\\\", "/");
            if (rv.endsWith("/")) {
               rv = rv.substring(0, rv.lastIndexOf('/'));
            }
            Path p = Paths.get(rv, lastPart);
            rv += lastPart;
            if (p.toFile().exists()) {
               return rv;
            }
         }
      } catch (UsbdmException e) {
         e.printStackTrace();
      }
      return null;
   }

   /**
    * Get Default Tool-chain Bin Directory
    * 
    * @return Path as string
    */
   static String getDefaultWindowsToolchainBinDirectory() {
      for (WinRegistryInformation info:winRegistryInformation) {
         String rv = getPath(info.key, info.name, info.pathExtra);
         if (rv != null) {
            System.err.print("Found new default toolchain at  = '" +rv+"'");
            return rv;
         }
      }
      return null;
   }

   static class LinuxInformation {
      final String path;

      public LinuxInformation(String path) {
         super();
         this.path = path;
      }
   }

   // Where to look for Linux tool-chain
   static final LinuxInformation linuxInformation[] = {
         new LinuxInformation("/opt/eclipse-usbdm/gcc-arm-none-eabi/bin/arm-none-eabi-gcc"),
         new LinuxInformation("/opt/gcc-arm-none-eabi-9-2020-q2-update/bin/arm-none-eabi-gcc"),
   };

   /**
    * Get Default Tool-chain Bin Directory
    * 
    * @return Path as string
    */
   static String getDefaultLinuxToolchainBinDirectory() {
      for (LinuxInformation info:linuxInformation) {
         File file = new File(info.path);
         if (file.exists()) {
            Activator.log("Found new default toolchain at '" + info.path + "'");
            return info.path;
         };
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

         String os    = System.getProperty("os.name");            
         if ((os != null) && os.toUpperCase().contains("LINUX")) {
            String gccCommand    = "gcc";
            toolPath = null;
            prefix = null;

            String path = getDefaultLinuxToolchainBinDirectory();
            if (path == null) {
               return;
            }
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
         else {
            String gccCommand    = "gcc.exe";

            toolPath = null;
            prefix = null;

            String path = getDefaultWindowsToolchainBinDirectory();
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

         String os    = System.getProperty("os.name");            
         boolean isLinux = (os != null) && os.toUpperCase().contains("LINUX");
         if (makeCommand.isEmpty()) {
            if (isLinux) {
               settings.put(UsbdmSharedConstants.USBDM_MAKE_COMMAND_VAR, "make");
            }
            else {
               settings.put(UsbdmSharedConstants.USBDM_MAKE_COMMAND_VAR, UsbdmSharedConstants.USBDM_MAKE_COMMAND_DEFAULT);
            }
         }         
         if (removeCommand.isEmpty() ) {
            if (isLinux) {
               settings.put(UsbdmSharedConstants.USBDM_RM_COMMAND_VAR, "rm");
            }
            else {
               settings.put(UsbdmSharedConstants.USBDM_RM_COMMAND_VAR, UsbdmSharedConstants.USBDM_RM_COMMAND_DFAULT);
            }
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
