package net.sourceforge.usbdm.deviceEditor.graphicModel;

import java.util.Arrays;
import java.util.Hashtable;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

import net.sourceforge.usbdm.deviceEditor.information.Variable;

class GraphicConnector extends GraphicBaseVariable {
      
      Graphic source = null;
      int sIndex;
      Graphic destination = null;
      int dIndex;
      String[] path;
      
      public GraphicConnector(String id, Graphic source, Graphic destination, Variable var, Point start, Point end, String[] path) {
         super(start.x, start.y, end.x-start.x, end.y-start.y, id, false, var);
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

      public static GraphicConnector create(String id, GraphicBaseVariable source, int sIndex, GraphicBaseVariable destination, int dIndex, Variable var, String[] path) {
         return new GraphicConnector(id, source, destination, var, source.getOutput(sIndex), destination.getInput(dIndex), path);
      }
      
      public static GraphicConnector create(String id, GraphicBaseVariable source, int sIndex, GraphicBaseVariable destination, int dIndex, Variable var) {
         return new GraphicConnector(id, source, destination, var, source.getOutput(sIndex), destination.getInput(dIndex), null);
      }
      
      public static GraphicConnector create(Hashtable<String, Graphic> graphicTable, String id, String fParams, Variable var) throws Exception {
         String[] params = null;
         GraphicBaseVariable source = null;
         int sIndex;
         GraphicBaseVariable destination = null;
         int dIndex;
         try {
            params = fParams.split(",");
            source = (GraphicBaseVariable)graphicTable.get(params[0].trim());
            sIndex = Integer.parseInt(params[1].trim());
            destination = (GraphicBaseVariable)graphicTable.get(params[2].trim());
            dIndex = Integer.parseInt(params[3].trim());
         } catch (NumberFormatException e) {
            throw new Exception("Failed parse arguments for "+id+", params = "+fParams);
         }
         if (source == null) {
            throw new Exception("Failed to find connector source "+params[0].trim()+" in "+id);
         }
         if (destination == null) {
            throw new Exception("Failed to find connector destination "+params[2].trim()+" in "+id);
         }
         String path[] = null;
         if (params.length > 4) {
            path = Arrays.copyOfRange(params, 4, params.length);
         }
         if (var == null) {
            // Default to source
            var = source.getVariable();
         }
         if (var == null) {
            // Default to destination
            var = destination.getVariable();
         }
         return create(id, source, sIndex, destination, dIndex, var, path);
      }

      private void drawConnector(Display display, GC gc, int startX, int startY, int endX, int endY) {
         super.draw(display, gc);
         
         moveTo(startX, startY);

         switch (checkVariableState(getVariable())) {
         case disabled:
            backGroundColor = DEFAULT_DISABLED_LINE_COLOR;
            lineColor       = DEFAULT_DISABLED_LINE_COLOR;
            break;
         case errored:
            backGroundColor = ERROR_COLOR;
            lineColor       = ERROR_COLOR;
            break;
         case normal:
         case notKnown:
            backGroundColor = DEFAULT_LINE_COLOR;
            lineColor       = DEFAULT_LINE_COLOR;
            break;
         }

         gc.setBackground(display.getSystemColor(lineColor));
         gc.setForeground(display.getSystemColor(lineColor));

         boolean drawingOn = true;
         if (path != null) {
            for (int index=0; index<path.length; index++) {
               String element = path[index].trim();
               switch (element.charAt(0)) {
                  default:
                     break;
                  case 'o': // Drawing on/off
                     if ("on".equalsIgnoreCase(element)) {
                        drawingOn = true;
                     }
                     else if ("off".equalsIgnoreCase(element)) {
                        drawingOn = false;
                     }
                     break;
                  case 'd': // Draw dot at current location
                     gc.fillOval(currentX-dotSize/2, currentY-dotSize/2, dotSize, dotSize);
                     break;
                  case '(': // Move X or Y relative to current location
                     if (element.charAt(1) == 'y') {
                        // Move Y relative to current location
                        int newY = endY + Integer.parseInt(element.substring(2, element.length()-1));
                        if (drawingOn) {
                           lineTo(gc, currentX, newY);
                        }
                        else {
                              moveTo(currentX, newY);
                        }
                     }
                     else if (element.charAt(1) == 'x') {
                        // Move Y relative to current location
                        int newX = endX + Integer.parseInt(element.substring(2, element.length()-1));
                        if (drawingOn) {
                           lineTo(gc, newX, currentY);
                        }
                        else {
                           moveTo(newX, currentY);
                        }
                     }
                     break;
                  case 'y': // Move on Y towards destination Y (fraction may be specified)
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
                     if (drawingOn) {
                        lineTo(gc, currentX, newY);
                     }
                     else {
                        moveTo(currentX, newY);
                     }
                     break;
                  case 'x': // Move on X towards destination X (fraction may be specified)
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
                     if (drawingOn) {
                        lineTo(gc, newX, currentY);
                     }
                     else {
                        moveTo(newX, currentY);
                     }
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
         if (!(destination instanceof GraphicJunction)) {
            drawArrow(display, gc);
         }
      }
      
      @Override
      void draw(Display display, GC gc) {
         
         fSelected = false;
         if (source != null) {
            fSelected = source.isSelected();
         }
         super.draw(display, gc);
         drawConnector(display, gc, x, y, x+w, y+h);
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

      @Override
      Point getEditPoint() {
         return null;
      }
      
      @Override
      public void reportParams(StringBuilder sb) {
         super.reportParams(sb);
         sb.append(String.format("%-20s", "type=\"connector\" "));

         StringBuilder params = new StringBuilder();
         String commaIfNeeded = (path != null)?",":"";
         params.append(String.format(" params=\"%-20s%-20s", source.getId()+","+sIndex+",", destination.getId()+","+dIndex+commaIfNeeded));
         if (path != null) {
            boolean needComma = false;
            for (int index=0; index<path.length; index++) {
               if (needComma) {
                  params.append(",");
               }
               needComma = true;
               params.append(path[index].trim());
            }
         }
         params.append("\" ");
         sb.append(String.format("%-90s", params.toString()));
      }
      
      @Override
      public int getDrawPriority() {
         return LOW_PRIORITY+10;
      }

   }