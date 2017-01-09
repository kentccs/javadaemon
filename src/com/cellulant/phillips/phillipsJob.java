package com.cellulant.phillips;

import static com.Ostermiller.util.StringHelper.replace;
import com.cellulant.phillips.db.MySQL;
import com.cellulant.phillips.utils.Logging;
import com.cellulant.phillips.utils.PhillipsConstants;
import com.cellulant.phillips.utils.Props;
import com.cellulant.phillips.utils.Utilities;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.IncorrectnessListener;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.ProxyConfig;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.host.Document;
import com.gargoylesoftware.htmlunit.javascript.host.Node;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import static java.lang.Integer.parseInt;
import static java.lang.System.in;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.NoSuchPaddingException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.NodeList;

/**
 *
 * @author kent email: kenneth.marete@cellulant.com
 */
class phillipsJob implements Runnable {

    //declare holding variables
    public volatile int reqid;
    public volatile String fwcode;
    public volatile String findtime;
    public volatile String findip;
    public volatile String findarea;
    public volatile String findtype;
    public volatile String timeInitiated;
    public volatile String dateModified;
    public volatile String dateCreated;
    public volatile String username;
    public volatile String password;
    public WebClient webclient2;
    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public boolean type = false;
    private static transient Props props;
    /**
     * Log class instance.
     */
    private static transient Logging logging;
    /**
     * System properties class instance.
     */
    private static transient MySQL mysql;
    /**
     * prepared stmt
     */
    private PreparedStatement stmt = null;

    /**
     * The string to append before the string being logged.
     */
    private String logPreString;

    /**
     * connection parameter
     */
    private Connection conn = null;

    //ResultSet rs = null;
    /**
     * parameter to store failed query string
     */
    public String storedQuery = null;

    /**
     * parameter to store params string arrays
     */
    public String[] params;
    private Object factory;

    /**
     * constructor for phillipsJob class makes major initializations
     *
     * @param reqid
     * @param fwcode
     * @param findtime
     * @param findip
     * @param findarea
     * @param findtype
     * @param timeInitiated
     * @param dateModified
     * @param dateCreated
     * @param props
     * @param logging
     * @param mySQL
     */
    public phillipsJob(int reqid, String fwcode, String findtime,
            String findip, String findarea, String findtype,
            String timeInitiated, String dateModified, String dateCreated,
            Props props, Logging logging, MySQL mySQL) {
        
        logPreString = " Philips Job class | ";
        
        this.props = props;
        this.logging = logging;
        this.mysql = mySQL;
        
        this.reqid = reqid;
        this.fwcode = fwcode;
        this.findtime = findtime;
        this.findip = findip;
        this.findarea = findarea;
        this.findtype = findtype;
        this.timeInitiated = timeInitiated;
        this.dateModified = dateModified;
        this.dateCreated = dateCreated;
        //this.username = "winsafeinterface";
        //this.password = "winsafe2014";

        this.username = props.getThirdPusername();
        this.password = props.getThirdPPassword();
        
        //logging.info(logPreString + "reached phillips jobs......");
        //logging.info(logPreString + "props.getUsingP = " + props.getUsingP());
        
        webclient2 = this.getWebclient();
        
    }

