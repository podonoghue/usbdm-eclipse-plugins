package net.sourceforge.usbdm.peripherals.view;

import java.util.HashMap;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.peripherals.model.BaseModel;

import org.eclipse.cdt.dsf.concurrent.DataRequestMonitor;
import org.eclipse.cdt.dsf.datamodel.DMContexts;
import org.eclipse.cdt.dsf.datamodel.IDMContext;
import org.eclipse.cdt.dsf.debug.service.IBreakpoints.IBreakpointsTargetDMContext;
import org.eclipse.cdt.dsf.debug.service.IStack.IFrameDMContext;
import org.eclipse.cdt.dsf.debug.service.command.ICommandControlService.ICommandControlDMContext;
import org.eclipse.cdt.dsf.mi.service.IMIExecutionDMContext;
import org.eclipse.cdt.dsf.mi.service.command.output.MIThread;
import org.eclipse.cdt.dsf.mi.service.command.output.MIThreadInfoInfo;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPage;

/**
 * @author podonoghue
 *
 */
public class FaultDialogue extends TitleAreaDialog {  

   private final long CFSR_ADDRESS  = 0xE000ED28L;
   private final long HFSR_ADDRESS  = 0xE000ED2CL;
   private final long DFSR_ADDRESS  = 0xE000ED30L;
   private final long MMFAR_ADDRESS = 0xE000ED34L;
   private final long BFAR_ADDRESS  = 0xE000ED38L;
   private final long ICSR_ADDRESS  = 0xE000ED04L;

   private LocalResourceManager resManager = null;
   private HashMap<String, Image> imageCache = new HashMap<String,Image>();

   public FaultDialogue(Shell parentShell) {
      super(parentShell);
      setHelpAvailable(false);
   }

   private long cfsr;
   private long hfsr;
   private long dfsr;
   private long mmfar;
   private long bfar;
   private long icsr;
   private long stackedPC;
   private long stackedLR;
   private long stackedR0; 
   private long stackedR1; 
   private long stackedR2; 
   private long stackedR3; 
   private long stackedR12;
   private long R4;
   private long R5;
   private long R6;
   private long R7;
   private long R8;
   private long R9;
   private long R10;
   private long R11;
   private long SP;
   private long MSP;
   private long PSP;

   private boolean exceptionFrameValid = false;
   @SuppressWarnings("unused")
   private boolean floatingFrameValid  = false;
   
   Pattern stringPattern = Pattern.compile(".*?((0x([0-9a-fA-F]+))|(0[0-7]*)|([1-9][0-9]*)).*?");

   /**
    * @param cfsr
    * @param hfsr
    * @param dfsr
    * @param mmfar
    * @param bfar
    */
   private void create(long cfsr, long hfsr, long dfsr, long mmfar, long bfar) {
      this.cfsr  = cfsr;
      this.hfsr  = hfsr;
      this.dfsr  = dfsr;
      this.mmfar = mmfar;
      this.bfar  = bfar;
      exceptionFrameValid = false;
      floatingFrameValid  = false;

      super.create();
      setTitle("Fault Report");
      if (exceptionFrameValid) {
         setMessage("Description of Target Fault state", IMessageProvider.INFORMATION);
      }
      else {
         setMessage("Target is not in Exception Processing state", IMessageProvider.WARNING);
      }
   }
   
   /**
    * 
    * @param imageId
    * @return
    */
   public Image getMyImage(String imageId) {
      Image image = imageCache.get(imageId);
      if ((Activator.getDefault() != null) && (image == null)) {
         ImageDescriptor imageDescriptor  = Activator.getDefault().getImageDescriptor(imageId);
         image = resManager.createImage(imageDescriptor);
         imageCache.put(imageId, image);
      }
      return image;
   }
   

   IDMContext getIDMContext() {
      IDMContext idmContext = null;
      IAdaptable debugContext = DebugUITools.getDebugContext();
      if (debugContext != null) {
         idmContext = (IDMContext)debugContext.getAdapter(IDMContext.class);
      }
      return idmContext;
   }

   IMIExecutionDMContext getExecutionContext(IDMContext idmContext) {
      return DMContexts.getAncestorOfType(idmContext, IMIExecutionDMContext.class);
   }

