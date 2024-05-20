package tests.internal;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriteFamilyCpp;

public class TestCreateCpp {
   
   private static final DirectoryStream.Filter<Path> csvFilter = new DirectoryStream.Filter<Path>() {
      @Override
      public boolean accept(Path path) throws IOException {
         return path.getFileName().toString().matches(".*\\.csv$");
      }
   };

   public static void main(String[] args) throws Exception {
      
      Path directory     = Paths.get("BulkTesting");
      Path dataDirectory = Paths.get("data");
      
      // Locate header output directory
      Path headerDirectory = directory.resolve("Project_Headers");

      // Locate source output directory
      Path sourcesDirectory = directory.resolve("Sources");

      // Create output directories if needed
      if (!directory.toFile().exists()) {
         Files.createDirectory(directory);
      }
      if (!headerDirectory.toFile().exists()) {
         Files.createDirectory(headerDirectory);
      }
      if (!sourcesDirectory.toFile().exists()) {
         Files.createDirectory(sourcesDirectory);
      }

      DirectoryStream<Path> folderStream = Files.newDirectoryStream(dataDirectory.toAbsolutePath(), csvFilter);
      for (Path filePath : folderStream) {
         if (!Files.isRegularFile(filePath)) {
            continue;
         }
         /*
          * Process each input file
          */
         System.err.println("Processing " + filePath.getFileName() + " ======================== ");
         DeviceInfo deviceInfo = DeviceInfo.createFromHardwareFile(filePath);
         WriteFamilyCpp writer = new WriteFamilyCpp();
         writer.writeCppFiles(directory, deviceInfo);
      }
      folderStream.close();
   }
}