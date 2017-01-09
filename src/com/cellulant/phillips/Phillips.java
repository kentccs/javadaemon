/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
 * Phillips Worker class
 * 
 * @author kent
 * email: kenneth.marete@cellulant.com
 */
public class Phillips implements Daemon, Runnable {
    /**
     * The worker thread that does all the work.
     */
    private transient Thread worker;
    /**
     * Flag to check if the worker thread should run.
     */
    private transient boolean working = false;
    
    private transient MySQL mySQL;
    /**
     * Logger for this application.
     */
    private transient Logging log;
    /**
     * The main run class.
     */
    private transient PhillipsWorker pw;
    /**
     * Properties instance.
     */
    private transient Props props;

    @Override
    public void init(DaemonContext context) throws DaemonInitException, 
            Exception {
        worker = new Thread(this);
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
        
        try{
        mySQL = new MySQL(props.getDbHost(), props.getDbPort(),
                props.getDbName(), props.getDbUserName(),
                props.getDbPassword(), props.getDbPoolName(), 20);
        } catch (Exception ex) {
            Logger.getLogger(Phillips.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
        
        //create an object with log, mysql and props passed
        
        pw = new PhillipsWorker(props, log, mySQL);
        
    }

    @Override
    public void start() throws Exception {
        working = true;
        worker.start();
        log.info("Starting Phillips daemon...");
    }

    @Override
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

    @Override
    public void destroy() {
        log.info("Destroying Phillips daemon...");
        log.info("Exiting...");
    }

    @Override
    @SuppressWarnings({ "SleepWhileHoldingLock", "SleepWhileInLoop" })
    public void run() {
        
        /**
         * Tell user on terminal the daemon is running
         */
        System.out.println("Phillips daemon running..... ");
        
         while (working) {
            log.info("");
            log.info("");
            
            try {
                pw.runDaemon();
            } catch (Exception ex) {
                log.fatal("Error occured: " + ex.getMessage());
            }
            
            try {
                Thread.sleep(props.getSleepTime());
            } catch (InterruptedException ex) {
                log.error(ex.getMessage());
            }            
            
         }
    }
    
    
    
}
