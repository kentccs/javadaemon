package com.cellulant.phillips.utils;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.IncorrectnessListener;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.ProxyConfig;
import com.gargoylesoftware.htmlunit.WebClient;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import static java.lang.Integer.parseInt;
import com.cellulant.phillips.utils.Props;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.NoSuchPaddingException;

/**
 *
 * @author kent marete
 * email: kenneth.marete@cellulant.com
 */
public class Utilities {
    public Props props;
    
    public static String getLogPreString() {
        return "phillips | ";
    }
    
     /**
     * Prepares the statement to store in the file.
     */
    public static String prepareSqlString(String query, String[] params, 
            int index) {
        if (!query.contains("?")) {
            return query;
        }
        String s = query.replaceFirst("\\?","'"+ params[index]+"'");
        return prepareSqlString(s, params, ++index);
    }
    
    /**
     * prepareSelectSqlString
     * @param query
     * @param params
     * @param index
     * @return 
     */
    public static String prepareSelectSqlString(String query, String[] params, 
            int index) {
        if (!query.contains("?")) {
            return query;
        }
        String s = query.replaceFirst("\\?",params[index]);
        return prepareSqlString(s, params, ++index);
    }

    /**
     * Store queries in a file NOTE: ----- Checks whether the queries file
     * exists and writes to it the queries that need to be performed
     *
     * Performance Issues ------------------- This has an effect on the speed of
     * execution-> speed reduces because of waiting for a lock to be unlocked,
     * the wait time is unlimited.
     *
     * @param file file and path to write to
     * @param data SQL string to write
     *
     * @return True if update was don and False if not
     */
    public static boolean updateFile(String file, String data) {
        boolean state = false;
        if (!fileExists(file)) {
            createFile(file);
            state = writeToFile(file, data);
        } else {
            state = writeToFile(file, data);
        }

        return state;
    }

    /**/
    /*--FILE MANIPULATION FUNCTIONS-- */
    /*
     Make a file
     NOTE: Must be used where we have read, write, update, delete access...
     */
    private static boolean createFile(String file) {
        boolean state = false;
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(file);
            state = true;
        } catch (Exception e) {
            state = false;
        } finally {
            try {
                fout.close();
            } catch (IOException ex) {
                fout = null;
            }
        }
        return state;
    }

    /*check for file*/
    private static boolean fileExists(String file) {
        boolean state = false;
        try {
            File myfile = new File(file);
            if (myfile.exists()) {
                state = true;
            } else {
                state = false;
            }

        } catch (Exception e) {
            state = false;
        }
        return state;
    }

    /*Add data to a file
     NOTE:
     -----
     <TOCHECK> This function appends the queries file...
     */
    public static boolean writeToFile(String filepath, String data) {
        PrintWriter pout = null;
        boolean state = false;
        try {
            state = false;
            pout = new PrintWriter(new FileOutputStream(filepath, true));
            pout.println(data);

            pout.close();
            pout = null;
        } catch (Exception e) {
            state = false;
        } finally {
            if (pout != null) {
                pout.close();
            }
            pout = null;
        }

        return state;
    }

    public static boolean checkInArray(int currentState, int[] myArray) {
        for (int i : myArray) {

            if (i == currentState) {
                return true;
            }
        }
        return false;
    }
    
    public WebClient getWebclient() {
        WebClient webclient = null;             
        try {
            props = new Props();
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(Utilities.class.getName()).
                    log(Level.SEVERE, null, ex);
        } catch (NoSuchPaddingException ex) {
            Logger.getLogger(Utilities.class.getName()).
                    log(Level.SEVERE, null, ex);
        }
        if (props.getProxied()) {
            webclient = new WebClient(BrowserVersion.INTERNET_EXPLORER_8);
            webclient.setProxyConfig(new ProxyConfig(props.getProxyServer(), 
                    parseInt(props.getProxyPort())));
            webclient.setRedirectEnabled(true);
            webclient.setThrowExceptionOnScriptError(false);
            webclient.setThrowExceptionOnFailingStatusCode(false);
            webclient.setJavaScriptEnabled(true);
            webclient.setPrintContentOnFailingStatusCode(true);
            webclient.setAjaxController(new NicelyResynchronizingAjaxController());
            webclient.setTimeout(parseInt(props.getProxyTimeout()));
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
            webclient.setTimeout(parseInt(props.getProxyTimeout()));
            webclient.setIncorrectnessListener(new IncorrectnessListener() {
                public void notify(final String arg0, final Object arg1) {
                }
            });

            webclient.setThrowExceptionOnFailingStatusCode(false);
            webclient.setThrowExceptionOnScriptError(false);
        }
        return webclient;
    }
    
    public static String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat
        (PhillipsConstants.TIME_FORMAT);
        return sdf.format(new Date());
    }
    
    public static String getDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat
        (PhillipsConstants.DATE_FORMAT);
        return sdf.format(new Date());
    }
    
    public static String fomartDateTime(final String dt){
        String datetime = null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(
                    PhillipsConstants.DATE_FORMAT);
            Date date = sdf.parse(dt);
            
            // *** same for the format String below
            SimpleDateFormat dt1 = new SimpleDateFormat
        (PhillipsConstants.URL_DATE_FORMAT);
            datetime = dt1.format(date);
        } catch (ParseException ex) {
            Logger.getLogger(Utilities.class.getName()).log(Level.SEVERE, null, ex);
        }
          
        return datetime;
    }
    
    public static String fomartURLDTtoMYSQLDT(final String dt){
        String datetime = null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat
        (PhillipsConstants.URL_DATE_FORMAT);
            Date date = sdf.parse(dt);
            
            // *** same for the format String below
            SimpleDateFormat dt1 = new SimpleDateFormat
        (PhillipsConstants.DATE_FORMAT);
            datetime = dt1.format(date);
        } catch (ParseException ex) {
            Logger.getLogger(Utilities.class.getName()).
                    log(Level.SEVERE, null, ex);
        }
          
        return datetime;
    }
    
    /*
    * usage 
    *   if (getAvailableMemory(1024) < 1) {
    *                  System.exit(99);
    *  }   
    *
    *    
    */
    private float getAvailableMemory(float max) {
        float available = 0.0f;
        int mb = 1048576;

        //Getting the runtime reference from system
        Runtime runtime = Runtime.getRuntime();
        max =runtime.maxMemory();
        System.out.println("Maximum Memory" + max);
        System.out.println("##### Heap utilization statistics [MB] #####");
        //Used memory      
       
        float used = (runtime.totalMemory() - runtime.freeMemory()) / mb;
        float total = runtime.totalMemory() / mb;
        
        total = total > max ? max : total;
        System.out.println("Total Memory:" + total);
        System.out.println("Free Memory:" + runtime.freeMemory()/mb);
        System.out.println("Used Memory:" + used);
        //Available
        available = total - used;
        System.out.println("Available Memory:" + available);

        return available;
    }
    
    
   /**
    * Constructor
    */ 
    
    public Utilities(){
       //empty
    }
    
}
