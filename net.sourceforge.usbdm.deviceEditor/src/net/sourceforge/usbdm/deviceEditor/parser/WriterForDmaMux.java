package net.sourceforge.usbdm.deviceEditor.parser;

import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.DmaInfo;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.Peripheral;
import net.sourceforge.usbdm.deviceEditor.information.PeripheralFunction;
import net.sourceforge.usbdm.deviceEditor.information.PinInformation;
import net.sourceforge.usbdm.deviceEditor.xmlParser.XmlDocumentUtilities;

/**
 * Class encapsulating the code for writing an instance of DigitalIO
 */
/**
 * @author podonoghue
 *
 */
public class WriterForDmaMux extends WriterBase {

   public WriterForDmaMux(DeviceInfo deviceInfo, Peripheral peripheral) {
      super(deviceInfo, peripheral);
   }

   static final String CLASS_BASE_NAME       = "Vref";
   static final String INSTANCE_BASE_NAME    = "vref";

   @Override
   public String getAliasName(String signalName, String alias) {
      return null;
   }

   @Override
   public String getInstanceName(MappingInfo mappingInfo, int fnIndex) {
      return null;
   }

   /** 
    * Get declaration as string e.g. 
    * <pre>
    * const USBDM::Gpio<b><i>A</b></i>&lt;<b><i>0</b></i>&gt;</b></i>
    * </pre>
    * @param mappingInfo    Mapping information (pin and peripheral function)
    * @param cppFile        Where to write
    * @throws Exception 
    */
   protected String getDeclaration(MappingInfo mappingInfo, int fnIndex) {
      int signal       = getFunctionIndex(mappingInfo.getFunctions().get(fnIndex));
      StringBuffer sb = new StringBuffer();
      sb.append(String.format("const %s::%s<%d>", DeviceInfo.NAME_SPACE, getClassName(), signal));
      return sb.toString();
   }
   
   @Override
   public String getAlias(String alias, MappingInfo mappingInfo, int fnIndex) {
      return null;
   }

   @Override
   public String getDefinition(MappingInfo mappingInfo, int fnIndex) {
      return super.getAlias(getInstanceName(mappingInfo, fnIndex), mappingInfo, fnIndex);
   }

   @Override
   public String getExternDeclaration(MappingInfo mappingInfo, int fnIndex) throws Exception {
      return "extern " + getDefinition(mappingInfo, fnIndex);
   }

   @Override
   public boolean useAliases(PinInformation pinInfo) {
      return false;
   }

   @Override
   public String getTitle() {
      return "Direct Memory Access (DMA)";
   }

   @Override
   public String getGroupBriefDescription() {
      return "Pins used Direct Memory Access (DMA)";
   }

   @Override
   public void writeExtraDefinitions(XmlDocumentUtilities documentUtilities) throws IOException {
      writeDmaXmlInfo(documentUtilities);
   }
   
   /**
    * Writes enumeration describing DMA slot use
    * 
    * e.g.<pre>
    * enum {
    *    DMA0_SLOT_Disabled                   = 0,
    *    DMA0_SLOT_UART0_Receive              = 2,
    *    DMA0_SLOT_UART0_Transmit             = 3,
    *    ...
    * };
    * </pre>
    * @param writer
    * @throws IOException
    */
   private void writeDmaXmlInfo(XmlDocumentUtilities documentUtilities) throws IOException {
      if (fPeripheral.getDmaInfoList().size() == 0) {
         return;
      }
      documentUtilities.openTag("dma");
      documentUtilities.setAttrWidth(30);

      for (DmaInfo item:fPeripheral.getDmaInfoList()) {
         documentUtilities.openTag("slot");
         documentUtilities.writeAttribute("source", item.dmaSource);
         documentUtilities.writeAttribute("num",    item.dmaChannelNumber);
         documentUtilities.closeTag();;
      }
      documentUtilities.popAttrWidth();
      documentUtilities.closeTag();;
   }

   @Override
   public void writeExtraInfo(DocumentUtilities documentUtilities) throws IOException {
      getDmaInfo(documentUtilities);
   }

   /**
    * Writes enumeration describing DMA slot use
    * 
    * e.g.<pre>
    * enum {
    *    DMA0_SLOT_Disabled                   = 0,
    *    DMA0_SLOT_UART0_Receive              = 2,
    *    DMA0_SLOT_UART0_Transmit             = 3,
    *    ...
    * };
    * </pre>
    * @param writer
    * @throws IOException
    */
   private void getDmaInfo(DocumentUtilities documentUtilities) throws IOException {
      if (fPeripheral.getDmaInfoList().size() == 0) {
         return;
      }
      StringBuffer sb = new StringBuffer();
      sb.append(
            "   /!* DMA channel numbers */\n"+
            "   enum DmaChannels = {\n");
      for (DmaInfo item:fPeripheral.getDmaInfoList()) {
         sb.append(String.format("      %-25s = %s,\n", item.dmaSource, item.dmaChannelNumber));
      }
      sb.append("   };\n");
      documentUtilities.write(sb.toString());
   }

   @Override
   public int getFunctionIndex(PeripheralFunction function) {
      return 0;
   }

}