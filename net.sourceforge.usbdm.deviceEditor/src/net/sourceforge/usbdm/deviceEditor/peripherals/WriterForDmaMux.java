package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.DmaInfo;
import net.sourceforge.usbdm.deviceEditor.information.Peripheral;
import net.sourceforge.usbdm.deviceEditor.xmlParser.XmlDocumentUtilities;

/**
 * Class encapsulating the code for writing an instance of DigitalIO
 */
/**
 * @author podonoghue
 *
 */
public class WriterForDmaMux extends Peripheral {

   public WriterForDmaMux(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "Direct Memory Access (DMA)";
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
      if (getDmaInfoList().size() == 0) {
         return;
      }
      documentUtilities.openTag("dma");
      documentUtilities.setAttrWidth(30);

      for (DmaInfo item:getDmaInfoList()) {
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
      if (getDmaInfoList().size() == 0) {
         return;
      }
      StringBuffer sb = new StringBuffer();
      sb.append(
            "   /* DMA channel numbers */\n"+
            "   enum DmaChannels {\n");
      for (DmaInfo item:getDmaInfoList()) {
         sb.append(String.format("      %-45s = %s,\n", "DMA0_SLOT_"+item.dmaSource, item.dmaChannelNumber));
      }
      sb.append("   };\n");
      documentUtilities.write(sb.toString());
   }
}