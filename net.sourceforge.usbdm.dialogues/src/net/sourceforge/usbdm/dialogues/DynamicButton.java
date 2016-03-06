package net.sourceforge.usbdm.dialogues;

import java.io.PrintStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

public class DynamicButton extends DynamicControl {

   @Override
   public void construct(Composite composite) {
      Button button = new Button(composite, SWT.NONE);
      button.setText(fTitle);
   }

   @Override
   public void writeSVD(PrintStream out) {
      out.print("<button");
      if (fName != null) {
         out.print(" name=\""+fName+"\"");
      }
      out.println("/>");
   }

}
