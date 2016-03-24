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
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      public String getGroupTitle() {
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      public String getGroupBriefDescription() {
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      public String getInstanceName(MappingInfo mappingInfo, int fnIndex) {
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      protected String getDeclaration(MappingInfo mappingInfo, int fnIndex) {
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      public String getAliasName(String signalName, String alias) {
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      public int getFunctionIndex(PeripheralFunction function) {
         // TODO Auto-generated method stub
         return 0;
      } 
      
   };
