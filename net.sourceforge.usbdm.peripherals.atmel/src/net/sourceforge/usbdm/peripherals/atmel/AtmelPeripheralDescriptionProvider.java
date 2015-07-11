package net.sourceforge.usbdm.peripherals.atmel;

import java.util.Vector;

import net.sourceforge.usbdm.peripheralDatabase.DefaultPeripheralDescriptionProvider;

public class AtmelPeripheralDescriptionProvider extends DefaultPeripheralDescriptionProvider {

   static final String LICENSE = 
      "End User License Agreement for Atmel (Version 1.0)\n"
      +"\n"
      +"License Terms\n"
      +"\n"
      +" ATMEL CORPORATION (\"LICENSOR\") hereby grants and you (\"LICENSEE\") hereby \n"
      +"accept a non-transferable, non-exclusive licence to use and copy the deliverables\n" 
      +"(\"Deliverables\") solely for the purpose of; (i) developing LICENSEE's development tools \n"
      +"and distributing such development tools to third parties; (ii) generating derivative \n"
      +"representations of the Deliverables to develop and debug software for LICENSOR's targeted \n"
      +"devices or device series identified within the Deliverables, (together the \"Purpose\") under the \n"
      +"following terms and conditions:\n"
      +"1.   Ownership.  The Deliverables are the property of LICENSOR. LICENSOR retains \n"
      +"full rights, title, and ownership including all patents, copyrights, trade secrets, trade \n"
      +"names, trademarks, and other intellectual property rights in and to the Deliverables. \n"
      +"LICENSEE acquires no right, title or interest in the Deliverables other than the \n"
      +"licence rights granted herein. \n"
      +" \n"
      +"2.  Use.  LICENSEE shall only be permitted to use the Deliverables for the \n"
      +"Purpose.  LICENSEE shall not reverse engineer, decompile or disassemble the \n"
      +"Deliverables, in whole or in part. \n"
      +" \n"
      +"3.  Copies.  All copies of the Deliverables must bear the same trademark or copyright \n"
      +"markings and notice(s) contained on the original copies of the Deliverables. \n"
      +" \n"
      +"4.  No Warranty and No Support. THE DELIVERABLES ARE PROVIDED \"AS IS\" \n"
      +"AND ANY EXPRESS, IMPLIED OR STATUTORY WARRANTIES, \n"
      +"INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF \n"
      +"MERCHANTABILITY, SATISFACTORY QUALITY. FITNESS FOR A \n"
      +"PARTICULAR PURPOSE AND NON-INFRINGEMENT ARE DISCLAIMED. \n"
      +"LICENSOR IS NOT OBLIGATED TO FURNISH OR MAKE AVAILABLE TO \n"
      +"LICENSEE ANY FURTHER INFORMATION, KNOW-HOW, SHOW-HOW, \n"
      +"BUG-FIXES, OR SUPPORT.  LICENSEE EXPRESSLY ASSUMES ALL \n"
      +"LIABILITIES AND RISKS, FOR USE OR OPERATION OF THE \n"
      +"DELIVERABLES. \n"
      +" \n"
      +"LICENSEE EXPRESSLY ASSUMES ALL LIABILITIES AND RISKS, FOR USE \n"
      +"OR OPERATION OF THE DELIVERABLES. \n"
      +" \n"
      +"5.  Early Access.  In the event that LICENSEE receives early access to the Deliverables, \n"
      +"LICENSEE acknowledges and agrees that; (a) notwithstanding the licence grants \n"
      +"above, LICENSEE shall only be permitted to use the Deliverables solely internally for \n"
      +"evaluation and providing feedback to LICENSOR; (b) except with respect to the \n"
      +"limited licence grants in 5(a), LICENSEE shall be subject to all of the terms and \n"
      +"conditions set out above; and (c) the Deliverables are confidential information and \n"
      +"LICENSEE shall maintain in confidence the Deliverables and apply security measures \n"
      +"no less stringent than the measures that LICENSEE applies to its own like \n"
      +"information, but not less than a reasonable degree of care, to prevent unauthorised \n"
      +"disclosure and use of the Deliverables. \n"
      +" \n"
      +"6.  Limitation of Liability.  IN NO EVENT SHALL LICENSOR BE LIABLE FOR \n"
      +"ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR \n"
      +"CONSEQUENTIAL DAMAGES HOWEVER CAUSED AND ON ANY THEORY \n"
      +"OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT \n"
      +"(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT \n"
      +"OF THE USE OF THE DELIVERABLES, EVEN IF ADVISED OF THE \n"
      +"POSSIBILITY OF SUCH DAMAGE.  IN NO EVENT SHALL THE LIABILITY OF \n"
      +"LICENSOR ARISING OUT OF OR RELATING TO THIS AGREEMENT EXCEED \n"
      +"THE GREATER OF ONE THOUSAND U.S. DOLLARS (US$1,000) OR THE \n"
      +"PRICE PAID BY LICENSEE TO LICENSOR FOR THE DELIVERABLES. \n"
      +" \n"
      +"7.  Entire Agreement.  THIS AGREEMENT IS THE ENTIRE AND EXCLUSIVE \n"
      +"AGREEMENT BETWEEN LICENSOR AND LICENSEE AND SUPERSEDES \n"
      +"ALL PRIOR ORAL AND WRITTEN AGREEMENTS AND COMMUNICATIONS \n"
      +"BETWEEN THE PARTIES PERTAINING TO THE SUBJECT MATTER OF THIS \n"
      +"AGREEMENT.  NO DIFFERENT OR ADDITIONAL TERMS WILL BE \n"
      +"ENFORCEABLE AGAINST LICENSOR UNLESS LICENSOR GIVES ITS \n"
      +"EXPRESS WRITTEN CONSENT, INCLUDING AN EXPRESS WAIVER OF THE \n"
      +"TERMS OF THIS AGREEMENT.\n"
      +"(20. August 2012)\n";

   
   static final String DESCRIPTION = 
         "SVD files for Atmel devices\n"
         + "Derived from Atmel distribution";
   
   static final String NAME =
         "Atmel ARM devices";

   static final String ID =
         "atmel.arm.devices";

   /**
    * Constructor for Freescale device peripherals library
    */
   public AtmelPeripheralDescriptionProvider() {
      super(Activator.getContext());
      setLicense(LICENSE);
      setName(NAME);
      setDescription(DESCRIPTION);
      setId(ID);
      }
   
   public static void main(String[] args) throws Exception {
      AtmelPeripheralDescriptionProvider provider = new AtmelPeripheralDescriptionProvider();
      Vector<String> fileNames = provider.getDeviceNames();
      for (String s : fileNames) {
         System.err.println("Name = " + s);
      }
   }

}
