package net.sourceforge.usbdm.peripherals.stmicro;

import java.util.Vector;

import net.sourceforge.usbdm.peripheralDatabase.DefaultPeripheralDescriptionProvider;

public class STMicroPeripheralDescriptionProvider extends DefaultPeripheralDescriptionProvider {

   static final String LICENSE = 
      "End User License Agreement for STMicroelectronics (Version 1.0)\n"
      + "\n"
      + "Licence Terms\n"
      + "STMicroelectronics International N.V.,   (\"LICENSOR\") hereby grants and you \n"
      + "(\"LICENSEE\") hereby accept a non transferable, non-exclusive licence to use and copy the \n"
      + "deliverables (\"Deliverables\") solely for the purpose of; (i) developing LICENSEE's \n"
      + "development tools and distributing such development tools to third parties; (ii) generating \n"
      + "derivative representations of the Deliverables to develop and debug software for \n"
      + "LICENSOR's targeted devices or device series identified within the Deliverables, (together \n"
      + "the \"Purpose\") under the following terms and conditions:\n"
      + "1.         Ownership.  The Deliverables are the property of LICENSOR. LICENSEE acquires \n"
      + "no right, title or interest in the Deliverables other than the licence rights granted herein.\n" 
      + "2.         Use.  LICENSEE shall only be permitted to use the Deliverables for the \n"
      + "Purpose.  LICENSEE shall not reverse engineer, decompile or disassemble the Deliverables, \n"
      + "in whole or in part.\n"
      + "3.         Copies.  All copies of the Deliverables must bear the same notice(s) contained on the \n"
      + "original copies of the Deliverables.\n"
      + "4.         No Warranty. THE DELIVERABLES ARE PROVIDED \"AS IS\" AND ANY\n" 
      + "EXPRESS, IMPLIED OR STATUTORY WARRANTIES, INCLUDING, BUT NOT \n"
      + "LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY, \n"
      + "SATISFACTORY QUALITY AND FITNESS FOR A PARTICULAR PURPOSE ARE \n"
      + "DISCLAIMED.  \n"
      + " \n "
      + "IN NO EVENT SHALL LICENSOR BE LIABLE FOR ANY DIRECT, INDIRECT,\n" 
      + "INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES \n"
      + "HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN \n"
      + "CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR \n"
      + "OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THE DELIVERABLES,\n" 
      + "EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. \n"
      + " \n"
      + "LICENSEE EXPRESSLY ASSUMES ALL LIABILITIES AND RISKS, FOR USE OR\n" 
      + "OPERATION OF THE DELIVERABLES.\n"
      + "5.         In the event that LICENSEE receives early access to the Deliverables, LICENSEE \n"
      + "acknowledges and agrees that; (a) notwithstanding the licence grants above, LICENSEE shall \n"
      + "only be permitted to use the Deliverables solely internally for evaluation and providing \n"
      + "feedback to LICENSOR; (b) except with respect to the limited licence grants in 5(a), \n"
      + "LICENSEE shall be subject to all of the terms and conditions set out above; and (c) the \n"
      + "Deliverables are confidential information and LICENSEE shall maintain in confidence the \n"
      + "Deliverables and apply security measures no less stringent than the measures that LICENSEE \n"
      + "applies to its own like information, but not less than a reasonable degree of care, to prevent\n" 
      + "unauthorised disclosure and use of the Deliverables.\n";


   static final String DESCRIPTION = 
         "SVD files for STM32 devices\n"
       + "Derived from STMicroelectronics distribution";

   static final String NAME = 
         "ST Microelectronics STM32";

   static final String ID =
         "stmicroelectronics.arm.devices";

   /**
    * Constructor for Freescale device peripherals library
    */
   public STMicroPeripheralDescriptionProvider() {
      super(Activator.getContext());
      setLicense(LICENSE);
      setName(NAME);
      setDescription(DESCRIPTION);
      setId(ID);
      }
   
   public static void main(String[] args) throws Exception {
      STMicroPeripheralDescriptionProvider provider = new STMicroPeripheralDescriptionProvider();
      Vector<String> fileNames = provider.getDeviceNames();
      for (String s : fileNames) {
         System.err.println("Name = " + s);
      }
   }

}
