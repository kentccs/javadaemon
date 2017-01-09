package com.cellulant.phillips.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

//import com.cellulant.encryption.Encryption;

/**
 * Loads system properties from a file.
 *
 *@author <a href="kentccs07@gmail.com">Kent Marete</a>
 */
@SuppressWarnings({"FinalClass", "ClassWithoutLogger"})
public final class Props {
    /**
     * The properties file.
     */
    private static final String PROPS_FILE = "conf/phillipsprop.xml";
    /**
     * A list of any errors that occurred while loading the properties.
     */
    private transient List<String> loadErrors;
    /**
     * Info log level. Default = INFO.
     */
    private transient String infoLogLevel = "INFO";
    /**
     * Error log level. Default = FATAL.
     */
    private transient String errorLogLevel = "ERROR";
    /**
     * Info log file name.
     */
    private transient String infoLogFile;
    /**
     * Error log file name.
     */
    private transient String errorLogFile;
    /**
     * No of threads that will be created in the thread pool to process
     * payments.
     */
    private transient int numOfChildren;
    /**
     * Hub API URL.
     */
    private transient String hubAPIUrl;
    /**
     * API connection timeout.
     */
    private transient int connectionTimeout;
    /**
     * API reply timeout.
     */
    private transient int replyTimeout;
    /**
     * Sleep time.
     */
    private transient int sleepTime;
    
    /**
     * The sms credentials properties file.
     */
    private static int refreshUrlsInterval;
    /**
     * The initialization vector used in SMS Password encryption.
     */
    private String initialisationVector;
    /**
     * The key used in SMS Password encryption.
     */
    private String encryptionKey;
    /**
     * Unprocessed Status.
     */
    private transient int unprocessedStatus;
    /**
     * Escalated Status.
     */
    private transient int escalatedStatus;
    /**
     * Success Status.
     */
    private transient int processedStatus;
    /**
     * Database connection pool name.
     */
    private String dbPoolName;
    /**
     * Database user name.
     */
    private String dbUserName;
    /**
     * Database password.
     */
    private String dbPassword;
    /**
     * Database host.
     */
    private String dbHost;
    /**
     * Database port.
     */
    private String dbPort;
    /**
     * Database name.
     */
    private String dbName;
    /**
     * Maximum number of times to retry executing the failed text Query.
     */
    private int maxFailedQueryRetries;
    /**
     * Number of seconds before a next send.
     */
    private int nextSendInterval;
    /**
     * Maximum possible value of the run id.
     */
    private int maxRunID;
   
    /**
     * Size of the messages to be fetched at one go.
     */
    private int bucketSize;
    /**
     * retries for pushing request
     */
   private int retries;
    /**
     * The Application name of the Push Payment Status Daemon.
     */
    private transient String appName;
    /**
     * The string of statuses to push.
     */
    private String statusToPush;
    /**
     * wrapper script
     */
    private String wrapperScript;
    /**
     * proxied status showing if proxy connection enabled
     */
    private boolean proxied = false;
    private transient String usingproxy;
    
    private transient String proxy_server;
    
    private transient String proxy_port;    
    
    private transient String proxy_timeout;
    
    private transient String push_url;
    
    /**
     * sms parameters
     */
    private String smsUsername;
    private String smsPassword;
    private String source;
    private String smsNetworkID;
    private String accessPoint;
    private String connectorrule;
    
    /**
     * third party credentials username and password
     */
    private String ThirdPUsername;
    
    private String ThirdPPassword;
    
    /**
     * connector rule
     */
    private int failedStatus;
    private int pushAckTimeoutPeriod;
    
    /**
     * pending request maximum timeout
     */
    private int PendRQTimeout;
    
    /**
     * pending request resetting Time
     */
    private int PendRQResetTime;

    /**
     * Constructor.
     *
     * @throws NoSuchAlgorithmException on error
     * @throws NoSuchPaddingException on error
     */
    public Props() throws NoSuchAlgorithmException, NoSuchPaddingException {
        System.out.println("" + System.getProperty("user.dir"));
        this.loadErrors = new ArrayList<String>(0);
        loadProperties(PROPS_FILE);
    }

