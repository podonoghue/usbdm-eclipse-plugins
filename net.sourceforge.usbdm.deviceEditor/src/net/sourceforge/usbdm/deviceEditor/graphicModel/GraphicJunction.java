package net.sourceforge.usbdm.deviceEditor.graphicModel;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

import net.sourceforge.usbdm.deviceEditor.information.DoubleVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;

public class GraphicJunction extends GraphicBaseVariable {

   public GraphicJunction(int x, int y, String id, Boolean canEdit, Variable var) {
      super(x, y, dotSize, dotSize, id, canEdit, var);
      inputs     = new Point[1];
      inputs[0]  = new Point(0, 0);
      outputs    = new Point[1];
      outputs[0] = new Point(0, 0);
   }

   public static GraphicJunction create(int originX, int originY, String id, String params, Boolean canEdit, Variable var) {

      String paramsArray[] = params.split(",");
      int x = originX+Integer.parseInt(paramsArray[0].trim());
      int y = originY+Integer.parseInt(paramsArray[1].trim());

      GraphicJunction t = new GraphicJunction(x, y, id, false, var);

      return t;
   }

   @Override
   void draw(Display display, GC gc) {
      super.draw(display, gc);
      
//      drawBoundary(gc);

      StringBuilder label = new StringBuilder();
      String name = getName();
      if (((getStyle()&NONAME) == 0) && (name != null) && !name.isBlank()) {
         label.append(getName());
      }
      if ((getStyle()&NOVALUE) == 0) {
         Variable var = getVariable();
         if ((var instanceof LongVariable) || (var instanceof DoubleVariable)) {
            if (label.length() != 0) {
               label.append(", ");
            }
            label.append(var.getValueAsString());
         }
      }
      if (label.length() != 0) {
         FontData data = display.getSystemFont().getFontData()[0];
         Font font = new Font(display, data.getName(), 10, SWT.NORMAL);
         gc.setFont(font);
         gc.drawText(label.toString(), (nameX+x+w-40), (nameY+y-25));

         font.dispose();
      }
      
      gc.setBackground(display.getSystemColor(lineColor));
      gc.fillOval(x-dotSize/2, y-dotSize/2, dotSize, dotSize);
   }

   @Override
   Point getEditPoint() {
      return new Point(x,y);
   }

   @Override
   public void reportParams(StringBuilder sb) {
      super.reportParams(sb);
      sb.append(String.format("%-20s", "type=\"junction\" "));

      StringBuilder params = new StringBuilder();
      params.append(String.format(" params=\"%4d,%4d\" ", x, y));
      sb.append(String.format("%-60s", params.toString()));
   }

}