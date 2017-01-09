package com.cellulant.phillips;

import com.cellulant.phillips.db.MySQL;
import com.cellulant.phillips.utils.Logging;
import com.cellulant.phillips.utils.PhillipsConstants;
import com.cellulant.phillips.utils.Props;
import com.cellulant.phillips.utils.Utilities;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.IncorrectnessListener;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.ProxyConfig;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.io.IOException;
import static java.lang.Integer.parseInt;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author kent Marete
 * email: kenneth.marete@cellulant.com
 */
class phillipsMsgJob implements Runnable {

    public volatile int outboxId;
    public volatile int requestId;
    public volatile String fwcode;
    public volatile String findtime;
    public volatile String findip;
    public volatile String message;
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
    PreparedStatement stmt = null;

    /**
     * The string to append before the string being logged.
     */
    private String logPreString;

    /**
     * connection
     */
    Connection conn = null;

    //ResultSet rs = null;
    public String storedQuery = null;

    //public String[] params;

    /**
     * constructor
     * This class only is executed when there was interruptions during sending 
     * og the msg to hub.
     * @param outboxId
     * @param message
     * @param findtime
     * @param findip
     * @param requestId
     * @param fwcode
     * @param dateCreated
     * @param props
     * @param logging
     * @param mysql 
     */
    public phillipsMsgJob(int outboxId, String message, String findtime,
            String findip, int requestId, String fwcode, String dateCreated,
            Props props, Logging logging, MySQL mysql) {

        logPreString = " Philips Msg Job class | ";

        this.props = props;
        this.logging = logging;
        this.mysql = mysql;

        this.outboxId = outboxId;
        this.fwcode = fwcode;
        this.findtime = findtime;
        this.findip = findip;
        this.requestId = requestId;
        this.message = message;
        this.dateCreated = dateCreated;

        webclient2 = this.getWebclient();
    }

    @Override
    public void run() {
        synchronized (webclient2) {
            String qr = null;
            String datetime = null;
            Connection conn2 = null;
            PreparedStatement stmt2 = null;
            int hubID = 0;
            try {

                logging.info("========= resending message to hub ==========");
                logging.info("Re-pushing response for outboxID " + outboxId
                        + "to HUB URL " + props.getHubAPIUrl());
                
                if(message == null){
                    logging.info(logPreString+" The message is null. Please"
                                + " check if it activated on cmessages table");
                    logging.error(logPreString+" The message is null."
                                + " Failed to route message to hub "
                                + "for outboxId " + outboxId);
                    /**
                     * reset message to later time hoping the cmessages for 
                     *consumer will be activated  
                     */                   
                    
                    resetMessage(outboxId);                    
                    return;
                }
                
                /*
                 HtmlPage hubPage =
                 (HtmlPage) webclient2.getPage(props.getHubAPIUrl()
                 + "?source=" + props.getSMSSource() 
                 + "&destination="+ findip+ "&MESSAGE=" + Message 
                 + "&networkID="+ props.getSMSNetworkID()
                 +"&connector="+ props.getConnector()
                 +"&clientSMSID=" + key);
                 */
                   
                // hub sms api
                logging.info("hub request sent :" + props.getHubAPIUrl()
                    + "?username=" + props.getSMSUsername()
                    + "&password=" + props.getSMSPassword()
                    + "&source=" + props.getSMSSource()
                    + "&destination=" + findip
                    + "&message=" + message
                    + "&networkID=" + props.getSMSNetworkID()
                    + "&clientSMSID=" + outboxId
                    + "&connectorRule=" + props.getConnector());
            
            HtmlPage hubPage
                    = (HtmlPage) webclient2.getPage(props.getHubAPIUrl()
                            + "?username=" + props.getSMSUsername()
                            + "&password=" + props.getSMSPassword()
                            + "&source=" + props.getSMSSource()
                            + "&destination=" + findip
                            + "&message=" + message
                            + "&networkID=" + props.getSMSNetworkID()
                            + "&clientSMSID=" + outboxId
                            + "&connectorRule=" + props.getConnector());

                String res = hubPage.asText();

                //System.out.println("res------- " + res);

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

                logging.info("res------- " + res);

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
                String updtqry = null;
                int outbxstatus = 2;
                
                if(ref_id != null){
                    hubID = Integer.parseInt(ref_id);
                }
                
                if(stat_description != null){
                    stat_description = stat_description.replace("'", "\\'");
                }

                //open db connection
                updtqry = "UPDATE outbox SET status = ?,"
                        + " hubId = ?, respMsg = ?  WHERE outboxId = ?";

                String[] params = {
                    String.valueOf(outbxstatus),
                    String.valueOf(hubID),
                    String.valueOf(stat_description),
                    String.valueOf(outboxId)
                };
                
                try {
                    conn2 = mysql.getConnection();
                    stmt2 = conn2.prepareStatement(updtqry);

                    stmt2.setInt(1, outbxstatus);
                    stmt2.setInt(2, hubID);
                    stmt2.setString(3, stat_description);
                    stmt2.setInt(4, outboxId);

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
                    storedQuery = Utilities.prepareSqlString(updtqry, params, 0);
                    Utilities.updateFile(PhillipsConstants.FAILED_QUERIES_FILE,
                            storedQuery);
                    logging.error(logPreString + " SQL Exception"
                            + ex.getMessage());
                    //Logger.getLogger(phillipsJob.class.getName()).
                    //log(Level.SEVERE, null, ex);
                }

            } catch (IOException | FailingHttpStatusCodeException |
                    JSONException ex) {                
                logging.error(logPreString + " HTTP Failed Exception"
                        + ex.getMessage());
                //reset message
                 resetMessage(outboxId);
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
     * resetMessage resets unprocessed msgs inorder to resend them.
     * it is called only when the HTTP or IOException occurs.
     * However when the maximum fail count reaches it will be expired.
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
