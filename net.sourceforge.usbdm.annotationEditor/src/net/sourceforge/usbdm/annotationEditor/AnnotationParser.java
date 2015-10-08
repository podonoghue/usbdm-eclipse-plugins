package net.sourceforge.usbdm.annotationEditor;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.annotationEditor.AnnotationModel.AnnotationModelNode;
import net.sourceforge.usbdm.annotationEditor.AnnotationModel.BinaryOptionModelNode;
import net.sourceforge.usbdm.annotationEditor.AnnotationModel.BitField;
import net.sourceforge.usbdm.annotationEditor.AnnotationModel.EnumValue;
import net.sourceforge.usbdm.annotationEditor.AnnotationModel.EnumeratedOptionModelNode;
import net.sourceforge.usbdm.annotationEditor.AnnotationModel.ErrorNode;
import net.sourceforge.usbdm.annotationEditor.AnnotationModel.Modifier;
import net.sourceforge.usbdm.annotationEditor.AnnotationModel.NumericOptionModelNode;
import net.sourceforge.usbdm.annotationEditor.AnnotationModel.OptionModelNode;
import net.sourceforge.usbdm.annotationEditor.AnnotationModel.Range;
import net.sourceforge.usbdm.annotationEditor.AnnotationModel.SelectionTag;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.viewers.TreeViewer;

/**
 * Used to create a model from the wizard description embedded in the C file
 * 
 * @author podonoghue
 *
 */
public class AnnotationParser {

   /** The associated document */
   private IDocument            document        = null;
   /** The associated model */
   private AnnotationModel      annotationModel = null;
   /** Current node being processed */
   private AnnotationModelNode  currentNode     = null;
   /** Current option within the current node */
   private OptionModelNode      currentOption   = null;
   /** Indicates start of wizard mark-up has been found */
   private boolean              wizardFound     = false;
   /** Indicates end of wizard mark-up has been found */
   private boolean              wizardEndFound  = false;
   /** Current line number */
   private int                  lineNumber      = 0;

   private final static String VARIABLENAME_GROUP    = "(?<variableName>\\w+)";
   private final static String CLASS_NAME_GROUP      = "(?<className>\\w[\\w|.]+)";
   private final static String NUMBER_PATTERN        = "(?:0x)?[0-9|a-f|A-F]*";
   private final static String ARGS_GROUP            = "\\(\\s*(?<args>"+NUMBER_PATTERN+"(?:\\s*,\\s*("+NUMBER_PATTERN+"))*)\\s*\\)";
   private final static String SELECTIONNAME_GROUP   = "(?<selectBody>\\s*(\\w*)\\s*,("+NUMBER_PATTERN+"))";
   
   private ArrayList<MyValidator> validators         = new ArrayList<MyValidator>();
   private ArrayList<MyValidator> newValidators      = new ArrayList<MyValidator>();
   
   private final static String  wizardStartString           = "<<<\\s*Use\\s*Configuration\\s*Wizard\\s*in\\s*Context\\s*Menu\\s*>>>";
   private final static String  wizardEndString             = "<<<\\s*end\\s*of\\s*configuration\\s*section\\s*>>>";
    
   private final static String  wizardAnnotationString      = "<\\s*(?<control>[ehioqs]|/e|/h|pllConfigure)\\s*(?<offset>\\d+)?(\\s*\\.\\s*(?<fStart>\\d+)(\\s*\\.\\.\\s*(?<fEnd>\\d+))?)?\\s*>\\s*(?<text>[^<]*)";
   private final static String  nameString                  = "<\\s*name\\s*=\\s*(?<nameBody>.*?)\\s*>";
   private final static String  selectionString             = "<\\s*selection\\s*=\\s*(?<selectionBody>.*?)\\s*>";
   
   private final static String  validateString              = "<\\s*validate\\s*=\\s*(?<validateBody>.*?)\\s*>";
   private final static String  constString                 = "<\\s*constant\\s*>";
   