    /**
     * implemented method run synchronized to make thread safe mutex
     */
    @Override
    public synchronized void run() {
        synchronized (webclient2) {
            String qr = null;
            String datetime = null;
            try {
                //print push url to logs
                logging.info("Pushing to URL " + props.getPushUrl());
                
                logging.info("===========================");
                logging.info("Parameter sending");
                logging.info("fwcode: " + fwcode);
                logging.info("findtime: " + findtime);
                logging.info("findarea: " + findarea);
                logging.info("username: " + username);
                logging.info("password: " + password);
                logging.info("===========================");
                /**
                 * HtmlPage used to easily manipulate web client pages to read
                 * write on web clients
                 */
                HtmlPage philipsPage
                        = (HtmlPage) webclient2.getPage(props.getPushUrl()
                                + "?fwcode=" + fwcode + "&findtime="
                                + findtime + "&findip=" + findip
                                + "&findarea=" + findarea + "&findtype="
                                + findtype + "&user=" + username + "&pwd=" 
                                + password);

                // print the result response from the url connection to the logs
                String res = philipsPage.asXml().trim();
                
                //logging.info("res ---" + res);

                //print the url with parameters
                logging.info(props.getPushUrl() + "?fwcode=" + fwcode 
                        + "&findtime=" + findtime + "&findip=" + findip 
                        + "&findarea=" + findarea + "&findtype=" + findtype 
                        + "&user=" + username + "&pwd=" + password);
                /**
                 * check the feedback
                 */
                String pagetext = philipsPage.asText();

                //logging.info(logPreString + " Text on page : " + pagetext);
                if (pagetext.contains("Server not found")) {
                    logging.info(logPreString + "Possible broken link to " 
                            + props.getPushUrl());
                    logging.error(logPreString + " Broken link or wrong URL "
                            + props.getPushUrl());
                    resetRequest(this.reqid);
                    return;
                }

                /**
                 * check if viewing the right page
                 */
                logging.info("checking nodes....");
                
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();                
                DocumentBuilder db = dbf.newDocumentBuilder();
                org.w3c.dom.Document dom
                        = db.parse(new ByteArrayInputStream(res.getBytes()));
                
                org.w3c.dom.Document doc = dom;
                XPath xPath = XPathFactory.newInstance().newXPath();
                XPathExpression expr = xPath.compile("//root/fwfindcount");
                Object result = expr.evaluate(doc, XPathConstants.NODESET);
                
                NodeList nodes = (NodeList) result;
                logging.info("Have I found valid philips response nodes? "
                        + (nodes.getLength() > 0 ? "Yes" : "No"));                
                
                logging.info("finished checking nodes....");

                /**
                 * check if the philips page response nodes exists. If not exit
                 * retry the request again.
                 */
                if (!(nodes.getLength() > 0)) {
                    logging.info(logPreString + " this may not be a valid "
                            + "philips API link " + props.getPushUrl());
                    
                    logging.info(logPreString + " retrying the request ...");
                    
                    resetRequest(this.reqid);
                    return;                    
                }

                /**
                 * Using XPath to get the first node value of a tag: msg,
                 * fwfindcount, fwfindtime manipulation of the elements
                 */
                HtmlElement element
                        = (HtmlElement) philipsPage.getByXPath("//msg").get(0);
                DomNode msg = element.getChildNodes().get(0);
                
                HtmlElement element1
                        = (HtmlElement) philipsPage.getByXPath("//fwfindcount").get(0);
                DomNode fwfindcount = element1.getChildNodes().get(0);
                
                HtmlElement element2
                        = (HtmlElement) philipsPage.getByXPath("//fwfindtime").get(0);
                DomNode fwfindtime = element2.getChildNodes().get(0);
                
                String count = this.nodeToString(fwfindcount);
                int intcount = Integer.parseInt(count);

                //print the nodes values to the logs
                logging.info("===========================");
                logging.info("Result for nodes");
                logging.info("message: " + msg);
                logging.info("count: " + fwfindcount);
                logging.info("time: " + fwfindtime);
                logging.info("===========================");

                /**
                 * check if msg and response values are not null then create a
                 * new message and send response to hub sms api
                 */
                if (msg != null && fwfindtime != null && intcount > 0) {
                    logging.info("reached here msg is not null");
                    type = (intcount == 1) ? true : false;
                    String fwt = this.nodeToString(fwfindtime);
                    //logging.info("fwt==== " + fwt);
                    
                    String messg = this.nodeToString(msg);
                    String fwfindc = this.nodeToString(fwfindcount);
                    
                    //logging.info("messg==== " + messg);
                    
                    datetime = Utilities.fomartURLDTtoMYSQLDT(fwt);
                    
                    //logging.info("datetime==== " + datetime);
                    
                    //logging.info("before qr==== " + qr);
                    
                    int status = 2;
                    
                    qr = "UPDATE requests SET status = ?, fwfindtime = ?, "
                            + "msg = ?, fwfindcount = ? WHERE requestID = ?";
                    
                    String[] params = {
                        String.valueOf(status),
                        String.valueOf(datetime),
                        String.valueOf(messg),
                        String.valueOf(fwfindc),
                        String.valueOf(reqid)
                    };
                    
                    try {
                        conn = mysql.getConnection();
                        stmt = conn.prepareStatement(qr);
                        
                        stmt.setInt(1, status);
                        stmt.setString(2, datetime);
                        stmt.setString(3, messg);
                        stmt.setString(4, fwfindc);
                        stmt.setInt(5, reqid);
                        
                        logging.info(qr);
                        
                        if (stmt.executeUpdate() > 0) {
                            logging.info("Successfully updated after response"
                                    + " on request " + reqid);
                            //create a message                   
                            this.logMessage(type, fwt, fwcode, reqid, findip, fwfindc);
                        } else {
                            logging.info("failed to update");
                        }
                        
                    } catch (SQLException ex) {
                        storedQuery = Utilities.prepareSqlString(qr, params, 0);
                        Utilities.updateFile(PhillipsConstants.FAILED_QUERIES_FILE,
                                storedQuery);
                        logging.error(logPreString + "SQLException " + ex);
                        //Logger.getLogger(phillipsJob.class.getName()).
                        //log(Level.SEVERE, null, ex);
                    } finally {
                        //close connection 
                        stmt.close();
                        stmt = null;
                        conn.close();
                        conn = null;
                        //cose connections               
                        try {
                            if (!conn.isClosed()) {
                                conn.close();
                            }
                            if (!stmt.isClosed()) {
                                stmt.close();
                            }
                        } catch (SQLException ex) {
                            logging.error(logPreString + "ERROR : Unable to "
                                    + "close conections...");
                        }
                        
                    }

                    //logging.info("----- executing update" +stmt.toString());                   
                } else {
                    
                    int status = 2;
                    String messg = "Product not found";
                    
                    qr = "UPDATE requests SET status = ?,"
                            + "msg = ?  WHERE requestID = ?";
                    
                    String[] params = {
                        String.valueOf(status),
                        String.valueOf(messg),
                        String.valueOf(reqid)
                    };
                    try {
                        conn = mysql.getConnection();
                        stmt = conn.prepareStatement(qr);
                        
                        stmt.setInt(1, status);
                        stmt.setString(2, messg);
                        stmt.setInt(3, reqid);
                        
                        if (stmt.executeUpdate() > 0) {
                            logging.info("Successfully updated");
                            //create a message
                            this.logMessage(type, "1970-01-01 00:00:00", fwcode,
                                    reqid, findip, "0");
                        } else {
                            logging.info("failed to update");
                            //write on file and retry logging
                        }
                        
                    } catch (SQLException ex) {
                        storedQuery = Utilities.prepareSqlString(qr, params, 0);
                        Utilities.updateFile(PhillipsConstants.FAILED_QUERIES_FILE,
                                storedQuery);
                        logging.error("SQLException " + ex);
                        //Logger.getLogger(phillipsJob.class.getName()).
                        //log(Level.SEVERE, null, ex);
                    } finally {
                        //close connection 
                        stmt.close();
                        stmt = null;
                        conn.close();
                        conn = null;
                        //cose web client windows               
                        try {
                            if (!conn.isClosed()) {
                                conn.close();
                            }
                            if (!stmt.isClosed()) {
                                stmt.close();
                            }
                        } catch (SQLException ex) {
                            //logging.error(logPreString + "ERROR : Unable to 
                            //close conections...");
                        }
                        
                    }
                    
                }

                //finally cleanup HtmlPage+
                philipsPage.cleanUp();
                
            } catch (IOException | FailingHttpStatusCodeException ex) {
                //reset to send the request 
                resetRequest(this.reqid);
                
                logging.error(logPreString + "IOException " + ex);
                //Logger.getLogger(phillipsJob.class.getName()).
                //log(Level.SEVERE, null, ex);                

            } finally {
                //cose connections and web client windows   
                webclient2.closeAllWindows();
                logging.info("Done.....");
                return;
            }
            
        }
    }

