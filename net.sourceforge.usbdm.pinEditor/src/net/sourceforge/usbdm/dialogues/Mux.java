package net.sourceforge.usbdm.dialogues;

import java.io.PrintStream;

public class Mux extends Common {
   protected Signal        fSignal       = null;
   protected MuxSelection  fMuxSelection = MuxSelection.Disabled;
   
   Mux(Signal pin, MuxSelection muxSelection) {
      fSignal        = pin;
      fMuxSelection  = muxSelection;
   }

   public void writeSVD(PrintStream out) {
      out.println(String.format("%s<mux signal=\"%s\" mux=\"%s\"/>", getIndentFill(), fSignal.getName(), fMuxSelection.value));
   }
}
