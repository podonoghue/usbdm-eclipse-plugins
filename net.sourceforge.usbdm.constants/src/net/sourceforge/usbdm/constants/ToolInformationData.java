package net.sourceforge.usbdm.constants;

import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;

import net.sourceforge.usbdm.constants.UsbdmSharedConstants.InterfaceType;

public class ToolInformationData {
   protected String                    description;
   protected String                    buildToolId;
   protected String                    pathVariableName;
   protected String                    prefixVariableName;
   protected Collection<net.sourceforge.usbdm.constants.UsbdmSharedConstants.InterfaceType> interfaceTypes = new HashSet<InterfaceType>();
   protected InterfaceType             preferredInterfaceType;

   static Hashtable<String, ToolInformationData> toolInformationTable = null;

   public ToolInformationData(String description, String buildToolId, String pathVariableName, String prefixVariableName) {
      this.description          = description;
      this.buildToolId          = buildToolId;
      this.pathVariableName     = pathVariableName;
      this.prefixVariableName   = prefixVariableName; 
   }
   
   public ToolInformationData(ToolInformationData other) {
      this.description           = other.description;
      this.buildToolId           = other.buildToolId;
      this.pathVariableName      = other.pathVariableName;
      this.prefixVariableName    = other.prefixVariableName;
   }

   public String        getDescription()              { return description; }
   public String        getPathVariableName()         { return pathVariableName; }
   public String        getPrefixVariableName()       { return prefixVariableName; }
   public String        getBuildToolId()              { return buildToolId; }
   public InterfaceType getPreferredInterfaceType()   { return preferredInterfaceType; }

   public boolean   applicableTo(InterfaceType interfaceType) { return interfaceTypes.contains(interfaceType); }
   
   protected ToolInformationData addInterfaceType(InterfaceType interfaceType) {
      interfaceTypes.add(interfaceType);
      if (preferredInterfaceType == null) {
         preferredInterfaceType = interfaceType;
      }
      return this;
   }

   public String toString() {
      return getDescription();
   }
   
   public static Hashtable<String, ToolInformationData> getToolInformationTable() {
      if (toolInformationTable == null) {
         toolInformationTable = new Hashtable<String, ToolInformationData>();
//         toolInformationTable.put(
//               UsbdmSharedConstants.CODESOURCERY_ARM_BUILD_ID,
//               new ToolInformationData(
//                  "Codesourcery Tools for ARM",   
//                  UsbdmSharedConstants.CODESOURCERY_ARM_BUILD_ID,
//                  UsbdmSharedConstants.CODESOURCERY_ARM_PATH_VAR,
//                  UsbdmSharedConstants.CODESOURCERY_ARM_PREFIX_VAR).addInterfaceType(InterfaceType.T_ARM)
//               );
         toolInformationTable.put(
               UsbdmSharedConstants.CODESOURCERY_COLDFIRE_BUILD_ID,
               new ToolInformationData(
                     "Codesourcery Tools for Coldfire",  
                     UsbdmSharedConstants.CODESOURCERY_COLDFIRE_BUILD_ID,
                     UsbdmSharedConstants.CODESOURCERY_COLDFIRE_PATH_VAR, 
                     UsbdmSharedConstants.CODESOURCERY_COLDFIRE_PREFIX_VAR).addInterfaceType(InterfaceType.T_CFV1)
                                                                           .addInterfaceType(InterfaceType.T_CFVX)               
               );
         toolInformationTable.put(
               UsbdmSharedConstants.ARMLTD_ARM_BUILD_ID,
               new ToolInformationData(
                     "ARM Ltd GNU Tools for ARM",          
                     UsbdmSharedConstants.ARMLTD_ARM_BUILD_ID,
                     UsbdmSharedConstants.ARMLTD_ARM_PATH_VAR,
                     UsbdmSharedConstants.ARMLTD_ARM_PREFIX_VAR).addInterfaceType(InterfaceType.T_ARM)
               );
      }
      return toolInformationTable;
   }
   
   /**  Find Tool Information based on tool id e.g. 
    *         net.sourceforge.usbdm.cdt.toolchain.processor.usbdmConfigure.codesourceryColdfire
    *         
    * @param buildToolId Id - see above
    * 
    * @return Description of this tool
    */
   public static ToolInformationData get(String buildToolId) {
      return toolInformationTable.get(buildToolId);
   }
}