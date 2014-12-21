package net.sourceforge.usbdm.deviceDatabase;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import net.sourceforge.usbdm.deviceDatabase.DeviceDatabase.FileType;
import net.sourceforge.usbdm.jni.Usbdm;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class PackageParser {

   private IPath fPath = null;
   
   protected PackageParser(IPath path) {
      fPath = path;
   }
   
   static public ArrayList<ProjectActionList> addPackageList(Device device, ArrayList<ProjectActionList> pal) {
      if (pal == null) {
         pal = new ArrayList<ProjectActionList>(); 
      }
      IPath packagesDirectoryPath = Usbdm.getResourcePath().append("Stationery/Packages");
      File[] packageDirectories = packagesDirectoryPath.toFile().listFiles();
      Arrays.sort(packageDirectories);
      if (packageDirectories == null) {
         System.out.flush();
         System.err.println("No packages found at " + packagesDirectoryPath.toOSString());
         return pal;
      }
      for (File packageDirectory : packageDirectories) {
         if (packageDirectory.isDirectory()) {
            // Get all XML files
            File[] files = packageDirectory.listFiles(new FileFilter() {
               public boolean accept(File arg0) {
                  return arg0.getName().matches(".*\\.xml$");
               }
            });
            Arrays.sort(files);
            Path path = new Path(packageDirectory.toPath().toAbsolutePath().toString());
            for(File f:files) {
               if (f.isFile()) {
                  try {
                     PackageParser                packParser         = new PackageParser(path);
                     Document                     document           = packParser.parseXmlFile(f.toString());
                     ArrayList<ProjectActionList> projectActionLists = packParser.parseDocument(document);
                     for (ProjectActionList projectActionList:projectActionLists) {
                        if (projectActionList.appliesTo(device)) {
//                         System.err.println("projectAction ID = " + projectActions.getId());
//                         System.err.println("projectAction applyWhen = " + projectActions.getApplyWhenCondition());
//                         for (ProjectAction projectAction : projectActions) {
//                            System.err.println("projectAction = " + projectAction.toString());
//                         }
//                       System.err.println("===============================================");
                         pal.add(projectActionList);
                      }
                     }
                  } catch (Exception e) {
                     e.printStackTrace();
                  }
               }
            }
         }
      }
      return pal;
   }
   
   /**
    * Parse the XML file into the XML internal DOM representation
    * 
    * @param path Either a full path to device SVD file or device name (default location & default extension will be added)
    * 
    * @return DOM Document representation (or null if locating file fails)
    * 
    * @throws Exception on XML parsing error or similar unexpected event
    */
   private Document parseXmlFile(String path) throws Exception {
      // Try deviceName as full path
      IPath databasePath = new Path(path);

//      System.err.println("PackParser.parseXmlFile() \'" + path + "\'");
      File file = databasePath.toFile();
      if (!file.exists()) {
         System.err.println("PackParser.parseXmlFile() \'" + file.toPath().toAbsolutePath().toString() + "\' not found");
         return null;
      }
      // Get the factory
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder        db  = dbf.newDocumentBuilder();

      //  Parse using builder to get DOM representation of the XML file
      return db.parse(databasePath.toOSString());
   }
   
   /**
    * @return ArrayList of ProjectActionLists (may be empty but never null)
    * 
    * @throws Exception
    */
   private ArrayList<ProjectActionList> parseDocument(Document documentElement) throws Exception {
      if (documentElement == null) {
         System.out.println("DeviceDatabase.parseDocument() - failed to get documentElement");
         return null;
      }
      ArrayList<ProjectActionList> fileElements = new ArrayList<ProjectActionList>();
      Element root = documentElement.getDocumentElement();
      for (Node node = root.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == "projectActionList") {
            fileElements.add(parseProjectActionListElement(element));
         }
         else {
            throw new Exception("Unexpected field in <root>' - \'" + element.getTagName() + "\' found");
         }
      }
      return fileElements;
   }

   /**
    * Parse a <projectActionList> element
    * 
    * @param    projectActionListElement <projectActionList> element
    * 
    * @return   Action list described 
    * 
    * @throws Exception 
    */
   private ProjectActionList parseProjectActionListElement(Element projectActionListElement) throws Exception {
      return parseProjectActionListElement(projectActionListElement, new ProjectActionList(), new HashMap<String, ProjectVariable>());
   }

   /**
    * Parse a <projectActionList> element
    * 
    * @param   listElement <projectActionList> element
    * @param   projectActionList List to use
    * @param   variableList Map to add variables to
    * @return  Action list described 
    * @throws  Exception
    */
   private ProjectActionList parseProjectActionListElement(Element listElement, ProjectActionList projectActionList, Map<String,ProjectVariable> variableList) throws Exception {

      IPath root = fPath;
      if (listElement.hasAttribute("root")) {
         root = root.append(listElement.getAttribute("root"));
      }
      if (listElement.hasAttribute("id")) {
         projectActionList.setId(listElement.getAttribute("id"));
      }
      // <projectActionList>
      for (Node node = listElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         // child node for <projectActionList>
         Element element = (Element) node;
         if (element.getTagName() == "projectActionListRef") {
            throw new Exception("<projectActionListRef> not supported in " + listElement.getNodeName());
         }
         else if (element.getTagName() == "applyWhen") {
            if (projectActionList.getApplyWhenCondition() != null) {
               throw new Exception("Multiple <applyWhen> in " + listElement.getNodeName());
            }
            projectActionList.setApplyWhenCondition(parseApplyWhenElement(element));
         }
         else if (element.getTagName() == "projectActionList") {
            parseProjectActionListElement(element, projectActionList, variableList);
         }
         else if (element.getTagName() == "variable") {
            ProjectVariable variable = parseVariableElement(element);
            variableList.put(variable.getId(), variable);
         }
         else if (element.getTagName() == "condition") {
            parseConditionElement(element, variableList, projectActionList, root);
         }
         else if (element.getTagName() == "excludeSourceFile") {
            ExcludeAction excludeAction = parseExcludeSourceFileElement(element);
            projectActionList.add(excludeAction);
         }
         else if (element.getTagName() == "excludeSourceFolder") {
            ExcludeAction excludeAction = parseExcludeSourceFolderElement(element);
            projectActionList.add(excludeAction);
         }
         else if (element.getTagName() == "createFolder") {
            CreateFolderAction createFolderAction = parseCreateFolderElement(element);
            createFolderAction.setRoot(root.toPortableString());
            projectActionList.add(createFolderAction);
         }
         else if (element.getTagName() == "copy") {
            FileAction fileAction = parseCopyElement(element);
            fileAction.setRoot(root.toPortableString());
            projectActionList.add(fileAction);
         }
         else if (element.getTagName() == "customAction") {
            ProjectCustomAction projectCustomAction = parseCustomActionElement(element);
            projectActionList.add(projectCustomAction);
         }
         else if (element.getTagName() == "projectOption") {
            ProjectOption projectOption = parseProjectOptionElement(element);
            projectActionList.add(projectOption);
         }
         else {
            throw new Exception("Unexpected element \""+element.getTagName()+"\" in <projectActionList>");
         }
      }
      return projectActionList;
   }

   /**
    * Parse a <applyWhen> element
    * 
    * @param    <applyWhen> element
    * 
    * @return   condition described 
    * 
    * @throws Exception 
    */
   private ApplyWhenCondition parseApplyWhenElement(Element applyWhenElement) throws Exception {
      ApplyWhenCondition applyWhenCondition = null;
      for (Node node = applyWhenElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         if (applyWhenCondition != null) {
            throw new Exception("Too many elements in <applyWhen>");
         }
         Element element = (Element)node;
         applyWhenCondition = parseApplyWhenSubElement(element);
      }
      if (applyWhenCondition == null) {
         throw new Exception("Empty <applyWhen>");
      }
      return applyWhenCondition;
   }
 
   /**
    * Parses:
    *  <deviceNameIs>, <deviceFamilyIs>, <deviceSubfamilyIs>, 
    *  <deviceNameMatches>, <deviceFamilyMatches>, <deviceSubfamilyMatches>, 
    *  <and>, <or> or <not> element
    * 
    * @param    applyWhenElement <applyWhen> element
    * 
    * @return   condition described 
    * 
    * @throws Exception 
    */
   private ApplyWhenCondition parseApplyWhenSubElement(Element element) throws Exception {
      if (element.getTagName() == "deviceNameIs") {
         return new ApplyWhenCondition(ApplyWhenCondition.Type.deviceNameIs, element.getTextContent().trim());
      }
      else if (element.getTagName() == "deviceFamilyIs") {
         return new ApplyWhenCondition(ApplyWhenCondition.Type.deviceFamilyIs, element.getTextContent().trim());
      }
      else if (element.getTagName() == "deviceSubfamilyIs") {
         return new ApplyWhenCondition(ApplyWhenCondition.Type.deviceSubfamilyIs, element.getTextContent().trim());
      }
      else if (element.getTagName() == "deviceNameMatches") {
         return new ApplyWhenCondition(ApplyWhenCondition.Type.deviceNameMatches, element.getTextContent().trim());
      }
      else if (element.getTagName() == "deviceFamilyMatches") {
         return new ApplyWhenCondition(ApplyWhenCondition.Type.deviceFamilyMatches, element.getTextContent().trim());
      }
      else if (element.getTagName() == "deviceSubfamilyMatches") {
         return new ApplyWhenCondition(ApplyWhenCondition.Type.deviceSubfamilyMatches, element.getTextContent().trim());
      }
      else if (element.getTagName() == "and") {
         return new ApplyWhenCondition(ApplyWhenCondition.Type.and, parseApplyWhenLogicalElement(element));
      }
      else if (element.getTagName() == "or") {
         return new ApplyWhenCondition(ApplyWhenCondition.Type.or, parseApplyWhenLogicalElement(element));
      }
      else if (element.getTagName() == "not") {
         ArrayList<ApplyWhenCondition> list = parseApplyWhenLogicalElement(element);
         if (list.size() > 1) {
            throw new Exception("Multiple elements in <not>");
         }
         return new ApplyWhenCondition(ApplyWhenCondition.Type.not, list);
      }
      throw new Exception("Unexpected element in <applyWhen> " + element.getTagName());
  }
   
   /**
    * Parse a <and> or <or> or <not> element
    * 
    * @param    applyWhenElement <and> or <or> or <not> element
    * 
    * @return   condition described 
    * 
    * @throws Exception 
    */
   private ArrayList<ApplyWhenCondition> parseApplyWhenLogicalElement(Element logicalOperands) throws Exception {
      ArrayList<ApplyWhenCondition> list = null;
      for (Node node = logicalOperands.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         if (list == null) {
            list = new ArrayList<ApplyWhenCondition>();
         }
         // child node for <and> or <or> or <not>
         list.add(parseApplyWhenSubElement((Element) node));
      }
      if (list == null) {
         throw new Exception("Empty list in \""+logicalOperands.getTagName());
      }
      return list;
   }
   
   /**
    * Parse a <variable> element
    * 
    * @param    <variable> element
    * 
    * @return   File list described 
    * @throws Exception 
    */
   private ProjectVariable parseVariableElement(Element projectVariableElement) {
      // <projectVariable>
      String id            = projectVariableElement.getAttribute("id");
      String name          = projectVariableElement.getAttribute("name");
      String description   = projectVariableElement.getAttribute("description");
      String defaultValue  = projectVariableElement.getAttribute("defaultValue");
      return new ProjectVariable(id, name, description, defaultValue);
   }

   /**
    * Parse a <condition> element
    * The child nodes are added top the actionlist
    * 
    * @param    conditionElement <condition> element
    * @param    variableList 
    * @param    projectActionList The device to add action to
    * 
    * @return   File list described 
    * @throws Exception 
    */
   private Condition parseConditionElement(
         Element conditionElement, 
         Map<String, ProjectVariable> variableList, 
         ProjectActionList projectActionList, 
         IPath root) throws Exception {

      String variableName = conditionElement.getAttribute("variable");
      ProjectVariable variable      = variableList.get(variableName);
      String          defaultValue  = "false";
      boolean         negated       = false;

      if (variable == null) {
         throw new Exception("Variable \'"+variableName+"\' not found in <condition>");
      }

      if (conditionElement.hasAttribute("defaultValue")) {
         defaultValue = conditionElement.getAttribute("defaultValue");
      }
      if (conditionElement.hasAttribute("negated")) {
         negated = Boolean.valueOf(conditionElement.getAttribute("negated"));
      }

      Condition condition = new Condition(variable, defaultValue, negated);

      // <condition>
      for (Node node = conditionElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         // element node for <fileList>
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         //         System.err.println("parseFileListElements() " + node.getNodeName());
         Element element = (Element) node;
         if (element.getTagName() == "excludeSourceFile") {
            ExcludeAction excludeAction = parseExcludeSourceFileElement(element);
            excludeAction.setCondition(condition);
            projectActionList.add(excludeAction);
         }
         else if (element.getTagName() == "excludeSourceFolder") {
            ExcludeAction excludeAction = parseExcludeSourceFolderElement(element);
            excludeAction.setCondition(condition);
            projectActionList.add(excludeAction);
         }
         else if (element.getTagName() == "createFolder") {
            CreateFolderAction action = parseCreateFolderElement(element);
            action.setCondition(condition);
            action.setRoot(root.toPortableString());
            projectActionList.add(action);
         }
         else if (element.getTagName() == "copy") {
            FileAction fileInfo = parseCopyElement(element);
            fileInfo.setRoot(root.toPortableString());
            fileInfo.setCondition(condition);
            projectActionList.add(fileInfo);
         }
         else if (element.getTagName() == "projectOption") {
            ProjectOption projectOption = parseProjectOptionElement(element);
            projectOption.setCondition(condition);
            projectActionList.add(projectOption);
         }
         else if (element.getTagName() == "customAction") {
            ProjectCustomAction action = parseCustomActionElement(element);
            action.setCondition(condition);
            projectActionList.add(action);
         }
         else {
            throw new Exception("Unexpected element \""+element.getTagName()+"\" in <condition>");
         }
      }
      return condition;
   }

   /**
    * Parse a <excludeSourceFile> element
    * 
    * @param    fileElement <excludeSourceFile> element
    * 
    * @return   File list described 
    * @throws Exception 
    */
   private ExcludeAction parseExcludeSourceFileElement(Element element) throws Exception {
      // <excludeFile target="..." excluded="..."  >
      String target     = element.getAttribute("target");
      boolean isExcluded = true;
      if (element.hasAttribute("excluded")) {
         isExcluded = !element.getAttribute("excluded").equalsIgnoreCase("false");
      }
      ExcludeAction fileInfo = new ExcludeAction(null, target, isExcluded, false);
      return fileInfo;
   }

   /**
    * Parse a <excludeSourceFolder> element
    * 
    * @param    fileElement <excludeSourceFolder> element
    * 
    * @return   File list described 
    * @throws Exception 
    */
   private ExcludeAction parseExcludeSourceFolderElement(Element element) throws Exception {
      // <excludeFolder target="..." excluded="..."  >
      String target     = element.getAttribute("target");
      boolean isExcluded = true;
      if (element.hasAttribute("excluded")) {
         isExcluded = !element.getAttribute("excluded").equalsIgnoreCase("false");
      }
      ExcludeAction fileInfo = new ExcludeAction(null, target, isExcluded, true);
      return fileInfo;
   }

   /**
    * Parse a <createFolder> element
    * 
    * @param  createFolderElement <createFolder> element
    * 
    * @return   File list described 
    * @throws Exception 
    */
   private CreateFolderAction parseCreateFolderElement(Element createFolderElement) {
      // <createFolder target="..." type="..." >
      String target  = createFolderElement.getAttribute("target");
      String type    = createFolderElement.getAttribute("type");
      return new CreateFolderAction(target, type);
   }

   /**
    * Parse a <copy> element
    * 
    * @param    fileElement <copy> element
    * 
    * @return   File list described 
    * @throws Exception 
    */
   private FileAction parseCopyElement(Element element) throws Exception {
      // <copy>
      String source     = element.getAttribute("source");
      String target     = element.getAttribute("target");
      if (source.isEmpty() || target.isEmpty()) {
         throw new Exception("Missing attribute in <file ...>");
      }
      String type       = element.getAttribute("type");
      FileType fileType = FileType.NORMAL;
      if (type.equalsIgnoreCase("link")) {
         fileType = FileType.LINK;
      }
      // Default to true
      boolean doMacroReplacement = !element.getAttribute("macroReplacement").equalsIgnoreCase("false");
      // Default to false
      boolean doReplacement      =  element.getAttribute("replace").equalsIgnoreCase("true");
      FileAction fileInfo        = new FileAction(null, source, target, fileType, doMacroReplacement, doReplacement);
      return fileInfo;
   }

   /**
    * Parse a <projectOption> element
    * 
    * @param    element <projectOption> element
    * 
    * @return   element described 
    * 
    * @throws Exception 
    */
   private ProjectOption parseProjectOptionElement(Element optionElement) throws Exception {
      // <projectOption>
      String id       = optionElement.getAttribute("id");
      String path     = null;
      if (optionElement.hasAttribute("path")) {
         path = optionElement.getAttribute("path");
      }
      boolean replace = false;
      if (optionElement.hasAttribute("replace")) {
         replace = optionElement.getAttribute("replace").equalsIgnoreCase("true");
      }
      if (id.isEmpty()) {
         throw new Exception("<projectOption> is missing required attribute");
      }
      ArrayList<String> values = new ArrayList<String>();

      for (Node node = optionElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == "value") {
            values.add(element.getTextContent().trim());
         }
      }
      if (values.size() == 0) {
         throw new Exception("Missing <value> element in <projectOption>");
      }
      //   System.err.println("parseOptionElement() value = "+values.get(0));

      return new ProjectOption(id, path, values.toArray(new String[values.size()]), replace);
   }

   /**
    * Parse a <customAction> element
    * 
    * @param    element <customAction> element
    * 
    * @return   element described 
    * 
    * @throws Exception 
    */
   private ProjectCustomAction parseCustomActionElement(Element customActionElement) throws Exception {
      // <projectOption>
      String className  = customActionElement.getAttribute("class");
      if (className.isEmpty()) {
         throw new Exception("<customAction> is missing required attribute");
      }
      ArrayList<String> values = new ArrayList<String>();

      for (Node node = customActionElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == "value") {
            values.add(element.getTextContent().trim());
         }
      }
      //   System.err.println(String.format("parseCustomActionElement(%s, %s)", className, (values.size()==0)?"<empty>":values.get(0)));

      return new ProjectCustomAction(className, values.toArray(new String[values.size()]));
   }

}
