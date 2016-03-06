package net.sourceforge.usbdm.dialogues;

import java.io.PrintStream;
import java.util.ArrayList;

public class Peripheral extends Common {
   protected String fName;
   protected ArrayList<Signal> fSignals;
   
   public Peripheral(String name) {
      fName = name;
      fSignals = new ArrayList<Signal>();
   }

   void addSignal(Signal signal) {
      fSignals.add(signal);
   }
   
   public void writeSVD(PrintStream out) {
      out.println(String.format("%s<peripheral name=\"%s\"", getIndentFill(), fName));
      increaseIndent();
      for (Signal signal:fSignals) {
         signal.writeSVD(out);
      }
      decreaseIndent();
      out.println("</peripheral>");
   }

}
