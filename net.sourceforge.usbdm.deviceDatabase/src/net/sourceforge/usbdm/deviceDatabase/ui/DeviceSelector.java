package net.sourceforge.usbdm.deviceDatabase.ui;

import net.sourceforge.usbdm.deviceDatabase.Device;
import net.sourceforge.usbdm.jni.Usbdm.TargetType;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

public class DeviceSelector extends TitleAreaDialog  {

   DeviceSelectorPanel deviceSelectorPanel;
   TargetType          fTargetType   = null;
   String              fTargetName   = null;
   Device              fTargetDevice = null;
   
   /**
    * Create dialog
    * 
    * @param parent     Parent dialog
    * @param targetType Target type - Used to determine which device list to display
    * @param targetName Target name - Used to determine initially selected device
    */
   public DeviceSelector(Shell parent, TargetType targetType, String targetName) {
      super(parent);
      fTargetType = targetType;
      fTargetName = targetName;
   }
   
   /**
    * Set target type<br>
    * Used to determine which device list to display
    * 
    * @param targetType
    */
   public void setTargetType(TargetType targetType) {
      fTargetType = targetType;
   }
   
   /**
    * Set device name<br>
    * Used to determine initially selected device
    *  
    * @param targetName
    */
   public void setDevice(String targetName) {
      fTargetName = targetName;
   }
   
   @Override
   protected Control createDialogArea(Composite parent) {
      Composite comp = (Composite)super.createDialogArea(parent);
      deviceSelectorPanel = new DeviceSelectorPanel(comp, SWT.NONE);
      deviceSelectorPanel.setTargetType(fTargetType);
      deviceSelectorPanel.setDevice(fTargetName);
      GridDataFactory.generate(deviceSelectorPanel, 1, 1);
      
      deviceSelectorPanel.addListener(SWT.CHANGED, new Listener() {
         @Override
         public void handleEvent(Event event) {
            validate();
         }
      });
      return comp;
   }

   /**
    * Validate dialogue contents and update status message
    */
   void validate() {
      if (deviceSelectorPanel != null) {
         String msg = deviceSelectorPanel.validate();
         if (msg != null) {
            setErrorMessage(msg+" (Matching devices: "+deviceSelectorPanel.getMatchingDevices()+")");
         }
         else {
            setErrorMessage(null);
         }
         getButton(IDialogConstants.OK_ID).setEnabled(msg == null);
         fTargetName   = deviceSelectorPanel.getDeviceName();
         fTargetDevice = deviceSelectorPanel.getDevice();
      }
   }
   
   @Override
   public void create() {
      super.create();
      setTitle("Select Device");
      validate();
   }

   @Override
   protected void configureShell(Shell newShell) {
     super.configureShell(newShell);
     newShell.setText("USBDM Device Selection");
   }

   @Override
   protected Point getInitialSize() {
     return new Point(500, 600);
   }
   
   @Override
   protected boolean isResizable() {
      return true;
   }
   
   /**
    * Get name of selected device
    * 
    * @return Name of device as string
    */
   public String getText() {
      return fTargetName;
   }
   
   /**
    * Get selected device
    * 
    * @return Device
    */
   public Device getDevice() {
      return fTargetDevice;
   }
   
   public static void main(String[] args) {
      Display display = new Display();
      Shell shell = new Shell(display);
      shell.setBackground(new Color(display, 255,0,0));
      
//      DeviceSelector deviceSelector = new DeviceSelector(shell, TargetType.T_ARM, null);
      DeviceSelector deviceSelector = new DeviceSelector(shell, TargetType.T_ARM, "K20DX128");
//      DeviceSelector deviceSelector = new DeviceSelector(shell, TargetType.T_ARM, "K20DX128m5");
      int rc = deviceSelector.open();
      System.out.println("rc = " + rc);
      if (rc == Window.OK) {
         System.out.println("Device = " + deviceSelector.getDevice().getName());
      }
      display.dispose();
   }
}
