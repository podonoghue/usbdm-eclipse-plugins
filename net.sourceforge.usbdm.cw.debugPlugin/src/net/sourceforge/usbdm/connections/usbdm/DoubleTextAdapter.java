package net.sourceforge.usbdm.connections.usbdm;

import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.widgets.Text;

   class DoubleTextAdapter extends Object {
      Text textField;
      
      Text getObject() {
         return textField;
      }
      
      DoubleTextAdapter(Text textField) {
         this.textField = textField;
         textField.addVerifyListener(new DoubleVerifyListener());
      }
      
      private class DoubleVerifyListener implements VerifyListener {
         public void verifyText(VerifyEvent e) {
            // Make sure a digit or decimal point (to exclude d/e etc)
            e.text = e.text.toUpperCase();
            String string = e.text;
            char[] chars = new char[string.length()];
            string.getChars(0, chars.length, chars, 0);
            for (int i = 0; i < chars.length; i++) {
               if (('0' > chars[i] || chars[i] > '9') && (chars[i] != '.')) {
                  e.doit = false;
                  return;
               }
            }
            // Make sure a valid float number or empty
            String originalValue = ((Text)e.getSource()).getText();
            String newValue      = originalValue.substring(0, e.start)+e.text+originalValue.substring(e.end, originalValue.length());
            if (!newValue.isEmpty()) {
               try {
                  // Check if valid float number
                  Float.parseFloat(newValue);
   //               System.out.println("float f = " + f);
                } catch (NumberFormatException nfe) {
   //               System.err.println("NumberFormatException: " + nfe.getMessage());
                  e.doit = false;
                }
            }
         }
      };

      public double getDoubleValue() {
         double value = 0.0;
         try {
            value = Float.parseFloat(textField.getText());
         } catch (NumberFormatException e) {
            // Quietly default to 0.0
         }
         return value;
      }

      public void setDoubleValue(Double value) {
         textField.setText(String.format("%4.2f", value));
      }
   }
   
