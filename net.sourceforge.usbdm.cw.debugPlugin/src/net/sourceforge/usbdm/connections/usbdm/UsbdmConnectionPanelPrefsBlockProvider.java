package net.sourceforge.usbdm.connections.usbdm;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;

import com.freescale.cdt.debug.cw.core.ui.publicintf.ICustomPhysicalConnectionPrefsBlockProvider;

public class UsbdmConnectionPanelPrefsBlockProvider
  implements ICustomPhysicalConnectionPrefsBlockProvider
{
  public static final String PhysicalConnectionAttributeBase = 
     "com.freescale.cdt.debug.cw.core.settings.GdiConnection.Common.PhysicalConnectionAttributeBase";
  private Long usbdmIdCount = Long.valueOf(0L);

  public byte[] getPrefsDataBlock(ILaunchConfiguration configuration) {
//    System.err.println("UsbdmConnectionPanelPrefsBlockProvider::getPrefsDataBlock()");
    String usbdmPrefix = "";
    try  {
      usbdmPrefix = configuration.getAttribute(PhysicalConnectionAttributeBase, "USBDM");
    } catch (CoreException coreEx) {
      usbdmPrefix = "USBDM";
    }

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream dos = new DataOutputStream(baos);
    try
    {
      dos.writeBytes(usbdmPrefix);
      dos.writeLong(this.usbdmIdCount.longValue());
      dos.flush();
    } catch (IOException localIOException) {
    }
    byte[] bytes = baos.toByteArray();
    return bytes;
  }
}
