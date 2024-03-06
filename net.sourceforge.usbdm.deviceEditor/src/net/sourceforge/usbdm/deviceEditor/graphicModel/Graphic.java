package net.sourceforge.usbdm.deviceEditor.graphicModel;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

public abstract class Graphic {
   
   public static final int LOW_PRIORITY  = 100;
   public static final int MID_PRIORITY  = 1000;
   public static final int HIGH_PRIORITY = 2000;
   
   public static enum Height        {small, large};
   public static enum ShowValue     {quiet};
   public static enum Orientation   {normal, rot90, rot180, rot270, mirror, rot90mirror, rot180mirror, rot270mirror, };
   public static enum Type          {variableBox, box, choice, mux, connector, node, junction, label, reference, annotation, group};
   
   public static final int NONE     =0b0000;
   public static final int NONAME   =0b0001;
   public static final int NOVALUE  =0b0010;
   
   final static int vScale             = 20;
   final static int hScale             = 10;
   final static int dotSize            = 7;

   final static int DEFAULT_BACKGROUND_COLOR     = SWT.COLOR_WHITE;
   final static int DEFAULT_LINE_COLOR           = SWT.COLOR_BLACK;
   final static int DEFAULT_SELECTED_LINE_COLOR  = SWT.COLOR_BLUE;
   final static int DEFAULT_FILL_COLOR           = DEFAULT_BACKGROUND_COLOR;
   final static int DEFAULT_SELECTED_FILL_COLOR  = SWT.COLOR_GRAY;
   final static int DEFAULT_DISABLED_LINE_COLOR  = SWT.COLOR_GRAY;
   final static int BOX_COLOR                    = SWT.COLOR_BLUE;
   final static int ERROR_COLOR                  = SWT.COLOR_RED;
   
   int backGroundColor = DEFAULT_BACKGROUND_COLOR;
   int lineColor       = DEFAULT_LINE_COLOR;
   
   int x,y,w,h;

   Orientation orientation;

   boolean fSelected = false;
   String name;
   String id;
   int nameX=0;
   int nameY=0;
   private int style;
   
   public void setName(String name) {
      this.name = name;
   }
   
   public Graphic(int x, int y, int w, int h, String id, Orientation orientation) {
      this.x = x;
      this.y = y;
      this.orientation = orientation;
      Point p = new Point(w, h);
      p = rotate(p);
      this.w = p.x;
      this.h = p.y;
      
      String text[] = id.split(",");
      this.id   = text[0];
      this.name = text[0].split(".")[0];
      
      this.style = NONE;
      
      try {
         for (int index=1; index<text.length; index++) {
            String modifier = text[index];
            if (modifier.startsWith("x")) {
               nameX = Integer.parseInt(modifier.substring(1));
            }
            else if (modifier.startsWith("y")) {
               nameY = Integer.parseInt(modifier.substring(1));
            }
            else {
               // Assume style
               String styles[] = text[1].split("\\|");
               for (String style:styles) {
                  if ("NONE".equalsIgnoreCase(style)) {
                  }
                  else if ("NONAME".equalsIgnoreCase(style)) {
                     this.style |= NONAME;
                  }
                  else if ("NOVALUE".equalsIgnoreCase(style)) {
                     this.style |= NOVALUE;
                  }
               }
            }
         }
      } catch (NumberFormatException e) {
         throw new RuntimeException("Failed to process name modifiers for " + getName());
      }
   }
   
   /**
    * Get style as bit-map
    * 
    * @return
    */
   public int getStyle() {
      return style;
   }
   