   private final static String rangePatternString          = "<\\s*(?<rStart>(0x[0-9a-fA-F]*)|(\\d+))(\\s*\\-\\s*(?<rEnd>(0x[0-9a-fA-F]*)|(\\d+))(\\s*:\\s*(?<rStep>(0x[0-9a-fA-F]*)|(\\d+)))?)?\\s*>";
   private final static String enumerationPatternString    = "<\\s*(?<enumValue>\\d+)\\s*=\\s*>(?<enumName>[^<]*)";
   private final static String modifierPatternString       = "<\\s*#\\s*(?<operation>.)\\s*(?<factor>(0x[0-9a-fA-F]*)|(\\d+))\\s*>";

   private final static int     matchFlags         = Pattern.DOTALL;
   private final static String  wizardPatternString = 
         "(?<wizardStart>"+wizardStartString+")|"+
         "(?<wizardEnd>"+wizardEndString+")|"+
         "(?<annotation>"+wizardAnnotationString+")|"+
         "(?<range>"+rangePatternString+")|"+
         "(?<modifier>"+modifierPatternString+")|"+
         "(?<enumeration>"+enumerationPatternString+")|"+
         "(?<name>"+nameString+")|"+
         "(?<selection>"+selectionString+")|"+
         "(?<validate>"+validateString+")|"+
         "(?<constant>"+constString+")|"+
//         "(?<pllConfigure>"+pllConfigureString+")|"+
         "(<[^>]*>[^<]*)"
         ;
   private final static Pattern wizardPattern     = Pattern.compile(wizardPatternString, matchFlags);

   private ArrayList<ErrorMarker> errorMarkers;
   
   private final String markerId = "net.sourceforge.usbm.annotationEditor.errorMarker";
   
   public class ErrorMarker {
      private int lineNumber;
      private String message;
      
      ErrorMarker(int lineNumber, String message) {
         this.lineNumber = lineNumber;
         this.message    = message;
      }
      
      private IMarker createMarker(IResource resource) throws CoreException {
         IMarker marker = resource.createMarker(markerId);
         marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
         marker.setAttribute(IMarker.MESSAGE, message);
         marker.setAttribute(IMarker.LINE_NUMBER, lineNumber); 
         return marker;
      }
   }
   
   public void attachErrorMarkers(IResource resource) throws CoreException {
      int depth = IResource.DEPTH_INFINITE;
      resource.deleteMarkers(markerId, true, depth);
      for (ErrorMarker errorMarker : errorMarkers) {
         errorMarker.createMarker(resource);
      }
   }

   public ArrayList<ErrorMarker> getErrorMarkers() {
      return errorMarkers;
   }

   private void clearErrorMarkers() {
      errorMarkers = new ArrayList<AnnotationParser.ErrorMarker>();
   }
   
   private void addErrorMarker(ErrorMarker errorMarker) {
      this.errorMarkers.add(errorMarker);
   }

   public ErrorNode constructErrorNode(String message, Throwable exception) {
      ErrorNode node = annotationModel.new ErrorNode(message);
      while (exception != null) {
         node.addChild(annotationModel.new ErrorNode(exception.getMessage()));
         exception =exception.getCause();
      }
      return node;
   }