    /**
     * Load system properties.
     *
     * @param propsFile the system properties XML file
     */
    @SuppressWarnings({
        "UseOfSystemOutOrSystemErr",
        "null",
        "ConstantConditions"
    })
    private void loadProperties(final String propsFile) {
        FileInputStream propsStream = null;
        Properties props;


        try {                          
            
            props = new Properties();
            try { 
            propsStream = new FileInputStream(propsFile);
            props.loadFromXML(propsStream);
            } catch (Exception e) {
                e.printStackTrace();
            }

            String error1 = "ERROR: %s is <= 0 or may not have been set";
            String error2 = "ERROR: %s may not have been set";


            infoLogLevel = props.getProperty("InfoLogLevel");
            if (getInfoLogLevel().isEmpty()) {
                getLoadErrors().add(String.format(error2, "InfoLogLevel"));
            }

            errorLogLevel = props.getProperty("ErrorLogLevel");
            if (getErrorLogLevel().isEmpty()) {
                getLoadErrors().add(String.format(error2, "ErrorLogLevel"));
            }

            infoLogFile = props.getProperty("InfoLogFile");
            if (getInfoLogFile().isEmpty()) {
                getLoadErrors().add(String.format(error2, "InfoLogFile"));
            }

            errorLogFile = props.getProperty("ErrorLogFile");
            if (getErrorLogFile().isEmpty()) {
                getLoadErrors().add(String.format(error2, "ErrorLogFile"));
            }

            dbPoolName = props.getProperty("DbPoolName");
            if (getDbPoolName().isEmpty()) {
                getLoadErrors().add(String.format(error2, "DbPoolName"));
            }

            dbUserName = props.getProperty("DbUserName");
            if (getDbUserName().isEmpty()) {
                getLoadErrors().add(String.format(error2, "DbUserName"));
            }

            dbPassword = props.getProperty("DbPassword");
            if (getDbPassword().isEmpty()) {
                getLoadErrors().add(String.format(error2, "DbPassword"));
            }

            dbHost = props.getProperty("DbHost");
            if (getDbHost().isEmpty()) {
                getLoadErrors().add(String.format(error2, "DbHost"));
            }

            dbPort = props.getProperty("DbPort");
            if (getDbPort().isEmpty()) {
                getLoadErrors().add(String.format(error2, "DbPort"));
            }

            dbName = props.getProperty("DbName");
            if (getDbName().isEmpty()) {
                getLoadErrors().add(String.format(error2, "DbName"));
            }

            appName = props.getProperty("ApplicationName");
            if (getAppName().isEmpty()) {
                getLoadErrors().add(String.format(error2, "ApplicationName"));
            }

            statusToPush = props.getProperty("StatusToPush");
            if (getStatusToPush().isEmpty()) {
                getLoadErrors().add(String.format(error2, "StatusToPush"));
            }

            initialisationVector = props.getProperty("IntializationVector");
            if (initialisationVector.isEmpty()) {
                getLoadErrors().add(String.format(error2, "IntializationVector"));
            }

            encryptionKey = props.getProperty("EncryptionKey");
            if (encryptionKey.isEmpty()) {
                getLoadErrors().add(String.format(error2, "EncryptionKey"));
            }

            wrapperScript = props.getProperty("WrapperScript");
            if (wrapperScript.isEmpty()) {
                getLoadErrors().add(String.format(error2, "WrapperScript"));
            }


            String noc = props.getProperty("NumberOfThreads");
            if (noc.isEmpty()) {
                getLoadErrors().add(String.format(error1, "NumberOfThreads"));
            } else {
                numOfChildren = Integer.parseInt(noc);
                if (numOfChildren <= 0) {
                    getLoadErrors().add(String.format(error1,
                        "NumberOfThreads"));
                }
            }

            String connTimeout = props.getProperty("ConnectionTimeout");
            if (connTimeout.isEmpty()) {
                getLoadErrors().add(String.format(error1,
                    "ConnectionTimeout"));
            } else {
                connectionTimeout = Integer.parseInt(connTimeout);
                if (connectionTimeout < 0) {
                    getLoadErrors().add(String.format(error1,
                        "ConnectionTimeout"));
                }
            }

            String replyTO = props.getProperty("ReplyTimeout");
            if (replyTO.isEmpty()) {
                getLoadErrors().add(String.format(error1,
                    "ReplyTimeout"));
            } else {
                replyTimeout = Integer.parseInt(replyTO);
                if (replyTimeout < 0) {
                    getLoadErrors().add(String.format(error1,
                        "ReplyTimeout"));
                }
            }

            String sleep = props.getProperty("SleepTime");
            if (sleep.isEmpty()) {
                getLoadErrors().add(String.format(error1,
                    "SleepTime"));
            } else {
                sleepTime = Integer.parseInt(sleep);
                if (sleepTime < 0) {
                    getLoadErrors().add(String.format(error1,
                        "SleepTime"));
                }
            }

            String unprocessed = props.getProperty("UnprocessedStatus");
            if (unprocessed.isEmpty()) {
                getLoadErrors().add(String.format(error1,
                    "UnprocessedStatus"));
            } else {
                unprocessedStatus = Integer.parseInt(unprocessed);
                if (unprocessedStatus < 0) {
                    getLoadErrors().add(String.format(error1,
                        "UnprocessedStatus"));
                }
            }

            String failed = props.getProperty("FailedStatus");
            if (failed.isEmpty()) {
                getLoadErrors().add(String.format(error1,
                    "FailedStatus"));
            } else {
                failedStatus = Integer.parseInt(failed);
                if (failedStatus < 0) {
                    getLoadErrors().add(String.format(error1,
                        "FailedStatus"));
                }
            }

            String rui = props.getProperty("RefreshUrlsInterval");
            if (rui.isEmpty()) {
                getLoadErrors().add(String.format(error1,
                    "RefreshUrlsInterval"));
            } else {
                refreshUrlsInterval = Integer.parseInt(rui);
                if (refreshUrlsInterval < 0) {
                    getLoadErrors().add(String.format(error1,
                        "RefreshUrlsInterval"));
                }
            }
            
            usingproxy = props.getProperty("UsingProxy");
            if (usingproxy.isEmpty()) {
                getLoadErrors().add(String.format(error2, "UsingProxy"));
            }
            
            //proxy_server
            proxy_server = props.getProperty("ProxyServer");
            if (proxy_server.isEmpty()) {
                //getLoadErrors().add(String.format(error2, "ProxyServer"));
            }            
            
            //proxy_timeout
            proxy_timeout = props.getProperty("ProxyTimeout");
            if (proxy_timeout.isEmpty()) {
                //getLoadErrors().add(String.format(error2, "ProxyTimeout"));
            }
            
            //proxy_port
            proxy_port = props.getProperty("ProxyPort");
            if (proxy_port.isEmpty()) {
               // getLoadErrors().add(String.format(error2, "ProxyPort"));
            }
            
            //push_url
            push_url = props.getProperty("PushUrl");
            if (push_url.isEmpty()) {
                getLoadErrors().add(String.format(error2, "PushUrl"));
            }
            
            //Hub sms push_url
            hubAPIUrl = props.getProperty("hubAPIUrl");
            if (hubAPIUrl.isEmpty()) {
                getLoadErrors().add(String.format(error2, "hubAPIUrl"));
            }
            
            /**
             * sms parameters
             */
            
            //smsUsername
            smsUsername = props.getProperty("smsUsername");
            if (smsUsername.isEmpty()) {
                getLoadErrors().add(String.format(error2, "smsUsername"));
            }
            
            //smsPassword
            smsPassword = props.getProperty("smsPassword");
            if (smsPassword.isEmpty()) {
                getLoadErrors().add(String.format(error2, "smsPassword"));
            }
            
            //source
            source = props.getProperty("source");
            if (source.isEmpty()) {
                getLoadErrors().add(String.format(error2, "source"));
            }
            
            //smsNetworkID
            smsNetworkID = props.getProperty("smsNetworkID");
            if (smsUsername.isEmpty()) {
                getLoadErrors().add(String.format(error2, "smsNetworkID"));
            }
            
            //accessPoint
            accessPoint = props.getProperty("accessPoint");
            if (accessPoint.isEmpty()) {
                getLoadErrors().add(String.format(error2, "accessPoint"));
            }
            //connectorrule
            connectorrule = props.getProperty("connectorrule");
            if (connectorrule.isEmpty()) {
                getLoadErrors().add(String.format(error2, "connectorrule"));
            }
            
            /**
             * Third party parameters reading
             */
            //Username
            ThirdPUsername = props.getProperty("ThirdPUsername");
            if (ThirdPUsername.isEmpty()) {
                getLoadErrors().add(String.format(error2, "ThirdPUsername"));
            }
            //password
            ThirdPPassword = props.getProperty("ThirdPPassword");
            if (ThirdPPassword.isEmpty()) {
                getLoadErrors().add(String.format(error2, "ThirdPPassword"));
            }
            
            
            String processed = props.getProperty("ProcessedStatus");
            if (processed.isEmpty()) {
                getLoadErrors().add(String.format(error1,
                    "ProcessedStatus"));
            } else {
                processedStatus = Integer.parseInt(processed);
                if (processedStatus < 0) {
                    getLoadErrors().add(String.format(error1,
                        "ProcessedStatus"));
                }
            }
            
            String escalated = props.getProperty("EscalatedStatus");
            if (escalated.isEmpty()) {
                getLoadErrors().add(String.format(error1,
                    "EscalatedStatus"));
            } else {
                escalatedStatus = Integer.parseInt(escalated);
                if (escalatedStatus < 0) {
                    getLoadErrors().add(String.format(error1,
                        "EscalatedStatus"));
                }
            }


            String nsi = props.getProperty("NextSendInterval");
            if (!nsi.isEmpty()) {
                nextSendInterval = Integer.parseInt(nsi);
                if (nextSendInterval <= 0) {
                    getLoadErrors().add(String.format(error1,
                        "NextSendInterval"));
                }
            } else {
                loadErrors.add(String.format(error1,
                    "NextSendInterval"));
            }
            
            String retry = props.getProperty("MaxRequestRetries");
            if (!retry.isEmpty()) {
                retries = Integer.parseInt(retry);
                if (retries <= 0) {
                    getLoadErrors().add(String.format(error1,
                        "MaxRequestRetries"));
                }
            } else {
                loadErrors.add(String.format(error1,
                    "MaxRequestRetries"));
            }

            String pat = props.getProperty("PushAckTimeoutPeriod");
            if (!pat.isEmpty()) {
                pushAckTimeoutPeriod = Integer.parseInt(pat);
                if (pushAckTimeoutPeriod <= 0) {
                    getLoadErrors().add(String.format(error1,
                        "PushAckTimeoutPeriod"));
                }
            } else {
                loadErrors.add(String.format(error1,
                    "PushAckTimeoutPeriod"));
            }

            String maxFQretiries = props.getProperty("MaximumFailedQueryRetries");
            if (!maxFQretiries.isEmpty()) {
                maxFailedQueryRetries = Integer.parseInt(maxFQretiries);
                if (maxFailedQueryRetries <= 0) {
                    getLoadErrors().add(String.format(error1,
                        "MaximumFailedQueryRetries"));
                }
            } else {
                getLoadErrors().add(String.format(error1,
                    "MaximumFailedQueryRetries"));
            }

            String bucket = props.getProperty("BucketSize");
            if (!bucket.isEmpty()) {
                bucketSize = Integer.parseInt(bucket);
                if (bucketSize <= 0) {
                    getLoadErrors().add(String.format(error1,
                        "BucketSize"));
                }
            } else {
                getLoadErrors().add(String.format(error1,
                    "BucketSize"));
            }
            
            
            /**
             * Maximum Pending RequestTimeout
             */          
            
            String PRTimeout = props.getProperty("PendingRequestTimeout");
            if (!PRTimeout.isEmpty()) {                
                PendRQTimeout = Integer.parseInt(PRTimeout);
                if (PendRQTimeout <= 0) {
                    getLoadErrors().add(String.format(error1,
                        "PendingRequestTimeout"));
                }
            } else {
                getLoadErrors().add(String.format(error1,
                    "PendingRequestTimeout"));
            }
            
            
            /**
             * Pending Request Reset 
             * The time lived for the system to retry the hanging pending request
             */          
            
            String PResetTime = props.getProperty("PendResetTime");
            if (!PResetTime.isEmpty()) {
                PendRQResetTime = Integer.parseInt(PResetTime);
                if (PendRQResetTime <= 0) {
                    getLoadErrors().add(String.format(error1,
                        "PendResetTime"));
                }
            } else {
                getLoadErrors().add(String.format(error1,
                    "PendResetTime"));
            }
                    
                    
            //close the file
            propsStream.close();
            
        } catch (NumberFormatException ne) {
            ne.printStackTrace();
                    
            System.err.println("Exiting. String value found, Integer is "
                + "required: " + ne.getMessage());

            try {
                if (propsStream != null) {
                    propsStream.close();
                }
            } catch (IOException ex) {
                System.err.println("Failed to close the properties file: "
                    + ex.getMessage());
            }
            System.exit(1);
        } catch (FileNotFoundException ne) {
            System.err.println("Exiting. Could not find the properties file: "
                + ne.getMessage());

            try {
                if (propsStream != null) {
                    propsStream.close();
                }
            } catch (IOException ex) {
                System.err.println("Failed to close the properties file: "
                    + ex.getMessage());
            }

            System.exit(1);
        } catch (IOException ioe) {
            System.err.println("Exiting. Failed to load system properties: "
                + ioe.getMessage());

            try {
                if (propsStream != null) {
                    propsStream.close();
                }
            } catch (IOException ex) {
                System.err.println("Failed to close the properties file");
            }

            System.exit(1);
        }
    }

