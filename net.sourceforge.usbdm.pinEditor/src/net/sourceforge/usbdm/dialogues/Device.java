package net.sourceforge.usbdm.dialogues;

import java.io.PrintStream;
import java.util.ArrayList;

public class Device extends Common {
   protected String fName;
   protected String fTitle;
   protected ArrayList<Pin> fPins;
   protected ArrayList<Peripheral> fPeripherals;
   
   public Device(String name, String title) {
      fName          = name;
      fTitle         = title;
      fPins          = new ArrayList<Pin>();
      fPeripherals   = new ArrayList<Peripheral>();
   }
   
   void addPin(Pin pin) {
      fPins.add(pin);
   }
   
   void addPeripheral(Peripheral peripheral) {
      fPeripherals.add(peripheral);
   }

   public void writeSVD(PrintStream out) {
      out.println(String.format("%s<device name=\"%s\" title=\"%s\" >", getIndentFill(), fName, fTitle));
      increaseIndent();
      for (Pin pin:fPins) {
         pin.writeSVD(out);
      }
      for (Peripheral peripheral:fPeripherals) {
         peripheral.writeSVD(out);
      }
      decreaseIndent();
      out.println("</device>");
   }
}