   private void createAnnotation(String control, int offset, BitField bitField, String text) throws Exception {
//      System.err.println(String.format("createAnnotation(<%s%d> %s) 1", control, offset, text));
      Pattern p = Pattern.compile("(.*?)\\s*\\*/");
      Matcher m = p.matcher(text);
      if (m.matches()) {
         text = m.group(1);
      }
      if (control.equals("h")) {
         // Start of Heading
         AnnotationModelNode heading = annotationModel.new HeadingModelNode(text);
         currentNode.addChild(heading);
         currentNode    = heading;
         currentOption  = null;
      }
      else if (control.equals("/h")) {
         // End of Heading
         AnnotationModelNode parentNode = currentNode.getParent();
         if (parentNode == null) {
            throw new Exception("</h> without preceeding <h>");
         }
         currentNode   = parentNode;
         currentOption = null;
      }
      else if (control.equals("e")) {
         // Start of Heading with enable
         // May specify bit-field 
         // May specify offset
         AnnotationModelNode heading = annotationModel.new OptionHeadingModelNode(text, offset, bitField);
         currentNode.addChild(heading);
         currentNode    = heading;
         currentOption  = (OptionModelNode) heading;
      }
      else if (control.equals("/e")) {
         // End of Heading with enable
         AnnotationModelNode parentNode = currentNode.getParent();
         if (parentNode == null) {
            throw new Exception("</e> without preceeding <e>");
         }
         currentNode   = parentNode;
         currentOption = null;
      }
      else if (control.equals("o")) {
         // Numeric Option with selection or number entry.
         // May specify bit-field 
         // May specify offset
         currentOption = annotationModel.new NumericOptionModelNode(text, offset, bitField);
         currentNode.addChild(currentOption);
      }
      else if (control.equals("pllConfigure")) {
         // Is this used??
         // May specify bit-field 
         // May specify offset
         currentOption = annotationModel.new PllConfigurationModelNode(text, offset, bitField);
         currentNode.addChild(currentOption);
      }
      else if (control.equals("q")) {
         // Binary option for bit values which can be set via a check-box or selection
         // May specify bit-field 
         // May specify offset
         currentOption = annotationModel.new BinaryOptionModelNode(text, offset, bitField);
         currentNode.addChild(currentOption);
      }
      else if (control.equals("s")) {
         // ASCII string entry.
         // May specify length 
         // May specify offset
         int length = 0;
         if (bitField != null) {
            length = bitField.getStart();
         }
         currentOption = annotationModel.new StringOptionModelNode(text, offset, length);
         currentNode.addChild(currentOption);
      }
      else if (control.equals("i")) {
         // Tool-tip applied to current option
         if (currentOption == null) {
            throw new Exception("<i> without previous item");
         }
         else {
            currentOption.addToolTip(text);
         }
      }
   }

   private void createRange(boolean useHex, long startValue, long endValue, long stepSize) throws Exception {
      if (!(currentOption instanceof NumericOptionModelNode)) {
         throw new Exception("Range field applied to non-numeric option");
      }
      ((NumericOptionModelNode)currentOption).setRange(new Range(startValue, endValue, stepSize));
      if (useHex) {
         ((NumericOptionModelNode)currentOption).setUseHex(true); 
      }
   }
   
   private void createEnumerationValue(String name, int value) throws Exception {
//      System.err.println(String.format("createEnumerationValue(%s, 0x%X)", name, value));
      if (currentOption instanceof BinaryOptionModelNode) {
         // A binary annotation but someone decided to name the options!
         ((BinaryOptionModelNode)currentOption).addEnumerationValue(new EnumValue(name, value));
         return;
      }
      if (!(currentOption instanceof NumericOptionModelNode)) {
         throw new Exception("Enumerated value applied to non-numeric option");
      }
      if (!(currentOption instanceof EnumeratedOptionModelNode)) {
         // A numeric annotation but using enumerated values
         EnumeratedOptionModelNode node = annotationModel.new EnumeratedOptionModelNode((NumericOptionModelNode) currentOption);
         AnnotationModelNode parent = currentOption.getParent();
         parent.removeChild(currentOption);
         parent.addChild(node);
         currentOption = node;
      }
      ((EnumeratedOptionModelNode)currentOption).addEnumerationValue(new EnumValue(name, value));
   }
   
   private void createModifier(String operation, long factor) throws Exception {
//      System.err.println(String.format("createModifier(%s, %d)", operation, factor));
      if (!(currentOption instanceof NumericOptionModelNode)) {
         throw new Exception("Modifier applied to non-numeric option");
      }
      ((NumericOptionModelNode)currentOption).addModifier(new Modifier(operation, factor));
   }
   
   private void createName(String variableName) throws Exception {
      currentOption.setName(variableName);
   }

   private void createSelection(String selectionName, String selectionIndex) throws Exception {
      if (!(currentOption instanceof EnumeratedOptionModelNode)) {
         System.err.println("currentOption = "+currentOption.getName()+": "+currentOption.getDescription());
         throw new Exception("selectionName may only be applied to an enumerated selection, found: " + currentOption.getClass().toString());
      }
      EnumeratedOptionModelNode o = (EnumeratedOptionModelNode)currentOption;
      o.addSelectionTag(new SelectionTag(o, selectionName, Long.parseLong(selectionIndex)));
   }