    /**
     * Info log level. Default = INFO.
     *
     * @return the infoLogLevel
     */
    public String getInfoLogLevel() {
        return infoLogLevel;
    }

    /**
     * Error log level. Default = FATAL.
     *
     * @return the errorLogLevel
     */
    public String getErrorLogLevel() {
        return errorLogLevel;
    }

    /**
     * Info log file name.
     *
     * @return the infoLogFile
     */
    public String getInfoLogFile() {
        return infoLogFile;
    }

    /**
     * Error log file name.
     *
     * @return the errorLogFile
     */
    public String getErrorLogFile() {
        return errorLogFile;
    }

    /**
     * Gets the Beep API URL.
     *
     * @return the Beep API URL
     */
    public String getHubAPIUrl() {
        return hubAPIUrl;
    }

    /**
     * No of threads that will be created in the thread pool to process
     * payments.
     *
     * @return the numOfChildren
     */
    public int getNumOfChildren() {
        return numOfChildren;
    }

    /**
     * Gets the connection timeout.
     *
     * @return the connection timeout
     */
    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * Gets the reply timeout.
     *
     * @return the reply timeout
     */
    public int getReplyTimeout() {
        return replyTimeout;
    }

    /**
     * Get the status for processing failure status
     *
     * @return the failure status
     */
    public int getUnprocessedStatus() {
        return unprocessedStatus;
    }