   IBreakpointsTargetDMContext getBreakpointContext(IMIExecutionDMContext executionContext) {
      return  DMContexts.getAncestorOfType(executionContext, IBreakpointsTargetDMContext.class);
   }

   ICommandControlDMContext getControlContext(IDMContext idmContext) {
      return  DMContexts.getAncestorOfType(idmContext, ICommandControlDMContext.class);
   }
   
   /**
    * @param gdbInterface
    */
   public void create(GdbCommonInterface gdbInterface) {
      if (gdbInterface == null) {
         return;
      }
      
      
      IDMContext dmContext = getIDMContext();

//      // TODO
//      ICommandControlDMContext commandControlDMContext = getControlContext(dmContext);
//      DataRequestMonitor<MIThreadInfoInfo> drm = new DataRequestMonitor<MIThreadInfoInfo>(fGdb.getExecutor(), null);
//
//      ILaunch launch = (ILaunch)dsfSession.getModelAdapter(ILaunch.class);
//
//      fGdb.queueCommand(factory.createMIThreadInfo(commandControlDMContext, "1"),
//            new DataRequestMonitor<MIThreadInfoInfo>(fGdb.getExecutor(), drm));
//      
//      MIThreadInfoInfo data = drm.getData();
//      MIThread[] threads = data.getThreadList();
//      for (MIThread thread:threads) {
//         ISourceLookupResult result = DebugUITools.lookupSource(thread.getTopFrame().getFile(), launch.getSourceLocator());
//         IWorkbenchPage page=PTPDebugUIPlugin.getActiveWorkbenchWindow().getActivePage();
//         DebugUITools.displaySource(result, page);
//      }

      
      
      try {
         // Get hardware registers
         cfsr  = BaseModel.getValue32bit(gdbInterface.readMemory(CFSR_ADDRESS,  4, 32));
         hfsr  = BaseModel.getValue32bit(gdbInterface.readMemory(HFSR_ADDRESS,  4, 32)); 
         dfsr  = BaseModel.getValue32bit(gdbInterface.readMemory(DFSR_ADDRESS,  4, 32));
         mmfar = BaseModel.getValue32bit(gdbInterface.readMemory(MMFAR_ADDRESS, 4, 32)); 
         bfar  = BaseModel.getValue32bit(gdbInterface.readMemory(BFAR_ADDRESS,  4, 32));
         icsr  = BaseModel.getValue32bit(gdbInterface.readMemory(ICSR_ADDRESS,  4, 32));

         exceptionFrameValid = false;
         floatingFrameValid  = false;

         IFrameDMContext exceptionStackFrameContext = gdbInterface.getExceptionStackFrameContext();
//         gdbInterface.setFrame(0);
         if (exceptionStackFrameContext != null) {
            long lr = gdbInterface.evaluateExpression(null, exceptionStackFrameContext, "$lr");
            if ((lr&0xFFFFFF00L) == 0xFFFFFF00L) {
               switch ((int)(lr&0xFF)) {
               case 0xE1: // Handler mode, Main + FP Extended frame
               case 0xE9: // Thread mode, Main + FP Extended frame
                  exceptionFrameValid = true;
                  floatingFrameValid  = true;
                  break;
               case 0xF1: // Handler mode, Main
               case 0xF9: // Thread mode, Main 
                  exceptionFrameValid = true;
                  break;
               case 0xED: // Thread mode, Process + FP Extended frame
                  exceptionFrameValid = true;
                  floatingFrameValid  = true;
                  break;
               case 0xFD: // Thread mode, Process
                  exceptionFrameValid = true;
                  break;
               default:
                  break;
               }
            }
            if (exceptionFrameValid) {
               SP = gdbInterface.evaluateExpression(null, exceptionStackFrameContext, "$sp");
               R7 = gdbInterface.evaluateExpression(null, exceptionStackFrameContext, "$r7");

               // Get stack related values (saved regs)
               MSP  = gdbInterface.evaluateExpression(null, exceptionStackFrameContext, "$msp");
               PSP  = gdbInterface.evaluateExpression(null, exceptionStackFrameContext, "$psp");

               // Determine stack in use at exception - assumes standard entry via _HardFault_Handler()
               long exceptionFramePtr = -1;;
               if ((lr & 0x04) == 0){
                  exceptionFramePtr = SP;
               }
               else {
                  exceptionFramePtr = PSP;
               }
               System.err.println(String.format("exceptionFramePtr = 0x%08X", exceptionFramePtr));

               // Recover register from stack frame
               stackedR0  = BaseModel.getValue32bit(gdbInterface.readMemory(exceptionFramePtr+0,  4, 32));
               stackedR1  = BaseModel.getValue32bit(gdbInterface.readMemory(exceptionFramePtr+4,  4, 32));
               stackedR2  = BaseModel.getValue32bit(gdbInterface.readMemory(exceptionFramePtr+8,  4, 32));
               stackedR3  = BaseModel.getValue32bit(gdbInterface.readMemory(exceptionFramePtr+12, 4, 32));
               stackedR12 = BaseModel.getValue32bit(gdbInterface.readMemory(exceptionFramePtr+16, 4, 32));
               stackedLR  = BaseModel.getValue32bit(gdbInterface.readMemory(exceptionFramePtr+20, 4, 32));
               stackedPC  = BaseModel.getValue32bit(gdbInterface.readMemory(exceptionFramePtr+24, 4, 32));

               // Get unchanged registers
               R4         = gdbInterface.evaluateExpression(null, exceptionStackFrameContext, "$r4");
               R5         = gdbInterface.evaluateExpression(null, exceptionStackFrameContext, "$r5");
               R6         = gdbInterface.evaluateExpression(null, exceptionStackFrameContext, "$r6");
               R8         = gdbInterface.evaluateExpression(null, exceptionStackFrameContext, "$r8");
               R9         = gdbInterface.evaluateExpression(null, exceptionStackFrameContext, "$r9");
               R10        = gdbInterface.evaluateExpression(null, exceptionStackFrameContext, "$r10");
               R11        = gdbInterface.evaluateExpression(null, exceptionStackFrameContext, "$r11");
               SP         = exceptionFramePtr+24;
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
      super.create();
      setTitle("Exception Report");
      if (exceptionFrameValid) {
         setMessage("Description of Target Exception state", IMessageProvider.INFORMATION);
      }
      else {
         setMessage("Target is not in Exception Processing state", IMessageProvider.WARNING);
      }
   }

   /**
    * 
    * @param selected
    * @return
    */
   private Image getSelectionImage(boolean selected) {
      if (selected) {
         return getMyImage(Activator.ID_CHECKBOX_CHECKED_IMAGE);
      }
      else {
         return getMyImage(Activator.ID_CHECKBOX_UNCHECKED_IMAGE);
      }
   }
   
   /**
    * Creates a Composite containing a pseudo-check-box and label which is added to parent
    * 
    * @param parent
    * @param text    - Text to display
    * @param value   - Whether pseudo-check-box is selected
    * 
    * @return value - so tooltip may be added
    */
   Control makeExceptionNumber(Composite parent, int exceptionNumber) {
      Composite area    = new Composite(parent, SWT.None);
      RowLayout rl      = new RowLayout();
      rl.pack           = true;
      rl.marginLeft     = 0;
      rl.marginRight    = 0;
      rl.marginTop      = 0;
      rl.marginBottom   = 0;
      area.setLayout(rl);
      
      Label image = new Label(area, SWT.NONE);
      image.setImage(getSelectionImage(exceptionNumber != 0));
      
      Text label = new Text(area, SWT.BORDER|SWT.READ_ONLY);
      label.setText(String.format("# %d", exceptionNumber));
      
      String toolTip = "Exception number";
      String exceptionName = getExceptionName(exceptionNumber);
      if (exceptionName != null) {
         toolTip += "\n" + exceptionName;
      }
      label.setToolTipText(toolTip);

      return label;
   }

   /**
    * Creates a Composite containing a pseudo-check-box and label which is added to parent
    * 
    * @param parent
    * @param text    - Text to display
    * @param value   - Whether pseudo-check-box is selected
    * 
    * @return value - so tooltip may be added
    */
   Control makeCheck(Composite parent, String text, boolean value) {
      Composite area = new Composite(parent, SWT.None);
      RowLayout rl = new RowLayout();
      rl.pack = true;
      rl.marginLeft = 0;
      rl.marginRight = 0;
      rl.marginTop = 0;
      rl.marginBottom = 0;
      area.setLayout(rl);
      
      Label image = new Label(area, SWT.NONE);
      image.setImage(getSelectionImage(value));
      
      Text label = new Text(area, SWT.NONE|SWT.READ_ONLY);
      label.setText(text);
      return label;
   }

   /**
    * Creates a pair of labels which are added to the given parent:
    *   - text 
    *   - value as 8-digit hex value
    * 
    * @param parent
    * @param text   Value for Text
    * @param value  Value for Value (displayed as hex number)
    * 
    * @return value - so tool-tip may be added
    */
   Control makeText(Composite parent, String text, long value) {
      Text label = new Text(parent, SWT.NONE|SWT.READ_ONLY);
      label.setText(text);

      Text valueText = new Text(parent, SWT.NONE|SWT.READ_ONLY);
      valueText.setText(String.format("0x%08X", (value&0xFFFFFFFFL)));
      return valueText;
   }

   /**
    * @param parent
    * 
    * @return 
    */
   protected Composite createRegistersGroup(Composite parent) {
      
      Composite container = new Composite(parent, SWT.FILL);

      GridLayout gridLayout = new GridLayout(1, false);
      container.setLayout(gridLayout);
      
      
      Group registersGroup = new Group(container, SWT.SHADOW_ETCHED_IN);
//      GridData gd = new GridData();
//      gd.verticalAlignment   = GridData.FILL;
//      gd.horizontalAlignment = GridData.FILL;
//      registersGroup.setLayoutData(gd);

      GridLayout gl = new GridLayout(2, false);
      registersGroup.setLayout(gl);
      registersGroup.setText("Exception registers");

      if (!exceptionFrameValid) {
         Label label = new Label(registersGroup, SWT.NONE);
         label.setText("No exception active");
         return container; 
      }
      
      Control control = null;
      
      control = makeText(registersGroup, "R0 ", stackedR0);
      control.setToolTipText("R0 value stacked during exception");

      control = makeText(registersGroup, "R1 ", stackedR1);
      control.setToolTipText("R1 value stacked during exception");

      control = makeText(registersGroup, "R2 ", stackedR2);
      control.setToolTipText("R2 value stacked during exception");

      control = makeText(registersGroup, "R3 ", stackedR3);
      control.setToolTipText("R3 value stacked during exception");

      control = makeText(registersGroup, "R4 ", R4);
      control.setToolTipText("R4 value");

      control = makeText(registersGroup, "R5 ", R5);
      control.setToolTipText("R5 value");

      control = makeText(registersGroup, "R6 ", R6);
      control.setToolTipText("R6 value");

      control = makeText(registersGroup, "R7 ", R7);
      control.setToolTipText("R7 value");

      control = makeText(registersGroup, "R8 ", R8);
      control.setToolTipText("R8 value");

      control = makeText(registersGroup, "R9 ", R9);
      control.setToolTipText("R9 value");

      control = makeText(registersGroup, "R10", R10);
      control.setToolTipText("R10 value");

      control = makeText(registersGroup, "R11", R11);
      control.setToolTipText("R11 value");

      control = makeText(registersGroup, "R12", stackedR12);
      control.setToolTipText("R12 value stacked during exception");

      control = makeText(registersGroup, "SP", SP);
      control.setToolTipText("SP value");

      control = makeText(registersGroup, "LR", stackedLR);
      control.setToolTipText("LR value stacked during exception");

      control = makeText(registersGroup, "PC", stackedPC);
      control.setToolTipText("PC value stacked during exception");

      control = makeText(registersGroup, "MSP", MSP);
      control.setToolTipText("MSP value");

      control = makeText(registersGroup, "PSP", PSP);
      control.setToolTipText("PSP value");

      return registersGroup;
   }
   
   /**
    * 
    * @param exceptionNumber
    * @return
    */
   protected String getExceptionName(int exceptionNumber) {
      /*
       * Exception Description
       */
      final String wellKnowExceptions[] = {
      "No exception",
      "Hard Reset",                          /*  1  Reset Handler                                                                    */
      "Non Maskable Interrupt",              /*  2, Non maskable Interrupt, cannot be stopped or preempted                           */
      "Hard Fault",                          /*  3, Hard Fault, all classes of Fault                                                 */
      "Memory Management",                   /*  4, Memory Management, MPU mismatch, including Access Violation and No Match         */
      "Bus Fault",                           /*  5, Bus Fault, Pre-Fetch-, Memory Access Fault, other address/memory related Fault   */
      "Usage Fault",                         /*  6, Usage Fault, i.e. Undef Instruction, Illegal State Transition                    */
      "Reserved # 7",                        /*  7, Reserved                                                                         */
      "Reserved # 8",                        /*  8, Reserved                                                                         */
      "Reserved # 9",                        /*  9, Reserved                                                                         */
      "Reserved # 10",                       /* 10, Reserved                                                                         */
      "System Service Call",                 /* 11, System Service Call via SVC instruction                                          */
      "Debug Monitor",                       /* 12, Debug Monitor                                                                    */
      "Reserved # 13",                       /* 13, Reserved                                                                         */
      "Pendable request for system service", /* 14, Pendable request for system service                                              */
      "System Tick Timer",                   /* 15, System Tick Timer                                                                */
      };
      String exceptionName = null;
      if ((exceptionNumber < wellKnowExceptions.length)) {
         exceptionName = wellKnowExceptions[exceptionNumber];
      }
      return exceptionName;
   }
   
   /**
    * Memory manage faults
    * 
    * @param container
    * @return
    */
   protected Group createMemoryManageFaults(Composite container) {
      Group memoryManageFaults = new Group(container, SWT.SHADOW_ETCHED_IN);
      GridData gd = new GridData();
      gd.verticalAlignment   = GridData.FILL;
      gd.horizontalAlignment = GridData.FILL;
      memoryManageFaults.setLayoutData(gd);

      memoryManageFaults.setLayout(new GridLayout(2, true));
      memoryManageFaults.setText("Memory Manage Faults (SCB.CFSR)");

      Label description;
      Text value;
      Control control;
      
      description = new Label(memoryManageFaults, SWT.NONE);
      description.setText("MM_FAULT_ADDR");
      value = new Text(memoryManageFaults, SWT.BORDER|SWT.READ_ONLY);
      value.setText(String.format("0x%08X", mmfar));
      value.setToolTipText("MemManage Fault Address Register");

      description = new Label(memoryManageFaults, SWT.NONE);
      description.setText("MM_FAULT_STAT");
      value = new Text(memoryManageFaults, SWT.BORDER|SWT.READ_ONLY);
      value.setText(String.format("0x%02X", (cfsr&0xFF)));
      value.setToolTipText("Fault status - see individual bits below");

      control = makeCheck(memoryManageFaults, "IACCVIOL", (cfsr&(1<<0))!= 0);
      control.setToolTipText("Access violation during instruction access");

      control = makeCheck(memoryManageFaults, "MUNSTKERR", (cfsr&(1<<3))!= 0);
      control.setToolTipText("Access violation during exception unstacking operation");

      control = makeCheck(memoryManageFaults, "DACCVIOL", (cfsr&(1<<1))!= 0);
      control.setToolTipText("Access violation during data read or write");

      control = makeCheck(memoryManageFaults, "MSTKERR", (cfsr&(1<<4))!= 0);
      control.setToolTipText("Access violation during exception stacking operation");

      control = makeCheck(memoryManageFaults, "MLSPERR", (cfsr&(1<<5))!= 0);
      control.setToolTipText("An access violation occurred during floating-point lazy state preservation");

      control = makeCheck(memoryManageFaults, "MMFARVALID", (cfsr&(1<<7))!= 0);
      control.setToolTipText("Memory Manage Fault AddressRegister Valid\n - MM_FAULT_ADDR shows a valid fault address");
      
      return memoryManageFaults;
   }
   
   /**
    * Bus faults
    * 
    * @param container
    * @return
    */
   protected Group createBusFaults(Composite container) {
      
      Group busFaults = new Group(container, SWT.SHADOW_ETCHED_IN);
      GridData gd = new GridData();
      gd.verticalAlignment   = GridData.FILL;
      gd.horizontalAlignment = GridData.FILL;
      busFaults.setLayoutData(gd);

      busFaults.setLayout(new GridLayout(2, true));
      busFaults.setText("Bus Faults (SCB.CFSR)");

      Label description;
      Text value;
      Control control;
      
      description = new Label(busFaults, SWT.NONE);
      description.setText("BUS_FAULT_ADDR");
      value = new Text(busFaults, SWT.BORDER|SWT.READ_ONLY);
      value.setText(String.format("0x%08X", bfar));
      value.setToolTipText("BusFault Address Register ");

      description = new Label(busFaults, SWT.NONE);
      description.setText("BUS_FAULT_STAT");
      value = new Text(busFaults, SWT.BORDER|SWT.READ_ONLY);
      value.setText(String.format("0x%02X", ((cfsr>>8)&0xFF)));
      value.setToolTipText("Fault status - see individual bits below");

      control = makeCheck(busFaults, "IBUSERR", ((cfsr&(8<<0))!= 0));
      control.setToolTipText("Access violation during instruction access");

      control = makeCheck(busFaults, "UNSTKERR", ((cfsr&(1<<11))!= 0));
      control.setToolTipText("BusFault on unstacking for a return from exception");

      control = makeCheck(busFaults, "PRECISERR", ((cfsr&(1<<9))!= 0));
      control.setToolTipText("Data bus error has occurred\n - PC value stacked for the exception return points to the instruction that caused the error");

      control = makeCheck(busFaults, "STKERR", ((cfsr&(1<<12))!= 0));
      control.setToolTipText("Stacking for an exception entry has caused one or more BusFaults");

      control = makeCheck(busFaults, "IMPRECISERR", ((cfsr&(1<<10))!= 0));
      control.setToolTipText("Data bus error has occurred\n - PC value in the stack frame is not related to the instruction that caused the error");

      control = makeCheck(busFaults, "BFARVALID", ((cfsr&(1<<15))!= 0));
      control.setToolTipText("BusFault Address Register Valid\n - BUS_FAULT_ADDR shows a valid fault address");

      control = makeCheck(busFaults, "LSPERR", ((cfsr&(1<<13))!= 0));
      control.setToolTipText("Bus fault occurred during floating-point lazy state preservation");
      
      return busFaults;
   }
   
   /**
    *  Usage faults
    *  
    * @param container
    * @return
    */
   protected Group createUsageFaults(Composite container) {
      Group usageFaults = new Group(container, SWT.SHADOW_ETCHED_IN);
      GridData gd = new GridData();
      gd.verticalAlignment   = GridData.FILL;
      gd.horizontalAlignment = GridData.FILL;
      usageFaults.setLayoutData(gd);

      usageFaults.setLayout(new GridLayout(2, true));
      usageFaults.setText("Usage Faults (SCB.CFSR)");

      Label description;
      Text value;
      Control control;
      
      description = new Label(usageFaults, SWT.NONE);
      description.setText("USAGE_FAULT_STAT");
      value = new Text(usageFaults, SWT.BORDER|SWT.READ_ONLY);
      value.setText(String.format("0x%04X", ((cfsr>>16)&0xFFFF)));
      value.setToolTipText("Fault status - see individual bits below");

      control = makeCheck(usageFaults, "UNDEFINSTR", ((cfsr&(1<<16))!= 0));
      control.setToolTipText("Execution of undefined instruction");

      control = makeCheck(usageFaults, "NOCP", ((cfsr&(1<<19))!= 0));
      control.setToolTipText("Access to nonexistent coprocessor");

      control = makeCheck(usageFaults, "INVSTATE", ((cfsr&(1<<17))!= 0));
      control.setToolTipText("Execute of an instruction that makes illegal use of the EPSR, e.g.\n"+
                            "- Loading a branch target address to PC with LSB=0.\n"+
                            "- Stacked PSR corrupted during exception or interrupt handling.\n"+
                            "- Vector table contains a vector address with LSB=0.");

      control = makeCheck(usageFaults, "UNALIGNED", ((cfsr&(1<<24))!= 0));
      control.setToolTipText("Unaligned memory access");

      control = makeCheck(usageFaults, "INVPC", ((cfsr&(1<<18))!= 0));
      control.setToolTipText("Illegal load of EXC_RETURN to the PC");

      control = makeCheck(usageFaults, "DIVBYZERO", ((cfsr&(1<<25))!= 0));
      control.setToolTipText("Execution of SDIV or UDIV instruction with a divisor of 0");
      
      return usageFaults;
   }
   
   /**
    * Hard faults
    * 
    * @param container
    * @return
    */
   protected Group createHardFaults(Composite container) {

      Group hardFaults = new Group(container, SWT.SHADOW_ETCHED_IN);
      GridData gd = new GridData();
      gd.verticalAlignment   = GridData.FILL;
      gd.horizontalAlignment = GridData.FILL;
      hardFaults.setLayoutData(gd);

      hardFaults.setLayout(new GridLayout(2, true));
      hardFaults.setText("Hard Faults (SCB.HFSR)");

      Label description;
      Text value;
      Control control;
      
      description = new Label(hardFaults, SWT.NONE);
      description.setText("HARD_FAULT_STAT");
      value = new Text(hardFaults, SWT.BORDER|SWT.READ_ONLY);
      value.setText(String.format("0x%08X", hfsr));
      value.setToolTipText("Fault status - see individual bits below");

      control = makeCheck(hardFaults, "DEBUGEVT", ((hfsr&(1<<31))!= 0));
      control.setToolTipText("Reserved for Debug use - must be 0");

      control = makeCheck(hardFaults, "FORCED", ((hfsr&(1<<30))!= 0));
      control.setToolTipText("Indicates a forced hard fault\n"+
                            "- Generated by escalation of a fault with configurable priority that cannot be handled");

      control = makeCheck(hardFaults, "VECTTBL", ((hfsr&(1<<1))!= 0));
      control.setToolTipText("BusFault on vector table read");
      
      return hardFaults;
   }
   
   /**
    * Debug Faults
    * 
    * @param container
    * @return
    */
   protected Group createDebugFaults(Composite container) {
      /*
       * Debug faults
       */
      Group debugFaults = new Group(container, SWT.SHADOW_ETCHED_IN);
      GridData gd = new GridData();
      gd.verticalAlignment   = GridData.FILL;
      gd.horizontalAlignment = GridData.FILL;
      debugFaults.setLayoutData(gd);

      debugFaults.setLayout(new GridLayout(2, true));
      debugFaults.setText("Debug Faults (SCB.DFSR)");

      Label description;
      Text value;
      Control control;
      
      description = new Label(debugFaults, SWT.NONE);
      description.setText("DEBUG_FAULT_STAT");
      value = new Text(debugFaults, SWT.BORDER|SWT.READ_ONLY);
      value.setText(String.format("0x%08X", dfsr));
//      value.setEnabled(false);
      value.setToolTipText("Fault status - see individual bits below");

      control = makeCheck(debugFaults, "EXTERNAL", ((dfsr&(1<<4))!= 0));
      control.setToolTipText("Debug event generated because of the assertion of EDBGRQ");

      control = makeCheck(debugFaults, "VCATCH", ((dfsr&(1<<3))!= 0));
      control.setToolTipText("Indicates triggering of a Vector catch");

      control = makeCheck(debugFaults, "DWTTRAP", ((dfsr&(1<<2))!= 0));
      control.setToolTipText("Debug event generated by the DWT");

      control = makeCheck(debugFaults, "BKPT", ((dfsr&(1<<1))!= 0));
      control.setToolTipText("Debug event generated by BKPT instruction execution or a breakpoint match in FPB");

      control = makeCheck(debugFaults, "HALTED", ((dfsr&(1<<0))!= 0));
      control.setToolTipText("Indicates a debug event generated by either a C_HALT or C_STEP request");

      return debugFaults;
   }
   
   /**
    * Exceptions group
    * 
    * @param container
    * @return
    */
   protected Group createExceptions(Composite container) {
      /*
       * Exception information
       */
      Group exceptionDescription = new Group(container, SWT.SHADOW_ETCHED_IN);
      GridData gd = new GridData();
      gd.verticalAlignment   = GridData.FILL;
      gd.horizontalAlignment = GridData.FILL;
      exceptionDescription.setLayoutData(gd);

      exceptionDescription.setLayout(new GridLayout(2, true));
      exceptionDescription.setText("Exception (SCB.ICSR)");

      Label description;
      Control control;
      
      control = makeCheck(exceptionDescription, "NMIPENDSET", ((icsr&(1<<31))!= 0));
      control.setToolTipText("NMI Interrupt is pending");

      control = makeCheck(exceptionDescription, "RETTOBASE", ((icsr&(1<<11))!= 0));
      control.setToolTipText("Preempted active exceptions");

      control = makeCheck(exceptionDescription, "PENDSVSET", ((icsr&(1<<28))!= 0));
      control.setToolTipText("Supervisor call exception is pending");

      control = makeCheck(exceptionDescription, "PENDSTSET", ((icsr&(1<<26))!= 0));
      control.setToolTipText("SysTick interrupt is pending.");

      control = makeCheck(exceptionDescription, "ISRPREEMPT", ((icsr&(1<<23))!= 0));
      control.setToolTipText("Will service a pending exception on debug exit");

      control = makeCheck(exceptionDescription, "ISRPENDING", ((icsr&(1<<22))!= 0));
      control.setToolTipText("External interrupt is pending");

      description = new Label(exceptionDescription, SWT.NONE);
      description.setText("Active Exception");
      control = makeExceptionNumber(exceptionDescription, (int)((icsr>>0)&0x1FFL));

      description = new Label(exceptionDescription, SWT.NONE);
      description.setText("Pending Exception");
      control = makeExceptionNumber(exceptionDescription, (int)((icsr>>12)&0x1FFL));

      return exceptionDescription;
   }
   
   /**
    * Faults group 
    * 
    * @param parent
    * @return
    */
   protected Composite createFaultGroups(Composite parent) {
      
      Composite container = new Composite(parent, SWT.FILL);
//      
      GridLayout gridLayout = new GridLayout(2, false);
      container.setLayout(gridLayout);
      
      createHardFaults(container);
      createUsageFaults(container);
      createMemoryManageFaults(container);
      createBusFaults(container);
      createDebugFaults(container);
      createExceptions(container);

      return container;
   }
   
   @Override
   protected Control createDialogArea(Composite parent) {
      
      // Create the manager and bind to main composite
      resManager = new LocalResourceManager(JFaceResources.getResources(), parent);

//      Composite area = (Composite) super.createDialogArea(parent);
      
      // Create the top level composite for the dialog area
      Composite area = new Composite(parent, SWT.NONE);
      FillLayout layout = new FillLayout();
      area.setLayout(layout);
      
      Composite  container = new Composite(area, SWT.FILL);
      RowLayout rl = new RowLayout();
      container.setLayout(rl);
      
      createFaultGroups(container);
      createRegistersGroup(container);

      return area;
   }
   
   /* (non-Javadoc)
    * @see org.eclipse.jface.dialogs.Dialog#createButton(org.eclipse.swt.widgets.Composite, int, java.lang.String, boolean)
    */
   @Override
   protected Button createButton(Composite parent, int id, String label, boolean defaultButton) {
      if (id == IDialogConstants.CANCEL_ID) {
         return null;
      }
      return super.createButton(parent, id, label, defaultButton);
   }

   // overriding this methods allows you to set the
   // title of the custom dialog
   @Override
   protected void configureShell(Shell newShell) {
      super.configureShell(newShell);
      newShell.setText("Exception Report");
      newShell.setLayout(new FillLayout());
   }

   @Override
   protected Point getInitialSize() {
      return new Point(650, 580);
   }

   @Override
   protected boolean isResizable() {
      return true;
   }

   /**
    * Test main
    * 
    * @param args
    */
   public static void main(String[] args) {
      Display display = new Display();

      Shell shell = new Shell(display);
      FaultDialogue dialogue = new FaultDialogue(shell);
      dialogue.create(0x12345678L, 0x87654321L, 0xAAAA5555L, 0x11111111L, 0x22222222L);
      dialogue.open();

      while (!shell.isDisposed()) {
         if (!display.readAndDispatch())
            display.sleep();
      }
      display.dispose();
   }

}