    /**
     * getWebclient
     *
     * @return webclient object
     */
    public WebClient getWebclient() {
        WebClient webclient = null;
        
        if (this.props.getProxied()) {
            webclient = new WebClient(BrowserVersion.INTERNET_EXPLORER_8);
            webclient.setProxyConfig(new ProxyConfig(this.props.getProxyServer(),
                    parseInt(this.props.getProxyPort())));
            webclient.setRedirectEnabled(true);
            webclient.setThrowExceptionOnScriptError(false);
            webclient.setThrowExceptionOnFailingStatusCode(false);
            webclient.setJavaScriptEnabled(true);
            webclient.setPrintContentOnFailingStatusCode(true);
            webclient.setAjaxController(new NicelyResynchronizingAjaxController());
            webclient.setTimeout(parseInt(this.props.getProxyTimeout()));
            webclient.setIncorrectnessListener(new IncorrectnessListener() {
                public void notify(final String arg0, final Object arg1) {
                }
            });
            
            webclient.setIncorrectnessListener(new IncorrectnessListener() {
                public void notify(final String arg0, final Object arg1) {
                }
            });
            
            webclient.setThrowExceptionOnFailingStatusCode(false);
            webclient.setThrowExceptionOnScriptError(false);
            
        } else {
            
            webclient = new WebClient(BrowserVersion.INTERNET_EXPLORER_8);
            webclient.setThrowExceptionOnScriptError(false);
            webclient.setThrowExceptionOnFailingStatusCode(false);
            webclient.setJavaScriptEnabled(true);
            webclient.setPrintContentOnFailingStatusCode(true);
            webclient.setAjaxController(new NicelyResynchronizingAjaxController());
            webclient.setTimeout(parseInt(this.props.getProxyTimeout()));
            webclient.setIncorrectnessListener(new IncorrectnessListener() {
                public void notify(final String arg0, final Object arg1) {
                }
            });
            
            webclient.setThrowExceptionOnFailingStatusCode(false);
            webclient.setThrowExceptionOnScriptError(false);
        }
        return webclient;
    }

