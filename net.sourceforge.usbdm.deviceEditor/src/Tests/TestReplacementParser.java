package Tests;

import java.util.HashMap;
import net.sourceforge.usbdm.deviceEditor.xmlParser.ReplacementParser;
import net.sourceforge.usbdm.deviceEditor.information.FileUtility.IKeyMaker;

public class TestReplacementParser {

      final static HashMap<String, String> exampleSymbols = new HashMap<String, String>();
      final static String                  TestPattern    = "hello th$ere $(aaa::toupper) $(b$(ccc)bb) $(dd:234)";

      static IKeyMaker keyMaker = new IKeyMaker() {

         @Override
         public String makeKey(String name) {
            return name;
         }
      };
      
      public static void main(String[] args) throws Exception {
         exampleSymbols.put("aaa",    "a-a");
         exampleSymbols.put("bbb",    "b-b");
         exampleSymbols.put("ccc",    "ccc");
         exampleSymbols.put("bcccbb", "XXXXX");
         
         ReplacementParser parser = new ReplacementParser(exampleSymbols, keyMaker);
         
         System.err.println("'" + TestPattern + "' => '" + parser.parse(TestPattern) + "'");
      }

}
