package net.sourceforge.usbdm.gdb.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;

   public class NumberTextAdapter extends Object {
      Text   textField;
      String name;
      String formatString = "%d";
      final int min;
      final int max;
     
      Text getObject() {
         return textField;
      }
      
      public NumberTextAdapter(String name, Text textField, int value) {
         this.textField = textField;
         this.name = name;
         this.max  = 10000;
         this.min  = 100;
         setDecimalValue(value);
//         formatString = new String("%10d");
         textField.setTextLimit(10);
         textField.addVerifyListener(new DecimalVerifyListener());
      }
      
      public NumberTextAdapter(String name, Text textField, int value, int min, int max) {
         this.textField = textField;
         this.name = name;
         this.max  = max;
         this.min  = min;
         setDecimalValue(value);
//         formatString = new String("%10d");
         textField.setTextLimit(10);
         textField.addVerifyListener(new DecimalVerifyListener());
      }
      
      private class DecimalVerifyListener implements VerifyListener {
         public void verifyText(VerifyEvent e) {
            String string = e.text;
            char[] chars = new char[string.length()];
            string.getChars(0, chars.length, chars, 0);
            for (int i = 0; i < chars.length; i++) {
               if ('0' > chars[i] || chars[i] > '9') {
                  e.doit = false;
                  return;
               }
            }
         }
      };
      
      public int getDecimalValue() {
         int value = 0;
         try {
            value = Integer.parseInt(textField.getText(), 10);
         } catch (NumberFormatException e) {
            // Quietly default to 0
         }
         return value;
      }
      
      public boolean validate() {
         int value = getDecimalValue();
         boolean ok = ((value >= min) && (value <= max));
         
         if (!ok) {
            MessageBox msg = new MessageBox(textField.getShell(), SWT.OK|SWT.ERROR);
            msg.setMessage("'"+name+"' is invalid.\n" +
            		"Value must be in the range "+min+" to "+max+".");
            msg.setText("Invalid range");
            msg.open();
         }
         return ok;
      }
      
      public void setDecimalValue(int value) {
         textField.setText(String.format(formatString, value));
      }
   }
   