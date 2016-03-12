package net.sourceforge.usbdm.dialogues;

import java.io.PrintStream;
import java.util.ArrayList;

import org.eclipse.swt.widgets.Shell;

public class DynamicDialogue {   
   
   private String fName;
   private ArrayList<DynamicPage> fPages;

   public DynamicDialogue() {
      fName  = "No Name";
      fPages = new ArrayList<DynamicPage>();
   }
   
   public void setName(String name) {
      fName = name;
   }

   public String getName() {
      return fName;
   }

   void addPage(DynamicPage page) {
      fPages.add(page);
   }

   ArrayList<DynamicPage> getPages() {
      return fPages;
   }

   public void writeSVD(PrintStream out) {
      out.println(String.format("<dialogue name=\"%s\">", fName));
      for (DynamicPage page:fPages) {
         page.writeSVD(out);
      }
      out.println("</dialogue>");
   }

   public void construct(Shell shell) {
      for (DynamicPage page:fPages) {
         page.construct(shell);
      }
   }
}
