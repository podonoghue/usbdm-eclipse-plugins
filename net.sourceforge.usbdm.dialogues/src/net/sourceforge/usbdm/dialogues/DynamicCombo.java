package net.sourceforge.usbdm.dialogues;

import java.io.PrintStream;
import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;

public class DynamicCombo extends DynamicControl {
   protected ArrayList<String> choices;
   private int fSelection;
   
   DynamicCombo() {
      super();
      choices     = new ArrayList<String>();
      fSelection  = 0;
   }
   
   @Override
   public void construct(Composite composite) {
      Combo combo = new Combo(composite, SWT.NONE);
      combo.setText(fTitle);
      for (String choice:choices) {
         combo.add(choice);
      }
      combo.select(fSelection);
   }

   @Override
   public void writeSVD(PrintStream out) {
      out.print("<combo");
      if (fName != null) {
         out.print(" name=\""+fName+"\"");
      }
      out.println("/>");
   }

   public void addChoice(String choice) {
      choices.add(choice);
   }

   public void setSelection(int selection) {
      fSelection = selection;
   }

}
