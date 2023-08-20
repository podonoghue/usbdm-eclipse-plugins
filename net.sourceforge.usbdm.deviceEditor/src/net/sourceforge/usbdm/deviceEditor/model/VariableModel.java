package net.sourceforge.usbdm.deviceEditor.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.widgets.Tree;

import net.sourceforge.usbdm.deviceEditor.editor.CellEditorProvider;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.Status.Severity;
import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;

/**
 * Model for a variable maintained by a provider
 */
public abstract class VariableModel extends EditableModel implements IModelChangeListener, CellEditorProvider {

   protected Variable fVariable;
   
   /**
    * Constructor - Create model from variable
    * 
    * @param parent     Parent model
    * @param variable   Variable being modelled
    * 
    * @note Added as child of parent if not null
    */
   public VariableModel(BaseModel parent, Variable variable) {
      super(parent, "");
      fVariable = variable;
      fVariable.addListener(this);
   }

   @Override
   /**
    * Get name with modified indication (* as suffix)
    * 
    * @return Name
    */
   public String getName() {
      String name = super.getName();
      if ((name == null) || name.isBlank()) {
         name = fVariable.getName();
      }
      boolean modified = !fVariable.isDerived() && !fVariable.isDefault();
      return name + (modified?" *":"");
   }

   /**
    * @return the Variable associated with this model
    */
   public Variable getVariable() {
      return fVariable;
   }

   @Override
   public String getValueAsString() {
      return fVariable.getValueAsString();
   }

   @Override
   public void setValueAsString(String value) {
      fVariable.setValue(value);
   }

   @Override
   protected void removeMyListeners() {
   }
   
   @Override
   public abstract CellEditor createCellEditor(Tree tree);
   
   /**
    * Check if the string is valid as a value for this Variable
    * 
    * @param value Value as String
    * 
    * @return Description of error or null if valid
    */
   public String isValid(String value) {
      return fVariable.isValid(value);
   }

   /**
    * Used to validate initial text entry in dialogues<br>
    * Allows entry of illegal strings while editing even though current result is invalid
    * 
    * @param character Character to validate
    * 
    * @return Error message or null if valid
    */
   public String isValidKey(char character) {
      return fVariable.isValidKey(character);
   }

   @Override
   public void modelElementChanged(ObservableModel observableModel) {
      updateAncestors();
   }

   @Override
   public void modelStructureChanged(ObservableModel observableModel) {
      StructuredViewer viewer = getViewer();
      if (viewer != null) {
         viewer.refresh(observableModel);
      }
   }

   @Override
   public boolean isEnabled() {
      return fVariable.isEnabled() &&
            (!(fParent instanceof BooleanVariableModel) || ((BooleanVariableModel)fParent).getValueAsBoolean());
   }

   @Override
   public boolean canEdit() {
      return super.canEdit() && isEnabled() && !fVariable.isLocked();
   }

   @Override
   public String getSimpleDescription() {
      return fVariable.getDescription();
   }

   @Override
   public String getToolTip() {
      return fVariable.getDisplayToolTip();
   }

   @Override
   protected Status getPropagatedStatus() {
      Status status = getStatus();
      if (status == null) {
         return null;
      }
      if (status.getSeverity().greaterThan(fVariable.getErrorPropagate())) {
         return status;
      }
      return null;
   }
   
   @Override
   Status getStatus() {
      Status rv =  super.getStatus();
      if ((rv != null) && rv.greaterThan(Severity.INFO)) {
         return rv;
      }
      return fVariable.getStatus();
   }

   @Override
   public void elementStatusChanged(ObservableModel observableModel) {
      updateAncestors();
   }
   
   @Override
   public VariableModel clone(BaseModel parentModel, VariableProvider provider, int index) throws CloneNotSupportedException  {
      VariableModel model = (VariableModel) super.clone(parentModel, provider, index);
      // Clone the referenced variable
      model.fVariable = fVariable.clone(provider, index);
      model.fVariable.addListener(model);
      return model;
   }

   @Override
   public boolean isHidden() {
      return super.isHidden() || ((fVariable != null) && fVariable.isHidden());
   }
   
   /**
    * Expand a list of names e.g.
    *    <li>A1-3;BB5-7    => A1,A2,A3,BB5,BB6,BB7 and then split on commas
    *    <li>PT(A-D)(0-7); => PTA0,PTA1,PTA2...PTD5,PTD6,PTD7<br>
    * After expansion %i is replace with the index of the name in the list<br>
    * 
    * List must contain at least 1 semicolon to trigger expansion
    * 
    * @param bitList List to expand
    * 
    * @return Expanded list as array
    */
   public static String[] expandNameList(String bitList) {

      if (!bitList.contains(";")) {
         // Simple list separated by commas
         return bitList.split(",");
      }

      // Expand bit list format

      String numberRange = "\\((\\d+)-(\\d+)\\)";           // 2 groups
      String letterRange = "\\(([a-z|A-Z])-([a-z|A-Z])\\)"; // 2 groups
      String[] patterns = bitList.split(";");
      //                            G1   G2    G3,4            G5,6       G7
      Pattern p = Pattern.compile("^(.*?)("+numberRange+"|"+letterRange+")(.*)$");
      StringBuilder resultSb = new StringBuilder();

      boolean moreToDo;
      do {
         resultSb = new StringBuilder();
         moreToDo = false;
         for (String pattern:patterns) {
            StringBuilder sb;
            Matcher m = p.matcher(pattern);
            sb = new StringBuilder();
            if (m.matches()) {
               moreToDo = true;
               if ((m.group(3) != null) && (m.group(4) != null)) {
                  // Number range
                  int start = Integer.parseInt(m.group(3));
                  int end   = Integer.parseInt(m.group(4));
                  if (start < end) {
                     for (int bitNum=start; bitNum<=end; bitNum++) {
                        if (bitNum != start) {
                           sb.append(';');
                        }
                        sb.append(m.group(1)+Integer.toString(bitNum)+m.group(7));
                     }
                  }
                  else {
                     for (int bitNum=end; bitNum>=start; bitNum--) {
                        if (bitNum != end) {
                           sb.append(';');
                        }
                        sb.append(m.group(1)+Integer.toString(bitNum)+m.group(7));
                     }
                  }
               }
               else if ((m.group(5) != null) && (m.group(6) != null)) {
                  // Letter range
                  char start = m.group(5).charAt(0);
                  char end   = m.group(6).charAt(0);
                  if (start < end) {
                     for (char bitNum=start; bitNum<=end; bitNum++) {
                        if (bitNum != start) {
                           sb.append(';');
                        }
                        sb.append(m.group(1)+bitNum+m.group(7));
                     }
                  }
                  else {
                     for (char bitNum=end; bitNum>=start; bitNum--) {
                        if (bitNum != end) {
                           sb.append(';');
                        }
                        sb.append(m.group(1)+bitNum+m.group(7));
                     }
                  }
               }
               else {
                  return new String[] {"Opps"};
               }
            }
            else {
               sb.append(pattern);
            }
            resultSb.append(sb.toString()+";");
         }
         patterns = resultSb.toString().split(";");
      } while (moreToDo);
      bitList = resultSb.toString();
      String[] bitListArray = bitList.split(";");
      for (int index=0; index<bitListArray.length; index++) {
         bitListArray[index] = bitListArray[index].replace("%i", Integer.toString(index));
      }
      return bitListArray;
   }
   
}
