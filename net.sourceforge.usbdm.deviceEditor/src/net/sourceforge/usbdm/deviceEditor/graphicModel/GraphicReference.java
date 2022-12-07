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

public class GraphicReference extends GraphicBaseVariable {

   public GraphicReference(int x, int y, int w, String id, Boolean canEdit, Variable var) {
      super(x, y, w, vScale, id, canEdit, var);
      inputs     = new Point[1];
      inputs[0]  = new Point(-w/2, 0);
      outputs    = new Point[1];
      outputs[0] = new Point(+w/2, 0);
   }

   public static GraphicReference create(int originX, int originY, String id, String params, Boolean canEdit, Variable var) {

      String paramsArray[] = params.split(",");
      int x = originX+Integer.parseInt(paramsArray[0].trim());
      int y = originY+Integer.parseInt(paramsArray[1].trim());
      int w = Integer.parseInt(paramsArray[2].trim());

      GraphicReference t = new GraphicReference(x, y, w, id, canEdit, var);

      t.addInputsAndOutputs(2, paramsArray, 10, 10);
      return t;
   }

   @Override
   void draw(Display display, GC gc) {
      super.draw(display, gc);

      gc.setBackground(display.getSystemColor(backGroundColor));
      gc.setForeground(display.getSystemColor(lineColor));
      
      drawBoundary(gc);

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
         gc.drawText(label.toString(), x-w/2+4, y-data.getHeight()/2-2);

         font.dispose();
      }
   }

   @Override
   Point getEditPoint() {
      return new Point(x-w/2+4, y-h/2+2);
   }

   @Override
   public void reportParams(StringBuilder sb) {
      super.reportParams(sb);
      sb.append(String.format("%-20s", "type=\"reference\" "));

      StringBuilder params = new StringBuilder();
      
      params.append(String.format(" params=\"%4d,%4d,%4d\" ", x, y, w));
      sb.append(String.format("%-60s", params.toString()));
   }

}