   private void collectScannedInformationFromNodes(AnnotationModelNode node) {
      for (AnnotationModelNode child : node.getChildren()) {
         if (child.getName() != null) {
            annotationModel.addNamedModelNode(child.getName(), child);
         }
         if (child instanceof EnumeratedOptionModelNode) {
            ArrayList<SelectionTag> selectionTags = ((EnumeratedOptionModelNode)child).getSelectionTags();
            if (selectionTags != null) {
               for (SelectionTag selectionTag:selectionTags) {
                  annotationModel.addSelection(selectionTag);
               }
            }
         }
         collectScannedInformationFromNodes(child);
      }
   }
   
   public void collectScannedInformation() {
      annotationModel.clearScannedInformation();
      collectScannedInformationFromNodes(annotationModel.getModelRoot());
//      annotationModel.getNameMap().entrySet();
//      System.err.println("collectNamedNodes()");
//      for (Entry<String, AnnotationModelNode> name : annotationModel.getNameMap().entrySet()) {
//         System.err.println(name.getKey() + " => " + name.getValue().getDescription());
//      }
   }
   
   private void parseComment(String buff) throws Exception {
      if (buff.startsWith("//!") || buff.startsWith("/*!")) {
         // Ignore these as may contain markup
         return;
      }
      Matcher m = wizardPattern.matcher(buff);
      if (!m.find(0)) {
         return;
      }
      try {
         do {
//            System.err.println("Found:"+buff.substring(m.start(), m.end()));
            if (m.group("wizardStart") != null) {
//               System.err.println("======== Wizard Start found ========");
               wizardFound = true;
            }
            else if (m.group("wizardEnd") != null) {
//               System.err.println("======== Wizard End found ========");
               wizardEndFound = true;
               break;
            }
            else if (!wizardFound) {
               // Ignore until Wizard Start string is found
               continue;
            }
            else if (m.group("annotation") != null) {
               String  control     = null;
               int     offset      = 0;
               int     fieldStart  = -1;
               int     fieldEnd    = -1;
               String text         = "";
               control = m.group("control").trim();
               if (m.group("offset") != null) {
                  offset = Integer.decode(m.group("offset").trim());
               }
               if (m.group("fStart") != null) {
                  fieldStart = Integer.decode(m.group("fStart").trim());
                  fieldEnd = fieldStart;
               }
               if (m.group("fEnd") != null) {
                  fieldEnd = Integer.decode(m.group("fEnd").trim());
               }
               if (m.group("text") != null) {
                  text = m.group("text").trim();
               }
               BitField bitField = null;
               if (fieldStart >= 0) {
                  bitField = new BitField(fieldStart,  fieldEnd);
               }
               createAnnotation(control, offset, bitField, text);
            }
            else if (m.group("range") != null) {
               long startValue   = 0;
               long endValue     = 0;
               long stepSize     = 1;
               boolean useHex      = false;
               startValue = Long.decode(m.group("rStart"));
               endValue   = Long.decode(m.group("rEnd"));
               if (m.group("rStart").trim().startsWith("0x") ||
                   m.group("rEnd").trim().startsWith("0x")) {
                  useHex = true;
               }
               if (m.group("rStep") != null) {
                  stepSize = Long.decode(m.group("rStep"));
               }
//               System.err.println(String.format("Range[%s,%s,-]", m.group("rStart"), m.group("rEnd")));
//               System.err.println(String.format("Range[0x%X,0x%X,x%X]", startValue, endValue, stepSize));
               createRange(useHex, startValue, endValue, stepSize);
            }
            else if (m.group("enumeration") != null) {
               int    value = Integer.decode(m.group("enumValue"));
               String name  = m.group("enumName").trim();
               createEnumerationValue(name, value);
            }
            else if (m.group("modifier") != null) {
               String operation = m.group("operation");
               long   factor    = Long.decode(m.group("factor"));
               createModifier(operation, factor);
            }
            else if (m.group("constant") != null) {
              currentOption.setModifiable(false);
            }
            else if (m.group("name") != null) {
               String nameBody = m.group("nameBody");
               if (nameBody == null) {
                  throw new Exception("No name found \'" + m.group("name") + "\'");
               }
               Pattern p = Pattern.compile(VARIABLENAME_GROUP);
               Matcher nm = p.matcher(nameBody);
               if (!nm.matches()) {
                  throw new Exception("Illegal name \'" + nameBody + "\'");
               }
               String variableName = nm.group("variableName");
               createName(variableName);
//               System.err.println("Adding name = "+variableName);
            }
            else if (m.group("selection") != null) {
               String selectionBody = m.group("selectionBody");
               if (selectionBody == null) {
                  throw new Exception("No selection found \'" + m.group("selection") + "\'");
               }
               // e.g. <selection=i2c0_sda,2>
               //"(?<selectionName>.*))";
               Pattern p = Pattern.compile(SELECTIONNAME_GROUP);
               Matcher nm = p.matcher(selectionBody);
               if (!nm.matches()) {
                  throw new Exception("Illegal selection \'" + selectionBody + "\'");
               }
               String selectionName = nm.group(2);
               String selectionIndex = nm.group(3);
               createSelection(selectionName, selectionIndex);
            }
            else if (m.group("validate") != null) {
               String validateBody = m.group("validateBody");
               if (validateBody == null) {
                  throw new Exception("No method name or arguments found \'" + m.group("validate") + "\'");
               }
               Pattern p = Pattern.compile(CLASS_NAME_GROUP+"\\s*("+ARGS_GROUP+")?");
               Matcher vm = p.matcher(validateBody);
               if (!vm.matches()) {
                  throw new Exception("Illegal method name or arguments \'" + validateBody + "\'");
               }
               String className = vm.group("className");
               String arguments = vm.group("args");
               createValidator(className, arguments);
            }
            else {
//               System.err.println("Other:"+m.group().trim());
               throw new Exception("Unrecognized option");
            }
         } while (m.find());
      } catch (Exception e) {
         String message = String.format("Error @line %d: Failed to parse annotation \'%s\'", 
               lineNumber, buff.substring(m.start(), m.end()).trim());
         currentNode.addChild(constructErrorNode(message, e));
         if ((e.getMessage() != null) && !e.getMessage().isEmpty()) {
            message = e.getMessage();
         }
         else {
            e.printStackTrace();
         }
         addErrorMarker(new ErrorMarker(lineNumber, message));
      }
   }
 
