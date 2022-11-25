package net.sourceforge.usbdm.deviceEditor.graphicModel;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Display;

public abstract class Graphic {
   
   public static enum Height        {small, large};
   public static enum ShowValue     {quiet};
   public static enum Orientation   {normal, rot90, rot180, rot270, mirror, rot90mirror, rot180mirror, rot270mirror, };
   public static enum Type          {variableBox, box, choice, mux, hmux, connector, node, label};
   
   static class Point {
      int x, y;
      
      Point(int x, int y) {
         this.x = x;
         this.y = y;
      }
   };

   final static int vScale             = 20;
   final static int hScale             = 10;
   
   final static int backgroundColor    = SWT.COLOR_WHITE;
   final static int lineColor          = SWT.COLOR_BLACK;
   final static int selectedLineColor  = SWT.COLOR_BLUE;
   final static int fillColor          = SWT.COLOR_WHITE;
   final static int selectedFillColor  = SWT.COLOR_GRAY;
   final static int disabledLineColor  = SWT.COLOR_GRAY;
   
   int x,y,w,h;

   Orientation orientation;

   boolean fSelected = false;
   String text;
   
   public Graphic(int x, int y, int w, int h, String text, Orientation orientation) {
      this.x = x;
      this.y = y;
      this.orientation = orientation;
      Point p = new Point(w, h);
      p = rotate(p);
      this.w = p.x;
      this.h = p.y;
      this.text = text;
   }
   
   public Graphic(int x, int y, int w, int h, String text) {
      this.x = x;
      this.y = y;
      this.orientation = Orientation.normal;
      Point p = new Point(w, h);
      p = rotate(p);
      this.w = p.x;
      this.h = p.y;
      this.text = text;
   }
   
   void draw(Display display,GC gc) {
      gc.setLineStyle(SWT.LINE_SOLID);
      gc.setLineWidth(1);

      if (fSelected) {
         gc.setBackground(display.getSystemColor(selectedFillColor));
         gc.setForeground(display.getSystemColor(selectedLineColor));
      }
      else {
         gc.setBackground(display.getSystemColor(fillColor));
         gc.setForeground(display.getSystemColor(lineColor));
      }
   }
   
   public boolean isSelected() {
      return fSelected;
   }
   
   public void setSelected(boolean selected) {
      fSelected = selected;
   }

   protected boolean contains(int xx, int yy) {
      return (xx>=(x-w/2)) && (xx<=(x+w/2)) && (yy>=(y-h/2)) && (yy<=(y+h/2));
   }

   @Override
   public String toString() {
      return text;
   }
   
   /**
    * Convert a point from global to graphic relative
    * 
    * @param point
    * 
    * @return
    */
   Point makeRelativeToOrigin(Point point) {
      return new Point(point.x-x, point.y-y);
   }
   
   /**
    * Convert a point from graphic relative to global
    * 
    * @param point
    * 
    * @return
    */
   Point makeAbsolute(Point point) {
      return new Point(point.x+x, point.y+y);
   }
   
   /**
    * Map a point based on orientation
    * 
    * @param x    X coordinate (relative)
    * @param y    Y coordinate (relative)
    * 
    * @return Mapped point (absolute)
    */
   Point rotate(int x, int y) {
      return rotate(new Point(x,y));
   }

   /**
    * Map a point based on orientation
    * 
    * @param point         Point relative to graphic origin
    * 
    * @return Mapped point (absolute)
    */
   Point rotate(Point point) {
      
      if (point == null) {
         System.err.println("Opps");
      }
      Point rotatedPoint;
      
      switch (orientation) {
      default:
      case normal:         rotatedPoint = new Point( point.x,  point.y); break;
      case mirror:         rotatedPoint = new Point( point.x, -point.y); break;
      case rot90:          rotatedPoint = new Point(-point.y,  point.x); break;
      case rot90mirror:    rotatedPoint = new Point( point.y,  point.x); break;
      case rot180:         rotatedPoint = new Point(-point.x, -point.y); break;
      case rot180mirror:   rotatedPoint = new Point(-point.x,  point.y); break;
      case rot270:         rotatedPoint = new Point( point.y, -point.x); break;
      case rot270mirror:   rotatedPoint = new Point(-point.y, -point.x); break;
      }
      return rotatedPoint;
   }
   
