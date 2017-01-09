package com.cellulant.phillips;

import com.cellulant.phillips.db.MySQL;
import com.cellulant.phillips.utils.Logging;
import com.cellulant.phillips.utils.Props;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;


/**
 *
 * @author kent
 * email: kenneth.marete@cellulant.com
 */
public class TestPhillipsDaemon {

    /**
     * The worker thread that does all the work.
     */
    private transient Thread worker;
    /**
     * Flag to check if the worker thread should run.
     */
    private transient boolean working = false;

    private static MySQL mySQL;
    /**
     * Logger for this application.
     */
    private static Logging log;
    /**
     * The main run class.
     */
    private static PhillipsWorker pw;
    /**
     * Properties instance.
     */
    private static Props props;

    public TestPhillipsDaemon() {

    }

    public static void init() {
        try {

            try {
                props = new Props();
            } catch (Exception ex) {
                Logger.getLogger(Phillips.class.getName())
                        .log(Level.SEVERE, null, ex);
            }

            log = new Logging(props);
            log.info("Initializing Phillips daemon...");
            
            /**
             * Tell user on terminal the daemon is running
             */
            System.out.println("Initializing Phillips daemon..... ");


            try {
                mySQL = new MySQL(props.getDbHost(), props.getDbPort(),
                        props.getDbName(), props.getDbUserName(),
                        props.getDbPassword(), props.getDbPoolName(), 20);
            } catch (Exception ex) {
                Logger.getLogger(Phillips.class.getName())
                        .log(Level.SEVERE, null, ex);
            }
            //create an object with log, mysql and props passed
            try {
                pw = new PhillipsWorker(props, log, mySQL);
            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (Exception ex) {
            log.error(ex.getMessage());
        }

    }

    public void start() throws Exception {
        working = true;
        worker.start();
        log.info("Starting Phillips daemon...");
    }

    @SuppressWarnings("SleepWhileInLoop")
    public void stop() throws Exception {
        log.info("Stopping Phillips daemon...");

        working = false;
        //check current pool shutdown and wait to complete task before shutdown
        
        /**
         * Tell user on terminal the daemon is running
         */
        System.out.println("Phillips daemon stopped..... ");

    }

    public void destroy() {
        log.info("Destroying Phillips daemon...");
        log.info("Exiting...");
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        init();
        /**
         * Tell user on terminal the daemon is running
         */
        System.out.println("Phillips daemon running..... ");
        while (true) {
            try {
                pw.runDaemon();
                //pw.runDaemontester();
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                System.err.println(ex.getMessage());
            }
        }
    }

}
