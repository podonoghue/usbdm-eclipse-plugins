package net.sourceforge.usbdm.deviceEditor.graphicModel;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import net.sourceforge.usbdm.deviceEditor.graphicModel.Graphic.Height;
import net.sourceforge.usbdm.deviceEditor.graphicModel.Graphic.Orientation;
import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.information.Variable.Units;

public class GraphicsDialogue {

   static class HoverShell {
      private Shell hoverShell;
      private Label label;
      
      public HoverShell(Shell shell) {
         hoverShell = new Shell(shell, SWT.ON_TOP | SWT.TOOL);
         hoverShell.setLayout(new FillLayout());
         label = new Label(hoverShell, SWT.NONE);
         label.setText("Unset");
      }
      
      public void show(String tooltip) {
         label.setText("XXXXX");
         hoverShell.pack();
         hoverShell.setVisible(true);
         hoverShell.open();
      }
      
      public void hide() {
         hoverShell.setVisible(false);
      }
   }
   
   HoverShell hoverShell = null;
   
   private ClockSelectionFigure fFigure;
   private Shell fParent;
   private String fTitle;

   public GraphicsDialogue(Shell parent, String title, ClockSelectionFigure figure) {
      fParent = parent;
      fFigure = figure;
      fTitle  = title;
   }

   public void paint(Display display, GC gc) {
      for (Graphic obj:fFigure.objects) {
         obj.draw(display, gc);
      }
   }

   private Graphic findTopObject(int x, int y) {
      for (int index=fFigure.objects.length; --index>=0; ) {
         Graphic obj = fFigure.objects[index];
         if (obj.contains(x,y)) {
            return obj;
         }
         if (index == 0) {
            System.err.println(obj);
         }
      }
      return null;
   }
   
   private void mouseDown(int x, int y) {
      Graphic obj = findTopObject(x, y);
      if (obj != null) {
         System.err.println("Clicked " + obj.toString()+"@("+x+", "+y+")");
      }
   }

   private void mouseMove(int x, int y) {
      hoverShell.hide();
//      System.err.println("move("+x+", "+y+")");
   }

   private void mouseHover(int x, int y) {
      Graphic obj = findTopObject(x, y);
      if (obj != null) {
         System.err.println("Hovered over " + obj.toString());
         if (obj instanceof GraphicBaseVariable) {
            Variable var = ((GraphicBaseVariable) obj).getVariable();
            hoverShell.show(var.getToolTip());
         }
      }
   }

   static ClockSelectionFigure createTestFigure() throws Exception {
      ClockSelectionFigure figure = new ClockSelectionFigure();

      LongVariable fastIrcVar  = new LongVariable(null, "/XX/fastIrc");
      fastIrcVar.setUnits(Units.Hz);
      fastIrcVar.setValue(1100000);
      fastIrcVar.setToolTip("help for fastIrc");
      GraphicBaseVariable fastIrc     = (GraphicBaseVariable) figure.add(new GraphicVariable(20,  25, 110, Height.large, "FAST IRC", fastIrcVar));
      
      LongVariable fcrdivVar  = new LongVariable(null, "/XX/fcrdiv");
      fcrdivVar.setToolTip("help for fcrdivVar");
      fcrdivVar.setUnits(Units.Hz);
      fcrdivVar.setValue(1000);
      GraphicBaseVariable fcrdiv = (GraphicBaseVariable) figure.add(new GraphicVariable(210, 35,  60, Height.small, "FCRDIV IRC", fcrdivVar));
      figure.add(GraphicConnector.create(fastIrc, 0, fcrdiv, 0, null));

      Variable slowIrcVar  = new LongVariable(null, "/XX/slowIrc");
      slowIrcVar.setToolTip("help for slowIrcVar");
      GraphicBaseVariable slowIrc     = (GraphicBaseVariable) figure.add(new GraphicVariable(20, 75, 110, Height.large, "SLOW IRC",  slowIrcVar));

      Variable frdivVar  = new LongVariable(null, "/XX/frdiv");
      frdivVar.setToolTip("help for frdivVar");
      GraphicBaseVariable frdiv       = (GraphicBaseVariable) figure.add(new GraphicVariable(20, 150, 90, Height.large, "FRDIV",  frdivVar));

      ChoiceVariable ircsVar  = new ChoiceVariable(null, "/XX/ircs");
      GraphicBaseVariable ircs        = (GraphicBaseVariable) figure.add(new GraphicMuxVariable(350, 25, 3, Orientation.normal, "IRCS", ircsVar));
      figure.add(GraphicConnector.create(fcrdiv,  0, ircs, 0, null));
      figure.add(GraphicConnector.create(slowIrc, 0, ircs, 1, null));
      figure.add(GraphicConnector.create(frdiv,   0, ircs, 2, null));

      GraphicBaseVariable ircs_node   = (GraphicBaseVariable) figure.add(new GraphicNode(400, 60, "IRCS", ircsVar));
      figure.add(GraphicConnector.create(ircs, 0, ircs_node, 0, null));

      fastIrc.setSelected(true);
      fcrdiv.setSelected(true);
      ircs.setSelected(true);
      ircs_node.setSelected(true);
      return figure;
   }

   public void open() {

      Display display = fParent.getDisplay();

      Shell shell = new Shell(fParent);
      shell.setSize(1000,800);
      shell.setText(fTitle);
      shell.setLayout(new FillLayout());

      final ScrolledComposite scrollComposite = new ScrolledComposite(shell, SWT.V_SCROLL | SWT.H_SCROLL |SWT.BORDER);
      final Canvas canvas = new Canvas(scrollComposite, SWT.NONE);
      canvas.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
      canvas.setSize(1000, 1000);
      scrollComposite.setContent(canvas);

      canvas.setBackground(display.getSystemColor(SWT.COLOR_WHITE));

      hoverShell = new HoverShell(fParent);
      
      canvas.addListener(SWT.Paint, evt -> {
         paint(display, evt.gc);
      });

      canvas.addListener(SWT.MouseDown, evt -> {
         mouseDown(evt.x, evt.y);
      });

      canvas.addListener(SWT.MouseHover, evt -> {
         mouseHover(evt.x, evt.y);
      });

      canvas.addListener(SWT.MouseMove, evt -> {
         mouseMove(evt.x, evt.y);
      });

      shell.open();
      while (!shell.isDisposed()) {
         if (!display.readAndDispatch())
            display.sleep();
      }
   }

   public static void main(String[] args) throws Exception {
      Display display = new Display();

      Shell shell = new Shell(display);
      shell.setText("Graphic Editor");
      shell.setLayout(new FillLayout());
      shell.setSize(800, 200);
      
      GraphicsDialogue gd = new GraphicsDialogue(shell, "GUI", createTestFigure());
      gd.open();
      
      display.dispose();
   }

}
