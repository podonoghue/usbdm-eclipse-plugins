package net.sourceforge.usbdm.dialogues;

import java.io.PrintStream;
import java.util.HashMap;

public class Signal extends Common {
   
   static HashMap<String, Signal> signalList = new HashMap<String, Signal>();

   static Signal factory(String name) {
      Signal signal = signalList.get(name);
      if (signal == null) {
         signal = new Signal(name);
         signalList.put(name, signal);
      }
      return signal;
   }
   
   static void list(PrintStream out) {
      out.println("===== Signals ============");
      for( String key:signalList.keySet()) {
         Signal signal = signalList.get(key);
         out.println(signal.getName());
      }
   }

   protected String fName;
   
   private Signal(String name) {
      fName = name;
   }

   String getName() {
      return fName;
   }
   
   public void writeSVD(PrintStream out) {
      out.println(String.format("%s<signal name=\"%s\" />", getIndentFill(), fName));
   }
}
