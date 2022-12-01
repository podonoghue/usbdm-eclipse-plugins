package net.sourceforge.usbdm.deviceEditor.graphicModel;

import java.util.ArrayList;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.Status;
import net.sourceforge.usbdm.deviceEditor.model.Status.Severity;

public abstract class GraphicBaseVariable extends Graphic {
   
   final private Variable fVar;
   final private boolean  fCanEdit;
   protected Point[] outputs = null;
   protected Point[] inputs  = null;
   
   protected GraphicBaseVariable(int x, int y, int w, int h, String text, boolean canEdit, Variable var) {
      super(x, y, w, h, text);
      this.fVar = var;
      fCanEdit = canEdit;
   }
   
   public Variable getVariable() {
      return fVar;
   }

   enum VariableState {normal, errored, disabled, notKnown};
   
   VariableState checkVariableState(Variable var) {
      if (var == null) {
         return VariableState.notKnown;
      }
      if (var != null) {
         if (!var.isEnabled()) {
            return VariableState.disabled;
         }
         Status s = var.getStatus();
         if ((s != null) && s.greaterThan(Severity.INFO)) {
            return VariableState.errored;
         }
      }
      return VariableState.normal;
   }
   
   @Override
   void draw(Display display, GC gc) {
      super.draw(display, gc);

      switch (checkVariableState(fVar)) {
      case disabled:
         backGroundColor = DEFAULT_FILL_COLOR;
         lineColor       = DEFAULT_DISABLED_LINE_COLOR;
         break;
      case errored:
         backGroundColor = DEFAULT_FILL_COLOR;
         lineColor       = ERROR_COLOR;
         break;
      case normal:
      case notKnown:
         backGroundColor = DEFAULT_FILL_COLOR;
         lineColor       = DEFAULT_LINE_COLOR;
         break;
      }
   }
   
   /**
    * Indicates if the graphic value can be edited
    * 
    * @return
    */
   boolean canEdit() {
      return fCanEdit;
   }
   
   abstract Point getEditPoint();
   
   /**
    * Get position of input
    * 
    * @param index Index of input
    * 
    * @return Position (relative)
    */
   Point getRelativeInput(int index) {
      if (inputs == null) {
         return null;
      }
      if (index>=inputs.length) {
         throw new ArrayIndexOutOfBoundsException("Input "+index+" doesn't exist on " + name);
      }
      return inputs[index];
   }
   
   /**
    * Get position of output
    * 
    * @param index Index of output
    * 
    * @return Position (relative)
    */
   Point getRelativeOutput(int index) {
      if (outputs == null) {
         return null;
      }
      if (index>=outputs.length) {
         throw new ArrayIndexOutOfBoundsException("Output "+index+" doesn't exist on " + name);
      }
      return outputs[index];
   }
   
   /**
    * Get position of input
    * 
    * @param index Index of input
    * 
    * @return Position (absolute)
    */
   Point getInput(int index) {
      return map(getRelativeInput(index));
   }

   /**
    * Get position of output
    * 
    * @param index Index of output
    * 
    * @return Position (absolute)
    */
   Point getOutput(int index) {
      return map(getRelativeOutput(index));
   }
   
   /**
    * Get position of input 0
    * 
    * @return Position (absolute)
    */
   Point getInput()  { return getInput(0); }
   
   /**
    * Get position of output 0
    * 
    * @return Position (absolute)
    */
   Point getOutput() { return getOutput(0); }
   /**
    * Get position of input 0
    * 
    * @return Position (absolute)
    */
   Point getRelativeInput()  { return getRelativeInput(0); }
   
   /**
    * Get position of output 0
    * 
    * @return Position (absolute)
    */
   Point getRelativeOutput() { return getRelativeOutput(0); }
   
   public void addInputsAndOutputs(int startAt, String paramsArray[], int w, int h) {

      if (paramsArray.length>startAt) {
         ArrayList<Point> tOutputs =  new ArrayList<Point>();
         ArrayList<Point> tInputs  =  new ArrayList<Point>();
         
         for (int index=startAt; index<paramsArray.length; index++) {
            String param = paramsArray[index].trim();
            if (param.charAt(0) == 'i') {
               switch(param.charAt(1)) {
               case 'n': tInputs.add(new Point(0,    -h/2)); break;
               case 's': tInputs.add(new Point(0,    +h/2)); break;
               case 'e': tInputs.add(new Point(+w/2, 0   )); break;
               case 'w': tInputs.add(new Point(-w/2, 0   )); break;
               }
            }
            else if (param.charAt(0) == 'o') {
               switch(param.charAt(1)) {
               case 'n': tOutputs.add(new Point(0,    -h/2)); break;
               case 's': tOutputs.add(new Point(0,    +h/2)); break;
               case 'e': tOutputs.add(new Point(+w/2, 0   )); break;
               case 'w': tOutputs.add(new Point(-w/2, 0   )); break;
               }
            }
         }
         if (!tOutputs.isEmpty()) {
            outputs = tOutputs.toArray(new Point[tOutputs.size()]);
         }
         if (!tInputs.isEmpty()) {
            inputs = tInputs.toArray(new Point[tInputs.size()]);
         }
      }
   }
   
}