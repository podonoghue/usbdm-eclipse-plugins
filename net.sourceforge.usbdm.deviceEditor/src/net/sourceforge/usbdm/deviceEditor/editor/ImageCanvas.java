package net.sourceforge.usbdm.deviceEditor.editor;

import java.awt.geom.AffineTransform;
import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.ScrollBar;

import net.sourceforge.usbdm.deviceEditor.model.PackageImageModel;

public class ImageCanvas extends Canvas {

   public class Region {
      int x1; 
      int y1; 
      int x2; 
      int y2;
      final String name;

      public Region(int x1, int y1, int x2, int y2, String name) {
         this.x1     = x1;
         this.y1     = y1;
         this.x2     = x2;
         this.y2     = y2;
         this.name   = name;
      }

      public boolean contains(Point location) {
         boolean rv = (location.x>=x1) && (location.y>=y1) && (location.x<x2) && (location.y<y2); 
//         System.err.println("("+location.x+">="+x1+") && ("+location.y+">="+y1+") && ("+location.x+"<"+x2+") && ("+location.y+"<"+y2+") => "+rv);
         return rv;
      }
   }
   
   ArrayList<Region> fRegions = new ArrayList<Region>();

   private Image              sourceImage; /* original image */
   private AffineTransform    fTransform = new AffineTransform();
   private final boolean      debugMode;

   /**
    * Create canvas
    *  
    * @param parent  
    * @param model
    */
   public ImageCanvas(Composite parent, PackageImageModel model) {
      super(parent, SWT.BORDER|SWT.NO_BACKGROUND|SWT.H_SCROLL|SWT.V_SCROLL);

      debugMode = false;
      
      addControlListener(new ControlAdapter() { /* resize listener. */
         public void controlResized(ControlEvent event) {
            fitCanvas();
            syncScrollBars();
         }
      });

      addPaintListener(new PaintListener() {
         public void paintControl(PaintEvent e) {
            paint(e.gc);
         }
      });

      addMouseListener(new MouseListener() {

         @Override
         public void mouseUp(MouseEvent arg0) {}

         @Override
         public void mouseDown(MouseEvent event) {
            Point location = SWT2Dutil.inverseTransformPoint(fTransform, new Point(event.x, event.y));
            if (!debugMode) {
               String name = null;
               for (Region x:fRegions) {
                  if (x.contains(location)) {
                     name = x.name;
                     break;
                  }
               }
               MessageBox messageBox = new MessageBox(event.display.getActiveShell(), SWT.ICON_INFORMATION);
               messageBox.setText("Mouse click!");
               if (name != null) {
                  messageBox.setMessage("Found "+name+" at position: X= " + location.x + ", Y=" + location.y);
                  messageBox.open();         
               }
            }
            else {
               createRegions(event, location);
            }
         }

         @Override
         public void mouseDoubleClick(MouseEvent arg0) {
         }
      });
      initScrollBars();
   }

   /**
    * Set the model to obtain the image from
    * 
    * @param model
    */
   public void setImage(Image image) {
      if (sourceImage != null) {
         sourceImage.dispose();
      }
      sourceImage = image;
      fitCanvas();
   }
   
   Point firstPoint   = new Point(0, 0);
   Point secondPoint  = new Point(0, 0);
   Boolean firstClick = true;

   /**
    * Create regions based upon selected area
    * 
    * @param event
    * @param location
    */
   void createRegions(MouseEvent event, Point location) {
      if (firstClick) {
         firstClick = false;
         firstPoint = location;
      }
      else {
         firstClick = true;
         MessageBox messageBox = new MessageBox(event.display.getActiveShell(), SWT.ICON_INFORMATION);
         secondPoint = location;
         messageBox.setText("Mouse click!");
         messageBox.setMessage("Position: X= " + location.x + ", Y=" + location.y);
         messageBox.open();         
         int numRegions = (int) Math.round((secondPoint.y-firstPoint.y)/32.0);
//         System.err.println("numRegions =" + numRegions );
         float verticalIncrement = ((float)(secondPoint.y-firstPoint.y))/numRegions;
         for (int index=0; index<numRegions; index++) {
            int x1 = firstPoint.x;
            int y1 = Math.round(firstPoint.y+(index*verticalIncrement));
            int x2 = secondPoint.x;
            int y2 = Math.round(firstPoint.y+((index+1)*verticalIncrement));
            System.err.println(String.format("addRegion(new Region(%4d,%4d,%4d,%4d,\"%s\"));",x1,y1,x2,y2,"" ));
         }
      }
   }
   
   /**
    * Add clickable region to image
    * 
    * @param region
    */
   protected void addRegion(Region region) {
      fRegions.add(region);
   }
   
   @Override
   public void dispose() {
      if ((sourceImage != null) && !sourceImage.isDisposed()) {
         sourceImage.dispose();
      }
      sourceImage = null;
      super.dispose();
   }

   /**
    * Paint function
    * 
    * @param gc
    */
   private void paint(GC gc) {
      Rectangle clientRect = getClientArea(); /* Canvas' painting area */
      if (sourceImage != null) {
         Rectangle imageRect =
               SWT2Dutil.inverseTransformRect(fTransform, clientRect);
         int gap = 2; /* find a better start point to render */
         imageRect.x -= gap; imageRect.y -= gap;
         imageRect.width += 2 * gap; imageRect.height += 2 * gap;

         Rectangle imageBound = sourceImage.getBounds();
         imageRect = imageRect.intersection(imageBound);
         Rectangle destRect = SWT2Dutil.transformRect(fTransform, imageRect);

         Image screenImage = new Image(getDisplay(), clientRect.width, clientRect.height);
         GC newGC = new GC(screenImage);
         newGC.setClipping(clientRect);
         newGC.setAntialias(SWT.ON);
         newGC.drawImage(
               sourceImage,
               imageRect.x,
               imageRect.y,
               imageRect.width,
               imageRect.height,
               destRect.x,
               destRect.y,
               destRect.width,
               destRect.height);
         newGC.dispose();
         gc.drawImage(screenImage, 0, 0);
         screenImage.dispose();
      } else {
         gc.setClipping(clientRect);
         gc.fillRectangle(clientRect);
         initScrollBars();
      }
   }

