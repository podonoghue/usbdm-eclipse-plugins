package net.sourceforge.usbdm.dialogues;

import java.io.PrintStream;

import org.eclipse.swt.widgets.Composite;

public abstract class DynamicControl { 
   
   protected String fName;
   protected String fTitle;

   DynamicControl() {
      fName = "";
   }
   
   public void setName(String name) {
      fName = name;
   }

   public String getName() {
      return fName;
   }

   public void setTitle(String title) {
      fTitle = title;
   }

   public String getTitle() {
      return fName;
   }

   public abstract void writeSVD(PrintStream out);

   abstract public void construct(Composite composite);
   
}
