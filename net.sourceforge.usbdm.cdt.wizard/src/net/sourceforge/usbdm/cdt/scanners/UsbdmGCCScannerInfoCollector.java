package net.sourceforge.usbdm.cdt.scanners;

import org.eclipse.cdt.make.core.scannerconfig.IScannerInfoCollector3;
import org.eclipse.cdt.make.internal.core.scannerconfig2.PerProjectSICollector;
import org.eclipse.cdt.managedbuilder.scannerconfig.IManagedScannerInfoCollector;

public class UsbdmGCCScannerInfoCollector extends PerProjectSICollector 
   implements IScannerInfoCollector3, IManagedScannerInfoCollector 
{
   public UsbdmGCCScannerInfoCollector() {
//      System.err.println("ARMGCCScannerInfoCollector.ARMGCCScannerInfoCollector()");
      super();
   }
}