   public Graphic(int x, int y, int w, int h, String id) {
      this.x = x;
      this.y = y;
      this.orientation = Orientation.normal;
      Point p = new Point(w, h);
      p = rotate(p);
      this.w = p.x;
      this.h = p.y;
      
      String text[] = id.split(",");
      this.id   = text[0];
      this.name = this.id.split("\\.")[0];
      
      this.style = NONE;
      
      try {
         for (int index=1; index<text.length; index++) {
            String modifier = text[index];
            if (modifier.startsWith("x")) {
               nameX = Integer.parseInt(modifier.substring(1));
            }
            else if (modifier.startsWith("y")) {
               nameY = Integer.parseInt(modifier.substring(1));
            }
            else {
               // Assume style
               String styles[] = text[index].split("\\|");
               for (String style:styles) {
                  if ("NONE".equalsIgnoreCase(style)) {
                  }
                  else if ("NONAME".equalsIgnoreCase(style)) {
                     this.style |= NONAME;
                  }
                  else if ("NOVALUE".equalsIgnoreCase(style)) {
                     this.style |= NOVALUE;
                  }
                  else {
                     System.err.println("Unexpected style '"+style+"'");
                  }
               }
            }
         }
      } catch (NumberFormatException e) {
         throw new RuntimeException("Failed to process name modifiers for " + getName());
      }
   }
   
   void draw(Display display,GC gc) {
      gc.setLineStyle(SWT.LINE_SOLID);
      gc.setLineWidth(1);

      backGroundColor = DEFAULT_BACKGROUND_COLOR;
      lineColor       = DEFAULT_LINE_COLOR;

      if (fSelected) {
         backGroundColor = DEFAULT_SELECTED_FILL_COLOR;
         lineColor       = DEFAULT_SELECTED_LINE_COLOR;
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
      return name;
   }
   
   public String getName() {
      if ((style&NONAME) != 0) {
         return null;
      }
      return name;
   }
   
   public String getId() {
      return id;
   }
   
   /**
    * Convert a point    public String getStyle(int style) {
      StringBuilder sb = new StringBuilder();
      if ((style&NONAME) != 0) {
         if (sb.length()!=0) {
            sb.append("|");
         }
         sb.append("NONAME");
      }
      if ((style&NOVALUE) != 0) {
         if (sb.length()!=0) {
            sb.append("|");
         }
         sb.append("NOVALUE");
      }
      return sb.toString();
   }
from global to graphic relative
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
      final int width  = 1;
      
      gc.setLineWidth(width);
      
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
   
   /**
    *  Draw arrow at current location and with current direction
    */
   void drawArrow(Display display, GC gc) {
      final int width  = 5;
      final int length = 10;
      Point p = new Point(currentX, currentY);
      int points[];
      switch (direction) {
         default:
         case up:
            points = new int[]{p.x-width,p.y+length, p.x,p.y, p.x+width,p.y+length};
            break;
         case down:
            points = new int[]{p.x-width,p.y-length, p.x,p.y, p.x+width,p.y-length};
            break;
         case left:
            points = new int[]{p.x+length,p.y+width, p.x,  p.y, p.x+length,p.y-width};
            break;
         case right:
            points = new int[]{p.x-length,p.y+width, p.x-1,p.y, p.x-length,p.y-width};
            break;
      }
      gc.setBackground(display.getSystemColor(lineColor));
      gc.fillPolygon(points);
   }

   /**
    * Get string representation of style
    * 
    * @param style
    * 
    * @return
    */
   public String getStyle(int style) {
      StringBuilder sb = new StringBuilder();
      if ((style&NONAME) != 0) {
         if (sb.length()!=0) {
            sb.append("|");
         }
         sb.append("NONAME");
      }
      if ((style&NOVALUE) != 0) {
         if (sb.length()!=0) {
            sb.append("|");
         }
         sb.append("NOVALUE");
      }
      return sb.toString();
   }
   
   /**
    * Returns XML describing the graphic
    * 
    * @param resultSb
    */
   final public String report() {
      StringBuilder sb = new StringBuilder();
      sb.append("<graphicItem ");
      reportParams(sb);
      sb.append("/>");
      return sb.toString();
   }

   /**
    * Adds XML parameters describing the graphic to buffer
    * 
    * @param sb
    */
   protected void reportParams(StringBuilder sb) {
      String styleString = getStyle(style);
      if (!styleString.isBlank()) {
         styleString = ","+styleString;
      }
      sb.append(String.format("%-32s", "id=\""+id+styleString+"\" "));
   }

   /**
    * Get priority for drawing this object
    * Higher values => drawn later
    */
   public int getDrawPriority() {
      return MID_PRIORITY;
   }

}