   private void createValidator(String className, String arguments) throws Exception {
      MyValidator validatorClass = null;
      String sArgs[] = null;
      if (arguments != null) {
         sArgs = arguments.split(",");
      }
      try {
         // Get validator class
         Class<?> clazz = Class.forName(className);
         if (sArgs == null) {
            // Use default constructor
//            System.err.println(String.format("Creating function: %s()", className));
            validatorClass = (MyValidator) clazz.newInstance();         
         }
         else {
            long args[] = new long[sArgs.length];
            for (int index=0; index<args.length; index++) {
               args[index] = Long.parseLong(sArgs[index].trim());
            }
            switch (sArgs.length) {
            case 1: 
//               System.err.println(String.format("Creating function: %s(%d)", className, args[0]));
               validatorClass = (MyValidator) clazz.getConstructor(long.class).newInstance(args[0]);
               break;
            case 2: 
//               System.err.println(String.format("Creating function: %s(%d,%d)", className, args[0], args[1]));
               validatorClass = (MyValidator) clazz.getConstructor(long.class,long.class).newInstance(args[0],args[1]);
               break;
            case 3: 
//               System.err.println(String.format("Creating function: %s(%d,%d,%d)", className, args[0], args[1], args[2]));
               validatorClass = (MyValidator) clazz.getConstructor(long.class,long.class,long.class).newInstance(args[0],args[1],args[2]);
               break;
            case 4: 
//               System.err.println(String.format("Creating function: %s(%d,%d,%d,%d)", className, args[0], args[1], args[2], args[3]));
               validatorClass = (MyValidator) clazz.getConstructor(long.class,long.class,long.class,long.class).newInstance(args[0],args[1],args[2],args[3]);
               break;
            } 
         }
         newValidators.add(validatorClass);
         validatorClass.setModel(annotationModel);
      } catch (Exception e) {
         throw new Exception("Failed to instantiate function \'"+className+"\'", e);
      }
   }

