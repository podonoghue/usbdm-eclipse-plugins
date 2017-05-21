package net.sourceforge.usbdm.cdt.ui.newProjectWizard;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.IBinary;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;

import net.sourceforge.usbdm.cdt.tools.UsbdmConstants;
import net.sourceforge.usbdm.deviceDatabase.Device;

public class LaunchParameterUtilities {

   /**
    * Adds device specific launch configuration attributes to map<br>
    * These are later used as substitution parameters in the launch configuration
    * 
    * @param paramMap Map to add attributes to
    * @param device   Device needed to obtain attributes
    * @param binPath 
    */
   public static void addLaunchParameters(Map<String, String> variableMap, Device device, IPath binPath) {

      variableMap.put(UsbdmConstants.TARGET_DEVICE_KEY,           device.getName());
      variableMap.put(UsbdmConstants.TARGET_DEVICE_NAME_KEY,      device.getName().toLowerCase());
      variableMap.put(UsbdmConstants.TARGET_DEVICE_FAMILY_KEY,    device.getFamily());
      variableMap.put(UsbdmConstants.TARGET_DEVICE_SUBFAMILY_KEY, device.getSubFamily());

      variableMap.put(UsbdmConstants.CLOCK_TRIM_FREQUENCY_KEY,    String.valueOf(device.getDefaultClockTrimFreq()));            
      variableMap.put(UsbdmConstants.NVM_CLOCK_TRIM_LOCATION_KEY, String.valueOf(device.getDefaultClockTrimNVAddress()));            
      variableMap.put(UsbdmConstants.ERASE_METHOD_KEY,            device.getPreferredEraseMethod().getOptionName());          
      variableMap.put(UsbdmConstants.RESET_METHOD_KEY,            device.getPreferredResetMethod().getOptionName());

      String projectName = variableMap.get(UsbdmConstants.PROJECT_NAME_KEY);
      if (binPath == null) {
         // Add default path to binary
         variableMap.put(UsbdmConstants.BIN_PATH_KEY,  "Debug/"+projectName+".elf");            
      }
      else {
         // Add path to binary
         variableMap.put(UsbdmConstants.BIN_PATH_KEY,  binPath.toPortableString());            
      }
   }

   /**
    * Search for executable
    * 
    * @param elements  Element to search (may be binaries or projects etc.).
    * @param mode      Launch mode.
    * 
    * @throws Throwable 
    */
   public static IBinary[] searchForExecutable(final Object[] elements, String mode) throws Exception {

      if ((elements == null) || (elements.length == 0)) {
         return new IBinary[0];
      }
      
      if ((elements.length == 1) && (elements[0] instanceof IBinary)) {
         return new IBinary[]{(IBinary) elements[0]};
      }
      
      final List<IBinary>  results = new ArrayList<IBinary>();

      IRunnableWithProgress runnable = new IRunnableWithProgress() {
         @Override
         public void run(IProgressMonitor pm) throws InterruptedException {
            int nElements = elements.length;
            pm.beginTask( "Looking for executables", nElements);
            try {
               IProgressMonitor sub = new SubProgressMonitor(pm, 1);
               for (int i = 0; i < nElements; i++) {
                  if (elements[i] instanceof IAdaptable) {
                     IResource r = (IResource) ((IAdaptable) elements[i]).getAdapter(IResource.class);
                     if (r != null) {
                        ICProject cproject = CoreModel.getDefault().create(r.getProject());
                        if (cproject != null) {
                           try {
                              IBinary[] bins = cproject.getBinaryContainer().getBinaries();
                              for (IBinary bin : bins) {
                                 if (bin.isExecutable()) {
                                    results.add(bin);
                                 }
                              }
                           } catch (CModelException e) {
                              // Ignored
                           }
                        }
                     }
                  }
                  if (pm.isCanceled()) {
                     throw new InterruptedException();
                  }
                  sub.done();
               }
            } finally {
               pm.done();
            }
         }
      };

      try {
         new ProgressMonitorDialog(null).run(true, false, runnable);
      } catch (InterruptedException e) {
         return null;
      } catch (InvocationTargetException e) {
         throw new Exception(e.getCause());
      }

      return results.toArray(new IBinary[results.size()]);
   }

}
