package net.sourceforge.usbdm.packageParser;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import net.sourceforge.usbdm.deviceDatabase.Device;
import net.sourceforge.usbdm.jni.Usbdm;
import net.sourceforge.usbdm.packageParser.FileAction.FileType;
import net.sourceforge.usbdm.packageParser.ProjectActionList.Visitor;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.SWT;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class PackageParser {

   private String lastProjectActionId = null;
   private int    projectActionId     = 0;

   /**
    * Locates actions that apply to this device. <br>
    * Only evaluates the top-level &lt;appliesWhen&gt; clause when deciding inclusion
    * 
    * @param device      Device to investigate
    * @param variableMap Variables to use when evaluation conditions
    * 
    * @return Applicable actions
    */
   static public ProjectActionList getDevicePackageList(final Device device, final Map<String, String> variableMap) {
      //      System.err.println(String.format("PackageParser.getDevicePackageList(): Device = %s",  device.getName()));
      ProjectActionList projectActionList = new ProjectActionList("---root " + device.getName() + " ---");
      IPath packagesDirectoryPath = Usbdm.getResourcePath().append("Stationery/Packages");
      File[] packageDirectories = packagesDirectoryPath.toFile().listFiles();
      Arrays.sort(packageDirectories);
      if (packageDirectories == null) {
         System.err.println("No packages found at " + packagesDirectoryPath.toOSString());
         return projectActionList;
      }
      final HashMap<String, ProjectVariable> projectVariables = new HashMap<String, ProjectVariable>();
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
                     PackageParser  packParser = new PackageParser(path, projectVariables);
                     Document       document   = packParser.parseXmlFile(f.toString());
                     ProjectActionList newProjectActionList = packParser.parseDocument(document);
                     if (newProjectActionList.appliesTo(device, variableMap)) {
//                        System.err.println("projectAction ID = " + newProjectActionList.getId());
//                        System.err.println("projectAction applyWhen = " + newProjectActionList.getApplyWhenCondition());
//                        for (ProjectAction projectAction : projectActionList.getProjectActionList()) {
//                           System.err.println("projectAction = " + projectAction.toString());
//                        }
//                        System.err.println("===============================================");
                        /*
                         * Add new project variables
                         */
                        for (String key:packParser.getCurrentVariables().keySet()) {
                           projectVariables.put(key, packParser.getCurrentVariables().get(key));
                        }
                        /*
                         * Collect constants for action-list
                         */
                        final Visitor visitor = new ProjectActionList.Visitor() {
                           @Override
                           public Result applyTo(ProjectAction action, ProjectActionList.Value result, IProgressMonitor monitor) {
                              try {
                                 //                                 System.err.println(String.format("PackageParser.getDevicePackageList(): applyTo(), %s",  action.toString()));
                                 if (action instanceof ProjectActionList) {
                                    ProjectActionList projectActionList = (ProjectActionList) action;
                                    Result rc = projectActionList.appliesTo(device, variableMap)?CONTINUE:PRUNE;
                                    //                                  System.err.println(String.format("PackageParser.getDevicePackageList(): Found ProjectActionList, %d, \'%s\'",  depth++, rc.toString()));
                                    return rc;
                                 }
                                 else if (action instanceof ProjectConstant) {
                                    ProjectConstant projectConstant = (ProjectConstant) action;
                                    //                                    System.err.println(String.format("PackageParser.getDevicePackageList(): Adding constant %s => %s",  projectConstant.getId(), projectConstant.getValue()));
                                    if (!projectConstant.doOverwrite()) {
                                       String value = variableMap.get(projectConstant.getId());
                                       if ((value != null) && !value.equals(projectConstant.getValue())) {
                                          return new Result(new Exception("Repeated constant"));
                                       }
                                    }
                                    variableMap.put(projectConstant.getId(), projectConstant.getValue());
                                 }
                                 return PRUNE;
                              } catch (Exception e) {
                                 return new Result(e);
                              }
                           }
                        };
                        newProjectActionList.visit(visitor, null);
                        // Add applicable actions
                        projectActionList.addProjectAction(newProjectActionList);
                     }
                  } catch (Exception e) {
                     e.printStackTrace();
                  }
               }
            }
         }
      }
      //      listOfProjectLists.setfVariableMap(projectVariables);
      return projectActionList;
   }

   /**
    * Locates all package lists
    * 
    * @param variableMap Variables to use when evaluation conditions for inclusion
    * 
    * @return ArrayList of ProjectActionLists (an empty list if none)
    * @throws Exception 
    */
   static public ProjectActionList getKDSPackageList(final Map<String, String> variableMap) throws Exception {
      ProjectActionList projectActionList = new ProjectActionList("---KDSPackageList---"); 
      IPath packagesDirectoryPath = Usbdm.getResourcePath().append("Stationery/KSDK_Libraries");
      File[] packageDirectories = packagesDirectoryPath.toFile().listFiles();
      Arrays.sort(packageDirectories);
      if (packageDirectories == null) {
         System.err.println("No packages found at " + packagesDirectoryPath.toOSString());
         return projectActionList;
      }
      HashMap<String, ProjectVariable> projectVariables = new HashMap<String, ProjectVariable>();
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
                     PackageParser  packParser = new PackageParser(path, projectVariables);
                     Document       document   = packParser.parseXmlFile(f.toString());
                     ProjectActionList newProjectActionList = packParser.parseDocument(document);
                     if (newProjectActionList.applies(variableMap)) {
                        //                        System.err.println("===============================================");
                        //                        System.err.println("projectAction = " + newProjectActionList.toString());
                        //                        System.err.println("projectAction applyWhen = " + newProjectActionList.getApplyWhenCondition().toString());


                        /*
                         * Collect constants for action-list
                         */
                        final Visitor visitor = new ProjectActionList.Visitor() {
                           @Override
                           public Result applyTo(ProjectAction action, ProjectActionList.Value result, IProgressMonitor monitor) {
                              try {
                                 //                                 System.err.println(String.format("PackageParser.getKDSPackageList(): applyTo(), %s",  action.toString()));
                                 if (action instanceof ProjectActionList) {
                                    ProjectActionList projectActionList = (ProjectActionList) action;
                                    Result rc = projectActionList.applies(variableMap)?CONTINUE:PRUNE;
                                    //                                    System.err.println(String.format("PackageParser.getDevicePackageList(): Found ProjectActionList, %d, \'%s\'",  depth++, rc.toString()));
                                    return rc;
                                 }
                                 else if (action instanceof ProjectConstant) {
                                    ProjectConstant projectConstant = (ProjectConstant) action;
                                    //                                    System.err.println(String.format("PackageParser.getDevicePackageList(): Adding constant %s => %s",  projectConstant.getId(), projectConstant.getValue()));
                                    if (!projectConstant.doOverwrite()) {
                                       String value = variableMap.get(projectConstant.getId());
                                       if ((value != null) && !value.equals(projectConstant.getValue())) {
                                          return new Result(new Exception("Repeated constant"));
                                       }
                                    }
                                    variableMap.put(projectConstant.getId(), projectConstant.getValue());
                                 }
                                 return PRUNE;
                              } catch (Exception e) {
                                 return new Result(e);
                              }
                           }
                        };
                        newProjectActionList.visit(visitor, null);
                        // Add applicable actions
                        projectActionList.addProjectAction(newProjectActionList);
                     }
                  } catch (Exception e) {
                     e.printStackTrace();
                  }
               }
            }
         }
      }
      //      listOfProjectLists.setfVariableMap(projectVariables);
      return projectActionList;
   }

   private IPath                            fPath                    = null;
   private HashMap<String, ProjectVariable> fProjectVariables        = null;
   private HashMap<String, ProjectVariable> fCurrentProjectVariables = null;

   /**
    * Create package parser
    * 
    * @param path              Path to where package is - Used for default directory to look for files
    * @param projectVariables  Project variables used for conditions etc. 
    */
   protected PackageParser(IPath path, HashMap<String, ProjectVariable> projectVariables) {
      fPath                      = path;
      fProjectVariables          = projectVariables;
      fCurrentProjectVariables   = new HashMap<String, ProjectVariable>();
   }

   /**
    * Get project variables added while parsing the current package
    * 
    * @return Project variables added
    */
   protected HashMap<String, ProjectVariable> getCurrentVariables() {
      return fCurrentProjectVariables;
   }

   /**
    * Find a variable
    * 
    * @param variableName
    * 
    * @return variable found or null if non-existent
    */
   private ProjectVariable findVariable(String variableName) {
      ProjectVariable rv = fCurrentProjectVariables.get(variableName);
      if (rv == null) {
         rv = fProjectVariables.get(variableName);
      }
      return rv;
   }

   /**
    * Parse the XML file into the XML internal DOM representation
    * 
    * @param path Full path to XML file
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

      dbf.setXIncludeAware(true);
      dbf.setNamespaceAware(true);

      DocumentBuilder        db  = dbf.newDocumentBuilder();

      //  Parse using builder to get DOM representation of the XML file
      return db.parse(databasePath.toOSString());
   }

   /**
    * @param projectActionList2 
    * @return ArrayList of ProjectActionLists (may be empty but never null)
    * 
    * @throws Exception
    */
   private ProjectActionList parseDocument(Document documentElement) throws Exception {
      ProjectActionList projectActionList = null;
      Element root = documentElement.getDocumentElement();
      for (Node node = root.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         try {
            Element element = (Element) node;
            if (element.getTagName() == "projectActionList") {
               if (projectActionList != null) {
                  throw new Exception("multiple <projectActionList> found in document' - \'" + documentElement.getDocumentURI());
               }
               projectActionList = parseProjectActionListTopElement(element);
            }
            else {
               throw new Exception("Unexpected field in <root>' - \'" + element.getTagName() + "\' found");
            }
         } catch (Exception e) {
            System.err.println("Exception while parsing URI = " + documentElement.getDocumentURI());
            e.printStackTrace();
         }
      }
      return projectActionList;
   }

   /**
    * Parse a <projectActionList> element
    * 
    * @param    projectActionListElement <projectActionList> element
    * @param    isTop Indicates this is the top <projectActionList> element
    * 
    * @return   Action list described 
    * 
    * @throws Exception 
    */
   private ProjectActionList parseProjectActionListElement(Element projectActionListElement, boolean isTop) throws Exception {

      String id = null;
      if (projectActionListElement.hasAttribute("id")) {
         id = projectActionListElement.getAttribute("id");
         lastProjectActionId = id;
         projectActionId = 1;
      }
      else {
         if (isTop) {
            throw new Exception("Top <projectActionList> must have an 'id'");
         } 
         id = lastProjectActionId + "-" + projectActionId++;
      }
      ProjectActionList projectActionList = new ProjectActionList(id);
      //    System.err.println("parseProjectActionListElement(): " +  id);

      IPath root = fPath;
      if (projectActionListElement.hasAttribute("root")) {
         root = root.append(projectActionListElement.getAttribute("root"));
      }

      for (Node node = projectActionListElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         try {
            // Child node for <projectActionList>
            Element element = (Element) node;
            if (element.getTagName() == "applyWhen") {
               projectActionList.setApplyWhenCondition(parseApplyWhenElement(element));
            }
            else if (element.getTagName() == "variable") {
               ProjectVariable newVariable = parseVariableElement(element);
               ProjectVariable variable    = findVariable(newVariable.getId());
               if (variable == null) {
                  fCurrentProjectVariables.put(newVariable.getId(), newVariable);
                  variable = newVariable;
                  //                  System.err.println("parseProjectActionListElement(): Adding variable " + newVariable.getId() + " from " + id);
               }
               projectActionList.add(variable);
            }
            else if (element.getTagName() == "page") {
               projectActionList.add(parsePageElement(element));
            }
            else if (element.getTagName() == "constant") {
               projectActionList.add(parseConstantElement(element));
            }
            else if (element.getTagName() == "projectActionList") {
               projectActionList.add(parseProjectActionListElement(element, false));
            }
            else if (element.getTagName() == "excludeSourceFile") {
               projectActionList.add(parseExcludeSourceFileElement(element));
            }
            else if (element.getTagName() == "excludeSourceFolder") {
               projectActionList.add(parseExcludeSourceFolderElement(element));
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
            else if (element.getTagName() == "deleteResource") {
               DeleteResourceAction fileInfo = parseDeleteElement(element);
               fileInfo.setRoot(root.toPortableString());
               projectActionList.add(fileInfo);
            }
            else if (element.getTagName() == "customAction") {
               projectActionList.add(parseCustomActionElement(element));
            }
            else if (element.getTagName() == "projectOption") {
               projectActionList.add(parseProjectOptionElement(element));
            }
            else {
               throw new Exception("Unexpected element \""+element.getTagName()+"\"");
            }
         } catch (Exception e) {
            System.err.println("Exception while parsing <" + projectActionListElement.getNodeName() + ">");
            throw (e);
         }
      }
      return projectActionList;
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
   private ProjectActionList parseProjectActionListTopElement(Element projectActionListElement) throws Exception {
      return parseProjectActionListElement(projectActionListElement, true);
   }

   /**
    * Parse a <projectActionList> element
    * 
    * @param    projectActionListElement <projectActionList> element
    * @param    path                     Path to use as base for any file actions
    * 
    * @return   Action list described 
    * 
    * @throws Exception 
    */
   public static ProjectActionList parseRestrictedProjectActionList(Element projectActionListElement, String path) throws Exception {

      String id = null;
      if (projectActionListElement.hasAttribute("id")) {
         id = projectActionListElement.getAttribute("id");
      }
      boolean doOnceOnly = false;
      if (projectActionListElement.hasAttribute("doOnceOnly")) {
         doOnceOnly = Boolean.valueOf(projectActionListElement.getAttribute("doOnceOnly"));
      }
      ProjectActionList projectActionList = new ProjectActionList(id);
      projectActionList.setDoOnceOnly(doOnceOnly);
      
      //    System.err.println("parseProjectActionListElement(): " +  id);

      IPath root;
      if (path.charAt(0) == '/') {
         root = new Path(path);
      }
      else {
         root = Usbdm.getResourcePath();
         if (projectActionListElement.hasAttribute("root")) {
            root = root.append("/"+projectActionListElement.getAttribute("root"));
         }
         else {
            root = root.append("/"+path);
         }
      }
      for (Node node = projectActionListElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         try {
            // Child node for <projectActionList>
            Element element = (Element) node;
            if (element.getTagName() == "excludeSourceFile") {
               projectActionList.add(parseExcludeSourceFileElement(element));
            }
            else if (element.getTagName() == "excludeSourceFolder") {
               projectActionList.add(parseExcludeSourceFolderElement(element));
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
            else if (element.getTagName() == "deleteResource") {
               DeleteResourceAction fileInfo = parseDeleteElement(element);
               fileInfo.setRoot(root.toPortableString());
               projectActionList.add(fileInfo);
            }
            else if (element.getTagName() == "customAction") {
               projectActionList.add(parseCustomActionElement(element));
            }
            else if (element.getTagName() == "projectOption") {
               projectActionList.add(parseProjectOptionElement(element));
            }
            else if (element.getTagName() == "constant") {
               projectActionList.add(parseConstantElement(element));
            }
            else {
               throw new Exception("Unexpected element \""+element.getTagName()+"\"");
            }
         } catch (Exception e) {
            System.err.println("Exception while parsing <" + projectActionListElement.getNodeName() + ">");
            throw (e);
         }
      }
      return projectActionList;
   }

   /**
    * Parse a <applyWhen> element
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
    *  <requirement>, <and>, <or> or <not> element
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
      else if (element.getTagName() == "deviceNameMatches") {
         return new ApplyWhenCondition(ApplyWhenCondition.Type.deviceNameMatches, element.getTextContent().trim());
      }
      else if (element.getTagName() == "deviceFamilyIs") {
         return new ApplyWhenCondition(ApplyWhenCondition.Type.deviceFamilyIs, element.getTextContent().trim());
      }
      else if (element.getTagName() == "deviceFamilyMatches") {
         return new ApplyWhenCondition(ApplyWhenCondition.Type.deviceFamilyMatches, element.getTextContent().trim());
      }
      else if (element.getTagName() == "deviceSubfamilyIs") {
         return new ApplyWhenCondition(ApplyWhenCondition.Type.deviceSubfamilyIs, element.getTextContent().trim());
      }
      else if (element.getTagName() == "deviceSubfamilyMatches") {
         return new ApplyWhenCondition(ApplyWhenCondition.Type.deviceSubfamilyMatches, element.getTextContent().trim());
      }
      else if (element.getTagName() == "hardwareIs") {
         return new ApplyWhenCondition(ApplyWhenCondition.Type.hardwareIs, element.getTextContent().trim());
      }
      else if (element.getTagName() == "hardwareMatches") {
         return new ApplyWhenCondition(ApplyWhenCondition.Type.hardwareMatches, element.getTextContent().trim());
      }
      else if (element.getTagName() == "variableRef") {
         String                        variableName  = element.getAttribute("idRef").trim();
         boolean                       defaultValue  = false;
         String                        value         = null;
         ApplyWhenCondition.Condition  condition     = ApplyWhenCondition.Condition.isTrue;
         if (element.hasAttribute("default")) {
            defaultValue = Boolean.valueOf(element.getAttribute("default"));
         }
         if (element.hasAttribute("condition")) {
            String sCondition = element.getAttribute("condition");
            condition = ApplyWhenCondition.Condition.valueOf(sCondition);
         }
         if (element.hasAttribute("value")) {
            value = element.getAttribute("value");
         }
         return new ApplyWhenCondition(ApplyWhenCondition.Type.variableRef, variableName, defaultValue, condition, value);
      }
      else if (element.getTagName() == "requirement") {
         String variableName  = element.getAttribute("idRef").trim();
         return new ApplyWhenCondition(ApplyWhenCondition.Type.requirement, variableName);
      }
      else if (element.getTagName() == "preclusion") {
         String variableName  = element.getAttribute("idRef").trim();
         return new ApplyWhenCondition(ApplyWhenCondition.Type.preclusion, variableName);
      }
      else if (element.getTagName() == "and") {
         return new ApplyWhenCondition(ApplyWhenCondition.Type.and, parseApplyWhenLogicalElement(element));
      }
      else if (element.getTagName() == "or") {
         return new ApplyWhenCondition(ApplyWhenCondition.Type.or, parseApplyWhenLogicalElement(element));
      }
      else if (element.getTagName() == "not") {
         ArrayList<ApplyWhenCondition> list = parseApplyWhenLogicalElement(element);
         return new ApplyWhenCondition(ApplyWhenCondition.Type.not, list);
      } 
      throw new Exception("Unexpected element in <applyWhen> \'" + element.getTagName() + "\'");
   }

   /**
    * Parse a <and> or <or> or <not> element
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
    * Parse a &lt;group&gt; element <br>
    * 
    *  &lt;page id="..." name="..." description="..."&gt; <br>
    *     &lt;group id="..." name="..." &gt; <br>
    *     ... <br>
    *  &lt;/page&gt;
    *  
    * @param  pageElement &lt;group&gt; element
    * 
    * @return WizardGroup described 
    * @throws Exception 
    */
   private WizardPageInformation parsePageElement(Element pageElement) throws Exception {
      String id          = pageElement.getAttribute("id");
      String name        = pageElement.getAttribute("name");
      String description = pageElement.getAttribute("description");

      if ((id == null) || (name == null) || (description == null)) {
         throw new Exception("Top <group> must have an 'id', 'name' and 'description'");
      } 
      WizardPageInformation wizardPage = new WizardPageInformation(id, name, description);

      for (Node node = pageElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         try {
            // Child node for <group>
            Element element = (Element) node;
            if (element.getTagName() == "group") {
               wizardPage.addGroup(parseGroupElement(element));
            }
            else {
               throw new Exception("Unexpected element \""+element.getTagName()+"\"");
            }
         } catch (Exception e) {
            System.err.println("Exception while parsing <" + pageElement.getNodeName() + ">");
            throw (e);
         }
      }
      return wizardPage;
   }

   /**
    * Parse a <group> element
    * 
    * @param  groupElement <group> element
    * 
    * @return WizardGroup described 
    * @throws Exception 
    */
   private WizardGroup parseGroupElement(Element groupElement) throws Exception {
      // <group id="..." name="..." >
      String id   = groupElement.getAttribute("id");
      String name = groupElement.getAttribute("name");
      WizardGroup group = new WizardGroup(id, name);
      if (groupElement.hasAttribute("row")) {
         int row = Integer.parseInt(groupElement.getAttribute("row"));
         group.setRow(row);
      }
      if (groupElement.hasAttribute("col")) {
         int col = Integer.parseInt(groupElement.getAttribute("col"));
         group.setCol(col);
      }
      if (groupElement.hasAttribute("width")) {
         int width = Integer.parseInt(groupElement.getAttribute("width"));
         group.setWidth(width);
      }
      if (groupElement.hasAttribute("span")) {
         int span = Integer.parseInt(groupElement.getAttribute("span"));
         group.setSpan(span);
      }
      return group;
   }

   /**
    * Parse a <variable> element
    * 
    * @param    <variable> element
    * 
    * @return   File list described 
    * @throws Exception 
    */
   private ProjectVariable parseVariableElement(Element projectVariableElement) throws Exception {
      // <projectVariable>
      String id            = projectVariableElement.getAttribute("id");
      String name          = projectVariableElement.getAttribute("name");
      String description   = projectVariableElement.getAttribute("description");
      String defaultValue  = projectVariableElement.getAttribute("defaultValue");
      ProjectVariable projectVariable = new ProjectVariable(id, name, description, defaultValue);
      if (projectVariableElement.hasAttribute("radioGroup")) {
         projectVariable.setGroup(projectVariableElement.getAttribute("radioGroup"), SWT.RADIO);
      }
      else if (projectVariableElement.hasAttribute("checkGroup")) {
         projectVariable.setGroup(projectVariableElement.getAttribute("checkGroup"), SWT.CHECK);
      }
      else {
         throw new Exception("<variable> must have either radioGroup or checkGroup, id = " + id);
      }
      if ((id.length() == 0)) {
         throw new Exception("Variable must have 'id' attribute");
      }
      for (Node node = projectVariableElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         // element node for <variable ...>
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == "enableWhen") {
            projectVariable.setRequirement(parseApplyWhenElement(element));
         }
         else {
            throw new Exception("Unexpected element \""+element.getTagName()+"\" in <projectVariable>");
         }
      }
      return projectVariable;
   }

   /**
    * Parse a <constant> element
    * 
    * @param    <constant> element
    * 
    * @return   File list described 
    * @throws Exception 
    */
   private static ProjectConstant parseConstantElement(Element projectConstantElement) throws Exception {
      // <constant>
      String id            = projectConstantElement.getAttribute("id");
      String value         = projectConstantElement.getAttribute("value");
      value = value.replaceAll("\\\\n", "\n");
      value = value.replaceAll("\\\\t", "\t");
      String overwrite     = projectConstantElement.getAttribute("overwrite");
      String weak          = projectConstantElement.getAttribute("weak");
      boolean doOverwrite  = overwrite.equalsIgnoreCase("true");
      boolean isWeak       = weak.equalsIgnoreCase("true");
      ProjectConstant projectconstant = new ProjectConstant(id, value, doOverwrite, isWeak);
      for (Node node = projectConstantElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         // element node for <variable ...>
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         throw new Exception("Unexpected element \""+element.getTagName()+"\" in <condition>");
      }
      return projectconstant;
   }

   /**
    * Parse a <excludeSourceFile> element
    * 
    * @param    fileElement <excludeSourceFile> element
    * 
    * @return   File list described 
    * @throws Exception 
    */
   private static ExcludeAction parseExcludeSourceFileElement(Element element) throws Exception {
      // <excludeFile target="..." excluded="..."  >
      String target     = element.getAttribute("target");
      boolean isExcluded = true;
      if (element.hasAttribute("excluded")) {
         isExcluded = !element.getAttribute("excluded").equalsIgnoreCase("false");
      }
      ExcludeAction fileInfo = new ExcludeAction(target, isExcluded, false);
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
   private static ExcludeAction parseExcludeSourceFolderElement(Element element) throws Exception {
      // <excludeFolder target="..." excluded="..."  >
      String target     = element.getAttribute("target");
      boolean isExcluded = true;
      if (element.hasAttribute("excluded")) {
         isExcluded = !element.getAttribute("excluded").equalsIgnoreCase("false");
      }
      ExcludeAction fileInfo = new ExcludeAction(target, isExcluded, true);
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
   private static CreateFolderAction parseCreateFolderElement(Element createFolderElement) {
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
    * 
    * @throws Exception 
    */
   private static FileAction parseCopyElement(Element element) throws Exception {
      // <copy>
      String source          = element.getAttribute("source");
      String target          = element.getAttribute("target");

      if (source.isEmpty() || target.isEmpty()) {
         throw new Exception("Missing attribute in <file ...>");
      }
      String type       = element.getAttribute("type");
      FileType fileType = FileType.NORMAL;
      if (type.equalsIgnoreCase("link")) {
         fileType = FileType.LINK;
      }
      FileAction.PathType sourcePathType = FileAction.PathType.UNKNOWN;
      String sSourcePathType = element.getAttribute("sourcePathType");
      if (sSourcePathType.equalsIgnoreCase("absolute")) {
         sourcePathType = FileAction.PathType.ABSOLUTE;
      }
      else if (sSourcePathType.equalsIgnoreCase("relative")) {
         sourcePathType = FileAction.PathType.RELATIVE;
      }
      // Default to true
      boolean doMacroReplace     = !element.getAttribute("macroReplace").equalsIgnoreCase("false");
      // Default to false
      boolean doFileOverwrite    =  element.getAttribute("overwrite").equalsIgnoreCase("true");
      // Default to false
      boolean doDerived          =  element.getAttribute("derived").equalsIgnoreCase("true");
      
      FileAction fileInfo        = new FileAction(source, target, fileType);
      
      fileInfo.setMacroReplace(doMacroReplace);
      fileInfo.setFileOverwrite(doFileOverwrite);
      fileInfo.setSourcePathType(sourcePathType);
      fileInfo.setDerived(doDerived);
      return fileInfo;
   }

   /**
    * Parse a <deleteResource> element
    * 
    * @param    fileElement <deleteResource> element
    * 
    * @return   File list described 
    * @throws Exception 
    */
   private static DeleteResourceAction parseDeleteElement(Element element) throws Exception {
      // <deleteResource>
      String target     = element.getAttribute("target");
      if (target.isEmpty()) {
         throw new Exception("Missing attribute in <deleteResource ...>");
      }
      return new DeleteResourceAction(target);
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
   private static ProjectOption parseProjectOptionElement(Element optionElement) throws Exception {
      // <projectOption>
      String id       = optionElement.getAttribute("id");
      String path     = null;
      if (optionElement.hasAttribute("path")) {
         path = optionElement.getAttribute("path");
      }
      String config   = null;
      if (optionElement.hasAttribute("config")) {
         config = optionElement.getAttribute("config");
      }
      if (config.equalsIgnoreCase("all")) {
         config = null;
      }
      boolean overwrite = false;
      if (optionElement.hasAttribute("overwrite")) {
         overwrite = optionElement.getAttribute("overwrite").equalsIgnoreCase("true");
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
      //   System.err.println("parseOptionElement() value = "+values.get(0));

      return new ProjectOption(id, path, values.toArray(new String[values.size()]), config, overwrite);
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
   private static ProjectCustomAction parseCustomActionElement(Element customActionElement) throws Exception {
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
