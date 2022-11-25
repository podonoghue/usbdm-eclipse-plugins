package net.sourceforge.usbdm.deviceEditor.graphicModel;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Display;

import net.sourceforge.usbdm.deviceEditor.information.Variable;

public class GraphicVariable extends GraphicBaseVariable {
   
   public GraphicVariable(int x, int y, int w, Height h, String text, Variable variable) {
      super(x, y, w, vScale*(h.ordinal()+1), text, variable);
      inputs     = new Point[1];
      inputs[0]  = new Point(-w/2, 0);
      outputs    = new Point[1];
      outputs[0] = new Point(+w/2, 0);
   }

   static public GraphicVariable create(String id, String params, Variable var) {

      String paramsArray[] = params.split(",");
      int    x = Integer.parseInt(paramsArray[0].trim());
      int    y = Integer.parseInt(paramsArray[1].trim());
      int    w = Integer.parseInt(paramsArray[2].trim());
      Height ht = Height.valueOf(paramsArray[3].trim());
      GraphicVariable t = new GraphicVariable(x, y, w, ht, id, var);
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
      
      fillRectangle(gc, -w/2, -h/2, w, h);
      
      gc.setLineWidth(2);

      drawRectangle(gc, -w/2, -h/2, w, h);

      FontData data = display.getSystemFont().getFontData()[0];
      Font font = new Font(display, data.getName(), 10, SWT.NORMAL);
      gc.setFont(font);
      
      if (h>=2*vScale) {
         Point p = map(-w/2+6, -17);
         gc.drawText(text,                             p.x, p.y);
         p = map(-w/2+6, 0);
         gc.drawText(getVariable().getValueAsString(), p.x, p.y);
      }
      else {
         String label = text+",  "+getVariable().getValueAsString();
         Point p = map(-w/2+6, -8);
         gc.drawText(label, p.x, p.y);
      }
      
      font.dispose();
   }

}