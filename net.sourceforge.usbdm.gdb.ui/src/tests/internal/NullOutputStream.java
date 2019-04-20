package tests.internal;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @since 4.12
 */
public class NullOutputStream extends OutputStream {

   @Override
   public void write(int b) throws IOException {
      // Discard output
   }

}
