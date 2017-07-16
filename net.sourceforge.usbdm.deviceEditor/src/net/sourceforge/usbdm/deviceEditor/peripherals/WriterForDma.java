package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.xmlParser.XmlDocumentUtilities;
import net.sourceforge.usbdm.peripheralDatabase.VectorTable;

/**
 * Class encapsulating the code for writing an instance of DigitalIO
 */
/**
 * @author podonoghue
 *
 */
public class WriterForDma extends PeripheralWithState {

   public WriterForDma(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "Direct Memory Access (DMA)";
   }

   @Override
   public void writeExtraXMLDefinitions(XmlDocumentUtilities documentUtilities) throws IOException {
      super.writeExtraXMLDefinitions(documentUtilities);
   }
   
   @Override
   public void writeExtraInfo(DocumentUtilities documentUtilities) throws IOException {
   }

   @Override
   public void writeNamespaceInfo(DocumentUtilities documentUtilities) throws IOException {
      super.writeNamespaceInfo(documentUtilities);
   }

   public void modifyVectorTable(VectorTable vectorTable) {
      super.modifyVectorTable(vectorTable, "^DMA((\\d+)?).*");
   }

}