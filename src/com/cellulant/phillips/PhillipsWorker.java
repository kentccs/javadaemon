/**
 * Author: Kenneth Marete email: kenneth.marete@cellulant.com
 */
package com.cellulant.phillips;

import com.cellulant.phillips.db.MySQL;
import com.cellulant.phillips.utils.Utilities;
import com.cellulant.phillips.utils.Logging;
import com.cellulant.phillips.utils.PhillipsConstants;
import com.cellulant.phillips.utils.Props;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class PhillipsWorker {

    /**
     * System properties class instance.
     */
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
     * The string to append before the string being logged.
     */
    private String logPreString;
    /**
     * Flag to check if current pool is completed.
     */
    private transient boolean isCurrentPoolShutDown = false;
    /**
     * The daemons current state.
     */
    private transient int daemonState;
    /**
     * File input stream to check for failed queries.
     */
    private FileInputStream fin;
    /**
     * Data input stream to check for failed queries.
     */
    private DataInputStream in;
    /**
     * Buffered reader to check for failed queries.
     */
    private BufferedReader br;
    /**
     * The print out to external .TXT file
     */
    private transient PrintWriter pout;

    /**
     * Constructor. Checks for any errors while loading system properties,
     * creates the thread pool and resets partially processed records.
     *
     * @param log the logger class used to log information and errors
     * @param properties the loaded system properties
     */
    PhillipsWorker(Props properties, Logging log, MySQL mySQL) {
        props = properties;
        logging = log;
        mysql = mySQL;

        logPreString = " Philips worker class | ";

        // Get the list of errors found when loading system properties
        List<String> loadErrors = props.getLoadErrors();
        int sz = loadErrors.size();

        if (sz > 0) {
            log.info(Utilities.getLogPreString() + "There were exactly "
                    + sz + " error(s) during the load operation...");

            for (String err : loadErrors) {
                log.fatal(Utilities.getLogPreString() + err);
            }

            log.info(Utilities.getLogPreString() + "Unable to start daemon "
                    + "because " + sz + " error(s) occured during load.");
            System.exit(1);
        } else {
            log.info(Utilities.getLogPreString()
                    + "All required properties were loaded successfully");
            daemonState = PhillipsConstants.DAEMON_RUNNING;
        }
    }

//    public void runDaemontester() {
//        System.out.println("******* runnning daemon *******");
//        int pingState = PhillipsConstants.PING_FAILED;
//        try {
//            pingState = pingDatabaseServer(this.mysql.getConnection());
//        } catch (SQLException ex) {
//            ex.printStackTrace();
//        }
//    }
    /**
     * runDaemon this is starting point for daemon running it calls doWork
     * method
     */
    public void runDaemon() {
        String logPreString = this.logPreString + "runDaemon() | -1 | ";
        doWork();
    }

    /**
     * doWork calls three other methods rollbackSystem reSendPendingMsgs
     * executeTasks
     */
    private synchronized void doWork() {
        logging.info("do work");
        rollbackSystem();
        reSendPendingMsgs();
        handleHangingUnprocessed();
        expireFailedUnprocessedMSG(); 
        expireFailedUnprocessed();
        executeTasks();
    }

    /* --Sleep-Time -- */
    public void doWait(long t) {
        String logPreString = this.logPreString + "doWait() | -1 | ";
        try {
            Thread.sleep(t);
        } catch (InterruptedException ex) {
            logging.error(logPreString
                    + "Thread could not sleep for the specified time");

            /* --DO NOTHING--*/ }
    }

    /**
     * executeTasks is the main task method it fetches request from database,
     * creates threads depending on available requests and executes them as
     * tasks
     */
    private void executeTasks() {
        //Declare parameter and initialize as null
        Connection conn = null;
        PreparedStatement stmt = null;
        PreparedStatement upstmt = null;
        ResultSet rs = null;
        ExecutorService executor = null;

        try {

            String logPreString = this.logPreString + "executeTasks() | -1 | ";
            String query = "";

            logging.info(logPreString
                    + "Fetching records to be processed");

            int status = 0;

            query = "SELECT * FROM requests WHERE status = ? "
                    + " AND ( NOW() >= statusNextSend OR statusNextSend IS NULL)"
                    + " AND retries <  ? LIMIT ?";

            //do conection
            conn = mysql.getConnection();
            stmt = conn.prepareStatement(query);

            stmt.setInt(1, status);
            stmt.setInt(2, props.getRequestRetries());
            stmt.setInt(3, props.getBucketSize());

            String[] params = {
                String.valueOf(status),
                String.valueOf(props.getRequestRetries()),
                String.valueOf(props.getBucketSize())
            };

            rs = stmt.executeQuery();

            int size = 0;
            if (rs.last()) {
                size = rs.getRow();
                rs.beforeFirst();
            }

            /**
             * checks the size of the request and create the same number of
             * thread. if maximum threads reached it creates just that maximum
             * number of threads
             */
            if (size > 0) {
                logging.info(logPreString
                        + "Fetch records using query = "
                        + Utilities.prepareSelectSqlString(query, params, 0));
                isCurrentPoolShutDown = false;

                if (size <= props.getNumOfChildren()) {
                    executor = Executors
                            .newFixedThreadPool(size);
                } else {
                    executor = Executors
                            .newFixedThreadPool(props.getNumOfChildren());
                }

                // declare holding variable
                int reqid;
                String fwcode;
                String findtime;
                String findip;
                String findarea;
                String findtype;
                String responseCode;
                String responseDesc;
                String timeInitiated;
                String dateModified;
                String dateCreated;

                while (rs.next()) {
                    reqid = rs.getInt("requestID");
                    fwcode = rs.getString("fwcode");
                    findtime = rs.getString("findtime");
                    findip = rs.getString("findip");
                    findarea = rs.getString("findarea");
                    findtype = rs.getString("findtype");
                    timeInitiated = rs.getString("timeInitiated");
                    dateModified = rs.getString("fwfindtime");
                    dateCreated = rs.getString("dateCreated");

                    /**
                     * fomart datetime to send to url
                     */
                    findtime = Utilities.fomartDateTime(findtime);

                    /**
                     * date time the request was picked
                     */
                    String datetimeinit = Utilities.getDateTime();

                    /**
                     * update the transaction to avoid picking it again set row
                     * status as picked for processing
                     */
                    
                    int updstatus = 1;                    
                    
                    String qr = "UPDATE requests SET status = ?,"
                            + " timeInitiated = ? WHERE requestID = ?";

                    upstmt = conn.prepareStatement(qr);
                    
                    upstmt.setInt(1, updstatus);
                    upstmt.setString(2, datetimeinit);
                    upstmt.setInt(3, reqid);
                    
                    if (upstmt.executeUpdate() > 0) {
                        logging.info("updated picked request...");
                    }

                    /**
                     * // Create a runnable task and submit it //create a
                     * thread
                     */
                    Runnable task = createTask(reqid, fwcode, findtime, findip,
                            findarea, findtype, timeInitiated, dateModified,
                            dateCreated, props, logging, mysql);

                    //execute thread
                    executor.execute(task);
                }

                rs.close();
                upstmt.close();
                stmt.close();
                conn.close();

                /*
                 * This will make the executor accept no new threads and
                 * finish all existing threads in the queue.
                 */
                shutdownAndAwaitTermination(executor);

            } else {
                logging.info(logPreString
                        + "No records were fetched from the DB for "
                        + "processing...");
            }

        } catch (SQLException ex) {
            logging.error(logPreString + "SQLException: " + ex.getMessage());
            //Logger.getLogger(PhillipsWorker.class.getName()).
            //log(Level.SEVERE, null, ex);
        } finally {
            try {
                //close the connection if not closed
                if (!conn.isClosed()) {
                    conn.close();
                }
//                if (!upstmt.isClosed()) {
//                    upstmt.close();
//                }
//                if (!stmt.isClosed()) {
//                    stmt.close();
//                }
//                if (!rs.isClosed()) {
//                    rs.close();
//                }
            } catch (SQLException ex) {
                //Logger.getLogger(PhillipsWorker.class.getName()).
                //log(Level.SEVERE, null, ex);
            }

        }

    }

    /**
     * rollbackSystem checks failed queries and rexecutes them
     */
    private void rollbackSystem() {
        String logPreString = this.logPreString + "rollbackSystem() | -1 | ";
        List<String> failedQueries = checkForFailedQueries(
                PhillipsConstants.FAILED_QUERIES_FILE);
        int failures = failedQueries.size();
        int recon = 0;

        if (failures > 0) {
            logging.info(logPreString + "I found " + failures
                    + " failed update queries in file: "
                    + PhillipsConstants.FAILED_QUERIES_FILE
                    + ", rolling back transactions...");

            do {
                String recon_query = failedQueries.get(recon);
                doRecon(recon_query, PhillipsConstants.RETRY_COUNT);
                //doWait(props.getSleepTime());
                recon++;
            } while (recon != failures);

            logging.info(logPreString
                    + "I have finished performing rollback...");
        }
    }

    /**
     * Loads a file with selected queries and re-runs them internally.
     *
     * @param file the file to check for failed queries
     */
    @SuppressWarnings("NestedAssignment")
    private List<String> checkForFailedQueries(final String file) {
        String logPreString = this.logPreString + "checkForFailedQueries() | -1 | ";
        List<String> queries = new ArrayList<String>(0);

        try {
            /*
             * If we fail to open the file, then the file has not been created
             * yet. This is good because it means that there is no error.
             */
            if (new File(file).exists()) {
                fin = new FileInputStream(file);
                in = new DataInputStream(fin);
                br = new BufferedReader(new InputStreamReader(in));

                String data;
                while ((data = br.readLine()) != null) {
                    if (!queries.contains(data)) {
                        queries.add(data);
                    }
                }
                br.close();
                fin.close();
                in.close();

            }
        } catch (Exception e) {
            logging.error(logPreString
                    + " Error reading from FAILED_QUERIES.TXT: " + e);
        }

        return queries;
    }

    /**
     * The following method shuts down an ExecutorService in two phases, first
     * by calling shutdown to reject incoming tasks, and then calling
     * shutdownNow, if necessary, to cancel any lingering tasks (after 6
     * minutes).
     *
     * @param pool the executor service pool
     */
    private void shutdownAndAwaitTermination(final ExecutorService pool) {
        String logPreString = this.logPreString + "shutdownAndAwaitTermination() | -1 | ";
        logging.info(logPreString
                + "Executor pool waiting for tasks to complete");
        pool.shutdown(); // Disable new tasks from being submitted

        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(6, TimeUnit.MINUTES)) {
                logging.error(logPreString
                        + "Executor pool  terminated with tasks "
                        + "unfinished. Unfinished tasks will be retried.");
                pool.shutdownNow(); // Cancel currently executing tasks

                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(6, TimeUnit.MINUTES)) {
                    logging.error(logPreString
                            + "Executor pool terminated with tasks "
                            + "unfinished. Unfinished tasks will be retried.");
                }
            } else {
                logging.info(logPreString + "Executor pool completed all tasks "
                        + "and has shut down normally");
            }
        } catch (InterruptedException ie) {
            logging.error(logPreString
                    + "Executor pool shutdown error: " + ie.getMessage());
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();

            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }

        isCurrentPoolShutDown = true;
    }

    /**
     * createTask method The following method creates tasks generated by
     * requests from execute method
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
     * @param log
     * @param mySQL
     * @return
     */
    private Runnable createTask(int reqid, String fwcode, String findtime,
            String findip, String findarea, String findtype,
            String timeInitiated, String dateModified, String dateCreated, 
            Props props, Logging log, MySQL mySQL) {

        String logPreString = this.logPreString + "createTask() | -1 | ";
        logging.info(logPreString
                + "Creating a task for message with reqid: "
                + reqid);
        return new phillipsJob(reqid, fwcode, findtime,
                findip, findarea, findtype, timeInitiated, dateModified, 
                dateCreated, props, log, mySQL);

    }

    /**
     * This function determines how the queries will be re-executed i.e. whether
     * SELECT or UPDATE.
     *
     * @param query the query to re-execute
     * @param tries the number of times to retry
     */
    private void doRecon(final String query, final int tries) {
        String logPreString = this.logPreString + "doRecon() | -1 | ";
        int maxRetry = props.getMaxFailedQueryRetries();
        if (query.toLowerCase().startsWith(PhillipsConstants.UPDATE_ID)) {
            int qstate = runUpdateRecon(query);
            if (qstate == PhillipsConstants.UPDATE_RECON_SUCCESS) {
                logging.info(logPreString + "Re-executed this query: "
                        + query + " successfully, deleting it from file...");
                deleteQuery(PhillipsConstants.FAILED_QUERIES_FILE, query);
            } else if (qstate == PhillipsConstants.UPDATE_RECON_FAILED) {
                logging.info(logPreString
                        + "Failed to re-execute failed query: " + query
                        + "[Try " + tries + " out of  " + maxRetry + "]");
                int curr_try = tries + 1;
                if (tries >= maxRetry) {
                    logging.info(logPreString
                            + "Tried to re-execute failed query "
                            + props.getMaxFailedQueryRetries()
                            + " times but still failed, exiting...");
                    System.exit(1);
                } else {
                    logging.info(logPreString + "Retrying in "
                            + (props.getSleepTime() / 1000) + " sec(s) ");
                    doWait(props.getSleepTime());
                    doRecon(query, curr_try);
                }
            }
        } else if (query.toLowerCase().startsWith(PhillipsConstants.INSERT_ID)){
            int qstate = runUpdateRecon(query);
            if (qstate == PhillipsConstants.UPDATE_RECON_SUCCESS) {
                logging.info(logPreString + "Re-executed this query: "
                        + query + " successfully, deleting it from file...");
                deleteQuery(PhillipsConstants.FAILED_QUERIES_FILE, query);
            } else if (qstate == PhillipsConstants.UPDATE_RECON_FAILED) {
                logging.info(logPreString
                        + "Failed to re-execute failed query: " + query
                        + "[Try " + tries + " out of  " + maxRetry + "]");
                int curr_try = tries + 1;
                if (tries >= maxRetry) {
                    logging.info(logPreString
                            + "Tried to re-execute failed query "
                            + props.getMaxFailedQueryRetries()
                            + " times but still failed, exiting...");
                    System.exit(1);
                } else {
                    logging.info(logPreString + "Retrying in "
                            + (props.getSleepTime() / 1000) + " sec(s) ");
                    doWait(props.getSleepTime());
                    doRecon(query, curr_try);
                }
            }
        }
    }

    /**
     * Re-executes the specified query.
     *
     * @param query the query to run
     */
    private int runUpdateRecon(final String query) {
        String logPreString = this.logPreString + "runUpdateRecon() | -1 | ";
        int result = 0;
        Statement stmt = null;
        Connection conn = null;

        try {
            conn = mysql.getConnection();
            stmt = conn.createStatement();
            stmt.executeUpdate(query);
            logging.info(logPreString + "I have just successfully "
                    + "re-executed this failed query: " + query);
            result = PhillipsConstants.UPDATE_RECON_SUCCESS;

            stmt.close();
            stmt = null;
            conn.close();
            conn = null;
        } catch (SQLException e) {
            logging.error(logPreString + "SQLException: " + e.getMessage());
            result = PhillipsConstants.UPDATE_RECON_FAILED;
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException sqlex) {
                    logging.error(logPreString + "runUpdateRecon --- "
                            + "Failed to close Statement object: "
                            + sqlex.getMessage());
                }
            }

            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException sqle) {
                    logging.error(logPreString + "runUpdateRecon --- "
                            + "Failed to close connection object: "
                            + sqle.getMessage());
                }
            }
        }

        return result;
    }

    /*--Delete a query from the failed_queries file after a successfull recon--*/
    public void deleteQuery(String queryfile, String query) {
        String logPreString = this.logPreString + "deleteQuery() | -1 | ";
        ArrayList<String> queries = new ArrayList<String>();
        try {
            fin = new FileInputStream(queryfile);
            in = new DataInputStream(fin);
            br = new BufferedReader(new InputStreamReader(in));

            String data = null;
            while ((data = br.readLine()) != null) {
                queries.add(data);
            }
            br.close();
            fin.close();
            in.close();
            /*--Find a match to the query--*/
            logging.info(logPreString + "About to remove this query: " + query
                    + " from file: " + queryfile);
            if (queries.contains(query)) {
                queries.remove(query);
                logging.info(logPreString + "I have removed this query: " + query
                        + " from file: " + queryfile);
            }
            /*--Now save the file--*/
            pout = new PrintWriter(new FileOutputStream(queryfile, false));
            for (String new_queries : queries) {
                pout.println(new_queries);
            }
            pout.close();
        } catch (Exception e) {
            /**
             * If we fail to open it then, the file has not been created yet'
             * This is good because it means that no error(s) have been
             * experienced yet
             */
            logging.error(logPreString
                    + "Error while deleting query from file. "
                    + "Exception message: " + e.getMessage());
        }
    }

    /**
     * reSendPendingMsgs fetches outbox messages that were not successfully sent
     * to hub sms api. It create threads for them and executes.
     */
    private void reSendPendingMsgs() {
        Connection conn = null;
        PreparedStatement stmt = null;
        PreparedStatement upstmt = null;
        ResultSet rs = null;
        ExecutorService executor2 = null;
        String qr = null;

        try {

            String logPreString = this.logPreString + "executeTasks() | -1 | ";
            String query = "";

            logging.info(logPreString
                    + "Fetching records to be processed");

            int status = 0;
            //fetch pending outbox msgs with a status of 0
            query = "SELECT * FROM outbox where"
                    + " status = ? "
                    + " AND (NOW()>= statusNextSend OR statusNextSend IS NULL)"
                    + " AND failcount <  ? LIMIT ?";

            conn = mysql.getConnection();
            stmt = conn.prepareStatement(query);

            stmt.setInt(1, status);
            stmt.setInt(2, props.getRequestRetries());
            stmt.setInt(3, props.getBucketSize());

            String[] params = {
                String.valueOf(status),
                String.valueOf(props.getRequestRetries()),
                String.valueOf(props.getBucketSize())
            };

            rs = stmt.executeQuery();

            int size = 0;
            if (rs.last()) {
                size = rs.getRow();
                rs.beforeFirst();
            }

            if (size > 0) {
                logging.info(logPreString
                        + "Fetch outbox records using query = "
                        + Utilities.prepareSelectSqlString(query, params, 0));
                isCurrentPoolShutDown = false;

                if (size <= props.getNumOfChildren()) {
                    executor2 = Executors
                            .newFixedThreadPool(size);
                } else {
                    executor2 = Executors
                            .newFixedThreadPool(props.getNumOfChildren());
                }

                // declare holding variable
                int outboxId;
                String message;
                String findtime;
                String findip;
                int requestId;
                String dateCreated;
                String fwcode;
                String fwfindc;

                while (rs.next()) {
                    outboxId = rs.getInt("outboxId");
                    message = rs.getString("message");
                    findtime = rs.getString("fwfindtime");
                    findip = rs.getString("findip");
                    requestId = rs.getInt("requestId");
                    dateCreated = rs.getString("timestamp");
                    fwcode = rs.getString("fwcode");
                    fwfindc = rs.getString("fwfindcount");                    

                    /**
                     * fomart datetime to send to url
                     */
                    findtime = Utilities.fomartDateTime(findtime);

                    /**
                     * date time the request was picked
                     */
                    String datetimeinit = Utilities.getDateTime();

                    /**
                     * update the transaction to avoid picking it again set row
                     * status as picked for processing
                     */
                    
                    int updstatus = 1;
                    
                    /**
                     * check if message is null re-fetch the message
                     * to send consumers
                     */
                    if(message == null){
                       String mquery = "SELECT msg FROM cmessage WHERE type = ?"
                         + " AND active = 1 LIMIT 1";
                        
                        int parseInt = Integer.parseInt(fwfindc);
                        
                        boolean type = (parseInt == 1) ? true : false;
                        //read message from database                
                            stmt = conn.prepareStatement(mquery);                
                            stmt.setBoolean(1, type);
                            rs = stmt.executeQuery();
                            if(rs.next()){
                                message = rs.getString("msg");
                            }
                            
                            message = message.replaceAll("<code>", fwcode);
                        
                            qr = "UPDATE outbox SET status = ?,"
                            + " timeInitiated = ?, message = ? "
                                    + " WHERE outboxID = ?";
                            
                            upstmt = conn.prepareStatement(qr);
                    
                            upstmt.setInt(1, updstatus);
                            upstmt.setString(2, datetimeinit);
                            upstmt.setString(3, message);
                            upstmt.setInt(4, outboxId);                        
                    }
                    else{
                            qr = "UPDATE outbox SET status = ?,"
                            + " timeInitiated = ?  WHERE outboxID = ?";
                            
                            upstmt = conn.prepareStatement(qr);
                    
                            upstmt.setInt(1, updstatus);
                            upstmt.setString(2, datetimeinit);
                            upstmt.setInt(3, outboxId);
                    }
                    
                    
                    if (upstmt.executeUpdate() > 0) {
                        logging.info("updated picked request...");
                    }

                    /**
                     * // Create a runnable task and submit it //create a
                     * thread
                     */
                    Runnable task = createTask2(outboxId, message, findtime,
                            findip, requestId, fwcode, dateCreated, props, 
                            logging, mysql);

                    //execute thread
                    executor2.execute(task);
                }
                //clos e open connections
                rs.close();
                upstmt.close();
                stmt.close();
                conn.close();

                /*
                 * This will make the executor accept no new threads and
                 * finish all existing threads in the queue.
                 */
                shutdownAndAwaitTermination(executor2);

            } else {
                logging.info(logPreString
                        + "No pending outbox records were fetched from the DB for "
                        + "processing...");
            }

        } catch (SQLException ex) {
            logging.error(logPreString + "SQLException: " + ex.getMessage());
            //Logger.getLogger(PhillipsWorker.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                //close persistsent open connections
                if (!conn.isClosed()) {
                    conn.close();
                }
//                if (!upstmt.isClosed()) {
//                    upstmt.close();
//                }
//                if (!stmt.isClosed()) {
//                    stmt.close();
//                }
//                if (!rs.isClosed()) {
//                    rs.close();
//                }
            } catch (SQLException ex) {
                //Logger.getLogger(PhillipsWorker.class.getName()).
                //log(Level.SEVERE, null, ex);
            }

        }
    }

    /**
     * createTask2 runnable method resending pending outbox messages
     *
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
     * @return phillipsMsgJob()
     */
    private Runnable createTask2(int outboxId, String message, String findtime,
            String findip, int requestId, String fwcode, String dateCreated,
            Props props, Logging logging, MySQL mysql) {
        String logPreString = this.logPreString + "createTask() | -1 | ";
        logging.info(logPreString
                + "Creating a task for message with outboxId: "
                + outboxId);
        return new phillipsMsgJob(outboxId, message, findtime,
                findip, requestId, fwcode, dateCreated,
                props, logging, mysql);
    }

    /**
     * expireFailedUnprocessed method explicitly expires requests that have
     * exceeded maximum retries. This is important in reporting, it
     * differentiates expired retried request and pending unprocessed request
     */
    private void expireFailedUnprocessed() {
        Connection conn = null;
        PreparedStatement stmt = null;
        PreparedStatement upstmt = null;
        ResultSet rs = null;
        String qr = null;
        String query = null;
        try {

            String logPreString = this.logPreString + "executeTasks() | -1 | ";

            int status = 0;

            query = "SELECT * FROM requests where"
                    + " status = ? "
                    + " AND statusNextSend IS NOT NULL"
                    + " AND retries >= ? LIMIT ?";

            //do conection
            conn = mysql.getConnection();
            stmt = conn.prepareStatement(query);

            stmt.setInt(1, status);
            stmt.setInt(2, props.getRequestRetries());
            stmt.setInt(3, props.getBucketSize());

            String[] params = {
                String.valueOf(status),
                String.valueOf(props.getRequestRetries()),
                String.valueOf(props.getBucketSize())
            };

            rs = stmt.executeQuery();

            int size = 0;
            if (rs.last()) {
                size = rs.getRow();
                rs.beforeFirst();
            }

            if (size > 0) {
                // declare holding variable
                int reqid;
                int reqstatus = 3;

                while (rs.next()) {
                    reqid = rs.getInt("requestID");                  

                    /**
                     * update the transaction to expiry state
                     */
                    
                    logging.info(logPreString
                    + "settling expired requests records...");
                    
                    qr = "UPDATE requests SET status = ?  WHERE requestID = ?";

                    String[] params2 = {
                        String.valueOf(reqstatus),
                        String.valueOf(reqid)
                    };

                    upstmt = conn.prepareStatement(qr);

                    upstmt.setInt(1, reqstatus);
                    upstmt.setInt(2, reqid);

                    if (upstmt.executeUpdate() > 0) {
                        logging.info("expired the requests... requestID : "
                                + reqid);
                    } else {
                        logging.info("expired the requests... requestID : "
                                + reqid);
                    }
                }
                //close open connections
                rs.close();
                upstmt.close();
                stmt.close();
                conn.close();

            } else {
//                logging.info(logPreString
//                        + "No records were fetched from the DB for "
//                        + "processing...");
            }

        } catch (SQLException ex) {
            logging.error(logPreString + "SQLException: " + ex.getMessage());
            //Logger.getLogger(PhillipsWorker.class.getName()).
            //log(Level.SEVERE, null, ex);
        } finally {
            try {
                //close the connection if not closed
                if (!conn.isClosed()) {
                    conn.close();
                }

            } catch (SQLException ex) {
                //Logger.getLogger(PhillipsWorker.class.getName()).
                //log(Level.SEVERE, null, ex);
            }

        }
    }

    /**
     * expireFailedUnprocessedMSG method explicitly expires messages that have
     * exceeded maximum fail count. This is important in reporting, it
     * differentiates expired retried messages and pending unprocessed messages
     */
    private void expireFailedUnprocessedMSG() {
        Connection conn = null;
        PreparedStatement stmt = null;
        PreparedStatement upstmt = null;
        ResultSet rs = null;
        String qr = null;
        String query = null;
        
        try {
            int status = 3;

            String logPreString = this.logPreString + "executeTasks() | -1 | ";

            query = "SELECT * FROM outbox WHERE statusNextSend IS NOT NULL "
                    + "AND status != ? AND failcount >= ? LIMIT ?";

            //do conection
            conn = mysql.getConnection();
            stmt = conn.prepareStatement(query);
            
            stmt.setInt(1, status);
            stmt.setInt(2, props.getRequestRetries());
            stmt.setInt(3, props.getBucketSize());

            String[] params = {
                String.valueOf(status),
                String.valueOf(props.getRequestRetries()),
                String.valueOf(props.getBucketSize())
            };

            rs = stmt.executeQuery();

            int size = 0;
            if (rs.last()) {
                size = rs.getRow();
                rs.beforeFirst();
            }

            if (size > 0) {
                // declare holding variable
                int outboxID;

                while (rs.next()) {
                    outboxID = rs.getInt("outboxID");

                    int outbxstatus = 3;
                    
                    logging.info(logPreString
                    + "settling expired message record...");

                    /**
                     * update the transaction to avoid picking it again set row
                     * status as picked for processing
                     */
                    qr = "UPDATE outbox SET status = ?  WHERE outboxId = ?";

                    String[] params2 = {
                        String.valueOf(outbxstatus),
                        String.valueOf(outboxID)
                    };

                    upstmt = conn.prepareStatement(qr);

                    upstmt.setInt(1, outbxstatus);
                    upstmt.setInt(2, outboxID);

                    if (upstmt.executeUpdate() > 0) {
                        logging.info("expired the message outbox... outboxID : "
                                + outboxID);
                    } else {
                        logging.info("expired the message outbox... outboxID : "
                                + outboxID);
                    }

                }
                //close open connections
                rs.close();
                upstmt.close();
                stmt.close();
                conn.close();

            } else {
//                logging.info(logPreString
//                        + "No records were fetched from the DB for "
//                        + "processing...");
            }

        } catch (SQLException ex) {
            logging.error(logPreString + "SQLException: " + ex.getMessage());
            //Logger.getLogger(PhillipsWorker.class.getName()).
            //log(Level.SEVERE, null, ex);
        } finally {
            try {
                //close the connection if not closed
                if (!conn.isClosed()) {
                    conn.close();
                }

            } catch (SQLException ex) {
                //Logger.getLogger(PhillipsWorker.class.getName()).
                //log(Level.SEVERE, null, ex);
            }

        }
    }
    
    /**
     * handleHangingUnprocessed method explicitly expires requests that have
     * got response from philips but system was not shutdown properly. 
     * This is important in either expiring or resetting the requests depending
     * on response differentiates expired retried request and pending 
     * unprocessed request
     */
    private void hangingExpiredUnprocessed(){
        Connection conn = null;
        PreparedStatement upstmt = null;
        ResultSet rs = null;
        String qr = null;
        String query = null;
        int status = 3;
        int reqstatus = 1;

        qr = "UPDATE requests SET status = ?  WHERE status = ? AND "
                + "NOW() > DATE_ADD(timeInitiated, interval ? MINUTE)";

        String[] params2 = {
            String.valueOf(status),
            String.valueOf(reqstatus),
            String.valueOf(props.getPendRQTimeout())
        };
        
        //logging.info("hangingExpiredUnprocessed qry = "
         //       +Utilities.prepareSelectSqlString(qr, params2, 0));

        try {
            //do conection
            conn = mysql.getConnection();
            upstmt = conn.prepareStatement(qr);

            upstmt.setInt(1, status);
            upstmt.setInt(2, reqstatus);
            upstmt.setInt(3, props.getPendRQTimeout());
            
            int rowsUpdated = upstmt.executeUpdate();

            if (rowsUpdated > 0) {
                logging.info("expired a requests... requestID that hanged for : "
                        + props.getPendRQTimeout());
            } else {
//                logging.info("failed to expire that requests hanged for : "
//                        + props.getPendRQTimeout());
            }
            
            upstmt.close();
            conn.close();
            
        } catch (SQLException ex) {
                logging.error(logPreString+" SQL ERROR on "
                        + "hangingExpiredUnprocessed : "+ex);
        }finally {
            try {
                //close the connection if not closed
                if (!conn.isClosed()) {
                    conn.close();
                }

            } catch (SQLException ex) {
                //Logger.getLogger(PhillipsWorker.class.getName()).
                //log(Level.SEVERE, null, ex);
            }

        }        
    }
    
    /**
     * handleHangingUnprocessed method explicitly resets requests that have
     * got response from philips but system was not shutdown properly. 
     * This is important in either expiring or resetting the depending on response
     * differentiates expired retried request and pending unprocessed request
     */
    
    private void hangingResetUnprocessed(){
        Connection conn = null;
        PreparedStatement stmt = null;
        PreparedStatement upstmt = null;
        ResultSet rs = null;
        String qr = null;
        String query = null;
        int status = 0;
        int reqstatus = 1;

        qr = "UPDATE requests SET status = ?  WHERE status = ? AND"
                + " NOW() > DATE_ADD(timeInitiated, interval ? MINUTE) AND "
                + " NOW() < DATE_ADD(timeInitiated, interval ? MINUTE)";

        String[] params2 = {
            String.valueOf(status),
            String.valueOf(reqstatus),
            String.valueOf(props.getPendResetTime()),
            String.valueOf(props.getPendRQTimeout())
        };
        
        //logging.info("hangingExpiredUnprocessed qry = "
         //       +Utilities.prepareSelectSqlString(qr, params2, 0));

        try {
            //do conection
            conn = mysql.getConnection();
            upstmt = conn.prepareStatement(qr);

            upstmt.setInt(1, status);
            upstmt.setInt(2, reqstatus);
            upstmt.setInt(3, props.getPendResetTime());
            upstmt.setInt(4, props.getPendRQTimeout());
            
            int rowsUpdated = upstmt.executeUpdate();

            if (rowsUpdated > 0) {
                logging.info(logPreString+"reset a requests... requestID that "
                        + "hanged for: " + props.getPendRQTimeout());
            } else {
//                logging.info("failed to reset that requests hanged for : "
//                        + props.getPendRQTimeout());
            }            
            upstmt.close();
            conn.close();
            
        } catch (SQLException ex) {
                logging.error(logPreString+" SQL ERROR on "
                        + "hangingExpiredUnprocessed : "+ex);
        }finally {
            try {
                //close the connection if not closed
                if (!conn.isClosed()) {
                    conn.close();
                }

            } catch (SQLException ex) {
                //Logger.getLogger(PhillipsWorker.class.getName()).
                //log(Level.SEVERE, null, ex);
            }

        }        
        
    }
    
    /**
     * handleHangingUnprocessed method - requests that have
     * got response from philips but system was not shutdown properly. 
     * This is important in either expiring or resetting the depending on response
     * differentiates expired retried request and pending unprocessed request
     * executes two methods
     * hangingExpiredUnprocessed()
     * hangingResetUnprocessed()
     * 
     */
    
    private void handleHangingUnprocessed() {
        hangingExpiredUnprocessed();
        hangingResetUnprocessed();        
    }

}