   /**
    * Map a point based on orientation
    * 
    * @param point         Point relative to graphic origin
    * 
    * @return Mapped point (absolute)
    */
   Point map(Point point) {
      Point rotatedPoint = rotate(point);
      return makeAbsolute(rotatedPoint);
   }
   
   /**
    * Map a point based on orientation
    * 
    * @param x    X coordinate (relative)
    * @param y    Y coordinate (relative)
    * 
    * @return Mapped point (absolute)
    */
   Point map(int x, int y) {
      return map(new Point(x,y));
   }

   void drawBoundary(GC gc) {
      gc.drawRectangle(x-w/2, y-h/2, w, h);
   }
   
   /**
    * Draw 4-line filled polygon
    * 
    * @param gc      Graphic context
    * @param points  Array of points (absolute)
    */
   void fillPoly(GC gc, Point[] points) {
      int values[] = new int[2*points.length];
      for (int index=0; index<points.length; index++) {
         values[2*index]   = points[index].x;
         values[2*index+1] = points[index].y;
      }
      gc.fillPolygon(values);
   }
   
   /**
    * Draw 4-line outline polygon
    * 
    * @param gc      Graphic context
    * @param points  Array of points (absolute)
    */
   void drawPoly(GC gc, Point[] points) {
      int values[] = new int[2*points.length];
      for (int index=0; index<points.length; index++) {
         values[2*index]   = points[index].x;
         values[2*index+1] = points[index].y;
      }
      gc.drawPolygon(values);
   }
   
   /**
    * Draw filled rectangle
    * 
    * @param gc
    * @param x
    * @param y
    * @param w
    * @param h
    */
   void fillRectangle(GC gc, int x, int y, int w, int h) {
      Point p1  = map(x,y);
      Point dim = rotate(w,h);
      gc.fillRectangle(p1.x, p1.y, dim.x, dim.y);
   }

   int currentX, currentY;
   Direction direction;
   
   
   enum Direction {up,down,left,right};
   
   /**
    * Draw rectangle outline
    * 
    * @param gc
    * @param x
    * @param y
    * @param w
    * @param h
    */
   void drawRectangle(GC gc, int x, int y, int w, int h) {
      Point p1  = map(x,y);
      Point dim = rotate(w,h);
      gc.drawRectangle(p1.x, p1.y, dim.x, dim.y);
   }

   /**
    * Move drawing point
    * 
    * @param x X coord (absolute)
    * @param y Y coord (absolute)
    */
   void moveTo(int x, int y) {
      currentX = x;
      currentY = y;
//      System.err.println("x="+x+", y="+y);
   }

   /**
    * Draw a line from current drawing point to X,Y
    * Updates current drawing point
    * 
    * @param gc
    * @param x X coord (absolute)
    * @param y Y coord (absolute)
    */
   void lineTo(GC gc, int x, int y) {
      gc.drawLine(currentX, currentY, x, y);
      if (y > currentY) {
         direction = Direction.down;
      }
      else if (y < currentY){
         direction = Direction.up;
      }
      if (x > currentX) {
         direction = Direction.right;
      }
      else if (x < currentX) {
         direction = Direction.left;
      }
      currentX = x;
      currentY = y;
      
//      System.err.println("x="+x+", y="+y);
   }
   
   void drawArrow(Display display, GC gc) {
      Point p = new Point(currentX, currentY);
      Point points[];
      switch (direction) {
         default:
         case up:
            points = new Point[]{
                  new Point(p.x-5,p.y+5),
                  new Point(p.x,  p.y),
                  new Point(p.x+5,p.y+5),
            };
            break;
         case down:
            points = new Point[]{
                  new Point(p.x-5,p.y-5),
                  new Point(p.x,  p.y),
                  new Point(p.x+5,p.y-5),
            };
            break;
         case left:
            points = new Point[]{
                  new Point(p.x+5,p.y+5),
                  new Point(p.x,  p.y),
                  new Point(p.x+5,p.y-5),
            };
            break;
         case right:
            points = new Point[]{
                  new Point(p.x-5,p.y+5),
                  new Point(p.x,p.y),
                  new Point(p.x-5,p.y-5),
            };
            break;
      }
      fillPoly(gc, points);
   }
   

}
