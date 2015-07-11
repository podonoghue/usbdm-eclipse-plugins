package net.sourceforge.usbdm.peripherals.freescale;

import java.util.Vector;

import net.sourceforge.usbdm.peripheralDatabase.DefaultPeripheralDescriptionProvider;

public class FreescalePeripheralDescriptionProvider extends DefaultPeripheralDescriptionProvider {

   static final String LICENSE = 
         "Redistribution and use in source and binary forms, with or without modification,\n"
         + "are permitted provided that the following conditions are met:\n"
         + " o Redistributions of source code must retain the above copyright notice, this list\n"
         + "   of conditions and the following disclaimer.\n"
         + " o Redistributions in binary form must reproduce the above copyright notice, this\n"
         + "   list of conditions and the following disclaimer in the documentation and/or\n"
         + "   other materials provided with the distribution.\n"
         + " o Neither the name of Freescale Semiconductor, Inc. nor the names of its\n"
         + "   contributors may be used to endorse or promote products derived from this\n"
         + "   software without specific prior written permission.\n"
         + " THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS &quot;AS IS&quot; AND\n"
         + " ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED\n"
         + " WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE\n"
         + " DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR\n"
         + " ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES\n"
         + " (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;\n"
         + " LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON\n"
         + " ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT\n"
         + " (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS\n"
         + " SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.";
   
   static final String DESCRIPTION = 
         "SVD files for Freescale Kinetis devices\n"
         + "Derived from Freesale distribution";
   
   static final String NAME =
         "Freescale Kinetis";

   static final String ID =
         "freescale.arm.devices";

   /**
    * Constructor for Freescale device peripherals library
    */
   public FreescalePeripheralDescriptionProvider() {
      super(Activator.getContext());
      setLicense(LICENSE);
      setName(NAME);
      setDescription(DESCRIPTION);
      setId(ID);
      }
   
   public static void main(String[] args) throws Exception {
      FreescalePeripheralDescriptionProvider provider = new FreescalePeripheralDescriptionProvider();
      Vector<String> fileNames = provider.getDeviceNames();
      for (String s : fileNames) {
         System.err.println("Name = " + s);
      }
   }

}
