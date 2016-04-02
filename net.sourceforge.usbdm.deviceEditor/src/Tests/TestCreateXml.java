package Tests;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.parser.ParseFamilyCSV;
import net.sourceforge.usbdm.deviceEditor.parser.WriteFamilyXML;

public class TestCreateXml {
   /** Base name for pin mapping file */
   private final static String pinMappingBaseFileName   = "pin_mapping";
   
   private static final DirectoryStream.Filter<Path> csvFilter = new DirectoryStream.Filter<Path>() {
      @Override
      public boolean accept(Path path) throws IOException {
         return path.getFileName().toString().matches(".*\\.csv$");
      }
   };

   public static void main(String[] args) throws Exception {
      
      Path directory = Paths.get("");
      
      // Locate data output directory  
      Path xmlDirectory = directory.resolve("xml");

      // Create output directories if needed  
      if (!xmlDirectory.toFile().exists()) {
         Files.createDirectory(xmlDirectory);
      }
      DirectoryStream<Path> folderStream = Files.newDirectoryStream(directory.resolve("data").toAbsolutePath(), csvFilter);
      for (Path filePath : folderStream) {
         if (!Files.isRegularFile(filePath)) {
            continue;
         }
         /*
          * Process each input file
          */
         System.err.println("Processing " + filePath.getFileName() + " ======================== ");
         String sourceName = filePath.getFileName().toString();
         String deviceName = sourceName.replaceAll("\\.csv", "");
         
         ParseFamilyCSV reader = new ParseFamilyCSV();
         DeviceInfo deviceInfo = reader.parseFile(filePath);
         
         String xmlFileName = pinMappingBaseFileName+"-"+deviceName+".hardware";
         
         WriteFamilyXML writer = new WriteFamilyXML();
         Path xmlFilePath = xmlDirectory.resolve(xmlFileName);
         writer.writeXmlFile(xmlFilePath, deviceInfo);
      }
   }
}
