package Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;

import net.sourceforge.usbdm.cdt.ui.handlers.CreateLaunchConfigurationHandler;

public class TestScrapeFiles {

   static class Tuple {
      String match;
      String result;
      
      Tuple(String match, String result) {
         this.match  = match;
         this.result = result;
      }
      public String toString() {
         return "["+match+","+result+"]";
      }
   };
   
   public static void main(String[] args) throws IOException {

      final Tuple tuples[] = {
            new Tuple(".*DEVICE_NAME.*\"(?:Freescale|NXP)_.*_(.+)\".*",                   "$1"),    // PE Launch
            new Tuple(".*gdbServerDeviceName.*\"\\s*value\\s*=\\s*\"M?(.+?)x+(.*?)\".*",  "$1M$2"), // Segger launch
            new Tuple(".*&lt;name&gt;(LPC.*)/(.*)&lt;/name&gt;&#13;.*",                   "$1_$2"), // mcuExpress .cproject
            new Tuple(".*&lt;name&gt;(.*)&lt;/name&gt;&#13;.*",                           "$1"),    // mcuExpress .cproject
      };
      final String probeFiles[] = {
            "testFiles/kds_PNE.launch",
            "testFiles/kds_Segger.launch",
            "testFiles/mcuExpress_cproject_cm0",
            "testFiles/mcuXpress_cproject",
      };
      for (String testFile:probeFiles) {
         System.err.println("============\nScraping "+testFile);
         for (Tuple tuple:tuples) {
            InputStream inputstream = new FileInputStream(testFile);
            Matcher m = CreateLaunchConfigurationHandler.scrapeFile(inputstream, tuple.match);
            if (m != null) {
               String context = m.group();
               System.err.println(String.format("%-70s",tuple)+" => Found '"+m.replaceAll(tuple.result)+"' within '"+context+"'");
               break;
            }
//            if (m == null) {
//               System.err.println(String.format("%-70s",tuple)+" => No match");
//            }
//            else {
//               String context = m.group();
//               System.err.println(String.format("%-70s",tuple)+" => Found '"+m.replaceAll(tuple.result)+"' within '"+context+"'");
//            }
            inputstream.close();
         }
      }
   }

}
