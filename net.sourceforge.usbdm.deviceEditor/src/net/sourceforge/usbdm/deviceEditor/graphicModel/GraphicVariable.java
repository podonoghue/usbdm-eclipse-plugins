package net.sourceforge.usbdm.deviceEditor.graphicModel;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

import net.sourceforge.usbdm.deviceEditor.information.Variable;

public class GraphicVariable extends GraphicBaseVariable {
   
   public GraphicVariable(int x, int y, int w, Height h, String text, Boolean canEdit, Variable variable) {
      super(x, y, w, vScale*(h.ordinal()+1), text, canEdit, variable);
      inputs     = new Point[1];
      inputs[0]  = new Point(-w/2, 0);
      outputs    = new Point[1];
      outputs[0] = new Point(+w/2, 0);
   }

   static public GraphicVariable create(int originX, int originY, String id, String params, Boolean canEdit, Variable var) throws Exception {

      String paramsArray[] = params.split(",");
      int    x = originX+Integer.parseInt(paramsArray[0].trim());
      int    y = originY+Integer.parseInt(paramsArray[1].trim());
      int    w = Integer.parseInt(paramsArray[2].trim());
      Height ht = Height.valueOf(paramsArray[3].trim());
      GraphicVariable t = new GraphicVariable(x, y, w, ht, id, canEdit, var);
      int h = vScale;
      if (ht == Height.large) {
         h = 2*vScale;
      }
      t.addInputsAndOutputs(4, paramsArray, w, h);
      return t;
   }

   @Override
   void draw(Display display, GC gc) {
      super.draw(display, gc);
      
      gc.setBackground(display.getSystemColor(backGroundColor));
      gc.setForeground(display.getSystemColor(lineColor));
      
      fillRectangle(gc, -w/2, -h/2, w, h);
      
      gc.setLineWidth(2);

      drawRectangle(gc, -w/2, -h/2, w, h);

      FontData data = display.getSystemFont().getFontData()[0];
      Font font = new Font(display, data.getName(), 10, SWT.NORMAL);
      gc.setFont(font);
      
      Variable var = getVariable();
//      if (var.getName().contains("icsClockMode")) {
//         System.err.println("Variable = "+var);
//      }
      if (h>=2*vScale) {
         if ((this.getStyle()&NONAME) == 0) {
            Point p = map(-w/2+6, -17);
            gc.drawText(name,                             p.x, p.y);
         }
         if ((this.getStyle()&NOVALUE) == 0) {
            Point p = map(-w/2+6, 0);
            gc.drawText(var.getValueAsBriefString(), p.x, p.y);
         }
      }
      else {
         String label = "";
         if ((this.getStyle()&NONAME) == 0) {
            label = name;
         }
         if ((this.getStyle()&NOVALUE) == 0) {
            if ((this.getStyle()&NONAME) == 0) {
               label = label +",  ";
            }
            label = label+var.getValueAsBriefString();
         }
         Point p = map(-w/2+6, -8);
         gc.drawText(label, p.x, p.y);
      }
      
      font.dispose();
   }

   @Override
   Point getEditPoint() {
      Point p;
      if (h>=2*vScale) {
         p = map(-w/2+6, 0);
      }
      else {
         p = map(-w/2+6, -8);
      }
      return p;
   }


   @Override
   public void reportParams(StringBuilder sb) {
      super.reportParams(sb);
      sb.append(String.format("%-20s", "type=\"variableBox\" "));

      StringBuilder params = new StringBuilder();
      params.append(String.format(" params=\"%4d,%4d,%4d, %s\" ", x, y, w, (h>20)?"large":"small"));
      sb.append(String.format("%-60s", params.toString()));
   }
}