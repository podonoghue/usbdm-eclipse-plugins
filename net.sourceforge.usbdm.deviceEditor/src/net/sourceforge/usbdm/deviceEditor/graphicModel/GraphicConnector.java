package net.sourceforge.usbdm.deviceEditor.graphicModel;

import java.util.Arrays;
import java.util.Hashtable;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Display;

import net.sourceforge.usbdm.deviceEditor.information.Variable;

class GraphicConnector extends GraphicBaseVariable {
      
      Graphic source = null;
      int sIndex;
      Graphic destination = null;
      int dIndex;
      String[] path;
      
      public GraphicConnector(Graphic source, Graphic destination, Variable var, Point start, Point end, String[] path) {
         super(start.x, start.y, end.x-start.x, end.y-start.y, null, var);
         this.source      = source;
         this.destination = destination;
         this.path        = path;
      }

//      public GraphicConnector(Graphic source, Graphic destination, Point start, Point end) {
//         super(start.x, start.y, end.x-start.x, end.y-start.y, null, null);
//         this.source      = source;
//         this.destination = destination;
//         this.path        = null;
//      }

      public static GraphicConnector create(GraphicBaseVariable source, int sIndex, GraphicBaseVariable destination, int dIndex, Variable var, String[] path) {
         return new GraphicConnector(source, destination, var, source.getOutput(sIndex), destination.getInput(dIndex), path);
      }
      
      public static GraphicConnector create(GraphicBaseVariable source, int sIndex, GraphicBaseVariable destination, int dIndex, Variable var) {
         return new GraphicConnector(source, destination, var, source.getOutput(sIndex), destination.getInput(dIndex), null);
      }
      
      public static GraphicConnector create(Hashtable<String, Graphic> graphicTable, String fId, String fParams, Variable var) throws Exception {
         String[] params = fParams.split(",");
         GraphicBaseVariable source       = (GraphicBaseVariable)graphicTable.get(params[0].trim());
         int sIndex                       = Integer.parseInt(params[1].trim());
         GraphicBaseVariable destination  = (GraphicBaseVariable)graphicTable.get(params[2].trim());
         int dIndex                       = Integer.parseInt(params[3].trim());
         String path[] = null;
         if (params.length > 4) {
            path = Arrays.copyOfRange(params, 4, params.length);
         }
         if (var == null) {
            // Default to source
            var = source.getVariable();
         }
         return create(source, sIndex, destination, dIndex, var, path);
      }

      private void drawConnector(Display display, GC gc, int startX, int startY, int endX, int endY) {
         super.draw(display, gc);
         
         moveTo(startX, startY);

         gc.setBackground(display.getSystemColor(lineColor));
         Variable var = getVariable();
         if ((var != null) && !var.isEnabled()) {
            gc.setBackground(display.getSystemColor(disabledLineColor));
            gc.setForeground(display.getSystemColor(disabledLineColor));
         }
         if (path != null) {
            for (int index=0; index<path.length; index++) {
               String element = path[index].trim();
               switch (element.charAt(0)) {
                  default:
                     break;
                  case 'd':
                     gc.fillOval(currentX-3, currentY-3, 7, 7);
                     break;
                  case 'y':
                     int newY = endY;
                     if (element.length()>1) {
                        element = element.substring(1);
                        if (element.indexOf('.')>=0) {
                           Float weight = Float.parseFloat(element);
                           newY = currentY + Math.round(weight*(endY-startY));
                        }
                        else {
                           newY = currentY + Integer.parseInt(element);
                        }
                     }
                     lineTo(gc, currentX, newY);
                     break;
                  case 'x':
                     int newX = endX;
                     if (element.length()>1) {
                        if (element.length()>1) {
                           element = element.substring(1);
                           if (element.indexOf('.')>=0) {
                              Float weight = Float.parseFloat(element);
                              newX = currentX + Math.round(weight*(endX-startX));
                           }
                           else {
                              newX = currentX + Integer.parseInt(element);
                           }
                        }
                     }
                     lineTo(gc, newX, currentY);
                     break;
               }
            }
         }
         boolean complete = (currentX==endX) && (currentY==endY);
         while (!complete) {
            if ((currentX==endX) || (currentY==endY)) {
               // Simple direct connection
               lineTo(gc, endX,             endY);
               complete = true;
            }
            else if (currentX<endX) {
               // Left-to-right
               lineTo(gc, (startX+endX)/2,  startY);
               lineTo(gc, (startX+endX)/2,  endY);
            }
            else {
               // right-to-left
               lineTo(gc,  startX+5,          startY);
               lineTo(gc,  startX+5,          (startY+endY)/2);
               lineTo(gc,  endX-5,            (startY+endY)/2);
               lineTo(gc,  endX-5,            endY);
            }
         }
         drawArrow(display, gc);
      }
      
      @Override
      void draw(Display display, GC gc) {
         super.draw(display, gc);
         fSelected = false;
         if (source != null) {
            fSelected = source.isSelected();
         }
         super.draw(display, gc);
         drawConnector(display, gc, x, y, x+w, y+h);
         gc.setBackground(display.getSystemColor(fillColor));

         // Try to pick up value from source or destination
//         Variable var = null;
//
//         if (source instanceof GraphicVariable) {
//            var = ((GraphicVariable) source).getVariable();
//            if (!(var instanceof LongVariable) && !(var instanceof DoubleVariable)) {
//               var = null;
//            }
//         }
//         if ((var == null) && (destination instanceof GraphicVariable)) {
//            var = ((GraphicVariable) destination).getVariable();
//            if (!(var instanceof LongVariable) && !(var instanceof DoubleVariable)) {
//               var = null;
//            }
//         }
//         if (var != null) {
//            String text = var.getValueAsString();
//            FontData data = display.getSystemFont().getFontData()[0];
//            Font font = new Font(display, data.getName(), 10, SWT.NORMAL);
//            gc.setFont(font);
//            gc.drawText(text, x+5, y-9);
//            font.dispose();
//         }
      }

      @Override
      protected boolean contains(int xx, int yy) {
         return false;
      }

      @Override
      Point getRelativeInput(int index) {
         return new Point(0,0);
      }

      @Override
      Point getRelativeOutput(int index) {
         return new Point(w,h);
      }
   }