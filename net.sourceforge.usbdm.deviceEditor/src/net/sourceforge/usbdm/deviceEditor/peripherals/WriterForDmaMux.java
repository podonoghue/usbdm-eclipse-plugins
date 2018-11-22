package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.DmaInfo;
import net.sourceforge.usbdm.deviceEditor.xmlParser.XmlDocumentUtilities;
import net.sourceforge.usbdm.jni.UsbdmException;

/**
 * Class encapsulating the code for writing an instance of DigitalIO
 */
/**
 * @author podonoghue
 *
 */
public class WriterForDmaMux extends PeripheralWithState {

   public WriterForDmaMux(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
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
    *     &lt;dma source="Disabled"       num="0" /&gt;
    *     &lt;dma source="UART0_Receive"  num="2" /&gt;
    *     &lt;dma source="UART0_Transmit" num="3" /&gt;
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
      documentUtilities.setAttrWidth(30);

      for (DmaInfo item:getDmaInfoList()) {
         documentUtilities.openTag("dma");
         documentUtilities.writeAttribute("source", item.dmaSource);
         documentUtilities.writeAttribute("num",    item.dmaChannelNumber);
         documentUtilities.closeTag();;
      }
      documentUtilities.popAttrWidth();
   }

}