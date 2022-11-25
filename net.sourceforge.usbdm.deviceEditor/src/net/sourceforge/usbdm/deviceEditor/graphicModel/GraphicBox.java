package net.sourceforge.usbdm.deviceEditor.graphicModel;

import java.awt.Polygon;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Display;

public class GraphicBox extends Graphic {
   
   private int[] pointsX;
   private int[] pointsY;
   private int[] pointsXY;
   private java.awt.Polygon polygon;
   
   public GraphicBox(int x, int y, int w, int h, String text) {
      super(x, y, w, h, text);
   }

   public GraphicBox(int[] pointsXY, String id) {
      super(pointsXY[0], pointsXY[1], 0, 0, id);

      pointsX = new int[pointsXY.length/2];
      pointsY = new int[pointsXY.length/2];
      
      this.pointsXY = pointsXY;
      
      for (int index=0; index<pointsX.length; index++) {
         pointsX[index] = pointsXY[2*index];
         pointsY[index] = pointsXY[2*index+1];
      }
      polygon = new Polygon(pointsX, pointsY, pointsX.length);
   }

   static public GraphicBox create(String id, String params) {

      String paramsArray[] = params.split(",");
      
      int pointsXY[] = new int[paramsArray.length];
      for (int index=0; index<paramsArray.length; index++) {
         pointsXY[index] = Integer.parseInt(paramsArray[index].trim());
      }
      return new GraphicBox(pointsXY,  id);
   }

   @Override
   void draw(Display display, GC gc) {
      super.draw(display, gc);

      gc.setLineWidth(2);
      gc.setLineStyle(SWT.LINE_DASH);
      gc.drawPolygon(pointsXY);
      gc.fillPolygon(pointsXY);
      
//      moveTo(pointsX[0], pointsY[0]);
//      for (int index=0; ++index<pointsX.length; ) {
//         lineTo(gc, pointsX[index], pointsY[index]);
//      }

      FontData data = display.getSystemFont().getFontData()[0];
      Font font = new Font(display, data.getName(), 10, SWT.NORMAL);
      gc.setFont(font);
      Point p = map(5,5);
      gc.drawText(text, p.x, p.y);
      font.dispose();
   }

   @Override
   protected boolean contains(int xx, int yy) {
      return polygon.contains(new java.awt.Point(xx,yy));
   }

}