    /**
     * function nodeToString converts DomNode to String
     *
     * @param node
     * @return string writer object in string
     */
    private String nodeToString(DomNode node) {
        logging.info("converting DomNode object to string");
        StringWriter sw = new StringWriter();
        try {
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            t.transform(new DOMSource((org.w3c.dom.Node) node), new StreamResult(sw));
        } catch (TransformerException te) {
            logging.error(logPreString + "failed to converting DomNode object"
                    + " to string" + te);
            //System.out.println("nodeToString Transformer Exception");
            //te.printStackTrace();
        }
        return sw.toString();
    }

    /**
     * logMessage method logs outbox message to DB that is channelled back to
     * HUB SMS API
     *
     * @param type
     * @param fwt
     * @param fwcode
     * @param reqID
     * @param findip
     */
    private void logMessage(boolean type, String fwt, String fwcode, int reqID,
            String findip, String fwfindc) {
        Connection connct = null;
        PreparedStatement stmt = null;
        PreparedStatement upstmt = null;
        ResultSet rs = null;
        try {
            
            int key = 0;
            
            logging.info("========= logging message ==========");
            
            logging.info(logPreString+"type ::" +type);
            
            String Message = null;
//            Message = (type == true) ? "The code you have submitted is valid. "
//                    + "Thank you for purchasing an original Philips product." : 
//                    "The code you have submitted is NOT valid. This is NOT "
//                    + "an original Philips product.. Please contact"
//                    + " our local sales office for support: 20 27 11885/926";
            
            String timenow = Utilities.fomartDateTime(Utilities.getDateTime());
            
            String mquery = "SELECT msg FROM cmessage WHERE type = ? "
                    + "AND active = 1 LIMIT 1";

            String query = "insert into outbox(fwcode, fwfindtime, message,"
                    + " requestId, timestamp, findip, fwfindcount)"
                    + " values(?,?,?,?,?,?,?)";
            
            String[] params = {
                String.valueOf(fwcode),
                String.valueOf(fwt),
                String.valueOf(Message),
                String.valueOf(reqID),
                String.valueOf(timenow),
                String.valueOf(findip),
                String.valueOf(fwfindc)
            };
            try {
                //connection to database
                connct = mysql.getConnection();
                
                //read message from database                
                stmt = connct.prepareStatement(mquery);                
                stmt.setBoolean(1, type);
                rs = stmt.executeQuery();
                if(rs.next()){
                    Message = rs.getString("msg");
                }
                
                Message = Message.replaceAll("<code>", fwcode);
                
                //create a new outbox message sending to consumer
                upstmt = connct.prepareStatement(query, 
                        Statement.RETURN_GENERATED_KEYS);
                
                upstmt.setString(1, fwcode);
                upstmt.setString(2, fwt);
                upstmt.setString(3, Message);
                upstmt.setInt(4, reqID);
                upstmt.setString(5, timenow);
                upstmt.setString(6, findip);
                upstmt.setString(7, fwfindc);
                
                if (upstmt.executeUpdate() > 0) {
                    logging.info("message queued for sending...");
                    ResultSet keys = upstmt.getGeneratedKeys();
                    
                    keys.next();
                    key = keys.getInt(1);
                    keys.close();
                    
                    // if the message is null don't send to hub blank message
                    if(Message != null){  
                        /**
                         * call the method to invoke to SMS API and send message
                         */
                        sendResponseToHub(Message, fwt, fwcode, reqid, findip, key);
                    }
                    else{
                        logging.info(logPreString+" The message is null. Please"
                                + " check if it activated on cmessages table");
                        logging.error(logPreString+" The message is null."
                                + " Failed to route message to hub "
                                + "for outboxId " + key);
                    }
                    
                } else {
                    logging.info(logPreString + "ERROR:: message NOT queued "
                            + "for sending...");

                    /**
                     * write on file and retry logging and sending NOTE: The
                     * reSendingMsgs() method does the resending of msgs to hub
                     */
                    storedQuery = Utilities.prepareSqlString(query, params, 0);
                    Utilities.updateFile(PhillipsConstants.FAILED_QUERIES_FILE,
                            storedQuery);
                }
                /**
                 * close opened connections
                 */
                stmt.close();
                stmt = null;
                rs.close();
                rs = null;
                upstmt.close();
                upstmt = null;
                connct.close();
                connct = null;
                
            } catch (SQLException ex) {
                logging.error(logPreString + " SQL Exception " + ex.getMessage());
                storedQuery = Utilities.prepareSqlString(query, params, 0);
                Utilities.updateFile(PhillipsConstants.FAILED_QUERIES_FILE,
                        storedQuery);
            }
            
        } catch (Exception ex) {
            logging.error(logPreString + " Exception " + ex.getMessage());
        }finally{
            try {
                if(connct != null){
                    connct.close();
                    connct = null;
                }
                
            } catch (Exception e) {
                //error closing connections
            }
        }
        
    }

