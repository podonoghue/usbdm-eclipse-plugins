package net.sourceforge.usbdm.cdt.ui.newProjectWizard;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.IBinary;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.ui.CElementLabelProvider;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.TwoPaneElementSelector;

import net.sourceforge.usbdm.cdt.tools.UsbdmConstants;
import net.sourceforge.usbdm.deviceDatabase.Device;
import net.sourceforge.usbdm.deviceDatabase.ui.DeviceSelector;
import net.sourceforge.usbdm.jni.Usbdm.TargetType;
import net.sourceforge.usbdm.packageParser.ISubstitutionMap;
import net.sourceforge.usbdm.packageParser.SubstitutionMap;

public class LaunchParameterUtilities {

   /**
    * Adds device specific launch configuration attributes to map<br>
    * These are later used as substitution parameters in the launch configuration
    * 
    * @param paramMap Map to add attributes to
    * @param device   Device needed to obtain attributes
    * @param binPath  Path to binary (may be null)
    * @throws Exception
    */
   public static void addLaunchParameters(ISubstitutionMap variableMap, Device device, IPath binPath) {

      try {
      variableMap.addValue(UsbdmConstants.TARGET_DEVICE_KEY,           device.getName());
      variableMap.addValue(UsbdmConstants.TARGET_DEVICE_NAME_KEY,      device.getName().toLowerCase());
      variableMap.addValue(UsbdmConstants.TARGET_DEVICE_FAMILY_KEY,    device.getFamily());
      variableMap.addValue(UsbdmConstants.TARGET_DEVICE_SUBFAMILY_KEY, device.getSubFamily());
      variableMap.addValue(UsbdmConstants.ERASE_METHOD_KEY,            device.getPreferredEraseMethod().getOptionName());
      variableMap.addValue(UsbdmConstants.RESET_METHOD_KEY,            device.getPreferredResetMethod().getOptionName());
      variableMap.addValue(UsbdmConstants.CLOCK_TRIM_FREQUENCY_KEY,    String.valueOf(device.getDefaultClockTrimFreq()));
      variableMap.addValue(UsbdmConstants.NVM_CLOCK_TRIM_LOCATION_KEY, Long.toHexString(device.getDefaultClockTrimNVAddress()));
      } catch (Exception e) {
         e.printStackTrace();
      }

      if (binPath != null) {
         // Add path to binary
         variableMap.addValue(UsbdmConstants.BIN_PATH_KEY,  binPath.toPortableString());
      }
//      if (binPath == null) {
//         // Add default path to binary
//         String projectName = variableMap.get(UsbdmConstants.PROJECT_NAME_KEY);
//         variableMap.addValue(UsbdmConstants.BIN_PATH_KEY,  "Debug/"+projectName+".elf");
//      }
   }

   /**
    * Reads a file into a string
    * 
    * @param filename
    * 
    * @return String containing file contents
    * @throws Exception
    */
   private static String readFile(String filename) throws Exception {
      URL url = new URL("platform:/plugin/net.sourceforge.usbdm.cdt.ui/files/" + filename);
      StringBuilder buffer = new StringBuilder();
      try {
         InputStream inputStream = url.openConnection().getInputStream();
         BufferedReader in = null;
         in = new BufferedReader(new InputStreamReader(inputStream));
         String inputLine;
         while ((inputLine = in.readLine()) != null) {
            buffer.append(inputLine);
            buffer.append('\n');
         }
         in.close();
      } catch (Exception e) {
         throw new Exception("Failed to open template file: "+url+"\n "+e.getMessage(), e);
      }
      return buffer.toString();
   }

   /**
    * Searches an inputStream line-by-line looking for a pattern
    * 
    * @param inputStream      Input stream to search
    * @param patternString    Pattern to look for
    * 
    * @return  Matcher holding line matched
    */
   public static Matcher scrapeFile(InputStream inputStream, String patternString) {
      BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
      Pattern pattern   = Pattern.compile(patternString);
      try {
         String inputLine;
         while ((inputLine = in.readLine()) != null) {
            Matcher matcher = pattern.matcher(inputLine);
            if (matcher.matches()) {
               return matcher;
            }
         }
      } catch (IOException e) {
      }
      return null;
   }

   /**
    * Tries to determine the target device name by inspecting an existing project file
    * 
    * @param file      Path of project file
    * @param tuple     Search information
    * 
    * @return  Name found or null if unsuccessful
    */
   private static String scrapeFile(IFile file, Tuple tuple) {
      String deviceName = null;
      try {
         InputStream in = file.getContents();
         Matcher m = scrapeFile(in, tuple.match);
         in.close();
         if (m != null) {
            deviceName = m.replaceAll(tuple.result);
//            System.err.println(String.format("%-70s",tuple)+" => Found '"+deviceName+"' within '"+m.group()+"'");
         }
      } catch (Exception e) {
      }
      return deviceName;
   }

   /**
    * Tuple to represent a pattern to look for
    */
   static class Tuple {
      String match;  // Pattern to look for (regex)
      String result; // How to extract information from pattern match

