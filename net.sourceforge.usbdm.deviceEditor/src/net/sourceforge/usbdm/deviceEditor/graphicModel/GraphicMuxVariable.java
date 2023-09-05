package net.sourceforge.usbdm.deviceEditor.graphicModel;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.information.VariableWithChoices;

public class GraphicMuxVariable extends GraphicBaseVariable {
   static final int slope = 20;
   
   final int height;
   
   int numInputs;

   public GraphicMuxVariable(int x, int y, int w, int h, int numInputs, Orientation orientation, String text, Boolean canEdit, VariableWithChoices variableWithChoices) {
      super(x, y, w, h, text, canEdit, variableWithChoices);
      this.numInputs    = numInputs;
      this.orientation  = orientation;
      height            = (numInputs+1)*vScale;
   }

   public GraphicMuxVariable(int x, int y, int numInputs, Orientation orientation, String text, VariableWithChoices variableWithChoices) {
      super(x, y, slope, (numInputs+1)*vScale, text, true, variableWithChoices);
      this.numInputs    = numInputs;
      this.orientation  = orientation;
      height            = (numInputs+1)*vScale;
   }

   public static GraphicMuxVariable create(int originX, int originY, String fId, String graphicParams, Boolean canEdit, Variable var) {
      
      String params[] = graphicParams.split(",");
      
      int x           = originX+Integer.parseInt(params[0].trim());
      int y           = originY+Integer.parseInt(params[1].trim());
      int numInputs   = Integer.parseInt(params[2].trim());
      
      if (var != null) {
         numInputs = ((VariableWithChoices)var).getChoiceCount();
      }
      
      Orientation orientation = Orientation.normal;
      if (params.length > 3) {
         orientation = Orientation.valueOf(params[3].trim());
      }
      int w;
      int h;
      
      switch(orientation) {
         default:
         case normal:
         case mirror:
         case rot180:
         case rot180mirror:
            w = slope;
            h = (numInputs+1)*vScale;
            break;
         case rot90:
         case rot90mirror:
         case rot270:
         case rot270mirror:
            w = (numInputs+1)*vScale;
            h = slope;
            break;
      }
      return new GraphicMuxVariable(x, y, w, h, numInputs, orientation, fId, canEdit, (VariableWithChoices)var);
   }
   
   @Override
   void draw(Display display, GC gc) {
      super.draw(display, gc);
      
//      drawBoundary(gc);

      gc.setBackground(display.getSystemColor(backGroundColor));
      gc.setForeground(display.getSystemColor(lineColor));
      
      fillPoly(gc, new Point[] {
            map(-slope/2, -height/2),
            map(slope/2,  -height/2+slope),
            map(slope/2,   height/2-slope),
            map(-slope/2,  height/2),
      });
      
      gc.setLineWidth(2);
      drawPoly(gc, new Point[] {
            map(-slope/2, -height/2),
            map(slope/2,  -height/2+slope),
            map(slope/2,   height/2-slope),
            map(-slope/2,  height/2),
      });
      
      gc.setBackground(display.getSystemColor(lineColor));
//      Point dot = map(dotSize/2, 10-dotSize/2);
      Point dot = map(getRelativeInput(0));
      gc.fillOval(dot.x+1-dotSize/2, dot.y-dotSize/2, dotSize, dotSize);
      
      Variable var = getVariable();
      int sel = (int)var.getValueAsLong();
      Point p1 = getInput(sel);
      Point p2 = getOutput();
      gc.drawLine(p1.x, p1.y, p2.x, p2.y);
      FontData data = display.getSystemFont().getFontData()[0];
      Font font = new Font(display, data.getName(), 10, SWT.NORMAL);

      gc.setFont(font);
      gc.setBackground(display.getSystemColor(DEFAULT_BACKGROUND_COLOR));
      Point textPoint;
      switch(orientation) {
      default:
      case normal:         textPoint = new Point( (x+(slope/2)),  (y-(height/2+5))); break;
      case mirror:         textPoint = new Point( (x+(slope/2)),  (y-(height/2+5))); break;
      case rot90:          textPoint = new Point( (x+(height/2)), (y-(slope/2)+10)); break;
      case rot90mirror:    textPoint = new Point( (x+(height/2)), (y-(slope/2)+10)); break;
      case rot180:         textPoint = new Point( (x-(slope/2)-20),  (y-(height/2)-20)); break;
      case rot180mirror:   textPoint = new Point( (x-(slope/2)-20),  (y-(height/2)-20)); break;
      case rot270:         textPoint = new Point( (x+(height/2)), (y-(slope/2)-10)); break;
      case rot270mirror:   textPoint = new Point( (x+(height/2)), (y-(slope/2)-10)); break;
      }
      gc.drawText(name, textPoint.x, textPoint.y);

      font.dispose();
   }
   
   @Override
   Point getRelativeInput(int index) {
      if (index >= numInputs) {
         throw new RuntimeException("No such input (" + index + ") on "+name);
      }
      return new Point(-slope/2-1, ((1-numInputs+2*index)*vScale)/2);
   }

   @Override
   Point getRelativeOutput(int index) {
      if (index > 0) {
         throw new RuntimeException("No such output " + index + " on "+name);
      }
      return new Point((slope/2), 0);
   }

   @Override
   Point getEditPoint() {
      return new Point(x,y);
   }
   
   @Override
   public void reportParams(StringBuilder sb) {
      super.reportParams(sb);
      sb.append(String.format("%-20s", "type=\"mux\" "));

      StringBuilder params = new StringBuilder();
      String orgString = "";
      if (orientation != Orientation.normal) {
         orgString = ", "+orientation.name();
      }
      params.append(String.format(" params=\"%4d,%4d,%4d%s\" ", x, y, numInputs, orgString));
      sb.append(String.format("%-60s", params.toString()));
   }

}