    /**
     * sendResponseToHub method send response to hub sms API
     *
     * @param Message
     * @param fwt
     * @param fwcode
     * @param reqid
     * @param findip
     * @param key
     */
    private synchronized void sendResponseToHub(String Message, String fwt,
            String fwcode, int reqid, String findip, int key) {
        Connection conn2 = null;
        PreparedStatement stmt2 = null;
        String updtqry = null;
        int hubID = 0;
        try {
            
            logging.info("========= sending message to hub ==========");
            logging.info("Pushing response for reqID " + key + "to HUB URL "
                    + props.getHubAPIUrl());

            /**
             * updateMsgProcessing method updates the msg as processing to
             * status one to avoid resending message to hub
             */
            updateMsgProcessing(key);

            /*
             HtmlPage hubPage =
             (HtmlPage) webclient2.getPage(props.getHubAPIUrl()
             + "?source=" + props.getSMSSource() 
             + "&destination="+ findip+ "&MESSAGE=" + Message 
             + "&networkID="+ props.getSMSNetworkID()
             +"&connector="+ props.getConnector()
             +"&clientSMSID=" + key);
             */
            logging.info("hub request sent :" + props.getHubAPIUrl()
                    + "?username=" + props.getSMSUsername()
                    + "&password=" + props.getSMSPassword()
                    + "&source=" + props.getSMSSource()
                    + "&destination=" + findip
                    + "&message=" + Message
                    + "&networkID=" + props.getSMSNetworkID()
                    + "&clientSMSID=" + key
                    + "&connectorRule=" + props.getConnector());
            
            HtmlPage hubPage
                    = (HtmlPage) webclient2.getPage(props.getHubAPIUrl()
                            + "?username=" + props.getSMSUsername()
                            + "&password=" + props.getSMSPassword()
                            + "&source=" + props.getSMSSource()
                            + "&destination=" + findip
                            + "&message=" + Message
                            + "&networkID=" + props.getSMSNetworkID()
                            + "&clientSMSID=" + key
                            + "&connectorRule=" + props.getConnector());
            
            String res = hubPage.asText();
            
            logging.info("res------- " + res);

            /**
             * extract the response values from hub in json format
             */
            JSONObject jsonResp = new JSONObject(res);
            
            String success = jsonResp.optString("success");
            //System.out.println("success = "+success);

            String stat_code = jsonResp.optString("stat_code");
            //System.out.println("stat_code = "+stat_code);

            String stat_description = jsonResp.optString("stat_description");
            //System.out.println("stat_description = "+stat_description);

            String ref_id = jsonResp.optString("ref_id");
            //System.out.println("ref_id = "+ref_id);

            String client_sms_id = jsonResp.optString("client_sms_id");
            //System.out.println("client_sms_id = "+client_sms_id); 

            logging.info("===========================");
            logging.info("Response from hub sms");
            logging.info("success: " + success);
            logging.info("stat_code: " + stat_code);
            logging.info("stat_description: " + stat_description);
            logging.info("ref_id: " + ref_id);
            logging.info("client_sms_id = " + client_sms_id);
            logging.info("===========================");

            /**
             * finally update the outbox msg with ref_id from hub
             */
            int outbxstatus = 2;
            
            if (ref_id == null) {
                hubID = Integer.parseInt(ref_id);
            }

            if (stat_description != null) {
                stat_description = stat_description.replace("'", "\\'");
            }
            
            //logging.info("************ stat _desc"+stat_description);

            //open db connection
            updtqry = "UPDATE outbox SET status = ?,"
                    + " hubId = ?, respMsg = ?  WHERE outboxId = ?";
            String[] params = {
                String.valueOf(outbxstatus),
                String.valueOf(hubID),
                String.valueOf(stat_description),
                String.valueOf(key)
            };
            
            storedQuery = Utilities.prepareSqlString(updtqry, params, 0);
            
            try {
                conn2 = mysql.getConnection();
                stmt2 = conn2.prepareStatement(updtqry);
                
                stmt2.setInt(1, outbxstatus);
                stmt2.setInt(2, hubID);
                stmt2.setString(3, stat_description);
                stmt2.setInt(4, key);
                
                if (stmt2.executeUpdate() > 0) {
                    logging.info(logPreString + "logged hub sms response...");
                } else {
                    logging.info(logPreString + "failed to log hub response...");
                }
                /*
                 *close open connections
                 */
                stmt2.close();
                stmt2 = null;
                conn2.close();
                conn2 = null;
            } catch (SQLException ex) {
                try {
                    Utilities.updateFile(PhillipsConstants.FAILED_QUERIES_FILE,
                            storedQuery);
                } catch (Exception e) {
                    logging.info(logPreString+"failed to update query to file"
                            +storedQuery+ " : " +e);
                }
                logging.error(logPreString + " SQL Exception"
                        + ex.getMessage());
                //Logger.getLogger(phillipsJob.class.getName()).
                //log(Level.SEVERE, null, ex);
            }
            
        } catch (IOException | FailingHttpStatusCodeException | JSONException ex) {
            //reset message
            resetMessage(key);
            // Logger.getLogger(phillipsJob.class.getName()).
            //log(Level.SEVERE, null, ex);
        } finally {
            
            try {
                if (!conn2.isClosed()) {
                    conn2.close();
                }
                if (!stmt2.isClosed()) {
                    stmt2.close();
                }
                
            } catch (Exception e) {
                //logging.error(logPreString + " Failed to close 
                //conn3 AND stmt3 "+e);
            }
        }
        
    }