   AnnotationParser(IDocument document) {
      this.document        = document;
      this.annotationModel = new AnnotationModel(document);
   }
   
   /**
    * Parses the document and creates a model
    * 
    * @return the model created
    * 
    * @note The mode should be set as root if necessary
    */
   public AnnotationModelNode parse() throws Exception {
      AnnotationModelNode rootNode =  annotationModel.new AnnotationModelNode("Root");
      currentNode = rootNode;
      currentNode.removeAllChildren();
      newValidators.clear();
      
      currentOption   = null;
      wizardEndFound  = false;
      
      annotationModel.getReferences().clearReferences();

      clearErrorMarkers();
      
      ITypedRegion[] partitions = document.getDocumentPartitioner().computePartitioning(0, document.getLength());
      for (ITypedRegion partition : partitions) {
//         System.err.print(
//               String.format("Partition type: [%d..%d] %s\n",
//               partition.getOffset(), partition.getLength(), partition.getType()));
         if (wizardEndFound) {
            break;
         }
         lineNumber = document.getLineOfOffset(partition.getOffset()) + 1;
         if (partition.getType() == PartitionScanner.C_COMMENT) {
//            System.err.println("C_COMMENT");
            parseComment(document.get(partition.getOffset(), partition.getLength()));
         }
         else if (partition.getType() == PartitionScanner.C_NUMBER) {
//            System.err.print(String.format("C_NUMBER [%d..%d]\n", partition.getOffset(), partition.getLength()));
            annotationModel.getReferences().addReference(annotationModel.new DocumentReference(partition.getOffset(), partition.getLength()));
         }
         else if (partition.getType() == PartitionScanner.C_STRING) {
//            System.err.print(String.format("C_STRING [%d..%d]\n", partition.getOffset(), partition.getLength()));
            annotationModel.getReferences().addReference(annotationModel.new DocumentReference(partition.getOffset(), partition.getLength()));
         }
      }
      if (validators.size() != newValidators.size()) {
//         System.err.println("****************Replacing all validators");
         validators = newValidators;
      }
      else {
         for (int index=0; index<validators.size(); index++) {
            if (validators.get(index).getClass() != newValidators.get(index).getClass()) {
//               System.err.println("****************Replacing validator");
               validators.set(index, newValidators.get(index));
            }
         }
      }
      return rootNode;
   }

   public AnnotationModelNode getModelRoot() throws RuntimeException {
      if (annotationModel == null) {
         throw new RuntimeException(" No annotationModel available to get model root");
      }
      return annotationModel.getModelRoot();
   }

   public void setModelRoot(AnnotationModelNode newRoot) throws RuntimeException {
      if (annotationModel == null) {
         if (newRoot != null) {
            throw new RuntimeException("No annotationModel available to set model root");
         }
         return;
      }
      annotationModel.setModelRoot(newRoot);
   }
   
   public AnnotationModel getModel() {
      return annotationModel;
   }
   
   public void listNodes() throws Exception {
      getModelRoot().listNodes(0);
   }

   public void validate(TreeViewer viewer) throws Exception {
      MyValidator[] lValidators = validators.toArray(new MyValidator[validators.size()]);
      for (MyValidator validator : lValidators) {
         validator.validate(viewer);
      }
   }
   
