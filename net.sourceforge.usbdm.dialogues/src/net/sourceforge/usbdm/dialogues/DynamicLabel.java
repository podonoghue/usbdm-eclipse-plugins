package net.sourceforge.usbdm.dialogues;

import java.io.PrintStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class DynamicLabel extends DynamicControl {

   @Override
   public void construct(Composite composite) {
      Label label = new Label(composite, SWT.NONE);
      label.setText(fTitle);
   }

   @Override
   public void writeSVD(PrintStream out) {
      out.print("<label");
      if (fName != null) {
         out.print(" name=\""+fName+"\"");
      }
      out.println("/>");
   }

}
