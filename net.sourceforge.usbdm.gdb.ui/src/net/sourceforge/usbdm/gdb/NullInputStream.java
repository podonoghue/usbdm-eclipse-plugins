package net.sourceforge.usbdm.gdb;

import java.io.IOException;
import java.io.InputStream;

/**
 * @since 4.12
 */
public class NullInputStream extends InputStream {

   @Override
   public int read() throws IOException {
      // Return EOF
      return -1;
   }
}
