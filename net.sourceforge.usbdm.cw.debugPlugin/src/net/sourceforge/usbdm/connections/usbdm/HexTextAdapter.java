package net.sourceforge.usbdm.connections.usbdm;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;

   class HexTextAdapter extends Object {
      Text textField;
      String name;

      String formatString = "%X";
      int min = 0;
      int max = 0xFFFFFFFF;
      
      public void setRange(int min, int max) {
         this.min = min; 
         this.max = max; 
      }
      Text getObject() {
         return textField;
      }
     
      HexTextAdapter(String name, Text textField, int value) {
         this.textField = textField;
         this.name = name;
         setHexValue(value);
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
      
      public int getHexValue() {
         int value = 0;
         try {
            value = Integer.parseInt(textField.getText(), 16);
         } catch (NumberFormatException e) {
            // Quietly default to 0
         }
         return value;
      }
      
      public boolean validate() {
         int value = getHexValue();
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
      
      public void setHexValue(int value) {
//         if (value == 0)
//            textField.setText("default");
         textField.setText(String.format(formatString, value));
      }
   }
   
