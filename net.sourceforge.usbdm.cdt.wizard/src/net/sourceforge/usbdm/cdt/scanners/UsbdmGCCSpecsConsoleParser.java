package net.sourceforge.usbdm.cdt.scanners;

import org.eclipse.cdt.core.IMarkerGenerator;
import org.eclipse.cdt.make.core.scannerconfig.IScannerInfoCollector;
import org.eclipse.cdt.make.internal.core.scannerconfig.gnu.GCCSpecsConsoleParser;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;

public class UsbdmGCCSpecsConsoleParser extends GCCSpecsConsoleParser {

   /* (non-Javadoc)
    * @see org.eclipse.cdt.make.internal.core.scannerconfig.gnu.GCCSpecsConsoleParser#startup(org.eclipse.core.resources.IProject, org.eclipse.core.runtime.IPath, org.eclipse.cdt.make.core.scannerconfig.IScannerInfoCollector, org.eclipse.cdt.core.IMarkerGenerator)
    */
   @Override
   public void startup(IProject project, IPath workingDirectory,
         IScannerInfoCollector collector, IMarkerGenerator markerGenerator) {
//      System.err.println("UsbdmGCCSpecsConsoleParser.startup() => "+project.toString());
      super.startup(project, workingDirectory, collector, markerGenerator);
   }

}
