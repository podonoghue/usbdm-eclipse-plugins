package net.sourceforge.usbdm.peripheralDatabase;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class to identify a SVD file 
 */
public class SVDIdentifier {
   final private String        providerId;
   private String              deviceName;
   private Path                path;
   /**
    * Create null identifier
    */
   public SVDIdentifier() {
      this.providerId = "";
      this.deviceName = "";
   }
   /**
    * 
    * @param id            Unique identifier for provider 
    * @param deviceName    Name of device (or regex)
    */
   public SVDIdentifier(String id, String deviceName) {
      this.providerId = id;
      this.deviceName = deviceName;
   }
   /**
    * 
    * @param id            Unique identifier for provider 
    * @param deviceName    Name of device (or regex)
    */
   public SVDIdentifier(Path path) {
      this.providerId = "";
      this.deviceName = "";
      this.path       = path;
   }
   /**
    * 
    */
   public SVDIdentifier(String identification) throws Exception {
      Pattern p1 = Pattern.compile("\\[SVDIdentifier:([^:]*):([^:]*).*\\]");
      Matcher m1 = p1.matcher(identification);
      if (m1.matches()) {
         // Identifies provider
         this.providerId = m1.group(1);
         this.deviceName = m1.group(2);
         this.path       = null;
         return;
      }
      Pattern p2 = Pattern.compile("\\[SVDIdentifier=(.*)\\]$");
      Matcher m2 = p2.matcher(identification);
      if (m2.matches()) {
         // Uses path
         this.path = Paths.get(m2.group(1));
      }
      System.err.println("Illegal identification: " + identification);
      throw new Exception("Invalid identification, should be \'[SVDIdentifier:provider:deviceName]\'");
   }
   public String getproviderId() {
      return providerId;
   }
   @Override
   public String toString() {
      if (path != null) {
         return "[SVDIdentifier:path=" + path + "]";
      }
      return "[SVDIdentifier:" + providerId + ":" + deviceName + "]";
   }
   public String getDeviceName() {
      return deviceName;
   }
   public void setDeviceName(String deviceName) {
      this.deviceName = deviceName;
   }
   public Path getPath() {
      return path;
   }
}
  
