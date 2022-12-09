package net.sourceforge.usbdm.deviceEditor.graphicModel;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import net.sourceforge.usbdm.deviceEditor.graphicModel.Graphic.Height;
import net.sourceforge.usbdm.deviceEditor.graphicModel.Graphic.Orientation;
import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.information.Variable.Units;
import net.sourceforge.usbdm.deviceEditor.information.VariableWithChoices;

public class GraphicsDialogue {

   static class HoverInfo {
      private Shell shell;
      private Label label;
      
      public HoverInfo(Shell parentShell) {
         shell = new Shell(parentShell, SWT.ON_TOP | SWT.TOOL);
         shell.setLayout(new FillLayout());
         label = new Label(shell, SWT.NONE);
         label.setText("Unset");
         label.setBackground(parentShell.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
      }

      
      public void show(int x, int y, String tooltip) {
         label.setText(tooltip);
         shell.pack();
         Rectangle rh = shell.getBounds();
         Rectangle rd = shell.getDisplay().getBounds();
         if ((x+rh.width+20)<(rd.x+rd.width)) {
            x += 20;
         }
         else {
            x = rd.x+rd.width-rh.width;
         }
         if ((y+rh.height)>(rd.y+rd.height)) {
            y = rd.y+rd.height-rh.height;
         }
         shell.setLocation(x,y);
         shell.setVisible(true);
         shell.open();
      }
      
      public void hide() {
         shell.setVisible(false);
      }
      
      public void dispose() {
         label.dispose();
         shell.dispose();
      }
   }
   
   HoverInfo hoverShell = null;
   
   private ClockSelectionFigure fFigure;
   private Shell fParentShell;
   private String fTitle;

