package net.sourceforge.usbdm.deviceEditor.editor;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriteFamilyCpp;
import net.sourceforge.usbdm.deviceEditor.xmlParser.ParseFamilyXML;

public class ProjectUtilities {

   static public void generateFiles(IProject project, IProgressMonitor monitor) throws Exception {
      IFile hardwareFile = project.getFile("usbdm/Configure.hardware");

      if (hardwareFile.exists()) {
         monitor.beginTask("Generating file", IProgressMonitor.UNKNOWN);
         
         // Parse the configuration
         Path filePath = Paths.get(hardwareFile.getLocation().toPortableString());
         ParseFamilyXML reader = new ParseFamilyXML();
         DeviceInfo deviceInfo = reader.parseFile(filePath);

         // Generate C code
         WriteFamilyCpp writer = new WriteFamilyCpp();
         writer.writeCppFiles(project, deviceInfo, monitor);
      }
   }

}
