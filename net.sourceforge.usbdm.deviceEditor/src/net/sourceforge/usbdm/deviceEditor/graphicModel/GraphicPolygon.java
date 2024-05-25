package net.sourceforge.usbdm.deviceEditor.graphicModel;

import java.awt.Polygon;
import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

import net.sourceforge.usbdm.deviceEditor.information.Variable;

public class GraphicPolygon extends GraphicBaseVariable {
   
   /** List of x,y coordinates outlining polygon */
   private int[] path;
   
   private java.awt.Polygon polygon;
   
   public GraphicPolygon(final int originX, final int originY, String[] params, String id, Boolean canEdit, Variable var) throws Exception {
      super(originX,originY,0,0, id, canEdit, var);

      ArrayList<Integer> x_coords    =  new ArrayList<Integer>();
      ArrayList<Integer> y_coords    =  new ArrayList<Integer>();
      ArrayList<Point>  inputsList   =  new ArrayList<Point>();
      ArrayList<Point>  outputsList  =  new ArrayList<Point>();
      
      int currentX = originX;
      int currentY = originY;
      
      int maxX = 0;
      int maxY = 0;
      
      // Example list y+10,i,x+20,i,+20:+30
      for (int index=0; index<params.length; index++) {
         String current = params[index].strip();
         String[] pair = current.split(":");
         if (pair.length>1) {
            if (pair[0].startsWith("+") || pair[0].startsWith("-")) {
               // X relative movement
               currentX = getMovement(originX, currentX, "x"+pair[0]);
            }
            else {
               // X absolute movement
               currentX = getMovement(originX, currentX, "x="+pair[0]);
            }
            if (pair[1].startsWith("+") || pair[1].startsWith("-")) {
               // Y relative movement
               currentY = getMovement(originY, currentY, "y"+pair[1]);
            }
            else {
               // Y absolute movement
               currentY = getMovement(originY, currentY, "y="+pair[1]);
            }
            x_coords.add(currentX);
            y_coords.add(currentY);
            continue;
         }
         if (current.startsWith("x")) {
            // X movement
            currentX = getMovement(originX, currentX, current);
         }
         else if (current.startsWith("y")) {
            // Y movement
            currentY = getMovement(originY, currentY, current);
         }
         else if (current.startsWith("i")) {
            // Input at current location
            inputsList.add(new Point(currentX,currentY));
            continue;
         }
         else if (current.startsWith("o")) {
            // Output at current location
            outputsList.add(new Point(currentX,currentY));
            continue;
         }
         else {
            throw new Exception("Unexpected param in list '"+params+"'");
         }
         x_coords.add(currentX);
         y_coords.add(currentY);
      }
      
      /**
       * Create:
       *    pointsX  - Array of X coordinates
       *    pointsY  - Array of Y coordinates
       *    pointsXY - Array of X,Y pairs (closed path)
       */
      int[] pointsX  = new int[x_coords.size()];
      int[] pointsY  = new int[y_coords.size()];
      
      int[] pointsXY = new int[2*x_coords.size()];
      
      for (int index=0; index<x_coords.size(); index++) {
         pointsX[index] = x_coords.get(index);
         pointsY[index] = y_coords.get(index);
         
         pointsXY[2*index]   = pointsX[index];
         pointsXY[2*index+1] = pointsY[index];
         
         maxX = Math.max(pointsX[index], maxX);
         maxY = Math.max(pointsY[index], maxY);
      }
//      // Close polygon
//      pointsXY[2*index]      = pointsXY[0];
//      pointsXY[2*index+1]    = pointsXY[1];
      
      polygon = new Polygon(pointsX, pointsY, pointsX.length);
      path    = pointsXY;

      x = pointsX[0];
      y = pointsY[0];
      w = maxX - x;
      h = maxY - y;
      
      if (!inputsList.isEmpty()) {
         inputs = inputsList.toArray(new Point[inputsList.size()]);
         for (int index=0; index<inputs.length; index++) {
            inputs[index] = new Point(inputs[index].x-x, inputs[index].y-y);
         }
      }
      if (!outputsList.isEmpty()) {
         outputs = outputsList.toArray(new Point[outputsList.size()]);
         for (int index=0; index<outputs.length; index++) {
            outputs[index] = new Point(outputs[index].x-x, outputs[index].y-y);
         }
      }
   }

   static int getMovement(int origin, int current, String movement) {

      int amount = 0;
      switch(movement.charAt(1)) {
      case '=' :
         amount = origin+Integer.parseInt(movement.substring(2));
         break;
      case '+' :
         amount = current + Integer.parseInt(movement.substring(2));
         break;
      case '-' :
         amount = current - Integer.parseInt(movement.substring(2));
         break;
      }
      return amount;
   }
   
   static public GraphicPolygon create(int originX, int originY, String id, String params, Boolean canEdit, Variable var) throws Exception {

      String paramsArray[] = params.split(",");

      return new GraphicPolygon(originX, originY, paramsArray, id, canEdit, var);
   }

   @Override
   void draw(Display display, GC gc) {
      super.draw(display, gc);

      gc.setBackground(display.getSystemColor(backGroundColor));
      gc.setForeground(display.getSystemColor(lineColor));

      gc.setLineWidth(4);
      gc.drawPolygon(path);
      gc.fillPolygon(path);

      FontData data = display.getSystemFont().getFontData()[0];
      Font font = new Font(display, data.getName(), 10, SWT.NORMAL);
      gc.setFont(font);
      
      // Draw title about the middle
      Point p = new Point(x+nameX,y+nameY);
      
      if ((this.getStyle()&NONAME) == 0) {
         gc.drawText(name,                             p.x, p.y);
      }
      Variable var = getVariable();
      if ((var != null) && ((this.getStyle()&NOVALUE) == 0)) {
         gc.drawText(getVariable().getValueAsBriefString(), p.x, p.y+20);
      }
      font.dispose();
   }

   @Override
   protected boolean contains(int xx, int yy) {
      return polygon.contains(new java.awt.Point(xx,yy));
   }

   @Override
   public void reportParams(StringBuilder sb) {
      super.reportParams(sb);
      sb.append(String.format("%-35s", "var=\"\" "));
      sb.append(String.format("%-20s", "type=\"box\" "));

      StringBuilder params = new StringBuilder();
      
      params.append(String.format(" params=\"%4d,%4d", x, y));
      int currentX = path[0];
      int currentY = path[1];
      for (int index=2; index<path.length-2; index+=2) {
         // Each pair is an (X,Y)
         if (path[index] != currentX) {
            // Change in x
            params.append(String.format(",%6s", "x"+path[index]));
            currentX = path[index];
         }
         if (path[index+1] != currentY) {
            // Change in y
            params.append(String.format(",%6s", "y"+path[index+1]));
            currentY = path[index+1];
         }
      }
      params.append("\" ");
      sb.append(String.format("%-60s", params.toString()));
   }

   @Override
   public int getDrawPriority() {
      return LOW_PRIORITY;
   }

   @Override
   Point getEditPoint() {
      return new Point(x+nameX,y+nameY+20);
   }

}