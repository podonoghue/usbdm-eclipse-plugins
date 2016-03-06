package net.sourceforge.usbdm.dialogues;

public class Common {
   protected static int indent = 0;
   
   static final String fill = "                                                     ";

   String getIndentFill() {
      return fill.substring(0, indent);
   }
   void increaseIndent() {
      indent += 3;
   }
   void decreaseIndent() {
      indent -= 3;
   }
}
