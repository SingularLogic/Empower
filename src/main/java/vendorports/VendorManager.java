/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vendorports;

import dataaccesslayer.Operation;
import dataaccesslayer.SoftwareComponent;
import dataaccesslayer.Schema;
import dataaccesslayer.Service;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.*;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.xml.sax.SAXException;
import xml.XSDParser;

/**
 *
 * @author eleni
 */
public class VendorManager extends HttpServlet {

    private PrintWriter out;
    private static String xml_rep_path = "/home/eleni/Documents/ubi/empower/empower-deliverable-september/empower";

    /**
     * Processes requests for both HTTP
     * <code>GET</code> and
     * <code>POST</code> methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        out = response.getWriter();
        HttpSession session;
        String operation;

        session = request.getSession(true);

        operation = request.getParameter("op");

        try {
            if (operation != null) {
                if (operation.equals("software_reg")) 
                    this.registerSoftware(request, response, session);
                 else if (operation.equals("show_components")) 
                    this.showComponents(request, response, session);
                 else if (operation.equals("schema_reg")) 
                    this.registerSchema(request, response, session);
                 //else if (operation.equals("show_schemas")) 
                 //   this.showSchemas(request, response, session);
                 else if (operation.equals("load_software_reg")) 
                    this.loadSoftwareReg(request, response, session);
                 else if (operation.equals("update_software")) 
                    this.updateSoftware(request, response, session);
                 else if (operation.equals("delete_software")) 
                    this.deleteSoftware(request, response, session);
                // else if (operation.equals("present_service_operations")) 
                 //   this.presentServiceOperationsTree(request, response, session);
                 //else if (operation.equals("annotate_operations")) 
                 //   this.annotateOperations(request, response, session);
                // else if (operation.equals("present_central_trees")) 
                //    this.presentOptionTrees(request, response, session);
                // else if (operation.equals("present_service_schema")) 
                //    this.presentServiceSchemaTree(request, response, session);
                 //else if (operation.equals("annotate")) 
                  //  this.annotate(request, response, session);
                




            }
        } catch (Throwable t) {
            t.printStackTrace();
            this.forwardToPage("/error/generic_error.jsp?errormsg= " + t.getMessage() + " " + operation, request, response);
        } finally {
            out.close();
        }

    }

    private void forwardToPage(String url, HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        RequestDispatcher dis;

        dis = getServletContext().getRequestDispatcher(url);
        dis.forward(req, resp);
        return;
    }

    protected void registerSoftware(HttpServletRequest request,
            HttpServletResponse response,
            HttpSession session)
            throws IOException, ServletException {
        int software_id;
        String softwareName = request.getParameter("software_name");
        String version = request.getParameter("software_version");

        VendorDBConnector vendorDBConnector = new VendorDBConnector();
        software_id = vendorDBConnector.insertSoftwareInfo((String) session.getAttribute("name"), softwareName, version);
        this.forwardToPage("/vendor/succ.jsp", request, response);
    }

    protected void updateSoftware(HttpServletRequest request, HttpServletResponse response, HttpSession session)
            throws IOException, ServletException {
        String softwareID = (String) request.getParameter("software_id");
        String softwareName = (String) request.getParameter("software_name");
        String version = (String) request.getParameter("software_version");

        VendorDBConnector vendorDBConnector = new VendorDBConnector();
        vendorDBConnector.updateSoftware(softwareID, softwareName, version);

        this.forwardToPage("/vendor/succ.jsp", request, response);

        return;
    }

    protected void deleteSoftware(HttpServletRequest request, HttpServletResponse response, HttpSession session)
            throws IOException, ServletException {
        String softwareID = (String) request.getParameter("software_id");

        VendorDBConnector vendorDBConnector = new VendorDBConnector();
        vendorDBConnector.deleteSoftware(softwareID);

        this.forwardToPage("/vendor/succ.jsp", request, response);

        return;
    }

    /*
     * show software components
     */
    protected void showComponents(HttpServletRequest request,
            HttpServletResponse response,
            HttpSession session)
            throws IOException, ServletException {
        Iterator compIterator;
        int rowID = 1;

        VendorDBConnector vendorDBConnector = new VendorDBConnector();
        compIterator = vendorDBConnector.getSoftwareComponents((String) session.getAttribute("name")).iterator();

        response.setContentType("text/xml; charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        out.write("<rows>");

        while (compIterator.hasNext()) {
            SoftwareComponent comp = (SoftwareComponent) compIterator.next();
            out.write("<row id=\"" + (rowID++) + "_" + comp.getSoftwareID()
                    + "_" + comp.getName() + "\"><cell>" + comp.getName()
                    + "</cell><cell>" + comp.getVersion() + "</cell>"
                    + "<cell>Delete^../VendorManager?op=delete_software&amp;software_id="
                    + comp.getSoftwareID()
                    + "^_self</cell><cell>Update^../VendorManager?op=load_software_reg&amp;software_id=" + comp.getSoftwareID()
                    + "&amp;software_name=" + comp.getName() + "&amp;software_version=" + comp.getVersion()
                    + "^_self</cell>"
                    + "<cell>Schemas^../DIController?op=show_schema&amp;xsd=" + rowID + "_" + comp.getNum_xsds()
                    + "_" + comp.getSoftwareID() + "^_self</cell>" + "</row>");
        }

        out.write("</rows>");
        out.flush();
    }

    protected void loadSoftwareReg(HttpServletRequest request, HttpServletResponse response, HttpSession session)
            throws IOException, ServletException {
        String softwareID = (String) request.getParameter("software_id");
        String softwareName = (String) request.getParameter("software_name");
        String version = (String) request.getParameter("software_version");

        this.forwardToPage("/vendor/softwareReg.jsp?operation=VendorManager%3Fop=update_software&software_name="
                + softwareName + "&software_version=" + version + "&software_id=" + softwareID, request, response);

        return;
    }

    protected void registerSchema(HttpServletRequest request, HttpServletResponse response, HttpSession session)
            throws IOException, ServletException, FileUploadException, Exception {
        // Check that we have a file upload request

        String web_service_name = null;
        String new_web_service_name = null;
        String operation_name = null;
        String schemaFilename = null;
        String schemaName = null;
        String inputoutput = null;
        int software_id = 0;
        String namespace = null;
        int schema_id = 0;

        boolean isMultipart = ServletFileUpload.isMultipartContent(request);

        // Create a factory for disk-based file items
        DiskFileItemFactory factory = new DiskFileItemFactory();

        // Set factory constraints
        factory.setSizeThreshold(30000);
        factory.setRepository(new File("/home/eleni/Desktop/"));
        ServletFileUpload upload = new ServletFileUpload(factory);

        upload.setSizeMax(30000);

        // Parse the request
        List items = upload.parseRequest(request);

        Iterator iter = items.iterator();

        while (iter.hasNext()) {
            FileItem item = (FileItem) iter.next();

            if (item.isFormField()) {

                System.out.println("Here we have: ");

                if (item.getFieldName().equals("software_id")) {
                    software_id = Integer.parseInt(item.getString());
                    System.out.println("software_id " + software_id);
                } else if (item.getFieldName().equals("schema_name")) {
                    schemaName = new String(item.getString());
                    System.out.println("schema_name " + schemaName);
                } else if (item.getFieldName().equals("web_service_name")) {
                    web_service_name = new String(item.getString());
                     System.out.println("web_service_name: " + web_service_name); 
                }else if (item.getFieldName().equals("new_web_service_name")) {
                    new_web_service_name = new String(item.getString());
                     System.out.println("new_web_service_name: " + new_web_service_name); 
                }else if (item.getFieldName().equals("operation_name")) {
                    operation_name = new String(item.getString());
                } else if (item.getFieldName().equals("inputoutput")) {
                    inputoutput = new String(item.getString());
                }

            } else {
                System.out.println("Here we put the file ");

                schemaFilename = new String(this.xml_rep_path + "/xsd/" + software_id + "_" + schemaName + "_" + ((int) (100000 * Math.random())) + ".xsd");
                System.out.println("schemaFilename: " + schemaFilename);
                File uploadedFile = new File(schemaFilename);
                item.write(uploadedFile);
            }
        }

        VendorDBConnector vendorDBConnector = new VendorDBConnector();
  
        schema_id = vendorDBConnector.insertSchemaInfo(software_id, schemaName, schemaFilename, xml_rep_path, new_web_service_name , web_service_name, operation_name, inputoutput);



        this.forwardToPage("/vendor/succ.jsp", request, response);
    }
    
    /*
    *  Comment because is done from DIController
    * 

    protected void showSchemas(HttpServletRequest request, HttpServletResponse response, HttpSession session)
            throws IOException, ServletException {
        Iterator XSDIterator;
        int rowID = 1;

        VendorDBConnector vendorDBConnector = new VendorDBConnector();
        XSDIterator = vendorDBConnector.getSchemas((String) request.getParameter("software_id")).iterator();

        response.setContentType("text/xml; charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        out.write("<rows>");

        while (XSDIterator.hasNext()) {
            Schema schema = (Schema) XSDIterator.next();
            out.write("<row id=\"" + schema.getSchema_id() + "\"><cell>" + schema.getName() + "</cell>"
                    + "<cell> Annotate Operations^./vendor/presentOperationTree.jsp?schema_id=" + schema.getSchema_id() + "^_self</cell>"
                    + "<cell> Annotate Data^./vendor/presentDataTree.jsp?schema_id=" + schema.getSchema_id() + "^_self</cell>"
                    //+ "<cell>Annotate Data^./vendor/presentServiceTree.jsp?service_id=" + schema.getSchema_id()+ "^_self</cell>"
                    //+ "<cell>Annotate Functions^./vendor/presentServiceFunctionTree.jsp?service_id=" + schema.getSchema_id()+ "^_self</cell>"
                    + "</row>");
        }

        out.write("</rows>");
        out.flush();

        return;
    }
    * */
    

    
    /*
    *  Comment because is done from DIController
    * 
    protected void presentServiceOperationsTree(HttpServletRequest request, HttpServletResponse response, HttpSession session)
            throws IOException, ServletException {

        int schema_id = Integer.parseInt(request.getParameter("schema_id"));

        VendorDBConnector vendorDBConnector = new VendorDBConnector();
        LinkedList<Operation> operations = vendorDBConnector.getOperationsBySchema(schema_id);

        response.setContentType("text/xml; charset=UTF-8");

        this.outputOperationsToXML(response.getWriter(), operations);

        //WSDLParser wsdlParser = new WSDLParser(service.getWsdl(),service.getNamespace());
        //wsdlParser.loadService(service.getName());
        //wsdlParser.outputFunctionsToXML(response.getWriter());
    }*/
    
    /*
    *  Comment because is done from DIController
    * 

    protected void presentServiceSchemaTree(HttpServletRequest request, HttpServletResponse response, HttpSession session)
            throws IOException, ServletException, ParserConfigurationException, SAXException {

        int schema_id = Integer.parseInt(request.getParameter("schema_id"));

        VendorDBConnector vendorDBConnector = new VendorDBConnector();
        Schema schema = vendorDBConnector.getSchema(schema_id);
        System.out.println("schema location: " + schema.getLocation());

        XSDParser p = new XSDParser(schema);
        String xml_string = p.convertSchemaToXML();
        System.out.println("xml_string: " + xml_string);

        response.setContentType("text/xml; charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.write(xml_string);
    }*/
    
     /*
    *  Comment because is done from DIController
    * 
    public void outputOperationsToXML(PrintWriter out, LinkedList<Operation> operations) {
        try {
            //work with collection. get services
            Map<Integer, String> services = new HashMap<Integer, String>();

            System.out.println("operations size " + operations.size());

            Iterator<Operation> op_it = operations.iterator();

            while (op_it.hasNext() == true) {

                Operation op = op_it.next();

                int service_id = op.getService_id();
                String web_service_name = op.getWeb_service_name();


                if (!services.containsKey(service_id)) {
                    services.put(service_id, web_service_name);
                }

            }

            out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");

            out.write("<tree id=\"0\">");

            //Get Map in Set interface to get key and value
            Iterator it = services.entrySet().iterator();

            while (it.hasNext()) {
                Map.Entry m = (Map.Entry) it.next();
                int key = (Integer) m.getKey();
                String value = (String) m.getValue();


                out.write("<item text=\"" + value
                        + "\" id=\"" + key + "\" nocheckbox=\"true\">");

                System.out.println("key: " + key);
                System.out.println("This is out: " + out);


                op_it = operations.iterator();
                while (op_it.hasNext()) {

                    Operation op = op_it.next();
                    if (op.getService_id() == key) {
                        out.write("<item text=\"" + op.getOperation_name() + "\" id=\"" + op.getOperation_id() + "\"/>");
                        System.out.println(" operationname " + op.getOperation_name() + " operationid " + op.getOperation_id());
                    }
                }
                out.write("</item>");
            }

            out.write("</tree>");
            out.close();

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    */
    
    
   
    
    /*
    *  Comment because is done from DIController
    * 
    protected void annotateOperations(HttpServletRequest request, HttpServletResponse response, HttpSession session)
            throws IOException, ServletException, ParserConfigurationException, SAXException, XPathExpressionException {
        int schema_id = Integer.parseInt((String) request.getParameter("schema_id"));
        int operation_id = Integer.parseInt((String) request.getParameter("selections"));
        String funcSelections = request.getParameter("funcselections");
        String name = (String) session.getAttribute("name");

        System.out.println(funcSelections + " " + schema_id + " " + operation_id + " " + name);

        VendorDBConnector vendorDBConnector = new VendorDBConnector();

        vendorDBConnector.insertTaxonomyToOperation(operation_id, funcSelections);

        //vendorDBConnector.insertCVPFunction(funcSelections, serviceID, selections, name);

        this.forwardToPage("/vendor/annotationResult.jsp?schema_id=" + schema_id, request, response);
    }
    */
    
   /*
    *  Comment because is done from DIController
    * 
    protected void presentOptionTrees(HttpServletRequest request, HttpServletResponse response, HttpSession session)
            throws IOException, ServletException {
        response.setContentType("text/xml; charset=UTF-8");
        PrintWriter out = response.getWriter();

        out.write("<?xml version='1.0' encoding='UTF-8'?><tree id='0'><item text='accountingEntriesComplexType' id='accountingEntriesComplexType'>"
                + "<item text='documentInfoComplexType' id='documentInfoComplexType'/><item text='entityInformationComplexType' id='entityInformationComplexType'/><item text='entryHeaderComplexType' id='entryHeaderComplexType\'>"
                + "<item text='entryDetailComplexType' id='entryDetailComplexType'/></item></item></tree>");

        out.flush();
        out.close();

    }
    * 
    */
   /*
    * comment because is done from DIController
    * 
    protected void annotate(HttpServletRequest request, HttpServletResponse response, HttpSession session)
            throws IOException, ServletException, ParserConfigurationException, SAXException, XPathExpressionException {
        int schema_id = Integer.parseInt((String) request.getParameter("schema_id"));
        String schema_data = ((String) request.getParameter("selections")).split("--")[0];
        String inputoutput = ((String) request.getParameter("selections")).split("--")[1];
        String centralTree = request.getParameter("centraltree");
        String mapping = null;

        VendorDBConnector vendorDBConnector = new VendorDBConnector();
        Schema schema = vendorDBConnector.getSchema(schema_id);
        String filename = ((String) schema.getLocation()).split("/xsd/")[1];

        if (inputoutput.equals("output")) mapping = vendorDBConnector.getMapping(schema_id, centralTree + "$" + schema_data);
        else  mapping = vendorDBConnector.getMapping(schema_id, schema_data + "$" + centralTree);

        if (mapping != null) {
            mapping = new String(mapping.replace("\"", "\\\""));
            System.out.println("==== mapping=" + mapping.substring(0, 500));
        }

        request.setAttribute("mapping", mapping);

        //we pass to the annotator the schema id as service id until only for the first deliverable of the empower project, so as not to double change the annotator without reason
        if (inputoutput.equals("output")) {

            //response.sendRedirect(response.encodeRedirectURL("http://127.0.0.1:8080/annotator?input=xsd/XBRL-GL-PR-2010-04-12/plt/case-c-b-m/gl-cor-content-2010-04-12.xsd&output=xsd/" + filename + "&service_id=" + schema_id + "&map_type=cvp&outputType=" + schema_data +"&inputType=" + centralTree + "&mapping="+ mapping)); 
            request.setAttribute("output", "xsd/" + filename);
            request.setAttribute("input", "xsd/XBRL-GL-PR-2010-04-12/plt/case-c-b-m/gl-cor-content-2010-04-12.xsd");
            request.setAttribute("map_type", "cvp");
            request.setAttribute("outputType", schema_data);
            request.setAttribute("inputType", centralTree);
            request.setAttribute("selections", centralTree + "$" + schema_data);
        } else {
            //response.sendRedirect(response.encodeRedirectURL("http://127.0.0.1:8080/annotator?input=xsd/" + filename + "&output=xsd/XBRL-GL-PR-2010-04-12/plt/case-c-b-m/gl-cor-content-2010-04-12.xsd&service_id=" + schema_id + "&map_type=cvp&outputType=" + centralTree + "&inputType=" + schema_data + "&mapping="));
            request.setAttribute("input", "xsd/" + filename);
            request.setAttribute("output", "xsd/XBRL-GL-PR-2010-04-12/plt/case-c-b-m/gl-cor-content-2010-04-12.xsd");
            request.setAttribute("map_type", "cvp");
            request.setAttribute("outputType", centralTree);
            request.setAttribute("inputType", schema_data);
            request.setAttribute("selections", schema_data + "$" + centralTree);
        }
        this.forwardToPage("/vendor/proceedDataTree.jsp?schema_id=" + schema_id, request, response);

    }*/

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP
     * <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP
     * <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
}