      /**
       * Used to represent search information
       * 
       * @param match   Pattern to look for (regex)
       * @param result  How to extract information from pattern match
       */
      Tuple(String match, String result) {
         this.match  = match;
         this.result = result;
      }

      @Override
      public String toString() {
         return "["+match+","+result+"]";
      }
   };

   /** Patterns to search files with */
   final static Tuple tuples[] = {
         new Tuple(".*net.sourceforge.usbdm.gdb.deviceName.*value\\s*=\\s*\"(.*)\".*", "$1"),    // USBDM Launch
         new Tuple(".*DEVICE_NAME.*\"(?:Freescale|NXP)_.*_(.+)\".*",                   "$1"),    // PE Launch
         new Tuple(".*gdbServerDeviceName.*\"\\s*value\\s*=\\s*\"M?(.+?)x+(.*?)\".*",  "$1M$2"), // Segger launch
         new Tuple(".*&lt;name&gt;(LPC.*)/(.*)&lt;/name&gt;&#13;.*",                   "$1_$2"), // mcuExpress .cproject
         new Tuple(".*&lt;name&gt;(.*)&lt;/name&gt;&#13;.*",                           "$1"),    // mcuExpress .cproject
   };

   /**
    * Tries to determine the target device name by inspecting the existing project files e.g. launch configurations
    * 
    * @param project Project to search
    * 
    * @return Target device name or null if none found
    */
   private static String scrapeDeviceName(IProject project) {

      // Which files to scrape
      final String probeFiles[] = {
            "Project_Settings/"+project.getName()+"-Debug.launch",               // Old USBDM
            "Project_Settings/"+project.getName()+"-Release.launch",             // Old USBDM
            "Project_Settings/"+project.getName()+"_Debug_USBDM.launch",         // New USBDM
            "Project_Settings/"+project.getName()+"_Release_USBDM.launch",       // New USBDM
            "Project_Settings/Debugger/"+project.getName()+"_Debug_PNE.launch",  // P&E
            project.getName()+"_Debug_PNE.launch",                               // P&E
            "Project_Settings/Debugger/"+project.getName()+"_Debug.launch",      // mcuExpress
            project.getName()+"_Debug.launch",                                   // mcuExpress
            ".cproject",
      };

      String deviceName = null;

      for (String filePath:probeFiles) {
//         System.err.println("Scraping "+filePath);
         IFile file = project.getFile(filePath);
         if (!file.exists()) {
//            System.err.println("File \'" + file + "\' doesn't exist");
            continue;
         }
         for (Tuple tuple:tuples) {
            deviceName = scrapeFile(file, tuple);
            if (deviceName != null) {
               break;
            }
         }
         if (deviceName != null) {
            break;
         }
      }
//      System.err.println("Found candidate target name \'" + deviceName + "\'");
      return deviceName;
   }

