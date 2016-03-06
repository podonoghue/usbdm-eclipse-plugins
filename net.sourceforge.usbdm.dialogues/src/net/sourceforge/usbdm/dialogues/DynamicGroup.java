package net.sourceforge.usbdm.dialogues;

import java.io.PrintStream;
import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

public class DynamicGroup extends DynamicControl {
   
   private ArrayList<DynamicControl>   fControls;
   private DynamicLayout               fLayout = null;
   private String                      fBorder;
   
   public DynamicGroup() {
      fControls = new ArrayList<DynamicControl>();
   }
   
   public void addControl(DynamicControl control) {
      fControls.add(control);
   }
   
   public ArrayList<DynamicControl> getControls() {
      return fControls;
   }

   public void writeSVD(PrintStream out) {
      out.print("<composite");
      if (fName != null) {
         out.print(" name=\""+fName+"\"");
      }
      out.println(">");
      if (fLayout != null) {
         fLayout.writeSVD(out);
      }
      for (DynamicControl control:fControls) {
         control.writeSVD(out);
      }
      out.println("</composite>");
   }

   public void construct(Composite owner) {
      Composite composite = null;
      if (fBorder.equalsIgnoreCase("none") && (fTitle.length() == 0)) {
         composite = new Composite(owner, SWT.FILL);
      }
      else if (fBorder.equalsIgnoreCase("etched")) {
         composite = new Group(owner, SWT.FILL|SWT.SHADOW_ETCHED_IN);
         ((Group)composite).setText(fTitle);
      }
      else {
         composite = new Group(owner, SWT.FILL|SWT.SHADOW_ETCHED_IN);
         ((Group)composite).setText(fTitle);
      }
      if (fLayout != null) {
         fLayout.addLayout(composite);
      }
      for (DynamicControl control:fControls) {
         control.construct(composite);
      }
   }

   public void setLayout(DynamicLayout layout) {
      if (fLayout != null) {
         throw new RuntimeException("Layout already set");
      }
      fLayout = layout;
   }

   public void setBorder(String attribute) {
      fBorder = attribute;
   }
}