   public static void main(String[] args) {
      String test[] = new String[] {
//            "<validate=net.sourceforge.usbdm.annotationEditor.validators.ClockValidate_MK64M12  >",
//            "<validate=net.sourceforge.usbdm.annotationEditor.validators.ClockValidate_MK64M12 (0x1234)  >",
//            "<validate=net.sourceforge.usbdm.annotationEditor.validators.ClockValidate_MK64M12(1,2,3,4)  >",
//            "<name=oscclk_clock  >", 
//            "<0-50000000>",
//            "<i> hello there */",
            "<selection=i2c0_sda,2>",
            "<selection=i2c0_sda,0x23>",
            "<selection=_i2c0_sda,1024>"
      };
      Pattern wizardPattern = Pattern.compile(wizardPatternString);
      for (String s:test) {
         System.err.println("\nTesting \""+s+"\"");
         Matcher wizardMatcher = wizardPattern.matcher(s);
         if (!wizardMatcher.matches()) {
            System.err.println("No match");
            continue;
         }
         String validateGroup = wizardMatcher.group("validate");
         String nameGroup = wizardMatcher.group("name");
         String annotationGroup = wizardMatcher.group("annotation");
         String selectionGroup = wizardMatcher.group("selection");
         if (validateGroup != null) {
            System.err.println("validateGroup = \'"+validateGroup+"\'");
            String validateBody = wizardMatcher.group("validateBody");
            System.err.println("validateBody = \'"+validateBody+"\'");
            Pattern p = Pattern.compile(CLASS_NAME_GROUP+"\\s*("+ARGS_GROUP+")?");
            Matcher m = p.matcher(validateBody);
            if (!m.matches()) {
               System.err.println("No match to \'validateBody\'");
               continue;
            }
            String className = m.group("className");
            String arguments = m.group("args");
            System.err.println(String.format("className = \"%s\"", className));
            System.err.println(String.format("arguments = \"%s\"", arguments));
            System.err.println("\n");
         }
         else if (nameGroup != null) {
            System.err.println("nameGroup = \'"+nameGroup+"\'");
            String nameBody = wizardMatcher.group("nameBody");
            System.err.println("nameBody = \'"+nameBody+"\'");
            Pattern p = Pattern.compile(VARIABLENAME_GROUP);
            Matcher m = p.matcher(nameBody);
            if (!m.matches()) {
               System.err.println("No match to \'nameBody\'");
               continue;
            }
            String variableName = m.group("variableName");
            System.err.println(String.format("variableName = \"%s\"", variableName));
            System.err.println("\n");
         }
         else if (annotationGroup != null) {
            System.err.println("annotationGroup = \'"+annotationGroup+"\'");
            String text = wizardMatcher.group("text");
            System.err.println("text = \'"+text+"\'");

            Pattern p = Pattern.compile("(.*?)\\s*\\*/");
            Matcher m = p.matcher(text);
            if (m.matches()) {
               text = m.group(1);
            }
            System.err.println("text = \'"+text+"\'");

            //            Pattern p = Pattern.compile(VARIABLENAME_GROUP);
//            Matcher m = p.matcher(nameBody);
//            if (!m.matches()) {
//               System.err.println("No match to \'nameBody\'");
//               continue;
//            }
//            String variableName = m.group("variableName");
//            System.err.println(String.format("variableName = \"%s\"", variableName));
//            System.err.println("\n");
         }
         else if (selectionGroup != null) {
            System.err.println("selectionGroup = \'"+selectionGroup+"\'");
            String selectionBody = wizardMatcher.group("selectionBody");
            System.err.println("selectionBody = \'"+selectionBody+"\'");
            System.err.println("SELECTIONNAME_GROUP = \'"+SELECTIONNAME_GROUP+"\'");
            
            // e.g. <selection=i2c0_sda,2>
            //"(?<selectionName>.*))";
            Pattern p = Pattern.compile(SELECTIONNAME_GROUP);
            Matcher nm = p.matcher(selectionBody);
            if (!nm.matches()) {
               System.err.println("Fails match");
            }
            String selectionName = nm.group(2);
            String selectionIndex = nm.group(3);
            System.err.println(String.format("selectionName = \"%s\"", selectionName));
            System.err.println(String.format("selectionIndex = \"%s\"", selectionIndex));

         }
         else {
            System.err.println("No match");            
         }
      }
   }
 }
