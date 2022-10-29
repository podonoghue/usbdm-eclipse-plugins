package net.sourceforge.usbdm.deviceEditor.model;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Tree;

import net.sourceforge.usbdm.deviceEditor.editor.RtcTimeDialogue;
import net.sourceforge.usbdm.deviceEditor.information.RtcTimeVariable;

public class RtcTimeModel extends LongVariableModel {

   public RtcTimeModel(BaseModel parent, RtcTimeVariable variable) {
      super(parent, variable);
   }

   static class RtcTextCellEditor extends DialogCellEditor {
      final RtcTimeModel fModel;
      
      public RtcTextCellEditor(Tree tree, RtcTimeModel rtcTimeModel) {
         super(tree, SWT.NONE);
         fModel = rtcTimeModel;
      }

      @Override
      protected Object openDialogBox(Control paramControl) {
         RtcTimeDialogue dialog = new RtcTimeDialogue(paramControl.getShell(), fModel.getVariable());
         if (dialog.open() == Window.OK) {
            return dialog.getResult();
         };
         return null;
      }

      @Override
      protected void doSetValue(Object value) {
         if (value instanceof String) {
            Pattern p = Pattern.compile("(\\d+)/(\\d+)/(\\d+)\\s+(\\d+):(\\d+):(\\d+)");
            Matcher m = p.matcher((String)value);
            if (m.matches()) {
               Calendar cal = new GregorianCalendar();
               cal.set(
                     Integer.parseInt(m.group(1)), // dd
                     Integer.parseInt(m.group(2)), // mm
                     Integer.parseInt(m.group(3)), // yyyy
                     Integer.parseInt(m.group(4)), // hh
                     Integer.parseInt(m.group(5)), // mm
                     Integer.parseInt(m.group(6))  // ss
                     );
               super.doSetValue(Math.round(cal.getTimeInMillis()/1000.0));
            }
         }
         super.doSetValue(value);
      }
   }

   @Override
   public String getValueAsString() {
      
      SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
      return sdf.format(new Date(1000*getVariable().getValueAsLong()));
   }

   public void setTime(Long timeInMilliseconds) {
      
      getVariable().setValue(Math.round(timeInMilliseconds/1000.0));
   }
   
   @Override
   public CellEditor createCellEditor(Tree tree) {
      
      return new RtcTextCellEditor(tree, this);
   }

}