package net.sourceforge.usbdm.configEditor.parser;

import net.sourceforge.usbdm.configEditor.information.DeviceInfo.DeviceFamily;
import net.sourceforge.usbdm.configEditor.information.MappingInfo;
import net.sourceforge.usbdm.configEditor.information.PeripheralFunction;

/**
 * Writer for left-over signals
 *  
 */
public class WriterForMiscellaneous extends WriterBase {

      WriterForMiscellaneous(DeviceFamily deviceType) {
         super(deviceType);
      }

      @Override
      public String getGroupName() {
         return null;
      }

      @Override
      public String getGroupTitle() {
         return null;
      }

      @Override
      public String getGroupBriefDescription() {
         return null;
      }

      @Override
      public String getInstanceName(MappingInfo mappingInfo, int fnIndex) {
         return null;
      }

      @Override
      protected String getDeclaration(MappingInfo mappingInfo, int fnIndex) {
         return null;
      }

      @Override
      public String getAliasName(String signalName, String alias) {
         return null;
      }

      @Override
      public int getFunctionIndex(PeripheralFunction function) {
         return 0;
      } 
      
   };