   /**
    *  Initialise the scroll-bar and register listeners. 
    */
   private void initScrollBars() {
      ScrollBar horizontal = getHorizontalBar();
      horizontal.setEnabled(false);
      horizontal.addSelectionListener(new SelectionAdapter() {
         public void widgetSelected(SelectionEvent event) {
            scrollHorizontally((ScrollBar) event.widget);
         }
      });
      ScrollBar vertical = getVerticalBar();
      vertical.setEnabled(false);
      vertical.addSelectionListener(new SelectionAdapter() {
         public void widgetSelected(SelectionEvent event) {
            scrollVertically((ScrollBar) event.widget);
         }
      });
   }

   /**
    * Scroll horizontally 
    * 
    * @param scrollBar
    */
   private void scrollHorizontally(ScrollBar scrollBar) {
      if (sourceImage == null) {
         return;
      }
      AffineTransform af = fTransform;
      double tx = af.getTranslateX();
      double select = -scrollBar.getSelection();
      af.preConcatenate(AffineTransform.getTranslateInstance(select - tx, 0));
      fTransform = af;
      syncScrollBars();
   }

   /** 
    * Scroll vertically 
    * 
    * @param scrollBar
    */
   private void scrollVertically(ScrollBar scrollBar) {
      if (sourceImage == null) {
         return;
      }
      AffineTransform af = fTransform;
      double ty = af.getTranslateY();
      double select = -scrollBar.getSelection();
      af.preConcatenate(AffineTransform.getTranslateInstance(0, select - ty));
      fTransform = af;
      syncScrollBars();
   }


   /**
    * Fit the image onto the canvas
    */
   public void fitCanvas() {
      if (sourceImage == null) {
         return;
      }
      Rectangle imageBound = sourceImage.getBounds();
      Rectangle destRect = getClientArea();
      double sx = (double) destRect.width / (double) imageBound.width;
      double sy = (double) destRect.height / (double) imageBound.height;
      double s = Math.min(sx, sy);
      double dx = 0.5 * destRect.width;
      double dy = 0.5 * destRect.height;
      centerZoom(dx, dy, s, new AffineTransform());
   }

   /**
    * Perform a zooming operation centred on the given point
    * (dx, dy) and using the given scale factor. 
    * The given AffineTransform instance is preconcatenated.
    * @param dx centre x
    * @param dy centre y
    * @param scale zoom rate
    * @param af original affinetransform
    */
   public void centerZoom(
         double dx,
         double dy,
         double scale,
         AffineTransform af) {
      af.preConcatenate(AffineTransform.getTranslateInstance(-dx, -dy));
      af.preConcatenate(AffineTransform.getScaleInstance(scale, scale));
      af.preConcatenate(AffineTransform.getTranslateInstance(dx, dy));
      fTransform = af;
      syncScrollBars();
   }

   /**
    * Synchronize the scroll-bar with the image. If the transform is out
    * of range, it will correct it. This function considers only following
    * factors :<b> transform, image size, client area</b>.
    */
   public void syncScrollBars() {
      if (sourceImage == null) {
         redraw();
         return;
      }
      AffineTransform af = fTransform;
      double sx = af.getScaleX(), sy = af.getScaleY();
      double tx = af.getTranslateX(), ty = af.getTranslateY();
      if (tx > 0) tx = 0;
      if (ty > 0) ty = 0;

      ScrollBar horizontal = getHorizontalBar();
      horizontal.setIncrement((int) (getClientArea().width / 100));
      horizontal.setPageIncrement(getClientArea().width);
      Rectangle imageBound = sourceImage.getBounds();
      int cw = getClientArea().width, ch = getClientArea().height;
      if (imageBound.width * sx > cw) { /* image is wider than client area */
         horizontal.setMaximum((int) (imageBound.width * sx));
         horizontal.setEnabled(true);
         if (((int) - tx) > horizontal.getMaximum() - cw)
            tx = -horizontal.getMaximum() + cw;
      } else { /* image is narrower than client area */
         horizontal.setEnabled(false);
         tx = (cw - imageBound.width * sx) / 2; //center if too small.
      }
      horizontal.setSelection((int) (-tx));
      horizontal.setThumb((int) (getClientArea().width));

      ScrollBar vertical = getVerticalBar();
      vertical.setIncrement((int) (getClientArea().height / 100));
      vertical.setPageIncrement((int) (getClientArea().height));
      if (imageBound.height * sy > ch) { /* image is higher than client area */
         vertical.setMaximum((int) (imageBound.height * sy));
         vertical.setEnabled(true);
         if (((int) - ty) > vertical.getMaximum() - ch)
            ty = -vertical.getMaximum() + ch;
      } else { /* image is less higher than client area */
         vertical.setEnabled(false);
         ty = (ch - imageBound.height * sy) / 2; //center if too small.
      }
      vertical.setSelection((int) (-ty));
      vertical.setThumb((int) (getClientArea().height));

      /* update transform. */
      af = AffineTransform.getScaleInstance(sx, sy);
      af.preConcatenate(AffineTransform.getTranslateInstance(tx, ty));
      fTransform = af;

      redraw();
   }

}
