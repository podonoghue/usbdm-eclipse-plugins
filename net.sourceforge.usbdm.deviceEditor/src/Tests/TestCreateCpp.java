package Tests;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.parser.ParseFamilyCSV;
import net.sourceforge.usbdm.deviceEditor.parser.WriteFamilyCpp;

public class TestCreateCpp {
   
   private static final DirectoryStream.Filter<Path> csvFilter = new DirectoryStream.Filter<Path>() {
      @Override
      public boolean accept(Path path) throws IOException {
         return path.getFileName().toString().matches(".*\\.csv$");
      }
   };

   public static void main(String[] args) throws Exception {
      
      Path directory = Paths.get("");
      
      // Locate header output directory  
      Path headerDirectory = directory.resolve("Project_Headers");

      // Locate source output directory  
      Path sourcesDirectory = directory.resolve("Sources");

      // Create output directories if needed  
      if (!headerDirectory.toFile().exists()) {
         Files.createDirectory(headerDirectory);
      }
      if (!sourcesDirectory.toFile().exists()) {
         Files.createDirectory(sourcesDirectory);
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
         
         ParseFamilyCSV reader = new ParseFamilyCSV();
         DeviceInfo deviceInfo = reader.parseFile(filePath);
         
         WriteFamilyCpp writer = new WriteFamilyCpp();
         writer.writeCppFiles(directory, deviceInfo);
      }
   }
}