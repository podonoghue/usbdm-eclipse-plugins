package net.sourceforge.usbdm.deviceDatabase.ui;

import net.sourceforge.usbdm.deviceDatabase.Device;
import net.sourceforge.usbdm.jni.Usbdm.TargetType;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
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
   TargetType          targetType   = null;
   String              targetName   = null;
   Device              targetDevice = null;
   
   /**
    * Create dialog
    * 
    * @param parent     Parent dialog
    * @param targetType Target type - Used to determine which device list to display
    * @param targetName Target name - Used to determine initially selected device
    */
   public DeviceSelector(Shell parent, TargetType targetType, String targetName) {
      super(parent);
      this.targetType = targetType;
      this.targetName = targetName;
   }
   
   /**
    * Set target type<br>
    * Used to determine which device list to display
    * 
    * @param targetType
    */
   public void setTargetType(TargetType targetType) {
      this.targetType = targetType;
   }
   
   /**
    * Set device name<br>
    * Used to determine initially selected device
    *  
    * @param targetName
    */
   public void setDevice(String targetName) {
      this.targetName = targetName;
   }
   
   /* (non-Javadoc)
    * @see org.eclipse.jface.dialogs.Dialog#createContents(org.eclipse.swt.widgets.Composite)
    */
   @Override
   protected Control createDialogArea(Composite parent) {
      Composite comp = (Composite)super.createDialogArea(parent);
      deviceSelectorPanel = new DeviceSelectorPanel(comp, SWT.NONE);
      deviceSelectorPanel.setTargetType(targetType);
      deviceSelectorPanel.setDevice(targetName);
      GridDataFactory.generate(deviceSelectorPanel, 1, 1);
      
      deviceSelectorPanel.addListener(SWT.CHANGED, new Listener() {
         @Override
         public void handleEvent(Event event) {
            validate();
         }
      });
      return comp;
   }

   void validate() {
      if (deviceSelectorPanel != null) {
         setMessage(deviceSelectorPanel.validate());
         getButton(IDialogConstants.OK_ID).setEnabled(deviceSelectorPanel.validate() == null);
         targetName   = deviceSelectorPanel.getText();
         targetDevice = deviceSelectorPanel.getDevice();
      }
   }
   
   /* (non-Javadoc)
    * @see org.eclipse.jface.dialogs.Dialog#create()
    */
   @Override
   public void create() {
      super.create();
      setTitle("Select Device");
      validate();
   }

   /*
    * (non-Javadoc)
    * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
    */
   @Override
   protected void configureShell(Shell newShell) {
     super.configureShell(newShell);
     newShell.setText("USBDM Device Selection");
   }

   /*
    * (non-Javadoc)
    * @see org.eclipse.jface.dialogs.Dialog#getInitialSize()
    */
   @Override
   protected Point getInitialSize() {
     return new Point(300, 500);
   }
   
   /*
    * (non-Javadoc)
    * @see org.eclipse.jface.dialogs.Dialog#isResizable()
    */
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
      return targetName;
   }
   
   /**
    * Get selected device
    * 
    * @return Device
    */
   public Device getDevice() {
      return targetDevice;
   }
   
   public static void main(String[] args) {
      Display display = new Display();
      Shell shell = new Shell(display);
      shell.setBackground(new Color(display, 255,0,0));
      
      DeviceSelector deviceSelector = new DeviceSelector(shell, TargetType.T_ARM, null);//"MK20DX128M5");
      deviceSelector.open();

      display.dispose();
   }
}
