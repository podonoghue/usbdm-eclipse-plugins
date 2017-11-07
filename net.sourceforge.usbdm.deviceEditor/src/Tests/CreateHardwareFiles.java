package Tests;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.peripherals.FamilyXmlWriter;

public class CreateHardwareFiles {
   
   private static final DirectoryStream.Filter<Path> sourceFilter = new DirectoryStream.Filter<Path>() {
      @Override
      public boolean accept(Path path) throws IOException {
         return path.getFileName().toString().matches(".*"+Pattern.quote(DeviceInfo.HARDWARE_CSV_FILE_EXTENSION)+"$");
      }
   };

   public static void main(String[] args) throws Exception {
      
      Path directory = Paths.get("");
      
      // Locate data output directory  
      Path xmlDirectory = directory.resolve("hardware");

      // Create output directories if needed  
      if (!xmlDirectory.toFile().exists()) {
         Files.createDirectory(xmlDirectory);
      }
      // Path to data folder - csv files describing the device
      DirectoryStream<Path> folderStream = Files.newDirectoryStream(directory.resolve("data").toAbsolutePath(), sourceFilter);
      for (Path filePath : folderStream) {
         if (!Files.isRegularFile(filePath)) {
            continue;
         }
         /*
          * Process each input file
          */
         System.err.println("Processing " + filePath.getFileName() + " ======================== ");
         String sourceName      = filePath.getFileName().toString();
         String destinationName = sourceName.replaceAll("(^.*)"+Pattern.quote(DeviceInfo.HARDWARE_CSV_FILE_EXTENSION)+"$", "$1"+DeviceInfo.HARDWARE_FILE_EXTENSION);
         
         DeviceInfo deviceInfo = DeviceInfo.create(filePath);
         
         Path xmlFilePath = xmlDirectory.resolve(destinationName);
         FamilyXmlWriter writer = new FamilyXmlWriter(deviceInfo);
         writer.writeXmlFile(xmlFilePath);
      }
   }
}