    /**
     * The following method resets the request to be resend to philips API
     * following a previous exception or unable to reach it.
     *
     * @param reqID
     */
    private void resetRequest(int reqID) {
        Connection conn3 = null;
        PreparedStatement stmt3 = null;
        String updtqry = null;
        try {
            logging.info(logPreString + "Resetting the request on requestID "
                    + reqID);
            int status = 0;
            String datetimenow = Utilities.getDateTime();

            //open db connection
            updtqry = "UPDATE requests SET status = ?,"
                    + " statusNextSend = DATE_ADD( NOW(), INTERVAL ? SECOND), "
                    + "retries = retries + 1  WHERE requestID = ?";
            
            String[] params = {
                String.valueOf(status),
                String.valueOf(props.getNextSendInterval()),
                String.valueOf(reqID)
            };
            
            storedQuery = Utilities.prepareSqlString(updtqry, params, 0);
            //logging.info("storedQuery :: "+storedQuery);
            try {
                conn3 = mysql.getConnection();
                stmt3 = conn3.prepareStatement(updtqry);
                
                stmt3.setInt(1, status);
                stmt3.setInt(2, props.getNextSendInterval());
                stmt3.setInt(3, reqID);

                //execute request
                if (stmt3.executeUpdate() > 0) {
                    logging.info(logPreString + "Request with ReqID "
                            + reqID + " has been reset");
                } else {
                    logging.info(logPreString + "Request with ReqID "
                            + reqID + " failed to reset");
                    //store query on file to reset when connection is good.
                    storedQuery = Utilities.prepareSqlString(updtqry, params, 0);
                    Utilities.updateFile(PhillipsConstants.FAILED_QUERIES_FILE,
                            storedQuery);
                    
                }
                stmt3.close();
                conn3.close();
                
            } catch (SQLException e) {
                //e.printStackTrace();
                logging.error(logPreString + "SQL Error occurred resetting "
                        + "requests" + e.getMessage());
                
                //storedQuery = Utilities.prepareSqlString(updtqry, params, 0);
                try {
                    Utilities.updateFile(PhillipsConstants.FAILED_QUERIES_FILE,
                            storedQuery);
                } catch (Exception ex) {
                    logging.info(logPreString + "Failed to update query to file"
                            + storedQuery + ex);
                }
            }
            
        } catch (Exception e) {
            logging.error(logPreString + " Error occcured restting requestID "
                    + reqID + " : " + e.getMessage());
        } finally {
            
            try {
                if (!conn3.isClosed()) {
                    conn3.close();
                }
                if (!stmt3.isClosed()) {
                    stmt3.close();
                }
                
            } catch (Exception e) {
                //logging.error(logPreString + " Failed to close 
                //conn3 AND stmt3 "+e);
            }
        }
    }

