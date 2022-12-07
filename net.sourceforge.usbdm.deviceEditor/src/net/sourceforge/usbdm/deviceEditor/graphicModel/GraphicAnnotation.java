package net.sourceforge.usbdm.deviceEditor.graphicModel;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

import net.sourceforge.usbdm.deviceEditor.information.Variable;

public class GraphicAnnotation extends GraphicBaseVariable {

   private final String format;

   public GraphicAnnotation(int x, int y, int w, int h, String id, String format, Boolean canEdit, Variable var) {
      super(x+w/2, y+h/2, w, h, id, canEdit, var);
      inputs     = new Point[1];
      inputs[0]  = new Point(0, 0);
      outputs    = new Point[1];
      outputs[0] = new Point(0, 0);
      
      this.format = format;
   }

   public static GraphicAnnotation create(int originX, int originY, String id, String params, Boolean canEdit, Variable var) {

      String paramsArray[] = params.split(",");
      int x = originX+Integer.parseInt(paramsArray[0].trim());
      int y = originY+Integer.parseInt(paramsArray[1].trim());
      int w = Integer.parseInt(paramsArray[2].trim());
      int h = Integer.parseInt(paramsArray[3].trim());
      String format = null;
      if (paramsArray.length>4) {
         format = paramsArray[4];
      }
      GraphicAnnotation t = new GraphicAnnotation(x, y, w, h, id, format, canEdit, var);

      t.addInputsAndOutputs(4, paramsArray, 10, 10);
      return t;
   }

   @Override
   void draw(Display display, GC gc) {
      super.draw(display, gc);

      gc.setBackground(display.getSystemColor(backGroundColor));
      gc.setForeground(display.getSystemColor(lineColor));
      
//      drawBoundary(gc);

      String label = "";
      if (format == null) {
         StringBuilder sb = new StringBuilder();
         String name = getName();
         if (((getStyle()&NONAME) == 0) && (name != null) && !name.isBlank()) {
            sb.append(getName());
         }
         if ((getStyle()&NOVALUE) == 0) {
            Variable var = getVariable();
            if (sb.length() != 0) {
               sb.append(", ");
            }
            sb.append(var.getValueAsString());
         }
         label = sb.toString();
      }
      else {
         label = format;
         label = label.replaceAll("%n", getName());
         Variable var = getVariable();
         label = label.replaceAll("%v", var.getValueAsString());
      }
      if (label.length() != 0) {
         FontData data = display.getSystemFont().getFontData()[0];
         Font font = new Font(display, data.getName(), 10, SWT.NORMAL);
         gc.setFont(font);
         gc.drawText(label, (x-w/2+nameX+2), (y-h/2+1+nameY));

         font.dispose();
      }
   }

   @Override
   Point getEditPoint() {
      return new Point((x-w/2+2), (y-8));
   }

   @Override
   public void reportParams(StringBuilder sb) {
      super.reportParams(sb);
      sb.append(String.format("%-20s", "type=\"annotation\" "));

      StringBuilder params = new StringBuilder();
      
      params.append(String.format(" params=\"%4d,%4d,%4d,%4d\" ", x-w/2, y-h/2, w, h));
      sb.append(String.format("%-60s", params.toString()));
   }

}