    /**
     * Gets the status for processing escalated status
     *
     * @return the escalated status
     */
    public int getEscalatedStatus() {
        return escalatedStatus;
    }

    /**
     * Gets the status for processing success status
     *
     * @return the success status
     */
    public int getProcessedStatus() {
        return processedStatus;
    }

    /**
     * Gets the bucket size for processing
     *
     * @return the failure status
     */
    public int getBucketSize() {
        return bucketSize;
    }

    /**
     * Gets the sleep time.
     *
     * @return the sleep time
     */
    public int getSleepTime() {
        return sleepTime;
    }

    /**
     * A list of any errors that occurred while loading the properties.
     *
     * @return the loadErrors
     */
    public List<String> getLoadErrors() {
        try {
            Collections.unmodifiableList(loadErrors);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.unmodifiableList(loadErrors);
    }

    /**
     * Contains the name of the database pool.
     *
     * @return the name of the database pool
     */
    public String getDbPoolName() {
        return dbPoolName;
    }

    /**
     * Contains the name of the database user.
     *
     * @return the name of the database user
     */
    public String getDbUserName() {
        return dbUserName;
    }

    /**
     * Contains the name of the database password.
     *
     * @return the name of the database password
     */
    public String getDbPassword() {
        return dbPassword;
    }

    /**
     * Contains the name of the database host.
     *
     * @return the name of the database host
     */
    public String getDbHost() {
        return dbHost;
    }

    /**
     * Contains the name of the database port.
     *
     * @return the name of the database port
     */
    public String getDbPort() {
        return dbPort;
    }

    /**
     * Contains the name of the database.
     *
     * @return the name of the database
     */
    public String getDbName() {
        return dbName;
    }

    /**
     * Maximum number of times to retry sending a payment.
     *
     * @return the maxSendRetries
     */
    public int getMaxFailedQueryRetries() {
        return maxFailedQueryRetries;
    }

    /**
     * Maximum number of times to retry sending a payment.
     *
     * @return the maxSendRetries
     */
    public int getNextSendInterval() {
        return nextSendInterval;
    }

    /**
     * Maximum possible value of the run id.
     *
     * @return the maxRunID
     */
    public int getMaxRunID() {
        return maxRunID;
    }

   

    /**
     * Gets statuses to be pushed.
     *
     * @return status to push to client
     */
    public String getStatusToPush() {
        return statusToPush;
    }

    /**
     * Gets the period between Url Props refresh.
     *
     * @return URL properties for different clients
     */
    public int getRefreshUrlsInterval() {
        return refreshUrlsInterval;
    }

    /**
     * Get the Encryption initialization vector.
     *
     * @return the initialization vector.
     */
    public String getInitialisationVector() {
        return initialisationVector;
    }

    /**
     * Get the Encryption Key.
     *
     * @return the Encryption Key.
     */
    public String getEncryptionKey() {
        return encryptionKey;
    }  

    
    /**
     * Gets the sms messaging application name.
     *
     * @return sms messaging application name
     */
    public String getAppName() {
        return appName;
    }
    /**
     * get failed status
     * @return failedStatus
     */
    public int getFailedStatus() {
        return failedStatus;
    }
    
    public int getPushAckTimeoutPeriod() {
        return pushAckTimeoutPeriod;
    }
    
    /**
     * 
     * @return retries
     */
    public int getRequestRetries(){
        return retries;
    }
    /**
     * get if connection set to use proxy
     */
    
    public String getUsingP(){
        return usingproxy;
    }
    
    public boolean getProxied(){
        return proxied = (usingproxy == "yes" ? true : false);
    }
    /**
     * proxy server
     */
    public String getProxyServer(){
        return proxy_server;
    }
    /**
     * proxy timeout
     */
    public String getProxyTimeout(){
        return proxy_timeout;
    }
    
    /**
     * proxy port
     */
    public String getProxyPort(){
        return proxy_port;
    }
    
    /**
     * push_url
     */
    public String getPushUrl(){
        return push_url;
    }
    
    /**
     * sms parameters smsUsername
     */
    
     public String getSMSUsername(){
        return smsUsername;
    }
     /**
     * sms parameters smsPassword
     */
    
     public String getSMSPassword(){
        return smsPassword;
    }
     /**
     * sms parameters source
     */
    
     public String getSMSSource(){
        return source;
    }
     /**
     * sms parameters smsNetworkID
     */
    
     public String getSMSNetworkID(){
        return smsNetworkID;
    }
     
     /**
     * sms parameters accessPoint
     */
    
     public String getAccessPoint(){
        return accessPoint;
    }
     
     /**
      * sms connector rule
      */
     public String getConnector(){
         return connectorrule;
     }
     
     /**
      * Thirdparty username i.e. Philips username
      */
     public String getThirdPusername(){
         return ThirdPUsername;
     }
     
     /**
      * Thirdparty Password i.e. Philips password
      */
     public String getThirdPPassword(){
         return ThirdPPassword;
     }
     
     /**
      * get Pending request timeout
      */
     public int getPendRQTimeout(){
         return PendRQTimeout;
     }
     
     /**
      * get pending requests time to reset after
      */
     public int getPendResetTime(){
         return PendRQResetTime;
     }        
         
     
}