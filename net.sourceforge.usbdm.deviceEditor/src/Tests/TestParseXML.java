package Tests;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.DeviceVariantInformation;
import net.sourceforge.usbdm.deviceEditor.information.DevicePackage;
import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriteFamilyXML;
import net.sourceforge.usbdm.deviceEditor.xmlParser.ParseFamilyXML;

public class TestParseXML {
   
   private static final DirectoryStream.Filter<Path>xmlFilter = new DirectoryStream.Filter<Path>() {
      @Override
      public boolean accept(Path path) throws IOException {
         return path.getFileName().toString().matches(".*\\.hardware$");
      }
   };

   public static void main(String[] args) throws Exception {
      
      Path directory = Paths.get("");
      
      // Locate data output directory  
      Path outputDirectory = directory.resolve("xml2");

      // Create output directories if needed  
      if (!outputDirectory.toFile().exists()) {
         Files.createDirectory(outputDirectory);
      }
      DeviceInfo deviceInfo = null;
      DirectoryStream<Path> folderStream = Files.newDirectoryStream(directory.resolve("xml").toAbsolutePath(), xmlFilter);
      for (Path filePath : folderStream) {
         if (!Files.isRegularFile(filePath)) {
            continue;
         }
         /*
          * Process each input file
          */
         System.err.println("Processing " + filePath.getFileName() + " ======================== ");
         
         ParseFamilyXML parser = new ParseFamilyXML();
         deviceInfo = parser.parseFile(filePath);
         
//         report(deviceInfo);
         
         WriteFamilyXML writer = new WriteFamilyXML();
         Path xmlFilePath = outputDirectory.resolve(filePath.getFileName());
         writer.writeXmlFile(xmlFilePath, deviceInfo);
      }
   }

   static void report(DeviceInfo deviceInfo) {
      for (String key:deviceInfo.getDeviceVariants().keySet()) {
         DeviceVariantInformation deviceInformation = deviceInfo.findVariant(key);
         System.err.println("deviceInformation = " + deviceInformation);
      }
      for (String packageName:deviceInfo.getDevicePackages().keySet()) {
         DevicePackage devicePackage = deviceInfo.findDevicePackage(packageName);
         System.err.println("Package = " + devicePackage);
         for (String pinName:devicePackage.getPins().keySet()) {
            String location = devicePackage.getLocation(pinName);
            System.err.print(pinName + " => " + location+", ");
         }
         System.err.println();
      }
      for(String pinName:deviceInfo.getPins().keySet()) {
         Pin pin = deviceInfo.findPin(pinName);
         System.err.print("Pin = " + pin.getName() + ", ");
      };
      System.err.println();
      for(String peripheralName:deviceInfo.getPeripheralNames()) {
         System.err.print("Peripheral = " + peripheralName+", ");
      };
      System.err.println();
      for(String peripheralFunction:deviceInfo.getPeripheralFunctions().keySet()) {
         System.err.print("PeripheralFunction = " + peripheralFunction + ", ");
      };
      System.err.println();

   }

}
