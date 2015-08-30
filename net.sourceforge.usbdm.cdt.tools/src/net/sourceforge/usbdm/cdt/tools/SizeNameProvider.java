package net.sourceforge.usbdm.cdt.tools;

import java.util.ArrayList;

import org.eclipse.cdt.managedbuilder.core.IManagedOutputNameProvider;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.core.runtime.IPath;

public class SizeNameProvider implements IManagedOutputNameProvider {

   @Override
   public IPath[] getOutputNames(ITool tool, IPath[] primaryInputNames) {
      ArrayList<IPath> iPaths= new ArrayList<IPath>();
      for (IPath x: primaryInputNames) {
         x = x.removeFileExtension();
         x = x.addFileExtension("size");
         iPaths.add(x);
      }
      IPath[] result = new IPath[iPaths.size()];
      return iPaths.toArray(result);
   }

}
