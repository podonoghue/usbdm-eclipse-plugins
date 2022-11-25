package net.sourceforge.usbdm.deviceEditor.graphicModel;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Display;

import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;

public class GraphicNode extends GraphicBaseVariable {
   
   public GraphicNode(int x, int y, String id, Variable var) {
      super(x, y, 10, 10, id, var);
      if ("MCGFLLCLK".equals(id)) {
         System.err.println("MCGFLLCLK");
      }
      inputs     = new Point[1];
      inputs[0]  = new Point(-w/2, 0);
      outputs    = new Point[1];
      outputs[0] = new Point(+w/2, 0);
   }

   public static GraphicNode create(String id, String params, Variable var) {
      
      String paramsArray[] = params.split(",");
      int x = Integer.parseInt(paramsArray[0].trim());
      int y = Integer.parseInt(paramsArray[1].trim());
      
      GraphicNode t = new GraphicNode(x, y, id, var);

      t.addInputsAndOutputs(2, paramsArray, 10, 10);
      return t;
   }

   @Override
   void draw(Display display, GC gc) {
      super.draw(display, gc);
      
//      drawBoundary(gc);
      
      gc.fillRectangle(x-w/2,   y-h/2,    w,    h);
      gc.setLineWidth(2);
      gc.drawRectangle(x-w/2,   y-h/2,    w,    h);
      
      FontData data = display.getSystemFont().getFontData()[0];
      Font font = new Font(display, data.getName(), 10, SWT.NORMAL);
      gc.setFont(font);
      
      String[] ids = text.split(",");
      if ((ids.length<2) || !"h".equalsIgnoreCase(ids[1])) {
         String label = ids[0];
         Variable var = getVariable();
         if (var instanceof LongVariable) {
            label = label + ", " + getVariable().getValueAsString();
         }
         gc.drawText(label, (x+w-40), (y-25));
      }
      
      font.dispose();
   }
     
}