    /**
     * updateMsgProcessing marks msg as processing to avoid resending it again
     *
     * @param key
     */
    private void updateMsgProcessing(int key) {
        Connection conn4 = null;
        PreparedStatement stmt4 = null;
        String updtqry2 = null;

        //open db connection
        int outbxstatus = 1;
        updtqry2 = "UPDATE outbox SET status = ?  WHERE outboxId = ?";
        String[] params = {
            String.valueOf(outbxstatus),
            String.valueOf(key)
        };
        
        try {
            conn4 = mysql.getConnection();
            stmt4 = conn4.prepareStatement(updtqry2);
            
            stmt4.setInt(1, outbxstatus);
            stmt4.setInt(2, key);
            
            if (stmt4.executeUpdate() > 0) {
                logging.info(logPreString + "marked msg in processing "
                        + "stage...");
            } else {
                logging.info(logPreString + "failed to marked msg in "
                        + "processing stage...");
            }
            /*
             *close open connections
             */
            stmt4.close();
            stmt4 = null;
            conn4.close();
            conn4 = null;
        } catch (SQLException ex) {
            logging.error(logPreString + " SQL Exception"
                    + ex.getMessage());
            //Logger.getLogger(phillipsJob.class.getName()).
            //log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (!conn4.isClosed()) {
                    conn4.close();
                }
                if (!stmt4.isClosed()) {
                    stmt4.close();
                }
                
            } catch (Exception e) {
                //logging.error(logPreString + " Failed to close 
                //conn3 AND stmt3 "+e);
            }
        }
    }
    
    /**
     * resetMessage resets unprocessed msgs inorder to resend them.
     * @param key is the primary key of the outbox column
     */
    private void resetMessage(int key) {
        Connection conn5 = null;
        PreparedStatement stmt5 = null;
        String updtqry3 = null;
        
        String datetimenow = Utilities.getDateTime();

        //open db connection
        int outbxstatus = 0;
        updtqry3 = "UPDATE outbox SET status = ?, "
                + "statusNextSend = DATE_ADD(NOW(), INTERVAL ? SECOND),"
                + "failcount = failcount + 1 WHERE outboxId = ?";
        //set params to display if needed
        String[] params = {
            String.valueOf(outbxstatus),
            String.valueOf(props.getNextSendInterval()),
            String.valueOf(key)
        };

        //stored Query
        storedQuery = Utilities.prepareSqlString(updtqry3, params, 0);
        
        try {
            conn5 = mysql.getConnection();
            stmt5 = conn5.prepareStatement(updtqry3);
            
            stmt5.setInt(1, outbxstatus);
            stmt5.setInt(2, props.getNextSendInterval());
            stmt5.setInt(3, key);
            
            if (stmt5.executeUpdate() > 0) {
                logging.info(logPreString + "marked msg in processing "
                        + "stage...");
            } else {
                logging.info(logPreString + "failed to marked msg in "
                        + "processing stage...");
            }
            /*
             *close open connections
             */
            stmt5.close();
            stmt5 = null;
            conn5.close();
            conn5 = null;
        } catch (SQLException ex) {
            try {
                Utilities.updateFile(PhillipsConstants.FAILED_QUERIES_FILE,
                        storedQuery);
            } catch (Exception e) {
                logging.info(logPreString + "Failed to update query to file"
                        +storedQuery);                
            }
            logging.error(logPreString + " SQL Exception on resetMessage "
                    + "outboxID " + key + " : " + ex.getMessage());
            //Logger.getLogger(phillipsJob.class.getName()).
            //log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (!conn5.isClosed()) {
                    conn5.close();
                }
                if (!stmt5.isClosed()) {
                    stmt5.close();
                }
                
            } catch (Exception e) {
                //logging.error(logPreString + " Failed to close 
                //conn3 AND stmt3 "+e);
            }
        }
    }
}
