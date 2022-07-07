package tests.internal;
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

   /**
    * Process each CSV input file xxx.csv to produce xxx.usbdmHardware file
    * 
    * @param filepath      File to process
    * @param xmlDirectory  Directory for resulting file
    * 
    * @throws Exception
    */
   private static void processFile(Path filepath, Path xmlDirectory) throws Exception {
      System.err.println(" ======================== Processing " + filepath.getFileName() + " ======================== ");
      String sourceName      = filepath.getFileName().toString();
      String destinationName = sourceName.replaceAll("(^.*)"+Pattern.quote(DeviceInfo.HARDWARE_CSV_FILE_EXTENSION)+"$", "$1"+DeviceInfo.HARDWARE_FILE_EXTENSION);
      
      DeviceInfo deviceInfo = DeviceInfo.createFromHardwareFile(filepath);
      
      Path xmlFilePath = xmlDirectory.resolve(destinationName);
      FamilyXmlWriter writer = new FamilyXmlWriter(deviceInfo);
      writer.writeXmlFile(xmlFilePath);
   }
   
   public static void main(String[] args) throws Exception {
      
      Path directory = Paths.get("");
      
      // Locate data output directory  
      Path xmlDirectory = directory.resolve("Hardware");

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
         processFile(filePath, xmlDirectory);
      }
   }
}
