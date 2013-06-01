package net.sourceforge.usbdm.connections.usbdm;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import net.sourceforge.usbdm.connections.usbdm.UsbdmCommon.BdmOptions;

public class USBDMAdvancedOptionsDialogue extends Dialog {

   protected Boolean result;
   protected Shell shlBdmAdvancedOptions;
   
   private NumberTextAdapter textPowerOffDurationAdapter;
   private NumberTextAdapter textPowerOnRecoveryIntervalAdapter;
   private NumberTextAdapter textResetDurationAdapter;
   private NumberTextAdapter textResetReleaseIntervalAdapter;
   private NumberTextAdapter textResetRecoveryIntervalAdapter;
   BdmOptions bdmOptions;
   public BdmOptions getBdmOptions() {
      return bdmOptions;
   }
   public void setBdmOptions(BdmOptions bdmOptions) {
      this.bdmOptions = bdmOptions;
   }
  
   /**
    * Create the dialog.
    * @param parent
    * @param style
    */
   public USBDMAdvancedOptionsDialogue(Shell parent, int style, BdmOptions bdmOptions) {
      super(parent, style);
      System.err.println("UsbdmConnectionPanelPrefsBlockProvider::getPrefsDataBlock()");
      setText("USBDM Advanced Settings");
      this.bdmOptions = bdmOptions;
   }

   /**
    * Open the dialog.
    * @return the result
    */
   public Object open() {
      createContents();
      shlBdmAdvancedOptions.open();
      shlBdmAdvancedOptions.layout();
      Display display = getParent().getDisplay();
      while (!shlBdmAdvancedOptions.isDisposed()) {
         if (!display.readAndDispatch()) {
            display.sleep();
         }
      }
      return result;
   }

   /**
    * Create contents of the dialog.
    */
   private void createContents() {
      shlBdmAdvancedOptions = new Shell(getParent(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
      shlBdmAdvancedOptions.setSize(300, 343);
      shlBdmAdvancedOptions.setText("BDM Advanced Options");
      shlBdmAdvancedOptions.setLayout(new GridLayout(1, false));

      Group grpBdmOptions = new Group(shlBdmAdvancedOptions, SWT.NONE);
      grpBdmOptions.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
      grpBdmOptions.setText("BDM Parameters");
      grpBdmOptions.setLayout(new GridLayout(3, false));
      
      //==================
      new Label(grpBdmOptions, SWT.NONE).setText("Power Off duration");
      Text textPowerOffDuration = new Text(grpBdmOptions, SWT.BORDER);
      textPowerOffDuration.setToolTipText("Duration to remove Power");
      textPowerOffDurationAdapter = new NumberTextAdapter("Power Off duration", textPowerOffDuration, bdmOptions.powerOffDuration);
      textPowerOffDuration.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
      new Label(grpBdmOptions, SWT.NONE).setText("ms");

      //==================
      new Label(grpBdmOptions, SWT.NONE).setText("Power On Recovery interval");
      Text textPowerOnRecoveryInterval = new Text(grpBdmOptions, SWT.BORDER);
      textPowerOnRecoveryInterval.setToolTipText("Interval to wait after power on");
      textPowerOnRecoveryIntervalAdapter = new NumberTextAdapter("Power On Recovery interval", textPowerOnRecoveryInterval, bdmOptions.powerOnRecoveryInterval);
      textPowerOnRecoveryInterval.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
      new Label(grpBdmOptions, SWT.NONE).setText("ms");
      
      //==================
      new Label(grpBdmOptions, SWT.NONE).setText("Reset duration");
      Text textResetDuration = new Text(grpBdmOptions, SWT.BORDER);
      textResetDuration.setToolTipText("Duration to reset assertion");
      textResetDurationAdapter = new NumberTextAdapter("Reset duration", textResetDuration, bdmOptions.resetDuration);
      textResetDuration.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
      new Label(grpBdmOptions, SWT.NONE).setText("ms");

      //==================
      new Label(grpBdmOptions, SWT.NONE).setText("Reset Release interval");
      Text textResetReleaseInterval = new Text(grpBdmOptions, SWT.BORDER);
      textResetReleaseInterval.setToolTipText("Interval to wait after reset before changing signals");
      textResetReleaseIntervalAdapter = new NumberTextAdapter("Reset Release interval", textResetReleaseInterval, bdmOptions.resetReleaseInterval);
      textResetReleaseInterval.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
      new Label(grpBdmOptions, SWT.NONE).setText("ms");
      
      //==================
      new Label(grpBdmOptions, SWT.NONE).setText("Reset Recovery interval");
      Text textResetRecoveryInterval = new Text(grpBdmOptions, SWT.BORDER);
      textResetRecoveryInterval.setToolTipText("Interval to wait after reset for recovery");
      textResetRecoveryIntervalAdapter = new NumberTextAdapter("Reset Recovery interval", textResetRecoveryInterval, bdmOptions.resetRecoveryInterval);
      textResetRecoveryInterval.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
      new Label(grpBdmOptions, SWT.NONE).setText("ms");
      
      Label lblNewLabel_2 = new Label(shlBdmAdvancedOptions, SWT.NONE);
      lblNewLabel_2.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true, 1, 1));
      
      Composite composite = new Composite(shlBdmAdvancedOptions, SWT.NONE);
      composite.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false, 1, 1));
      composite.setLayout(new GridLayout(3, true));
      
      //====================
      Label lblNewLabel = new Label(composite, SWT.NONE);
      lblNewLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));     
      
      //====================
      Button btnOK = new Button(composite, SWT.NONE);
      GridData gd_btnOK = new GridData(SWT.LEFT, SWT.BOTTOM, false, false, 1, 1);
      gd_btnOK.widthHint = 80;
      btnOK.setLayoutData(gd_btnOK);
      btnOK.setText("OK");
      btnOK.addSelectionListener(new SelectionAdapter() {
         public void widgetSelected(SelectionEvent e) {
            if (!validate()) {
               return;
            }
            bdmOptions.powerOffDuration        = textPowerOffDurationAdapter.getDecimalValue();
            bdmOptions.powerOnRecoveryInterval = textPowerOnRecoveryIntervalAdapter.getDecimalValue();
            bdmOptions.resetDuration           = textResetDurationAdapter.getDecimalValue();
            bdmOptions.resetReleaseInterval    = textResetReleaseIntervalAdapter.getDecimalValue();
            bdmOptions.resetRecoveryInterval   = textResetRecoveryIntervalAdapter.getDecimalValue();
            shlBdmAdvancedOptions.close();
         }
      });

      //====================
      Button btnCancel = new Button(composite, SWT.NONE);
      GridData gd_btnCancel = new GridData(SWT.LEFT, SWT.BOTTOM, false, false, 1, 1);
      gd_btnCancel.widthHint = 80;
      btnCancel.setLayoutData(gd_btnCancel);
      btnCancel.setText("Cancel");
      btnCancel.addSelectionListener(new SelectionAdapter() {
         public void widgetSelected(SelectionEvent e) {
            shlBdmAdvancedOptions.close();         }
      });
   }

   public boolean validate() {
      return textPowerOffDurationAdapter.validate() &&
             textPowerOnRecoveryIntervalAdapter.validate() &&
             textResetDurationAdapter.validate() &&
             textResetReleaseIntervalAdapter.validate() &&
             textResetRecoveryIntervalAdapter.validate();
   }
}
