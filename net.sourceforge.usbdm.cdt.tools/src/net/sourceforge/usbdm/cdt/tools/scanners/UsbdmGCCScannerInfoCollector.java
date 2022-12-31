package net.sourceforge.usbdm.cdt.tools.scanners;

import org.eclipse.cdt.make.core.scannerconfig.IScannerInfoCollector3;
import org.eclipse.cdt.make.internal.core.scannerconfig2.PerProjectSICollector;
import org.eclipse.cdt.managedbuilder.scannerconfig.IManagedScannerInfoCollector;

@SuppressWarnings("restriction")
public class UsbdmGCCScannerInfoCollector extends PerProjectSICollector
   implements IScannerInfoCollector3, IManagedScannerInfoCollector
{
   public UsbdmGCCScannerInfoCollector() {
//      System.err.println("ARMGCCScannerInfoCollector.ARMGCCScannerInfoCollector()");
      super();
   }
}
