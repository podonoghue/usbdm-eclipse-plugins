package net.sourceforge.usbdm.dialogues;

import java.io.PrintStream;
import java.util.ArrayList;

import org.eclipse.swt.widgets.Composite;

public class DynamicPage {
   
   private ArrayList<DynamicGroup> fComposites;
   private String fName;
   private String fTitle;
   
   public DynamicPage() {
      fName     = "No Name";
      fComposites =  new ArrayList<DynamicGroup>();
   }
   
   public void setName(String name) {
      fName = name;
   }

   public String getName() {
      return fName;
   }

   public void addControl(DynamicGroup control) {
      fComposites.add(control);
   }
   
   public ArrayList<DynamicGroup> getControls() {
      return fComposites;
   }

   public void writeSVD(PrintStream out) {
      out.println(String.format("<page name=\"%s\">", fName));
      for (DynamicGroup composite:fComposites) {
         composite.writeSVD(out);
      }
      out.println("</page>");
   }

   public void construct(Composite composite) {
      for (DynamicGroup dComposite:fComposites) {
         dComposite.construct(composite);
      }
   }

   public void setTitle(String title) {
      fTitle = title;
   }
}