   public GraphicsDialogue(Shell parent, String title, ClockSelectionFigure figure) {
      fParentShell = parent;
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
            System.err.println("Found top = "+obj.getName());
         }
      }
      return null;
   }
   
   private EditorInfo currentEditor = null;
   
   abstract class EditorInfo {
      final Variable var;
      
      public EditorInfo(Variable var) {
         this.var = var;
      }
      
      /**
       * Disposes SWT resources
       */
      abstract public void dispose();
   }
   
   class TextEditor extends EditorInfo {
      
      final Text text;
      
      public TextEditor(Canvas canvas, Variable var, Point p) {
         super(var);

         text = new Text(canvas, SWT.ON_TOP);
         text.setText(var.getValueAsString());
         text.setFocus();
         text.setBackground(canvas.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
         text.setBounds(p.x, p.y, 100, 20);
         text.selectAll();
         text.setVisible(true);
         text.addKeyListener(new KeyListener() {

            @Override
            public void keyReleased(KeyEvent keyEvent) {
               if ((keyEvent.keyCode==SWT.CR) || (keyEvent.keyCode==SWT.KEYPAD_CR)) {
                  String value = text.getText();
                  if ((value != null) && !value.isBlank()) {
                     var.setValue(value.trim());
                  }
                  dispose();
                  currentEditor = null;
               }
               else if (keyEvent.keyCode==SWT.ESC) {
                  dispose();
                  currentEditor = null;
               }
            }

            @Override
            public void keyPressed(KeyEvent keyEvent) {
            }
         });
      }
      
      @Override
      public void dispose() {
         if (text != null) {
            text.setVisible(false);
            if (!text.isDisposed())
            text.dispose();
         }
      }
      
   }
   
   class ComboEditor extends EditorInfo {
      
      final Combo combo;
      
      public ComboEditor(Canvas canvas, VariableWithChoices var, Point p) {
         super(var);

         combo = new Combo(canvas, SWT.ON_TOP|SWT.READ_ONLY|SWT.DROP_DOWN);
         combo.setItems(var.getChoices());
         combo.setText(var.getValueAsString());
         combo.setFocus();
         combo.setBackground(canvas.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
         combo.setBounds(p.x,p.y,100,200);
         combo.pack();
         combo.setVisible(true);
         
//         combo.addListener (SWT.DefaultSelection, e -> System.out.println (e.widget + " - Default Selection"));
//         combo.addListener (SWT.Selection,        e -> System.out.println (e.widget + " - Changed"));
         
         combo.addSelectionListener(new SelectionAdapter() {
            
            @Override
            public void widgetSelected(SelectionEvent arg0) {
               String value = combo.getText();
//               System.err.println("Combo value = " + value);
               if ((value != null) && !value.isBlank()) {
                  VariableWithChoices cVar = var;
                  cVar.setValueByName(value.trim());
               }
               dispose();
               currentEditor = null;
            }
         });
         
         
//         ;(new KeyListener() {
//
//            @Override
//            public void keyReleased(KeyEvent keyEvent) {
//               if ((keyEvent.keyCode==SWT.CR) || (keyEvent.keyCode==SWT.KEYPAD_CR)) {
//                  String value = combo.getText();
//                  System.err.println("Combo value = " + value);
//                  if ((value != null) && !value.isBlank()) {
//                     var.setValue(value.trim());
//                  }
//                  dispose();
//                  currentEditor = null;
//               }
//               else if (keyEvent.keyCode==SWT.ESC) {
//                  dispose();
//                  currentEditor = null;
//               }
//            }
//
//            @Override
//            public void keyPressed(KeyEvent keyEvent) {
//            }
//         });
      }
      
      @Override
      public void dispose() {
         if (combo != null) {
            combo.setVisible(false);
            if (!combo.isDisposed())
            combo.dispose();
         }
      }
      
   }
   
   private void mouseDown(Event evt) {
      if (currentEditor != null) {
         currentEditor.dispose();
         currentEditor = null;
      }
      Graphic obj = findTopObject(evt.x, evt.y);
      if (obj != null) {
         if (obj instanceof GraphicBaseVariable) {
            GraphicBaseVariable graphicVar = (GraphicBaseVariable) obj;
            if (graphicVar.canEdit()) {
               Variable var = graphicVar.getVariable();
               if (var instanceof LongVariable) {
                  Point p = graphicVar.getEditPoint();
                  currentEditor = new TextEditor(canvas, var, p);
                  hoverShell.hide();

               }
               else if (var instanceof VariableWithChoices) {
                  Point p = graphicVar.getEditPoint();
                  currentEditor = new ComboEditor(canvas, (VariableWithChoices) var, p);
                  hoverShell.hide();
               }

            }
         }
         System.err.println("Clicked " + obj.toString()+"@("+evt.x+", "+evt.y+")");
      }
   }

   private void mouseMove(int x, int y) {
      hoverShell.hide();
   }

   private void mouseHover(Event evt) {
      if (currentEditor != null) {
         return;
      }
      Graphic obj = findTopObject(evt.x, evt.y);
      if (obj != null) {
         if (obj instanceof GraphicBaseVariable) {
            Variable var = ((GraphicBaseVariable) obj).getVariable();
            if (var == null) {
               return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(var.getKey());
//            String tooltip = var.getToolTip();
//            if (tooltip != null) {
//               sb.append("\n");
//               sb.append(tooltip.toString());
//            }
            String toolTip = var.getDisplayToolTip();
            if (toolTip != null) {
               sb.append("\n\n");
               sb.append(toolTip);
            }
//            String origin = var.getOrigin();
//            if (origin != null) {
//               sb.append("\nOrigin: ");
//               sb.append(origin);
//            }
//            Status status = var.getStatus();
//            if (status != null) {
//               sb.append("\n");
//               sb.append(status.getText());
//            }
            Point p = canvas.toDisplay(evt.x, evt.y);
            hoverShell.show(p.x, p.y, sb.toString());
         }
      }
   }

   static ClockSelectionFigure createTestFigure() throws Exception {
      ClockSelectionFigure figure = new ClockSelectionFigure();

      LongVariable fastIrcVar  = new LongVariable(null, "/XX/fastIrc");
      fastIrcVar.setUnits(Units.Hz);
      fastIrcVar.setValue(1100000);
      fastIrcVar.setToolTip("help for fastIrc");
      GraphicBaseVariable fastIrc     = (GraphicBaseVariable) figure.add(new GraphicVariable(20,  25, 110, Height.large, "FAST IRC", true, fastIrcVar));
      
      LongVariable fcrdivVar  = new LongVariable(null, "/XX/fcrdiv");
      fcrdivVar.setToolTip("help for fcrdivVar");
      fcrdivVar.setUnits(Units.Hz);
      fcrdivVar.setValue(1000);
      GraphicBaseVariable fcrdiv = (GraphicBaseVariable) figure.add(new GraphicVariable(210, 35,  60, Height.small, "FCRDIV IRC", true, fcrdivVar));
      figure.add(GraphicConnector.create("", fastIrc, 0, fcrdiv, 0, null));

      Variable slowIrcVar  = new LongVariable(null, "/XX/slowIrc");
      slowIrcVar.setToolTip("help for slowIrcVar");
      GraphicBaseVariable slowIrc     = (GraphicBaseVariable) figure.add(new GraphicVariable(20, 75, 110, Height.large, "SLOW IRC", true,  slowIrcVar));

      Variable frdivVar  = new LongVariable(null, "/XX/frdiv");
      frdivVar.setToolTip("help for frdivVar");
      GraphicBaseVariable frdiv       = (GraphicBaseVariable) figure.add(new GraphicVariable(20, 150, 90, Height.large, "FRDIV", true,  frdivVar));

      ChoiceVariable ircsVar  = new ChoiceVariable(null, "/XX/ircs");
      GraphicBaseVariable ircs        = (GraphicBaseVariable) figure.add(new GraphicMuxVariable(350, 25, 3, Orientation.normal, "IRCS", ircsVar));
      figure.add(GraphicConnector.create("", fcrdiv,  0, ircs, 0, null));
      figure.add(GraphicConnector.create("", slowIrc, 0, ircs, 1, null));
      figure.add(GraphicConnector.create("", frdiv,   0, ircs, 2, null));

      GraphicBaseVariable ircs_node   = (GraphicBaseVariable) figure.add(new GraphicNode(400, 60, "IRCS", true, ircsVar));
      figure.add(GraphicConnector.create("", ircs, 0, ircs_node, 0, null));

      fastIrc.setSelected(true);
      fcrdiv.setSelected(true);
      ircs.setSelected(true);
      ircs_node.setSelected(true);
      return figure;
   }

   Canvas canvas;
   
   public Canvas getCanvas() { return canvas; }
   
   public void open() {

      Display display = fParentShell.getDisplay();

      Shell shell = new Shell(fParentShell, SWT.RESIZE|SWT.CLOSE|SWT.MAX);
      shell.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
      shell.setSize(1150,800);
      shell.setText(fTitle);
      shell.setLayout(new FillLayout());

      final ScrolledComposite scrollComposite = new ScrolledComposite(shell, SWT.V_SCROLL|SWT.H_SCROLL|SWT.BORDER);
      scrollComposite.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
      canvas = new Canvas(scrollComposite, SWT.NONE);
      canvas.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
      canvas.setSize(1100, 2000);
      scrollComposite.setContent(canvas);

      canvas.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
      canvas.toDisplay(0, 0);

      hoverShell = new HoverInfo(shell);
      
      canvas.addListener(SWT.Paint, evt -> {
         paint(display, evt.gc);
      });

      canvas.addListener(SWT.MouseDown, evt -> {
         mouseDown(evt);
      });

      canvas.addListener(SWT.MouseHover, evt -> {
         mouseHover(evt);
      });

      canvas.addListener(SWT.MouseMove, evt -> {
         mouseMove(evt.x, evt.y);
      });

      shell.open();
      while (!shell.isDisposed()) {
         if (!display.readAndDispatch())
            display.sleep();
      }
      shell.dispose();
      hoverShell.dispose();
   }

   public static void main(String[] args) throws Exception {
      Display display = new Display();

      Shell shell = new Shell(display);
      shell.setText("Graphic Editor");
      shell.setLayout(new FillLayout());
      shell.setSize(1100, 1400);
      
      GraphicsDialogue gd = new GraphicsDialogue(shell, "GUI", createTestFigure());
      gd.open();
      
      display.dispose();
   }

}
