package net.sourceforge.usbdm.deviceEditor.graphicModel;

import java.awt.Polygon;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

public class GraphicBox extends Graphic {
   
   /** List of x,y coordinates outlining box */
   private int[] path;
   
   private java.awt.Polygon polygon;
   
   public GraphicBox(int x, int y, int w, int h, String id) {
      super(x, y, w, h, id);
   }

   public GraphicBox(int x, int y, String[] path, String id) {
      super(x, y, 0, 0, id);

      int[] pointsX  = new int[path.length+2];
      int[] pointsY  = new int[path.length+2];
      
      this.path = new int[2*(path.length+2)];
      
      int currentX = x;
      int currentY = y;

      /**
       * Create:
       *    pointsX  - Array of X coordinates
       *    pointsY  - Array of Y coordinates
       *    pointsXY - Array of X,Y pairs
       */
      pointsX[0]    = currentX;
      pointsY[0]    = currentY;
      this.path[0]  = currentX;
      this.path[1]  = currentY;
      
      int polyIndex = 1;
      for (int index=0; index<path.length; index++, polyIndex++) {
         if (path[index].charAt(0) == 'x') {
            if (path[index].length()>1) {
               int newCoord = Integer.parseInt(path[index].substring(1));
               currentX += newCoord;
            }
            else {
               currentX = this.path[0];
            }
         }
         else if (path[index].charAt(0) == 'y') {
            if (path[index].length()>1) {
               int newCoord = Integer.parseInt(path[index].substring(1));
               currentY += newCoord;
            }
            else {
               currentY = this.path[1];
            }
         }
         pointsX[polyIndex]    = currentX;
         pointsY[polyIndex]    = currentY;
         this.path[2*polyIndex]      = currentX;
         this.path[2*polyIndex+1]    = currentY;
      }
      // Close polygon
      pointsX[polyIndex]    = x;
      pointsY[polyIndex]    = y;
      this.path[2*polyIndex]      = currentX;
      this.path[2*polyIndex+1]    = currentY;
      polygon = new Polygon(pointsX, pointsY, pointsX.length);
   }

   static public GraphicBox create(int originX, int originY, String id, String params) {

      String paramsArray[] = params.split(",");
      
      int x = originX+Integer.parseInt(paramsArray[0].trim());
      int y = originY+Integer.parseInt(paramsArray[1].trim());

      String path[] = new String[paramsArray.length-2];
      for (int index=2; index<paramsArray.length; index++) {
         path[index-2] = paramsArray[index].trim();
      }
      return new GraphicBox(x, y, path, id);
   }

   @Override
   void draw(Display display, GC gc) {
      super.draw(display, gc);

      gc.setForeground(display.getSystemColor(BOX_COLOR));
      gc.setBackground(display.getSystemColor(DEFAULT_BACKGROUND_COLOR));
      
      gc.setLineWidth(2);
      gc.setLineStyle(SWT.LINE_DASH);
      gc.drawPolygon(path);
      gc.fillPolygon(path);

      FontData data = display.getSystemFont().getFontData()[0];
      Font font = new Font(display, data.getName(), 10, SWT.NORMAL);
      gc.setFont(font);
      
      // Draw title below the middle of 1st segment
      Point p = new Point((path[0]+path[2])/2-20,path[1]);
      gc.drawText(name, p.x, p.y);
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

}