package net.sourceforge.usbdm.utilities.handlers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.IBinary;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import jakarta.inject.Named;

public class EstimateStackUsageHandler {

   /**
    * Display basic error dialogue
    * 
    * @param shell   Shell for dialogue
    * @param msg     Message to display
    */
   @SuppressWarnings("unused")
   private static void displayError(Shell shell, String msg) {
      MessageBox mbox = new MessageBox(shell, SWT.ICON_ERROR|SWT.OK);
      mbox.setMessage(msg);
      mbox.setText("USBDM - Can't create launch configuration");
      mbox.open();
   }

   /**
    * Process a call-graph file
    * 
    * @param path       Path to file
    * @param callGraph  The call-graph produced is added to this list
    * 
    * @throws Exception
    */
   void processFiles(Path path, ArrayList<Graph>callGraph) throws Exception {
      if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
         DirectoryStream<Path> stream = Files.newDirectoryStream(path);
         for (Path entry:stream) {
            processFiles(entry, callGraph);
         }
         stream.close();
      }
      else {
         if (path.getFileName().toString().endsWith(".ci")) {
            BufferedReader reader = Files.newBufferedReader(path);
            CI_Parser parser = new CI_Parser(reader);
            Graph graph = parser.parseGraph();
            callGraph.add(graph);
            reader.close();
         }
      }
   }

   enum StackSizeType {
      Unknown, Static, Dynamic, Bounded;

      static StackSizeType getStackType(String name) {
         if (name.equalsIgnoreCase("Static")) {
            return Static;
         }
         if (name.equalsIgnoreCase("Dynamic")) {
            return Dynamic;
         }
         if (name.equalsIgnoreCase("Bounded")) {
            return Bounded;
         }
         if (name.equalsIgnoreCase("Unknown")) {
            return Unknown;
         }
         return null;
      }
   };

   /**
    * Represents an annotated node in a call-graph.
    * The node represents a C function
    */
   class Node {

      /**
       * Overhead of calling a function
       * For ARM there is no call cost and pushing of link-reg in callee is already including in stack cost
       */
      static final int      callCost=0;

      /// Title of the node
      private String         fTitle;
      /// Label for the node
      private String         fLabel;
      /// File
      private String         fFile;
      /// Stack-size of this function in isolation (without callCost)
      private int            stackSize = 0;
      /// Type of stackSize information
      private StackSizeType  fStackSizeType = StackSizeType.Unknown;
      /// The worse stack size required for this function and its children (including callCosts)
      private int            worseStackSize = -1;
      /// The child function associated with the most expensive call-path
      private Node           worseChild = null;
      /// Functions called by this function
      private ArrayList<Node> targetNodes = new ArrayList<Node>();

      private boolean fReported = false;
      
      public String getTitle() {
         return fTitle;
      }

      public String getLabel() {
         return fLabel;
      }

      public String getFile() {
         return fFile;
      }

      public boolean isReported() {
         return fReported;
      }

      public StackSizeType getStackSizeType() {
         return fStackSizeType;
      }

      /**
       * Calculates the stack cost. <br>
       * It will return a cached value on subsequent calls.
       * 
       * @return Worse case stack cost to call this function (including calling overhead).
       */
      int getStackSize() {
         if (worseStackSize >= 0) {
            return worseStackSize;
         }
         worseStackSize = 0;
         for (Node targetNode:targetNodes) {
            int childCost  = targetNode.getStackSize();
            if (childCost>worseStackSize) {
               worseStackSize = childCost;
               worseChild     = targetNode;
            }
         }
         worseStackSize += stackSize + callCost;
         return worseStackSize;
      }

      static final int initialIndent      = 100;
      static final int incrmentalIndent   = 50;
      
      /**
       * Get the most expensive call-path
       * 
       * @return Call path as string including costs and function names
       * @throws IOException
       */
      void writeMostExpensiveCallPathHtml(HtmlWriter writer) throws IOException {
         writer.writeParagraph(0, String.format("(%d) = (%s) %s",
               getStackSize(),
               (fStackSizeType==StackSizeType.Unknown)?"-":Integer.toString(stackSize+callCost),
                     "<b>"+HtmlWriter.escapeHtml(fLabel)+"</b>"));
         if (worseChild != null) {
            worseChild.writeMostExpensiveCallPathHtml(writer, initialIndent);
         }
      }
      
      /**
       * Get the most expensive call-path
       * 
       * @param sb Used to assemble the call-path as string including costs and function names
       * @throws IOException
       */
      private void writeMostExpensiveCallPathHtml(HtmlWriter writer, int indent) throws IOException {
         writer.writeParagraph(indent,
               String.format(
                     "(%s) %s ",
                     (fStackSizeType==StackSizeType.Unknown)?"-":Integer.toString(stackSize+callCost),
                           "<b>"+HtmlWriter.escapeHtml(fLabel)+"</b>"));
         if (worseChild != null) {
            worseChild.writeMostExpensiveCallPathHtml(writer, indent+incrmentalIndent);
         }
      }

      /**
       * Get the most expensive call-path
       * 
       * @return Call path as string including costs and function names
       */
      String getMostExpensiveCallPath() {
         getStackSize();
         StringBuilder sb = new StringBuilder();
         getMostExpensiveCallPath(sb);
         return sb.toString();
      }

      /**
       * Get the most expensive call-path
       * 
       * @param sb Used to assemble the call-path as string including costs and function names
       */
      private void getMostExpensiveCallPath(StringBuilder sb) {
         fReported = true;
         sb.append(String.format("(%s) %s ", (fStackSizeType==StackSizeType.Unknown)?"-":Integer.toString(stackSize+callCost), fLabel));
         if (worseChild != null) {
            sb.append("=> ");
            worseChild.getMostExpensiveCallPath(sb);
         }
      }

      /**
       * Create node from information derived from call-graph generated by GCC with <b>-fcallgraph-info=su</b> option
       * 
       * @param title      Title of function
       * @param label      Label describing node (including stack depth)
       * 
       * @throws Exception
       */
      Node(String title, String label) throws Exception {
         fTitle = title;
         String[] parts = label.split("\n");
         fLabel = parts[0];
         if (parts.length>1) {
            // This is the filename:line:col e.g '.../Sources/delay.c:112:6'
            fFile = parts[1];
         }
         if (parts.length>2) {
            // This is the stack usage e.g. 'nn bytes (static)'
            Pattern p = Pattern.compile("\\s*(\\d*)\\s*bytes\\s*\\(\\s*(\\w*)\\s*\\)");
            Matcher m = p.matcher(parts[2]);
            if (!m.matches()) {
               throw new Exception("Expected similar to 'nn bytes (static)', found "+parts[2]);
            }
            stackSize = Integer.parseInt(m.group(1));
            fStackSizeType = StackSizeType.getStackType(m.group(2));
         }
      }

      @Override
      public String toString() {
         return String.format("node { stack=%-6s, title=%-60s label=%-70s }",
               (fStackSizeType==StackSizeType.Unknown)?"?":Integer.toString(callCost+stackSize),
               "'"+fTitle+"'",
               "'"+fLabel+"'");
      }
      
      public String toHtml() {
         return (String.format("node {<br>stack=%-6s<br> title=%-60s<br> label=%-70s<br> }<br>",
               "<b>"+((fStackSizeType==StackSizeType.Unknown)?"&quest;":Integer.toString(callCost+stackSize))+"</b>",
               "<b>"+HtmlWriter.escapeHtml(fTitle)+"</b>",
               "<b>"+HtmlWriter.escapeHtml(fLabel)+"</b>"));
      }
   }

   /**
    * Class representing an edge in the call-graph i.e. a function call
    */
   class Edge {
      
      // Label describing the call
      private String fLabel;
      // Caller function
      private String fSource;
      // Callee function
      private String fTarget;

      /**
       * Get label describing the call
       * 
       * @return
       */
      public String getLabel() {
         return fLabel;
      }
      
      /**
       * Get caller function
       * 
       * @return
       */
      public String getSource() {
         return fSource;
      }
      
      /**
       * Get callee function
       * 
       * @return
       */
      public String getTarget() {
         return fTarget;
      }
      
      /**
       * Create an edge
       * 
       * @param label   Label describing the call
       * @param source  Caller function
       * @param target  Callee function
       */
      Edge(String label, String source, String target) {
         fLabel  = label;
         fSource = source;
         fTarget = target;
      }
      @Override
      public String toString() {
         return("edge { source='"+fSource+"', target='"+fTarget+"', label='"+fLabel+"'}");
      }
      public String toHtml() {
         return ("edge {<br> source=<b>"+HtmlWriter.escapeHtml(fSource)+"</b><br>"
               + " target=<b>"+HtmlWriter.escapeHtml(fTarget)+"</b><br>"
                     + " label=<b>"+HtmlWriter.escapeHtml(fLabel)+"</b><br>}<br>");
      }
   }

   /**
    * Class representing an entire call-graph
    */
   class Graph {
      
      // Nodes in the graph (functions)
      private ArrayList<Node> fNodes = new ArrayList<Node>();
      // Edges in the graph (function calls)
      private ArrayList<Edge> fEdges = new ArrayList<Edge>();
      // File graph was loaded from
      private final String fFilename;

      public ArrayList<Node> getNodes() {
         return fNodes;
      }

      public ArrayList<Edge> getEdges() {
         return fEdges;
      }

      public String getfFilename() {
         return fFilename;
      }

      /**
       * Create empty graph
       * 
       * @param filename   Filename for graph
       */
      Graph(String filename) {
         fFilename = filename;
      }

      /**
       * Lists nodes and edges in graph to System.out
       */
      void report() {
         fNodes.forEach(new Consumer<Node>() {
            @Override
            public void accept(Node node) {
               System.out.print(node.toString());
            }

         });
         fEdges.forEach(new Consumer<Edge>() {
            @Override
            public void accept(Edge edge) {
               System.out.print(edge.toString());
            }
         });
      }

      @Override
      public String toString() {
         return new String("Graph for '"+fFilename+"' "+fNodes.size()+" nodes, "+fEdges.size()+" edges");
      }
   }

   /**
    * Class used to tokenize ci graph file e.g. main.ci
    */
   private class CI_Parser {

      StreamTokenizer fStreamTokenizer;
      int             fCurrentToken;
      Graph           fGraph;

      /**
       * Create parser for reader
       * 
       * @param reader Reader to obtain text from
       */
      public CI_Parser(Reader reader) {

         fStreamTokenizer = new StreamTokenizer(reader);
         fStreamTokenizer.ordinaryChar('/');

         try {
            fCurrentToken = fStreamTokenizer.nextToken();
         } catch (IOException e) {
            fCurrentToken = StreamTokenizer.TT_EOF;
         }
      }

      private void getNext() throws IOException {
         fCurrentToken = fStreamTokenizer.nextToken();
      }

      /**
       * Get identifier e.g. 'name:' or 'title   :'
       * 
       * @return Identifier found or null if none
       * 
       * @throws Exception if the identifier was not followed by a colon.
       */
      //      private String getIdentifier() throws Exception {
      //         if (currentToken != StreamTokenizer.TT_WORD) {
      //            return null;
      //         }
      //         String val = streamTokenizer.sval;
      //         getNext();
      //         if (currentToken != ':') {
      //            throw new Exception("Expected ':' after identifier '"+val+"'");
      //         }
      //         getNext();
      //         return val;
      //      }

      /**
       * Get identifier e.g. name or title. <br>
       * If the expected tokens are found, then they are consumed.
       * 
       * @return Identifier found or null if none
       */
      private String getSimpleIdentifier() throws Exception {
         if (fCurrentToken != StreamTokenizer.TT_WORD) {
            return null;
         }
         String val = fStreamTokenizer.sval;
         getNext();
         return val;
      }

      /**
       * Check if the next tokens are the given identifier followed by a colon. <br>
       * If the expected tokens are found, then they are consumed.
       * 
       * @param id   Identifier to check
       * 
       * @return  true if an identifier followed by colon is found
       * 
       * @throws Exception if matching identifier is not followed by a colon
       */
      private boolean checkIdentifier(String id) throws Exception {
         if (fCurrentToken != StreamTokenizer.TT_WORD) {
            return false;
         }
         String val = fStreamTokenizer.sval;
         if (!val.equals(id)) {
            return false;
         }
         getNext();
         if (fCurrentToken != ':') {
            throw new Exception("Expected ':' after identifier '"+val+"'");
         }
         getNext();
         return true;
      }

      /**
       * Get a quoted string e.g. "name" or "title". <br>
       * Expected tokens are consumed.
       * 
       * @return String found or null if none
       * 
       * @throws Exception on unexpected token.
       */
      private String getQuotedString() throws Exception {
         if (fCurrentToken != '"') {
            throw new Exception("Quoted string expected");
         }
         String value = fStreamTokenizer.sval;
         getNext();
         return value;
      }

      /**
       * Parse a node { [title: "a_title"| label: "a_label" |shape: "a_shape"] }
       * 
       * @throws Exception on unexpected token.
       */
      private void getNode() throws Exception {
         if (fCurrentToken != '{') {
            throw new Exception("'}' expected after 'node:'");
         }
         getNext();
         boolean complete = false;
         String title = null;
         String label = null;
         do {
            if (checkIdentifier("title")) {
               title = getQuotedString();
            }
            else if (checkIdentifier("label")) {
               label = getQuotedString();
            }
            else if (checkIdentifier("shape")) {
               String shape = getSimpleIdentifier();
               if (shape == null) {
                  throw new Exception("Expected 'shape-type' after 'shape:");
               }
               // Discard shape
            }
            else {
               complete = true;
            }
         } while (!complete);
         if (fCurrentToken != '}') {
            throw new Exception("'}' expected to close <node>");
         }
         getNext();
         fGraph.getNodes().add(new Node(title, label));
      }

      /**
       * Parse an edge { [sourcename: "a_sourcename"| targetname: "a_targetname" |label: "a_label"] }
       * 
       * @throws Exception on unexpected token.
       */
      private void getEdge() throws Exception {
         if (fCurrentToken != '{') {
            throw new Exception("'}' expected after 'edge:'");
         }
         getNext();
         boolean complete = false;
         String label = null;
         String sourceName = null;
         String targetName = null;
         do {
            if (checkIdentifier("sourcename")) {
               sourceName = getQuotedString();
            }
            else if (checkIdentifier("targetname")) {
               targetName = getQuotedString();
            }
            else if (checkIdentifier("label")) {
               label = getQuotedString();
            }
            else {
               complete = true;
            }
         } while (!complete);
         if (fCurrentToken != '}') {
            throw new Exception("'}' expected to close <edge>");
         }
         getNext();
         fGraph.getEdges().add(new Edge(label, sourceName, targetName));
      }

      /**
       * Parse either a node or edge
       * 
       * @return True if node or edge found and processed. False otherwise.
       * 
       * @throws Exception on unexpected token.
       */
      private boolean getNodeOrEdge() throws Exception {
         if (fCurrentToken == '}') {
            return false;
         }
         if (fCurrentToken != StreamTokenizer.TT_WORD) {
            throw new Exception("'node:' or 'edge:' expected");
         }
         if (checkIdentifier("node")) {
            getNode();
         }
         else if (checkIdentifier("edge")) {
            getEdge();
         }
         else {
            throw new Exception("'edge', 'node' or '}' expected");
         }
         return true;
      }

      Graph parseGraph() throws Exception {
         if (fCurrentToken == StreamTokenizer.TT_EOF) {
            return null;
         }
         // graph: { title: "name" <node...> }
         if (!checkIdentifier("graph")) {
            throw new Exception("Expected 'graph:'");
         }
         if (fCurrentToken != '{') {
            throw new Exception("'{' expected after 'graph:'");
         }
         getNext();
         if (!checkIdentifier("title")) {
            throw new Exception("Expected 'title:'");
         }
         fGraph = new Graph(getQuotedString());
         while (getNodeOrEdge()) {
         }
         if (fCurrentToken != '}') {
            throw new Exception("'}' expected to close <graph>");
         }
         getNext();
         if (fCurrentToken != StreamTokenizer.TT_EOF) {
            throw new Exception("Unexpected tokens after complete graph");
         }
         return fGraph;
      }
   };

   /**
    * Process a binary <br>
    * The binary is used to locate the build directory. <br>
    * This directory is then processed.
    * 
    * @param binary Binary to process
    * @throws IOException
    * @throws CoreException
    */
   void processBinary(IBinary binary) throws IOException, CoreException {

      ICElement parent = binary.getParent();
      IResource buildTarget = parent.getResource();
      System.err.println("Processing target = " + buildTarget.getName());

      Path folderPath = Paths.get(buildTarget.getLocationURI().getPath());
      // Process files
      ArrayList<Graph> callGraph = new ArrayList<Graph>();
      try {
         processFiles(folderPath, callGraph);
      } catch (Exception e) {
         e.printStackTrace();
      }
      
      final String callGraphFilename = "call-graph.html";
      Path outputPath = folderPath.resolve(callGraphFilename);
      HtmlWriter writer = new HtmlWriter(outputPath, folderPath.getFileName().toString());
      writer.openBody();
      writer.openSection();
      writer.writeHeading("Folder:" + folderPath.getFileName().toString(), 1);
      
      // Collect nodes from graph for lookup (using node.title)
      HashMap<String, Node> nodes = new HashMap<String, Node>();
      ArrayList<Edge> edges = new ArrayList<Edge>();
      callGraph.forEach(new Consumer<Graph>() {

         @Override
         public void accept(Graph graph) {
            graph.getNodes().forEach(new Consumer<Node>() {
               @Override
               public void accept(Node node) {
//                  if (node.getTitle().contains("console_initialise")) {
//                     System.err.println("Found "+node.getTitle());
//                  }
                  Node previousNode = nodes.get(node.getTitle());
                  if (previousNode != null) {
                     System.err.println("Found previous node P:" + previousNode.toString());
                     System.err.println("Found previous node N:" + node.toString());
                     if (node.getStackSizeType() == StackSizeType.Unknown) {
                        // Discard if not adding stack information
                        return;
                     }
                  }
                  nodes.put(node.getTitle(), node);
                  //System.out.print(String.format("%4d: ", ++nodeCount)+node.toString());
               }
            });
            graph.getEdges().forEach(new Consumer<Edge>() {
               @Override
               public void accept(Edge edge) {
                  edges.add(edge);
               }
            });
         }
      });

//      edges.sort(new Comparator<Edge>() {
//         @Override
//         public int compare(Edge e1, Edge e2) {
//            return e1.getSource().compareTo(e2.getSource());
//         }
//      });

//      writer.writeHeading("Function calls", 2);
//      for (Edge edge:edges) {
//         writer.write(edge.toHtml());
//      }
//
      // Attach target nodes to nodes from edge information
      callGraph.forEach(new Consumer<Graph>() {
         // Integer edgeCount=0;

         @Override
         public void accept(Graph graph) {
            graph.getEdges().forEach(new Consumer<Edge>() {
               @Override
               public void accept(Edge edge) {
                  Node sourceNode = nodes.get(edge.getSource());
                  if (sourceNode == null) {
                     System.err.println("Failed to locate source node "+edge.getSource());
                     return;
                  }
                  Node targetNode = nodes.get(edge.getTarget());
                  if (targetNode == null) {
                     System.err.println("Failed to locate target node "+edge.getTarget());
                     return;
                  }
                  sourceNode.targetNodes.add(targetNode);
                  // System.out.println(String.format("%4d: %s ==> %s", ++edgeCount, edge.source, edge.target));
               }
            });
         }
      });

      Node[] sortedNodes = nodes.values().toArray(new Node[0]);

//      Arrays.sort(sortedNodes, new Comparator<Node>() {
//         @Override
//         public int compare(Node o1, Node o2) {
//            return o1.getLabel().compareToIgnoreCase(o2.getLabel());
//         }
//      });
//      writer.writeHeading("Functions", 2);
//      for (Node node:sortedNodes) {
//         writer.write(String.format("%s", node.toHtml()));
//      }
//
      writer.writeHeading("Function Stack depth - by cost", 2);
      Arrays.sort(sortedNodes, new Comparator<Node>() {
         @Override
         public int compare(Node o1, Node o2) {
            return Integer.compare(o2.getStackSize(), o1.getStackSize());
         }
      });

      for (Node node:sortedNodes) {
         if (node.isReported()) {
            continue;
         }
         node.writeMostExpensiveCallPathHtml(writer);
      }

//      writer.writeHeading("Function Stack depth - alphabetic", 2);
//      writer.newline();
//
//      Arrays.sort(sortedNodes, new Comparator<Node>() {
//         @Override
//         public int compare(Node o1, Node o2) {
//            return o1.getLabel().compareToIgnoreCase(o2.getLabel());
//         }
//      });
//
//      //      int worseStack = 0;
//      //      Node worseNode = null;
//
//      for (Node node:sortedNodes) {
//         int cost = node.getStackSize();
//         //         if (cost>worseStack) {
//         //            worseNode = node;
//         //            worseStack = cost;
//         //         }
//         writer.write(String.format("%5d = %s", cost, node.getMostExpensiveCallPath()));
//         writer.newline();
//      }
      writer.closeSection();
      writer.closeBody();
      writer.close();
      
      // Refresh workspace so file is visible
      buildTarget.refreshLocal(1, null);
      
      // Open resulting file in editor
      IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
      IPath path = buildTarget.getFullPath().append(callGraphFilename);
      IDE.openEditor(page, ResourcesPlugin.getWorkspace().getRoot().getFile(path));
   }

   /**
    * Process a project
    * 
    * @param element          Element to obtain the project from
    * 
    * @throws IOException
    * @throws CoreException
    */
   void processProject(Object element) throws IOException, CoreException {

      if (!(element instanceof IAdaptable)) {
         return;
      }
      IResource resource = ((IAdaptable) element).getAdapter(IResource.class);
      if (resource == null) {
         return;
      }
      ICProject cproject = CoreModel.getDefault().create(resource.getProject());
      if (cproject == null) {
         return;
      }
      System.err.println("cProject = " + cproject);
      IBinary[] binaries = cproject.getBinaryContainer().getBinaries();
      for (IBinary binary:binaries) {
         if (!binary.isExecutable()) {
            continue;
         }
         processBinary(binary);
      }
      resource.refreshLocal(0, null);
      
      IFolder folder = ((IAdaptable)element).getAdapter(IFolder.class);
      if (folder == null) {
         return;
      }
   }

   @Execute
   public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell shell, @Named(IServiceConstants.ACTIVE_SELECTION) IStructuredSelection activeSelection) throws CModelException {
      // We expect a selection containing >=1 projects to process
      System.err.println(activeSelection);
      for (Object selection:activeSelection) {
         try {
            processProject(selection);
         } catch (CoreException | IOException e) {
            e.printStackTrace();
         }
      }
   }
   
   static class HtmlWriter {
      BufferedWriter writer;

      HtmlWriter(Path path, String title) throws IOException {
         writer = Files.newBufferedWriter(path);
         writer.write("<html>\n");
         writer.write("<head>\n"
               + "<title>"
               + escapeHtml(title)
               + "</title>\n"
               + "<meta charset=\"UTF-8\">"
               + "</head>\n");
      }
      
      static String escapeHtml(String text) {
         String[] from  = {"&", "\"", "<", ">"};
         String[] to    = {"&amp;", "&quot;", "&lt;", "&gt;"};
         for (int index=0; index<from.length; index++) {
            text = text.replaceAll(from[index], to[index]);
         }
         return text;
      }
      void writeHeading(String s, int level) throws IOException {
         writer.write("<h"+level+">\n"
               +escapeHtml(s)
               + "</h"+level+">\n");
      }
      
      void writeParagraph(String s) throws IOException {
         writer.write("<p>"
               +(s)
               + "</p>\n");
      }
      
      void writeParagraph(int indent, String s) throws IOException {
         writer.write("<p style = \"text-indent:"+String.format("%03d",indent)+"px\">"
               +(s)
               + "</p>\n");
      }
      
      String encloseParagraph(String s, int indent) throws IOException {
         return("<p style = \"text-indent:"+String.format("%03d",indent)+"px\">"
               +(s)
               + "</p>\n");
      }
      
      void openBody() throws IOException {
         writer.write("<body>");
      }
      void closeBody() throws IOException {
         writer.write("</body>\n");
      }
      void openSection() throws IOException {
         writer.write("<section>");
      }
      void closeSection() throws IOException {
         writer.write("</section>\n");
      }
      void close() throws IOException {
         writer.write("</html>\n");
         writer.close();
      }
      
      void newline() throws IOException {
         writer.write("<br>\n");
      }

      void write(String s) throws IOException {
         writer.write(s);
      }
   }
}
