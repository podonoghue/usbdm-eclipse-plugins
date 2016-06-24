package net.sourceforge.usbdm.dialogues;

import java.io.PrintStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;

public class DynamicLayout {
   String fType;
   private int fColumns;
   @SuppressWarnings("unused")
   private int fRows;
   private String fDirection;

   DynamicLayout(String style) {
      fType = style;
   }   

   public void writeSVD(PrintStream out) {
      out.println(String.format("<layout type=\"%s\"/>", fType));
   }

   public void addLayout(Composite composite) {
      if (fType.equalsIgnoreCase("grid")) {
         GridLayout layout = new GridLayout();
         layout.numColumns = fColumns;
         layout.makeColumnsEqualWidth = false;
         layout.horizontalSpacing = 20;
         composite.setLayout(layout);
      }
      else if (fType.equalsIgnoreCase("row")) {
         RowLayout layout = new RowLayout();
         layout.wrap = false;
         layout.pack = false;
         layout.justify = false;
         layout.spacing = 10;
         if (fDirection.equalsIgnoreCase("vertical")) {
            layout.type = SWT.VERTICAL;
         }
         else {
            layout.type = SWT.HORIZONTAL;
         }
         layout.marginLeft = 5;
         layout.marginTop = 5;
         layout.marginRight = 5;
         layout.marginBottom = 5;
         layout.spacing = 0;
         composite.setLayout(layout);
      }
      else if (fType.equalsIgnoreCase("fill")) {
         FillLayout layout = new FillLayout();
         if (fDirection.equalsIgnoreCase("vertical")) {
            layout.type = SWT.VERTICAL;
         }
         else {
            layout.type = SWT.HORIZONTAL;
         }
         composite.setLayout(layout);
      }
   }

   public void setColumns(int columns) {
      fColumns = columns;
   }

   public void setRows(int rows) {
      fRows = rows;
   }

   public void setDirection(String direction) {
      fDirection = direction;
   }
}
