package net.sourceforge.usbdm.deviceEditor.graphicModel;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

import net.sourceforge.usbdm.deviceEditor.information.Variable;

public class GraphicLabel extends GraphicBaseVariable {
   
   public GraphicLabel(int x, int y, int w, Height h, String text, String name, Boolean canEdit, Variable variable) {
      super(x, y, w, vScale*(h.ordinal()+1), text, canEdit, variable);
      if (name != null) {
         setName(name);
      }
      inputs     = new Point[1];
      inputs[0]  = new Point(-w/2, 0);
      outputs    = new Point[1];
      outputs[0] = new Point(+w/2, 0);
   }

   static public GraphicLabel create(int originX, int originY, String id, String name, String params, Boolean canEdit, Variable var) throws Exception {

      String paramsArray[] = params.split(",");
      int    x = originX+Integer.parseInt(paramsArray[0].trim());
      int    y = originY+Integer.parseInt(paramsArray[1].trim());
      int    w = Integer.parseInt(paramsArray[2].trim());
      Height ht = Height.valueOf(paramsArray[3].trim());
      GraphicLabel t = new GraphicLabel(x, y, w, ht, id, name, canEdit, var);
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
      
      Point p = map(-w/2+6, -8);
      if (h>=2*vScale) {
         p = map(-w/2+6, -17);
      }
      gc.drawText(name, p.x, p.y);
      
      font.dispose();
   }

   @Override
   Point getEditPoint() {
      Point p = map(-w/2+6, -8);
      if (h>=2*vScale) {
         p = map(-w/2+6, -17);
      }
      return p;
   }

}