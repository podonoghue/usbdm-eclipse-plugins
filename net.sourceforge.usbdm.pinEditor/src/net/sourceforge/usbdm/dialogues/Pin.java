package net.sourceforge.usbdm.dialogues;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;

public class Pin extends Common {
   
   static HashMap<String, Pin> pinList = new HashMap<String, Pin>();
   
   protected String fName;
   protected ArrayList<Mux>     fMuxes;
   protected ArrayList<Package> fPackages;
   
   static Pin factory(String name) {
      Pin pin = pinList.get(name);
      if (pin == null) {
         pin = new Pin(name);
         pinList.put(name, pin);
      }
      return pin;
   }
   
   static void list(PrintStream out) {
      out.println("===== Pins ============");
      for( String key:pinList.keySet()) {
         Pin pin = pinList.get(key);
         out.println(pin.getName());
      }
   }

   private String getName() {
      return fName;
   }

   private Pin(String name) {
      fName     = name;
      fMuxes    = new ArrayList<Mux>();
      fPackages = new ArrayList<Package>();
   }
   
   void addMux(Mux mux) {
      fMuxes.add(mux);
   }

   void addPackage(Package pkg) {
      fPackages.add(pkg);
   }

   public void writeSVD(PrintStream out) {
      out.println(String.format("%s<pin name=\"%s\">", getIndentFill(), fName));
      increaseIndent();
      for (Mux mux:fMuxes) {
         mux.writeSVD(out);
      }
      for (Package pkg:fPackages) {
         pkg.writeSVD(out);
      }
      decreaseIndent();
      out.println(String.format("%s</pin>", getIndentFill()));
   }
}
