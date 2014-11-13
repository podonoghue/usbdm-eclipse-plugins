package net.sourceforge.usbdm.gdb.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;

   public class HexTextAdapter extends Object {
      Text textField;
      String name;

      final String FORMAT_STRING = "%X";
      long min = 0L;
      long max = 0xFFFFFFFFL;
      
      public void setRange(long min, long max) {
         this.min = min; 
         this.max = max; 
      }
      Text getObject() {
         return textField;
      }
     
      public HexTextAdapter(String name, Text textField, long value) {
         this.textField = textField;
         this.name = name;
         setValue(value);
//         formatString = new String("%5X");
         textField.setTextLimit(10);
         textField.addVerifyListener(new HexVerifyListener());
//         textField.addModifyListener(new HexModifyListener());
      }
      
//      private class HexModifyListener implements ModifyListener {
//
//         @Override
//         public void modifyText(ModifyEvent e) {
//          System.err.println("modifyText()");
//         }
//      }
      
      private class HexVerifyListener implements VerifyListener {
         public void verifyText(VerifyEvent e) {
            e.text = e.text.toUpperCase(); // Force display as upper-case
            String string = e.text;
            char[] chars = new char[string.length()];
            string.getChars(0, chars.length, chars, 0);
            for (int i = 0; i < chars.length; i++) {
               if (('0' > chars[i] || chars[i] > '9') &&
                     ('A' > chars[i] || chars[i] > 'F')) {
                  e.doit = false;
                  return;
               }
            }
         }
      };
      
      public long getHexValue() {
         long value = 0;
         try {
            value = Long.parseLong(textField.getText(), 16);
         } catch (NumberFormatException e) {
            // Quietly default to 0
         }
         return value;
      }
      
      public boolean validate() {
         long value = getHexValue();
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
      
      public void setValue(long l) {
         textField.setText(String.format(FORMAT_STRING, l));
      }

      public void setHexValue(String s) {
         if (s==null) {
            setValue(0);
         }
         if (s.trim().isEmpty()) {
            setValue(0);
         }
         setValue(Long.parseLong(s, 16));
      }

      public void setValue(String s) {
         if (s==null) {
            setValue(0);
         }
         if (s.trim().isEmpty()) {
            setValue(0);
         }
         setValue(Long.parseLong(s));
      }
   }
   
