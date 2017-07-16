package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.DmaInfo;
import net.sourceforge.usbdm.deviceEditor.xmlParser.XmlDocumentUtilities;

/**
 * Class encapsulating the code for writing an instance of DigitalIO
 */
/**
 * @author podonoghue
 *
 */
public class WriterForDmaMux extends PeripheralWithState {

   public WriterForDmaMux(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "Direct Memory Access (DMA)";
   }

   @Override
   public void writeExtraXMLDefinitions(XmlDocumentUtilities documentUtilities) throws IOException {
      super.writeExtraXMLDefinitions(documentUtilities);
      writeDmaXmlInfo(documentUtilities);
   }
   
   /**
    * Writes enumeration describing DMA slot use
    * 
    * e.g. <pre>
    *  &lt;dma&gt;
    *     &lt;slot source="Disabled"       num="0" /&gt;
    *     &lt;slot source="UART0_Receive"  num="2" /&gt;
    *     &lt;slot source="UART0_Transmit" num="3" /&gt;
    *  &lt;dma&gt;
    * </pre>
    * 
    * @param writer
    * 
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
   }

   @Override
   public void writeNamespaceInfo(DocumentUtilities documentUtilities) throws IOException {
      super.writeNamespaceInfo(documentUtilities);
      writeDmaInfo(documentUtilities);
   }

   /**
    * Writes enumeration describing DMA slot use
    * 
    * e.g.<pre>
    * enum DmaSlot {
    *    DmaSlot_Disabled       = 0,
    *    DmaSlot_UART0_Receive  = 2,
    *    DmaSlot_UART0_Transmit = 3,
    *    ...
    * };
    * </pre>
    * @param writer
    * @throws IOException
    */
   private void writeDmaInfo(DocumentUtilities documentUtilities) throws IOException {
      if (getDmaInfoList().size() == 0) {
         return;
      }
      StringBuffer sb = new StringBuffer();
      sb.append(
            "/** \n" +
            " * DMA channel numbers \n" +
            " */\n"+
            "enum DmaSlot {\n");
      for (DmaInfo item:getDmaInfoList()) {
         sb.append(String.format("   %-35s = %s,\n", "DmaSlot_"+item.dmaSource, item.dmaChannelNumber));
      }
      sb.append("};\n\n");
      documentUtilities.write(sb.toString());
   }
}