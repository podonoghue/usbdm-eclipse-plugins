package net.sourceforge.usbdm.configEditor.parser;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.configEditor.information.DeviceInfo;
import net.sourceforge.usbdm.configEditor.information.DeviceInfo.DeviceFamily;
import net.sourceforge.usbdm.configEditor.information.PeripheralFunction;
import net.sourceforge.usbdm.configEditor.information.PeripheralTemplateInformation;

/**
 * This template is used when no matching template can be found
 */
public class MiscellaneousPeripheralTemplateInformation extends PeripheralTemplateInformation {

   public MiscellaneousPeripheralTemplateInformation(DeviceInfo deviceInfo, DeviceFamily deviceFamily) {
      super(deviceInfo, "Misc", "MISC", "", new WriterForMiscellaneous(deviceFamily));
   }

   final class PinDescription {
      Pattern pattern;
      Boolean include;
      
      PinDescription(String regex) {
         this.pattern = Pattern.compile(regex);
         this.include = false;
      }
      PinDescription(String regex, boolean include) {
         this.pattern = Pattern.compile(regex);
         this.include = include;
      }
   }

   final PinDescription[] altPinNamePatterns = {
         new PinDescription("^\\s*(Disabled)()()\\s*$", true),
         new PinDescription("^\\s*(PT)([A-Z])(\\d+)\\s*$", true),
         new PinDescription("^\\s*(GPIO)([A-Z])_(\\d+)\\s*$", true),
         new PinDescription("^\\s*(FTM|TPM)(\\d+)_(CH\\d+)\\s*$", true),
         new PinDescription("^\\s*(FTM)(\\d+)_(QD_PH[A|B]|FLT2|CLKIN[0-1]|FLT[0-9])\\s*$", true),
         new PinDescription("^\\s*(SPI)(\\d+)_(SOUT|SIN|SCK|SS|(PCS(\\d+)?)|MOSI|MISO|SS_B)\\s*$", true),
         new PinDescription("^\\s*(I2C)(\\d+)_((SDA)|(SCL|4WSCLOUT|4WSDAOUT))\\s*$", true),
         new PinDescription("^\\s*(LPTMR)(\\d+)_ALT(\\d+)\\s*$", true),
         new PinDescription("^\\s*(LPTMR)()(_ALT\\d+)\\s*$", true),
         new PinDescription("^\\s*(UART)(\\d+)_(CTS_b|RTS_b|COL_b|RX|TX)\\s*$", true),
         new PinDescription("^\\s*(LPUART)(\\d+)_(CTS_b|RTS_b|COL_b|RX|TX)\\s*$", true),      
         new PinDescription("^\\s*(TSI)(\\d+)_(CH\\d+)\\s*$", true),
         new PinDescription("^\\s*(LLWU)()_(P\\d+)\\s*$", true),
         new PinDescription("^\\s*(SCI)(\\d+)_(RTS|CTS|TxD|RxD)\\s*$", true),
   };
         final PinDescription[] pinNamePatterns = {
         new PinDescription("^\\s*(ADC)(\\d+)_(?:DM|DP|SE)(\\d+[ab]?)\\s*$", true),
         new PinDescription("^\\s*(FTM|TPM)()_(CLKIN\\d+)\\s*$", true),
         new PinDescription("^\\s*(SDHC)(\\d+)_((CLKIN)|(D\\d)|(CMD)|(DCLK))\\s*$", true),
         new PinDescription("^\\s*(I2S)(\\d+)_(TX_BCLK|TXD[0-1]|RXD[0-1]|TX_FS|RX_BCLK|MCLK|RX_FS|TXD1)\\s*$", true),
         new PinDescription("^\\s*(A?CMP)(\\d+)_((IN\\d*)|(OUT\\d*))\\s*$", true),
         new PinDescription("^\\s*(JTAG)()_(TCLK|TDI|TDO|TMS|TRST_b)\\s*$", true),
         new PinDescription("^\\s*(SWD)()_(CLK|DIO|IO)\\s*$", true),
         new PinDescription("^\\s*(EZP)()_(CLK|DI|DO|CS_b)\\s*$", true),
         new PinDescription("^\\s*(TRACE)()_(SWO)\\s*$", true),
         new PinDescription("^\\s*(NMI)()_[bB]()\\s*$", true),
         new PinDescription("^\\s*(USB\\d*)(\\d*)_(CLKIN|SOF_OUT|DP|DM)\\s*$", true),
         new PinDescription("^\\s*(E?XTAL(?:32K?)?)(\\d*)()\\s*$", true),
         new PinDescription("^\\s*(EWM)()_(IN|OUT_b|OUT)\\s*$", true),
         new PinDescription("^\\s*(PDB)(\\d+)_(EXTRG)\\s*$", true),
         new PinDescription("^\\s*(CMT)(\\d*)_(IRO)\\s*$", true),
         new PinDescription("^\\s*(RTC)(\\d*)_(CLKOUT|CLKIN|WAKEUP_B)\\s*$", true),
         new PinDescription("^\\s*(DAC)(\\d+)_(OUT)\\s*$", true),
         new PinDescription("^\\s*(VREF)(\\d*)_(OUT)\\s*$", true),
         new PinDescription("^\\s*(CLKOUT)()()\\s*$", true),
         new PinDescription("^\\s*(TRACE)()_(CLKOUT|D[0-3])\\s*$", true),
         new PinDescription("^\\s*(CLKOUT32K)()()\\s*$", true),
         new PinDescription("^\\s*(R?MII)(\\d+)_(RXCLK|RXER|RXD[0-4]|CRS_DV|RXDV|TXEN|TXD[0-4]|TXCLK|CRS|TXER|COL|MDIO|MDC)\\s*$", true),
         new PinDescription("^\\s*(CAN)(\\d+)_(TX|RX)\\s*$", true),
         new PinDescription("^\\s*(FB)()_((AD?(\\d+))|OE_b|RW_b|CS[0-5]_b|TSIZ[0-1]|BE\\d+_\\d+_BLS\\d+_\\d+_b|TBST_b|TA_b|ALE|TS_b)\\s*$", true),
         new PinDescription("^\\s*(ENET)(\\d*)_(1588_TMR[0-3]|CLKIN|1588_CLKIN)\\s*$", true),
         new PinDescription("^\\s*(KBI)(\\d+)_(P\\d+)\\s*$", true),
         new PinDescription("^\\s*(IRQ)()()\\s*$", true),
         new PinDescription("^\\s*(RESET_[b|B])()()\\s*$", true),
         new PinDescription("^\\s*(BUSOUT)()()\\s*$", true),
         new PinDescription("^\\s*(RTCCLKOUT)()()\\s*$", true),
         new PinDescription("^\\s*(AFE)()_(CLK)\\s*$", true),
         new PinDescription("^\\s*(EXTRG)()_(IN)\\s*$", true),
         new PinDescription("^\\s*(CMP)(\\d)(OUT|P[0-9])\\s*$", true),
         new PinDescription("^\\s*(TCLK)(\\d+)()\\s*$", true),
         new PinDescription("^\\s*(PWT)()_(IN\\d+)\\s*$", true),
         new PinDescription("^\\s*(LCD)()_(P\\d+)(_fault)?\\s*$", true),
         new PinDescription("^\\s*(LCD)()(\\d+)\\s*$", true),
         new PinDescription("^\\s*(QT)(\\d+)()\\s*$", true),
         new PinDescription("^\\s*(audioUSB)()_(SOF_OUT)\\s*$", true),
         new PinDescription("^\\s*(PXBAR)()_((IN\\d+)|(OUT\\d+))\\s*$", true),
         new PinDescription("^\\s*(LGPIOI)()_(M\\d+)\\s*$", true),
         new PinDescription("^\\s*(SDAD)()((M|P)[0-3])\\s*$", true),
         new PinDescription("^\\s*(FXIO)(\\d+)_(D\\d+)\\s*$", true),
         new PinDescription("^\\s*(VOUT33|VREGIN)()()\\s*$", true),
   };

   @Override
   public PeripheralFunction appliesTo(DeviceInfo factory, String name) {         
      PeripheralFunction peripheralFunction = null;
      for (PinDescription pinNamePattern:pinNamePatterns) {
         Matcher matcher = pinNamePattern.pattern.matcher(name);
         if (!matcher.matches()) {
            continue;
         }
         peripheralFunction = factory.createPeripheralFunction(name, matcher.group(1), matcher.group(2), matcher.group(3));
      }
      if (peripheralFunction == null) {
         System.err.println("Warning - Couldn't find " + name);
      }
      for (PinDescription pinNamePattern:altPinNamePatterns) {
         Matcher matcher = pinNamePattern.pattern.matcher(name);
         if (!matcher.matches()) {
            continue;
         }
         peripheralFunction = factory.createPeripheralFunction(name, matcher.group(1), matcher.group(2), matcher.group(3));
      }
      return peripheralFunction;
   }

}