   /**
    * Search for executable
    * 
    * @param elements  Element to search (may be binaries or projects etc.).
    * 
    * @return Array of binaries found  may be empty
    */
   public static IBinary[] searchForExecutable(final Object[] elements) {

      //      System.err.println("Elements        = " + elements);
      //      System.err.println("Elements.length = " + elements.length);

      if ((elements == null) || (elements.length == 0)) {
//         System.err.println("Empty elements[]");
         return new IBinary[0];
      }

      if ((elements.length == 1) && (elements[0] instanceof IBinary)) {
         IBinary bin = (IBinary) elements[0];
//         System.err.println("Found elements[0] == binary"+bin.getElementName());
         return new IBinary[]{bin};
      }

      final List<IBinary>  results = new ArrayList<IBinary>();

      IRunnableWithProgress runnable = new IRunnableWithProgress() {
         @Override
         public void run(IProgressMonitor pm) throws InterruptedException {
            int nElements = elements.length;
            SubMonitor subMonitor = SubMonitor.convert(pm, "Looking for executables", nElements);
            subMonitor.split(1);
            for (int i = 0; i < nElements; i++) {
//             System.err.println("Checking IResource " + elements[i] );
               if (elements[i] instanceof IAdaptable) {
//                System.err.println("Checking IAdaptable " + elements[i] );
                  IResource r = ((IAdaptable) elements[i]).getAdapter(IResource.class);
//                System.err.println("Checking IResource " + r );
                  if (r != null) {
                     //                        System.err.println("Found IResource " + r.getName());
                     ICProject cproject = CoreModel.getDefault().create(r.getProject());
                     if (cproject != null) {
//                      System.err.println("Found project " + cproject.getPath() );
                        try {
                           IBinary[] bins = cproject.getBinaryContainer().getBinaries();
                           for (IBinary bin : bins) {
                              if (!bin.isExecutable()) {
                                 continue;
                              }
//                            System.err.println("Found suitable binary " + bin.getPath() );
                              results.add(bin);
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
            }
         }
      };
      try {
         new ProgressMonitorDialog(null).run(true, false, runnable);
      } catch (Exception e) {
         return null;
      }
      return results.toArray(new IBinary[results.size()]);
   }

   /**
    * Creates a launch file from template file and map of variables for substitution
    * 
    * @param variableMap
    * 
    * @return
    * @throws Exception
    */
   private static InputStream getLaunchFile(ISubstitutionMap variableMap) throws Exception {
      String launchFilePath = "launchTemplate.xml";

      String fileContents;
      fileContents = readFile(launchFilePath);
      fileContents = variableMap.substitute(fileContents);
      ByteArrayInputStream contents = new ByteArrayInputStream(fileContents.getBytes());

      return contents;
   }

   /**
    * Creates a launch configuration as a physical file<br>
    * It attempts to deduce the device name from project files and asks the user to select/confirm the device choice.
    * 
    * @param shell   Shell for dialogues
    * @param project Project to create launch configuration within
    * @param bin Path to binary
    * @param build   String describing build e.g. debug or release. Used in naming launch configuration
    * 
    * @return A launch configuration
    * @throws Exception
    */
   public static ILaunchConfiguration createLaunchConfig(final Shell shell, final IProject project, IBinary bin) throws Exception {

      IPath binPath = bin.getResource().getProjectRelativePath();
      String buildName = "Default";
      if (binPath.segmentCount() >= 2) {
         // Use containing folder as build name
         buildName = binPath.segment(binPath.segmentCount()-2);
      }
      // Directories to create launch configuration in (order of preference)
      String[]   folders = {"Project_Settings/Debugger", "Project_Settings"};
      IContainer folder  = null;
      for (String trialFolder:folders) {
         IFolder t  = project.getFolder(trialFolder);
         if (t.exists()) {
            folder = t;
            break;
         }
      }
      if (folder == null) {
         // Project_Settings' folder doesn't exist, creating launch config in root directory
         folder = project;
      }
      // Create launch file name e.g. project_debug_USBDM.launch
      IFile launchFile = folder.getFile(new Path(project.getName()+"_"+buildName+"_USBDM.launch"));

      if (launchFile.exists()) {
//         System.err.println("File " + launchFile + " already exists");
         throw new Exception("Launch configuration \"" + launchFile.getName() + "\" already exists");
      }
      // Try to get device name from existing project files
      String deviceName = scrapeDeviceName(project);

      // Ask user to select/confirm device
      DeviceSelector deviceSelector = new DeviceSelector(shell, TargetType.T_ARM, deviceName);

      int rc = deviceSelector.open();
      if (rc != Window.OK) {
         return null;
      }

      Device device = deviceSelector.getDevice();

      // Map used to hold variable for launch file creation
//      ISimpleSubstitution variableMap = new HashMap<String, String>();
      ISubstitutionMap variableMap = new SubstitutionMap();

      // Add device name to project creation map
      variableMap.addValue(UsbdmConstants.PROJECT_NAME_KEY, project.getName());

      // Add launch parameters from device information
      LaunchParameterUtilities.addLaunchParameters(variableMap, device, binPath);

      // Create launch file and let the system know about it
      InputStream contents = getLaunchFile(variableMap);
      launchFile.create(contents, true, null);
      launchFile.refreshLocal(IResource.DEPTH_ONE, null);
      project.refreshLocal(IResource.DEPTH_INFINITE, null);

      // Return the launch configuration
      return DebugPlugin.getDefault().getLaunchManager().getLaunchConfiguration(launchFile);
   }
   
   /**
    * Prompts the user to select a binary
    * 
    * @param binList   The array of binaries.
    *
    * @return the selected binary or <code>null</code> if none.
    */
   public static IBinary chooseBinary(Shell shell, IBinary[] binList) {
      if (binList.length == 0) {
         return null;
      }
      if (binList.length == 1) {
         return binList[0];
      }
      // Prompt user for selection
      CElementLabelProvider programLabelProvider = new CElementLabelProvider() {
         @Override
         public String getText(Object element) {
            if (element instanceof IBinary) {
               return ((IBinary) element).getPath().lastSegment();
            }
            return super.getText(element);
         }
      };

      CElementLabelProvider qualifierLabelProvider = new CElementLabelProvider() {
         @Override
         public String getText(Object element) {
            if (element instanceof IBinary) {
               IBinary bin = (IBinary) element;
               StringBuilder name = new StringBuilder();
               name.append(bin.getCPU()
                     + (bin.isLittleEndian() ? "le" : "be")); //$NON-NLS-1$ //$NON-NLS-2$
               name.append(" - "); //$NON-NLS-1$
               name.append(bin.getPath().toString());
               return name.toString();
            }
            return super.getText(element);
         }
      };

      TwoPaneElementSelector dialog = new TwoPaneElementSelector(shell, programLabelProvider, qualifierLabelProvider);
      dialog.setElements(binList);
      dialog.setTitle("LaunchShortcut Launcher");
      dialog.setMessage("Choose an executable");
      dialog.setUpperListLabel("LaunchShortcut Binaries");
      dialog.setLowerListLabel("LaunchShortcut Qualifier");
      dialog.setMultipleSelection(false);

      if (dialog.open() == Window.OK) {
         return (IBinary) dialog.getFirstResult();
      }

      return null;
   }

}
