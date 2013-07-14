package net.sourceforge.usbdm.cdt.tools.scanners;

import org.eclipse.cdt.make.core.scannerconfig.IScannerInfoCollector;
import org.eclipse.cdt.managedbuilder.internal.scannerconfig.ManagedGCCScannerInfoConsoleParser;
import org.eclipse.core.resources.IProject;

@SuppressWarnings("restriction")
public class UsbdmManagedGCCScannerInfoConsoleParser extends ManagedGCCScannerInfoConsoleParser
{
  public void startup(IProject oProject, IScannerInfoCollector oCollector) {
//    System.err.println("UsbdmManagedGCCScannerInfoConsoleParser.startup()");
    super.startup(oProject, oCollector);
  }
}