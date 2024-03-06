package net.sourceforge.usbdm.deviceEditor.graphicModel;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.DoubleVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;

public class GraphicNode extends GraphicBaseVariable {

   boolean isBooleanVariable = false;
   
   public GraphicNode(int x, int y, String id, Boolean canEdit, Variable var) {
      super(x, y, 10, 10, id, canEdit, var);
      
      isBooleanVariable = (var != null) && (var instanceof BooleanVariable);
      
      inputs     = new Point[1];
      inputs[0]  = new Point(-w/2, 0);
      outputs    = new Point[1];
      outputs[0] = new Point(+w/2, 0);
   }

   public static GraphicNode create(int originX, int originY, String id, String params, Boolean canEdit, Variable var) throws Exception {
      try {
         String paramsArray[] = params.split(",");
         int x = originX+Integer.parseInt(paramsArray[0].trim());
         int y = originY+Integer.parseInt(paramsArray[1].trim());

         GraphicNode t = new GraphicNode(x, y, id, canEdit, var);

         t.addInputsAndOutputs(2, paramsArray, 10, 10);
         return t;
      } catch (Exception e) {
         throw new Exception("Expected parameters 'x,y [, in/out...]'", e);
      }
   }

   @Override
   void draw(Display display, GC gc) {
      super.draw(display, gc);

      gc.setBackground(display.getSystemColor(backGroundColor));
      gc.setForeground(display.getSystemColor(lineColor));
      
      //      drawBoundary(gc);

      gc.fillRectangle(x-w/2,   y-h/2,    w,    h);
      gc.setLineWidth(2);
      gc.drawRectangle(x-w/2,   y-h/2,    w,    h);

      if (isBooleanVariable && getVariable().getValueAsBoolean()) {
         gc.drawLine(x-w/2,   y-h/2,    x+w/2,    y+h/2);
         gc.drawLine(x+w/2,   y-h/2,    x-w/2,    y+h/2);
      }
      StringBuilder label = new StringBuilder();
      String name = getName();
      if (((getStyle()&NONAME) == 0) && (name != null) && !name.isBlank()) {
         label.append(getName());
      }
      if ((getStyle()&NOVALUE) == 0) {
         Variable var = getVariable();
//         if (var.getName().contains("cop_timeout")) {
//            System.err.println("Found it " + var.getName());
//         }
         if ((var instanceof LongVariable) || (var instanceof DoubleVariable)) {
            if (label.length() != 0) {
               label.append(", ");
            }
            label.append(var.getValueAsBriefString());
         }
      }
      if (label.length() != 0) {
         FontData data = display.getSystemFont().getFontData()[0];
         Font font = new Font(display, data.getName(), 10, SWT.NORMAL);
         gc.setFont(font);
         gc.drawText(label.toString(), (nameX+x+w-40), (nameY+y-25));

         font.dispose();
      }
   }

   @Override
   Point getEditPoint() {
      return new Point((x+w-40), (y-25));
   }

   @Override
   public void reportParams(StringBuilder sb) {
      super.reportParams(sb);
      sb.append(String.format("%-20s", "type=\"node\" "));

      StringBuilder params = new StringBuilder();
      params.append(String.format(" params=\"%4d,%4d\" ", x, y));
      sb.append(String.format("%-60s", params.toString()));
   }

   @Override
   public int getDrawPriority() {
      return MID_PRIORITY+10;
   }

}