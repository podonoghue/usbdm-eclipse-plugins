package net.sourceforge.usbdm.annotationEditor.validators;

import net.sourceforge.usbdm.annotationEditor.AnnotationModel.NumericOptionModelNode;
import net.sourceforge.usbdm.annotationEditor.AnnotationModel.Severity;
import net.sourceforge.usbdm.annotationEditor.Message;
import net.sourceforge.usbdm.annotationEditor.MyValidator;

import org.eclipse.jface.viewers.TreeViewer;

public class ClockValidate_USB extends MyValidator {

   private final static long DEFAULT_USB_CLOCK_FREQ = 48000000;

   private final long USB_CLOCK_FREQ;

   public enum ClockModes {NONEClock, FEIClock, FEEClock, FBIClock, BLPIClock, FBEClock, BLPEClock, PBEClock,  PEEClock};

   class ClockDividerTuple { 
      int frac; 
      int div;
      ClockDividerTuple(int frac, int div) {
         this.frac=frac; this.div=div;
      } 
      long getUsbClock(long inputClock) {
         return (inputClock * (frac+1))/(div+1);
      }
      int getClkMask() {
         return (div<<1)|frac;
      }
   };
   final ClockDividerTuple values[] = {
         new ClockDividerTuple(1,0), new ClockDividerTuple(0,0), new ClockDividerTuple(1,2), new ClockDividerTuple(0,1), 
         new ClockDividerTuple(1,4), new ClockDividerTuple(0,2), new ClockDividerTuple(1,6), new ClockDividerTuple(0,3), 
         new ClockDividerTuple(0,4), new ClockDividerTuple(0,5), new ClockDividerTuple(0,6), new ClockDividerTuple(0,7), 
   };

   public ClockValidate_USB(long usbClock) {
      USB_CLOCK_FREQ  = usbClock;
   }

   public ClockValidate_USB() {
      this(DEFAULT_USB_CLOCK_FREQ);
   }

   @Override
   public void validate(TreeViewer viewer) throws Exception {
      super.validate(viewer);

      NumericOptionModelNode sim_sopt2_pllfllselNode   =  getNumericModelNode("sim_sopt2_pllfllsel");
      NumericOptionModelNode sim_clkdiv2_usbNode       =  getNumericModelNode("sim_clkdiv2_usb");
      NumericOptionModelNode sim_sopt2_usbsrcNode      =  getNumericModelNode("sim_sopt2_usbsrc");

      if (sim_clkdiv2_usbNode == null) {
         return;
      }

      long   inputClock           = 0;
      long   pllFllSelect         = sim_sopt2_pllfllselNode.safeGetValueAsLong();
      String usbClockSource       = null;
      Message sim_clkdiv2_message = null;
      int     sim_clkdiv2_value   = 0;

      if (sim_sopt2_usbsrcNode.getValueAsLong() == 0) {
         /*
          * Externally provided clock - No clock divider
          */
         usbClockSource      = "External USB clock (USB_CLKIN)";
         inputClock          = getNumericModelNode("system_usb_clkin_clock").getValueAsLong();
         sim_clkdiv2_message = new Message("Clock source = USB_CLKIN, no divider used", Severity.INFORMATION);
      }
      else {
         // Determine clock source
         switch((int)pllFllSelect) {
         case 0 : // FLL
            usbClockSource       = "FLL";
            sim_clkdiv2_message  = new Message("FLL clock does not meet USB stability specification", Severity.WARNING);
            inputClock           = getNumericModelNode("fllTargetFrequency").getValueAsLong();
            break;
         case 1 : // PLL
            usbClockSource       = "PLL";
            sim_clkdiv2_message  = new Message("Clock source = PLL", Severity.INFORMATION);
            inputClock           = getNumericModelNode("pllTargetFrequency").getValueAsLong();
            break;
         case 3: // IRC48MHz (on some targets only)
            NumericOptionModelNode node = getNumericModelNode("irc48m_clock");
            if (node != null) {
               usbClockSource       = "Internal reference clock (48MHz)";
               sim_clkdiv2_message  = new Message("Clock source = IRC48MHz", Severity.INFORMATION);
               inputClock           = getNumericModelNode("irc48m_clock").getValueAsLong();
            }
            else {
               // Assume that irc48m_clock is not available so invalid option
               sim_clkdiv2_message = new Message("Invalid clock source in SIM_SOPT2[PLLFLLSEL] (= 3, irc48m not available)");
            }
            break;
         default :
            sim_clkdiv2_message = new Message(String.format("Invalid clock source in SIM_SOPT2[PLLFLLSEL] (= %d)", pllFllSelect));
            break;
         }
         if (inputClock > 0) {
            boolean foundValue = false;
            for (ClockDividerTuple t:values) {
               if (t.getUsbClock(inputClock) == USB_CLOCK_FREQ) {
                  sim_clkdiv2_value = t.getClkMask();
                  foundValue        = true;
                  break;
               }
            }
            if (!foundValue) {
               sim_clkdiv2_message = new Message(String.format(
                     "No suitable clock divisor for input frequency %d, from %s", 
                     inputClock,
                     usbClockSource));
            }
         }
      }
      update(viewer, sim_clkdiv2_usbNode, sim_clkdiv2_value, sim_clkdiv2_message);
   }

}
