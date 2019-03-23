/**
 * @brief Enduro/X ATMI Context Class
 *   This is main class used for almost all operations with Enduro/X middleware
 *
 * @class AtmiCtx
 */
/* -----------------------------------------------------------------------------
 * Enduro/X Middleware Platform for Distributed Transaction Processing
 * Copyright (C) 2009-2016, ATR Baltic, Ltd. All Rights Reserved.
 * Copyright (C) 2017-2018, Mavimax, Ltd. All Rights Reserved.
 * This software is released under one of the following licenses:
 * AGPL or Mavimax's license for commercial use.
 * -----------------------------------------------------------------------------
 * AGPL license:
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License, version 3 as published
 * by the Free Software Foundation;
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Affero General Public License, version 3
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along 
 * with this program; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * -----------------------------------------------------------------------------
 * A commercial use license is available from Mavimax, Ltd
 * contact@mavimax.com
 * -----------------------------------------------------------------------------
 */
package org.endurox;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.endurox.exceptions.AtmiTPEINVALException;
import org.endurox.exceptions.AtmiTPESYSTEMException;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.endurox.exceptions.AtmiTPEOSException;

/*! @mainpage Enduro/X Programming main page
 *
 * @section standard_sec Programming standard page
 *
 * This section lists API practices used by Enduro/X Java package
 *
 * @subsection atmi_api_sec ATMI API
 * ATMI API uses lower case function names (instead of Java camel case). This
 * is for clener compatibility with C XATMI API. The same goes with the constants.
 * ATMI constants goes in upper case as in the C side.
 * 
 * Class names are typed as with Java Standard.
 *
 * @subsection java_api_sec Java Specific API
 * Java Specific APIs are using standard camel cases method names.
 * @subsection garbage_collection Java Specific API
 * All classes which are linked with C resources have the finalize() method
 * overriden, but due mostly undefined logic of GC's finalize() invocation
 * frequency, Enduro/X Java API implements cleanup() method, so that in code
 * explicitly resources could be free'd up.
 *
 * @subsection errorhandling_sec Error handling
 * In Enduro/X Java module error handling is done in a Java native way exceptions
 * are thrown. Non checked exceptions are used and exceptions which might be
 * throw by underlaying Enduro/X C API calls are denoted in documentation.
 * There might be other exceptions thrown by the APIs, in case if there are
 * some issues with JNI calls, like out of memory or missing classes. This
 * normally shall not happen.
 */
public class AtmiCtx {
	
    /**
     * Pointer to C ATMI Context object
     */
    long ctx = 0;

    /**
     * Get The C Context
     * @return 
     */
    public long getCtx() {
        return ctx;
    }

    /**
     * Set the C context
     * @param ctx 
     */
    public void setCtx(long ctx) {
        this.ctx = ctx;
    }

    static {
       System.loadLibrary("exjava"); // Load native library at runtime
                            // *.dll (Windows) or *.so (Unixes)
    }
    
    /* Have some static hash list of the services we advertise */
    
    /**
     * Internal list of services
     */
    private static Map<String, Service> svcMap = new HashMap<String, Service>();
    
    /**
     * List of ATMI Contexts currently open
     */
    private static Map<Long, Long> ctxMap = new HashMap<Long, Long>();
    
    /**
     * Context map mutex
     */
    private static final Lock ctxMapMutex = new ReentrantLock(true);
    
    
    /**
     * Unsolicited callback handler
     */
    UnsolCallback unslcb = null;
    
    
    /**
     * Server interface (if we run in server mode)
     */
    private Server svr = null;  
    
    /**
     * Get server object (if any)
     * @return 
     */
    public Server getSvr() {
        return svr;
    }
    
    /* TODO: We need a registry with non terminated ATMI contexts
     * so that we can hook up the JVM and remove all open contexts at
     * shutdown 
     */

    /**
     * Get ATMI Error 
     * @return Error tuple (code and message)
     */
    private native ErrorTuple getAtmiError();

    /**
     * Allocate new ATMI Context
     * ptr to ATMI context
     * @return C pointer to context object
     * @exception AtmiTPESYSTEMException
     */
    private static native long tpnewctxt();

    /**
     * Free up ATMI Context
     * @param ctx ATMI Context
     */
    private static native void tpfreectxt(long ctx);

    /**
     * Allocate buffer
     * @param btype buffer type name
     * @param bsubtype buffer sub type or empty string (if no subtype)
     * @param size buffer size in bytes
     * @return allocate ATMI buffer
     * @throw AtmiTPEINVALException invalid arguments passed to {@code btype} or 
     *  {@code bsubtype}
     * @throw AtmiTPEOTYPEException invalid types specified (or sub-type)
     * @throw AtmiTPESYSTEMException system exception occurred
     * @throw AtmiTPEOSException Operating System error occurred
     */
    public native TypedBuffer tpalloc(String btype, String bsubtype, long size);

    /**
     * Allocate new ATMI Context
     * @throw AtmiTPESYSTEMException Failed to allocate new context object in
     *  C space.
     */
    public AtmiCtx()  {
        /* This thorws TPESYSTEM if failed.*/
       ctx = tpnewctxt();

        /* Add context to static synced list of context
         * this contexts shall be terminated when JVM stops.
         */
        if (0x0 != ctx)
        {
            // register this context in hash list for free up...
            ctxMapMutex.lock();
            try {
                //Hmm seems this does not allow to perform GC on object...
                //as we are in the list...
                ctxMap.put((Long)ctx, (Long)ctx);
            }
            finally {
                ctxMapMutex.unlock();
            }
        }
    }
    
    /**
     * Terminate XATMI Session. This does not remove the context.
     * To remove context (including XATMI terminate), use \ref cleanup() method.
     * @throws AtmiTPEPROTOException Called from XATMI server (main thread)
     * @throws AtmiTPESYSTEMException Failed to close conversations
     * @throws AtmiTPEOSException System failure occurred during serving. 
     *  See logs i.e. user log, or debugs for more info. 
     *  That could insufficient memory or other error
     */
    public native void tpterm();
    
    /**
     * Clean up the object (basically this is destructor) as we cannot relay
     * on finalize() auto call of from the Garbage Collector. Thus we object
     * goes out of the scope, this method needs to be called. This automatically
     * invokes \ref tpterm() too.
     */
    public void cleanup() {
        if (0x0 != ctx)
        {
            /* clean up the hash */
            ctxMapMutex.lock();
            try {
                /* terminate context at C side 
                 * Contexts can be removed by shutdown hooks...
                 */
                if (ctxMap.get((Long)ctx) != 0)
                {
                   tplogError(">>> About to GC: %x!!!", ctx);
                   finalizeC(ctx);
                   ctxMap.remove((Long)ctx);
                }
            }
            finally {
                ctx = 0;
                ctxMapMutex.unlock();
            }
        }
    }
    
    /**
     * Kill all (terminate all contexts)
     */
    static void destructAll()
    {
        ctxMapMutex.lock();
        try
        {
            Iterator it = ctxMap.entrySet().iterator();
            while (it.hasNext()) {
                
                Map.Entry pair = (Map.Entry)it.next();
                Long cPtr = (Long)pair.getKey();
                finalizeC((long)cPtr);
                it.remove();
            }
            
            /* Delete all from hash map here? */
        } finally {
            ctxMapMutex.unlock();
        }
    }
    
    /**
     * Register shutdown hook for cleaning up ATMI context instances
     */
    static{
        try{
            
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                destructAll();
            }
        });
            
        }catch(Exception e){
            throw new RuntimeException("Exception occured in creating singleton instance");
        }
    }
    
    /**
     * Terminate the context at C side (tpterm + remove ctx by it self)
     * @param cPtr ATMI Context pointer
     */
    private static native void finalizeC(long cPtr);
    
    /**
     * Terminate the ATMI Context
     * @throws Throwable 
     */
    @Override
    protected void finalize() throws Throwable {

        cleanup();

        //Remove ATMI context...
        super.finalize();
    }
    
    /**
     * Perform tp return
     * @param rval return value (TPSUCCESS/TPFAIL)
     * @param rcode user return code
     * @param data data buffer to return, can be NULL to?
     * @param flags 
     */
    public native void tpreturn(int rval, long rcode, TypedBuffer data, long flags);
    
    /**
     * Forward the call to other service for processing
     * @param svc target service name
     * @param data data buffer to send
     * @param flags call flags (reserved )
     */
    public native void tpforward(String svcname, TypedBuffer data, long flags);
    
    /**
     * Incoming service call dispatch to advertised service.
     * Called by C side
     * @param svcInfo service call infos
     */
    void tpCallDispatch(TpSvcInfo svcInfo) {
        try
        {
            /* the exception will be captured at C side */
            svcMap.get(svcInfo.getName()).tpService(this, svcInfo);
        }
        catch (Exception e)
        {
            //Log exception (i.e. backtrace)...
            
            tplogndrxex(AtmiConst.LOG_ERROR, 
                    String.format("Service [%s] generated exception", 
                            svcInfo.getName()), e);
            
            //Return fail with the same buffer in case of exception!
            tpreturn(AtmiConst.TPFAIL, AtmiConst.TPESVCERR, 
                    svcInfo.data, AtmiConst.TPSOFTERR);
        }
    }

    /**
     * Run server in context
     * @param arg Command line arguments passed to java
     * @param nocheck Do not check the arguments
     */
    private native int tpRunC(String[] arg, boolean nocheck);
    
    /**
     * Run server instance. Only one thread is allowed to step into this
     * @param svr server class
     * @param arg Command line argumenst passed to the Enduro/X core. This is
     *  optional and can be NULL. In that case NDRX_SV* environment variables
     *  are used.
     * @return -1 (failed with out specified error) or 0 Success (finished ok)
     * @throws  AtmiTPEINVALException invalid command line arguments or invalid
     *  ATMI context.
     */
    public synchronized int tprun(Server svr, String[] arg)
    {
        if (null==svr)
        {
            throw new AtmiTPEINVALException("svr argument is null!");
        }
        
        this.svr = svr;
        
        tplogndrx(AtmiConst.LOG_INFO, "Booting server");
        
        /* Call native server entry (this should in return boot call server
         * interface 
         */
        return tpRunC(arg, false);
    }
    
    /**
     * Run server using Enduro/X environment variables
     * @param svr server class instance
     * @return -1 (failed with out specified error) or 0 Success (finished ok)
     * @throws  AtmiTPEINVALException invalid command line arguments or invalid
     *  ATMI context.
     */
    public synchronized int tprun(Server svr)
    {
        if (null==svr)
        {
            throw new AtmiTPEINVALException("svr argument is null!");
        }
        
        this.svr = svr;
        
        tplogndrx(AtmiConst.LOG_INFO, "Booting server");
        
        /* Call native server entry (this should in return boot call server
         * interface 
         */
        return tpRunC(null, true);
    }
    
    /**
     * TP logger
     * @param lev log level
     * @param file optional file name
     * @param line optional line number in file (if no metadata infos, use -1)
     * @param message log message
     */
    private native void tplogC(int lev, String file, long line, String message);

    /**
     * NDRX package internal logger
     * @param lev log level
     * @param file optional file name
     * @param line optional line number in file (if no metadata infos, use -1)
     * @param message log message
     */
    private native void tplogndrxC(int lev, String file, long line, String message);
    
    /**
     * Query logger information
     * @param lev current log level
     * @param flags  See TPLOGQI_GET_ and TPLOGQI_EVAL_ flag constants
     * @return LOG_FACILITY_ bits, TPLOGQI_RET_ bits, and bits from 24..32 
     *  represents log level.
     */
    public native int tplogqinfo(int lev, long flags);
    
    /**
     * Call the C side of advertise. This basically performs the low level
     * C side advertise of the internal dispatching method.
     * During the service call dispatching method shall resolve the actual object
     * of the "svc" and invoke tpService().
     * @param svcname Service name
     * @param funcname Function name
     * @throws AtmiTPEOSException System failure occurred during serving. 
     *    See logs i.e. user log, or debugs for more info. That could insufficient 
     *    memory or other error.
     */
    private native void tpadvertiseC(String svcname, String funcname);

    /**
     * Advertise service.
     * @param svcname Service name
     * @param funcname Function name
     * @param svc interface to object implementing Service
     * @throw AtmiTPEOSException System failure occurred during serving. 
     *    See logs i.e. user log, or debugs for more info. That could insufficient 
     *    memory or other error.
     */
    public void tpadvertise(String svcname, String funcname, Service svc) {
    
	/* call the native interface - advertise service*/
        tpadvertiseC(svcname, funcname);
	
	/* add service to hash list */
        svcMap.put(svcname, svc);
    }
    
    /**
     * Log exception to UBF logger
     * @param lev debug level
     * @param msg custom message
     * @param e exception to backtrace
     */
    native void tplogubfex(int lev, String msg, Throwable e);
    
    /**
     * Log exception to NDRX logger
     * @param lev debug level
     * @param msg custom message
     * @param e exception to back
     */
    native void tplogndrxex(int lev, String msg, Throwable e);
    
    /**
     * Log exception to user logger
     * @param lev debug level
     * @param msg trace message
     * @param e exception to backtrace
     */
    public native void tplogex(int lev, String msg, Throwable e);
    
    /**
     * Log exception to ulog
     * @param msg custom message
     * @param e exception to backtrace
     */
    public native void userlogex(String msg, Throwable e);
    
    /**
     * Write the ndrx log (for library internal use). Package level access.
     * @param lev Log level
     * @param directCall is this function called directly or via logger wrapper?
     * @param format format string
     * @param arguments  format arguments
     */
    void tplogndrx(int lev, String format, Object... arguments) {

        int log_config = tplogqinfo(lev,
                AtmiConst.TPLOGQI_GET_NDRX | AtmiConst.TPLOGQI_EVAL_DETAILED);

        String filename = "";
        long line = AtmiConst.FAIL;

        if (log_config <= 0) {

            /* nothing to log */
            return;
        }

        if ((log_config & AtmiConst.TPLOGQI_EVAL_DETAILED) > 0) {

            /* backtrace the file and line number */
            StackTraceElement[] s = Thread.currentThread().getStackTrace();

            filename = s[2].getFileName();
            line = s[2].getLineNumber();
        }
        /* write the log according to the detail level with or with out
         * stack tracking
         */
        tplogndrxC(lev, filename, line, String.format(format, arguments));
    }

    /**
     * Write the user log
     * @param lev Log level
     * @param directCall is this function called directly or via logger wrapper?
     * @param format format string
     * @param arguments  format arguments
     */
    public void tplog(int lev, boolean directCall, String format, Object... arguments) {
        
        int log_config = tplogqinfo(lev, 
                AtmiConst.TPLOGQI_GET_TP | AtmiConst.TPLOGQI_EVAL_DETAILED);
        
        String filename = "";
        long line = AtmiConst.FAIL;
        
        if (log_config <= 0) {
            
            /* nothing to log */
            return;
        }
        
        if ((log_config & AtmiConst.TPLOGQI_RET_HAVDETAILED) > 0) {
            
            /* backtrace the file and line number */
            StackTraceElement[] s = Thread.currentThread().getStackTrace();
            
            if (directCall) {
                filename = s[2].getFileName();
                line = s[2].getLineNumber();
            } else {
                filename = s[3].getFileName();
                line = s[3].getLineNumber();
            }
        }
        /* write the log according to the detail level with or with out
         * stack tracking
         */
        tplogC(lev, filename, line, String.format(format, arguments));
    }
    
    /**
     * Log Always
     * @param format format string
     * @param arguments variable args 
     */
    public void tplogAlways(String format, Object... arguments) {
        tplog(AtmiConst.LOG_ALWAYS, false, format, arguments);
    }   
    
    /**
     * Log Error
     * @param format format string
     * @param arguments variable args 
     */
    public void tplogError(String format, Object... arguments) {
        tplog(AtmiConst.LOG_ERROR, false, format, arguments);
    }
    
    /**
     * Log Warning
     * @param format format string
     * @param arguments variable args 
     */
    public void tplogWarn(String format, Object... arguments) {
        tplog(AtmiConst.LOG_WARN, false, format, arguments);
    }
    
    /**
     * Log Info
     * @param format format string
     * @param arguments variable args 
     */
    public void tplogInfo(String format, Object... arguments) {
        tplog(AtmiConst.LOG_INFO, false, format, arguments);
    }
    
    /**
     * Log Debug
     * @param format format string
     * @param arguments variable args 
     */
    public void tplogDebug(String format, Object... arguments) {
        tplog(AtmiConst.LOG_DEBUG, false, format, arguments);
    }
    
    /**
     * Log Dump
     * @param format format string
     * @param arguments variable args 
     */
    public void tplogDump(String format, Object... arguments) {
        tplog(AtmiConst.LOG_DUMP, false, format, arguments);
    }
    
    /**
     * Initialize current ATMI Context as a ATMI client
     * @param tpinfo might be NULL. Currently not used by Enduro/X
     * @throws AtmiTPEINVALException environment not configured
     * @throws AtmiTPESYSTEMException System failure occurred during serving. 
     *  See logs i.e. user log, or debugs for more info.
     * @throws AtmiTPEOSException System failure occurred during serving. 
     *  See logs i.e. user log, or debugs for more info.
     */
    public native void tpinit(TpInit tpinfo);
    
    /**
     * Write user log message
     * @param msg formatted message to log in user log
     */
    private native void userlogC(String msg);
    
    /**
     * Write user log (for more critical events)
     * @param format java format string
     * @param arguments arguments for format string
     */
    public void userlog(String format, Object... arguments) {
        userlogC(String.format(format, arguments));
    }
    
    /**
     * Call the service
     * See tpcall(3) manpage for more information.
     * @param svc XATMI service name to call
     * @param idata Input buffer
     * @param flags ATMI flags; TPNOTRAN, TPSIGRSTRT, TPNOTIME, TPNOCHANGE, TPTRANSUSPEND,
     *  TPNOBLOCK
     * @throws AtmiTPEINVALException Invalid parameter is given to function. 
     *  Either service name is NULL or flags does not allow to change the value.
     * @throws AtmiTPENOENTException No service (svc parameter) 
     *  advertised in system.
     * @throws AtmiTPETIMEException Service did not reply in given time 
     *  (NDRX_TOUT).
     * @throws AtmiTPESVCFAILException Service returned TPFAIL. This is 
     *  application level failure.
     * @throws AtmiTPESVCERRException System level service failure. Server 
     *  died during the message presence in service queue.
     * @throws AtmiTPESYSTEMException System failure occurred during serving. 
     *  See logs i.e. user log, or debugs for more info.
     * @throws AtmiTPEOSException System failure occurred during serving. 
     *  See logs i.e. user log, or debugs for more info.
     * @throws AtmiTPEBLOCKException Service request queue was full and 
     *  TPNOBLOCK flag was specified.
     * @throws AtmiTPNOABORTException Do not abort global transaction (if one 
     *  in progress), even if service failed.
     * @return We return the buffer form the call.
     */
    public native TypedBuffer tpcall(String svc, TypedBuffer idata, long flags);
    
    /**
     * Asynchronous service call
     * See tpacall(3) manpage for more information.
     * @param svc service name to call
     * @param idata input typed buffer to send to service
     * @param flags call flags: TPNOTRAN, TPSIGRSTRT, TPNOBLOCK, TPNOREPLY
     * @throws AtmiTPEINVALException Invalid parameter is given to function. 
     *  Either service name is NULL or flags does not allow to change the value.
     * @throws AtmiTPENOENTException No service (svc parameter) advertised in system.
     * @throws AtmiTPETIMEException Service did not reply in given time (NDRX_TOUT).
     * @throws AtmiTPESVCFAILException Service returned TPFAIL. This is 
     *  application level failure.
     * @throws AtmiTPESVCERRException System level service failure. Server 
     *  died during the message presence in service queue.
     * @throws AtmiTPESYSTEMException System failure occurred during serving. 
     *  See logs i.e. user log, or debugs for more info.
     * @throws AtmiTPEOSException System failure occurred during serving. 
     *  See logs i.e. user log, or debugs for more info.
     * @throws AtmiTPEBLOCKException Service queue was full and 
     *  TPNOBLOCK flag was specified.
     * @throws AtmiTPNOTIMEException Do not expire call by server process, 
     *  If message age is older than NDRX_TOUT timeout (or timeout 
     *  overridden by tptoutset(3)).
     * @return call descriptor
     */
    public native int tpacall(String svc, TypedBuffer idata, long flags);
    
    /**
     * Get reply from Asynchronous call tpacall().
     * See tpgetrply(3) manpage for more information.
     * @param[in] cd call descriptor returned by  \ref tpacall(). In case if 
     *  TPGETANY flag is set, the field value is ignored
     * @param[in] idata input data buffer used for receiving the reply data
     *  copied/reallocated to TpgetrplyResult
     * @param flags  TPGETANY, TPNOBLOCK, TPSIGRSTRT, TPNOTIME,
     *  TPNOCHANGE, TPNOABORT 
     * @return result buffer and call descriptor of call returned.
     * @throws AtmiTPEBLOCKException *TPENOBLOCK* was specified in flags and no 
     *  message is in queue.
     * @throws AtmiTPEINVALException Invalid parameter is given to function. 
     *  Particularly pointer parameters are NULL.
     * @throws AtmiTPETIMEException Service did not reply in 
     *  given time (NDRX_TOUT time).
     * @throws AtmiTPESVCFAILException Service returned TPFAIL. 
     *  This is application level failure.
     * @throws AtmiTPESVCERRException System level service failure. 
     *  Server died during the message presence in service queue.
     * @throws AtmiTPESYSTEMException System failure occurred during serving. 
     *  See logs i.e. user log, or debugs for more info.
     * @throws AtmiTPEOSException System failure occurred during serving. 
     *  See logs i.e. user log, or debugs for more info.
     * @thorws AtmiTPEBADDESCException Invalid call descriptor - out of range or
     *  not issued/canceled value. This exception is thrown only in case if TPGETANY
     *  flag is not passed to the call.
     */
    public native TpgetrplyResult tpgetrply(int cd, TypedBuffer idata, long flags);
    
    /**
     * Conversational methods group
     * @defgroup Convers conversational methods
     * @{
     */
    
    /**
     * Connect to XATMI conversational server
     * See tpconnect(3) manpage for more information.
     * @param svc service name
     * @param idata input XATMI buffer
     * @param flags AtmiConst flags - TPNOTRAN, TPSIGRSTRT, TPNOTIME, TPTRANSUSPEND,
     *  TPSENDONLY, TPRECVONLY. Binary or'able.
     * @return connection descriptor (cd)
     * @throws AtmiTPEINVALException Invalid parameter is given to function. 
     *  Either service name is NULL or data is not NULL, but not allocated by tpalloc()
     * @throws AtmiTPENOENTException No service (svc parameter) 
     *  advertised in system.
     * @throws AtmiTPELIMITException Max number of connections are reached.
     *  Currently max number of connections is limited to 5 (MAX_CONNECTIONS macro).
     * @throws AtmiTPETIMEException Service did not reply in 
     *  given time (NDRX_TOUT).
     * @throws AtmiTPESVCFAILException Service returned TPFAIL. This is 
     *  application level failure.
     * @throws AtmiTPESVCERRException System level service failure. Server 
     *  died during the message presence in service queue.
     * @throws AtmiTPESYSTEMException System failure occurred during serving. 
     *  See logs i.e. user log, or debugs for more info.
     * @throws AtmiTPEOSException System failure occurred during serving. See 
     *  logs i.e. user log, or debugs for more info.
     */
    public native int tpconnect(String svc, TypedBuffer idata, long flags);
    
    /**
     * Send data to conversational endpoint
     * See tpsend(3) manpage for more information.
     * @param cd conversation descriptor
     * @param idata input data buffer. Data buffer is valid after the call
     * @param flags TPRECVONLY, TPNOBLOCK, TPSIGRSTRT
     * @return receive event. If sent ok w/o event, then return code is 0.
     *  Event constants: TPEV_SVCERR, TPEV_SVCFAIL
     * @throws AtmiTPEINVALException Invalid call descriptor cd passed in or 
     *  data pointer is not pointing to buffer allocated by tpalloc().
     * @throws AtmiTPETIMEException Was unable to send message in given 
     *  time(NDRX_TOUT env param.).
     * @throws AtmiTPESYSTEMException System failure occurred during 
     *  serving. See logs i.e. user log, or debugs for more info.
     * @throws AtmiTPEOSException System failure occurred during 
     *  serving. See logs i.e. user log, or debugs for more info.
     */
    public native long tpsend(int cd, TypedBuffer idata, long flags);
    
    /**
     * Received data from endpoint
     * @param cd conversation id
     * @param idata input data buffer into which received data shall be stored.
     *  This object becomes invalid after the call, new instance is provided
     *  in return object. The data type may be changed of the buffer.
     * @param flags TPRECVONLY, TPNOBLOCK, TPSIGRSTRT, TPNOBLOCK
     * @return Receive result (event, cd, typed buffer). Possible event constants:
     *  TPEV_DISCONIMM, TPEV_SENDONLY, TPEV_SVCERR, TPEV_SVCFAIL, TPEV_SVCSUCC
     * 
     * @throws AtmiTPEINVALException Invalid call descriptor cd passed in.
     * @throws AtmiTPETIMEException Service did not reply in given time (NDRX_TOUT).
     * @throws AtmiTPEPROTOException System level service failure. Server died during 
     *  the message presence in service queue.
     * @throws AtmiTPESYSTEMException System failure occurred during serving. See logs 
     *  i.e. user log, or debugs for more info.
     * @throws AtmiTPEOSException System failure occurred during serving. See logs i.e. 
     *  user log, or debugs for more info.
     */
    public native TprecvResult tprecv(int cd, TypedBuffer idata, long flags);
    
    /**
     * Disconnect from conversation
     * @param cd conversation descriptor / id
     * @throws AtmiTPEINVALException Invalid connection descriptor passed in.
     * @throws AtmiTPEOSException System failure occurred during serving. See 
     * logs i.e. user log, or debugs for more info.
     */
    public native void tpdiscon(int cd);
    
    /** @} */ // end of Convers
    
    /**
     * Cancel a tpacall(). This basically marks the in the call descriptor registry
     * that response is no more waited from the caller.
     * @param cd call descriptor from the tpacall()
     * @throws AtmiTPEBADDESCException Invalid call descriptor (out of range).
     * @throws AtmiTPEINVALException Enduro/X is not configured.
     * @throws AtmiTPESYSTEMException System failure occurred during serving. 
     *  See logs i.e. user log, or debugs for more info.
     * @throws AtmiTPEOSException System failure occurred during serving. 
     * See logs i.e. user log, or debugs for more info.
     */
    public native void tpcancel(int cd);
    
    /**
     * tpsetunsol(3) tpbroadcast(3) tpchkunsol(3) tpnotify(3)
     * tpdequeue(3) tpdequeueex(3) tpenqueue tpenqueueex
     * tpabort(3) tpbegin(3) tpcommit(3) tpopen(3) tpclose(3) tpsuspend(3) tpresume(3)
     */
    
    /**
     * Group of boolean expression routines
     * @defgroup Bbool boolean expression handling routines
     * @{
     */
    
    /**
     * Compile boolean expression.
     * For more information see Bboolco(3) manpage
     * @param expr UBF boolean expression
     * @return Compiled boolean expression handler
     * @throws UbfBALIGNERRException Corrupted buffer or pointing to not aligned memory area.
     * @throws UbfBNOTFLDException Buffer not fielded, not correctly allocated or corrupted.
     * @throws UbfBBADFLDException Invalid field id passed.
     * @throws UbfBNOTPRESException Field not present.
     * @throws UbfFMALLOCException Malloc failed.
     * @throws UbfBSYNTAXException Synax error in script.
     * @throws UbfBBADNAMEException Bad field name specified.
     */
    public native BExprTree Bboolco(String expr);
    
    /**
     * Print compiled expression to output stream.
     * For more information see Bboolpr(3) manpage
     * @param cexpr compiled expression
     * @param ostream output stream. write(byte[] bytes) is used for printing
     *  the output.
     */
    public native void Bboolpr(BExprTree cexpr, OutputStream ostream);
    
    /**
     * Set Expression evaluator function callback.
     * Then C will proxy back to java side for actual function invoke.
     * @param funcname function name to register for evaluator
     */
    native void BboolsetcbfC (String funcname);
    
    /**
     * UBF expression callback mappings 
     */
    static Map<String, Bboolcbf> ubfcbMap = new HashMap<String, Bboolcbf>();
    
    /**
     * Lock for hashmap of the UBF callback
     */
    static final Lock ubfcbMapMutex = new ReentrantLock(true);
    
    /**
     * Get the boolean expression callback object in synchronized way
     * @param funcname function for which to lookup
     * @return null if  not found, not null if object found
     */
    static Bboolcbf BoolgetcbfObj(String funcname) {
        
        ubfcbMapMutex.lock();
        
        try
        {
            return ubfcbMap.get(funcname);
        }
        finally
        {
            ubfcbMapMutex.unlock();
        }
    }
    
    /**
     * Set callback function from the expression, so that this function
     * can be referenced/called from the compiled script/evaluated UBF boolean
     * expression script. This function is not thread safe. meaning that
     * any other thread shall not execute Boolean expressions while the
     * callback are being registered.
     * The function registers the callbacks at the java side into hashmap.
     * and registration is done at C side too. The mapping is done in a singleton
     * as it might be used by different contexts.
     * For more information see Bboolsetcbf(3) manpage.
     * @param funcname function name (C style function name)
     * @param callback interface to callback object
     * @throws UbfBBADNAMEException Invalid function name. See Bboolsetcbf(3)
     *  manpage.
     */
    public void Bboolsetcbf (String funcname, Bboolcbf callback) {
        
        if (null==callback) {
            throw new AtmiTPEINVALException("callback argument must not be null!");
        }
        
        /* register the callback func down to C level */
        BboolsetcbfC(funcname);
                
        /* Register at java level */
        ubfcbMapMutex.lock();
        try
        {
            ubfcbMap.put(funcname, callback);
        }
        finally
        {
            ubfcbMapMutex.unlock();
        }
        
    }
    
    /** @} */ // end of Bbool
    
    
    /**
     * Return field type in string format. The possible values are following:
     * 'short', 'long', 'char', 'float', 'double', 'string', 'carray'
     * See Btype(3) manpage for more information.
     * @param bfldid compiled field ied.
     * @return returns field type string
     * @throws UbfBTYPERRException Invalid field - 
     *  bad type extracted from oldest bits.
     */
    public native String Btype(int bfldid);
    
    /**
     * Get field name by give compiled field id. If field tables are bad
     * or field is not found, then output id is formatted as: "((BFLDID32)%d)"
     * where "%d" is filled with \p bfldid
     * See BFname(3) manpage for more information.
     * @param bfldid compiled filed
     * @return String name of the field
     */
    public native String Bfname(int bfldid);
    
    /**
     * Return current XATMI timeout for IPC communications.
     * By default method uses NDRX_TOUT env variable for readings if value
     * is not override by \see AtmiCtx::tptoutset
     * See tptoutget(3) manpage for more information.
     * @return timeout in seconds currently used by XATMI IPC sub-system
     */
    public native int tptoutget();
    
    /**
     * Returns last user return code from tpcall/tpgetrply result.
     * See tpurcode(3) manpage for more information.
     * @return User return code (a second argument used to pass in tpreturn)
     */
    public native long tpurcode();
    
    /**
     * Set XATMI sub-system timeout value
     * @param tout number of seconds for XATMI calls like tpcall or tpgetrply
     *  before returning timeout exception
     * See tptoutset(3) manpage for more information.
     * @throws AtmiTPEINVALException tout parameter is less or equal to 0.
     */
    public native void tptoutset(int tout);
    
    /**
     * Allocate string object with value
     * @param s string value to set
     * @return Typed string filled with value
     */
    public TypedString newString(String s) {
        
        TypedString ret = (TypedString)tpalloc("STRING", "", s.getBytes().length);
        
        ret.setString(s);
        
        return ret;
    }
    
    /**
     * Allocate JSON object
     * @param j string value to set
     * @return Typed string filled with value
     */
    public TypedJson newJson(String j) {
        
        TypedJson ret = (TypedJson)tpalloc("JSON", "", j.getBytes().length);
        
        ret.setJSON(j);
        
        return ret;
    }
    
    public TypedCarray newCarray(byte  [] b) {
        
        TypedCarray ret = (TypedCarray)tpalloc("CARRAY", "",b.length);
        
        ret.setBytes(b);
        
        return ret;
        
    }
    
    /**
     * List XATMI buffers (C side pointers)
     * @return List of allocated XATMI buffers.
     *  May be used for debug purposes, i.e. detect gc operations.
     */
    public native long [] getBuffers();
    
    
    /**
     * Unsolicited message handling
     * @defgroup Unsol unsolicited message handling group
     * @{
     */
    
    /**
     * Set the notification handler at C side
     * @param cb null or callback object. Used to detect when to deactivate
     *  the unsolicited message handling.
     */
    native void tpsetunsolC(UnsolCallback cb);
    
    /**
     * Unsolicited message handling
     * Call the "unslcb" callback if have one registered.
     * @param idata input data buffer for callback
     * @param flags flags for callback
     */
    void unsolDispatch(TypedBuffer idata, long flags) {
        
        if (null!=unslcb) {
            unslcb.unsolCallback(this, idata, flags);
        }
    }
    
    /**
     * Register unsolicited message callback handler
     * See tpsetunsol(3) manpage for more information.
     * @param cb callback handler
     * @return previous callback handler
     * @throws AtmiTPEINVALException Environment variables not configured, 
     *  see ex_env(5) page.
     * @throws AtmiTPESYSTEMException System failure occurred during serving. 
     *  See logs i.e. user log, or debugs for more info.
     * @throws AtmiTPEOSException System failure occurred during serving. 
     *  See logs i.e. user log, or debugs for more info.
     */
    public UnsolCallback tpsetunsol(UnsolCallback cb) {
        
        UnsolCallback tmp = this.unslcb;
        
        this.unslcb = cb;
        
        tpsetunsolC(cb);
        
        return tmp;
    }
    
    /**
     * Send notification to clients
     * See tpnotify(3) manpage for more information.
     * @param clientid client id (received in service call)
     * @param idata input typed buffer
     * @param flags valid flags:
     *  - TPNOBLOCK Do not block on full client queue, instead return error.
     *  - TPNOTIME Do not timeout when waiting on full queue (TPNOBLOCK is not set).
     *  - TPSIGRSTRT Restart the system call in progress if interrupted by signal 
     *  handler. This affects only underlaying mq_* function calls.
     *  - TPACK Reserved for future use, Enduro/X silently ignores this flag. 
     *  Thus tpnotify() call does not get any acknowledgement signal that client 
     *  is processed the message. This is limitation of Enduro/X.
     * @throws AtmiTPEINVALException Environment variables not configured, 
     *  see ex_env(5) page, or invalid parameters have been passed to the 
     *  function, for example clientid is NULL or corrupted.
     * @throws AtmiTPENOENTException The local delivery was about to be 
     *  performed (no remote client call) and the client process did not 
     *  exist on local machine. This error will be reported 
     *  regardless of the TPACK flag.
     * @throws AtmiTPETIMEException Blocking message delivery did timeout. 
     *  Meaning that client queue was full and TPNOBLOCK nor TPNOTIME was set. 
     *  Error is returned from local clients only regardless of the TPACK flag. 
     *  If client resists on remote node,
     *  then this error can be returned only when time-out occurred while s
     *  ending message to then local bridge server.
     * @throws AtmiTPEBLOCKException Client queue was full and TPNOBLOCK flag 
     *  was not specified.
     * @throws AtmiTPESYSTEMException System failure occurred during serving. 
     *  See logs i.e. user log, or debugs for more info.
     * @throws AtmiTPEOSException System failure occurred during serving. 
     *  See logs i.e. user log, or debugs for more info.
     */
    public native void tpnotify(ClientId clientid, TypedBuffer idata, long flags);
    
    /**
     * Broadcast a message to matched clients or send message to remove
     * remote server's '@TPBROADNNN' service for local broadcasting.
     * See tpbroadcast(3) manpage for more information.
     * @param lmid cluster node id to which message shall be delivered.
     *  if flag TPREGEXMATCH is present, then regexp is used for the given
     *  string to match the cluster nodes to which message shall be delivered.
     *  Max string length is MAXTIDENT*2-1.
     * @param usrname this is reserved for future use.
     * @param cltname executable name which shall be matched for notification
     *  delivery. If flag TPREGEXMATCH is set, then this is regular expression
     *  of the binary name to be matched. Max string length in bytes
     *  are MAXTIDENT*2-1.
     * @param idata input typed buffer (can be null) for notification delivery
     * @param flags following broadcast flags may be set:
     *  - TPNOBLOCK Do not block on full client queue, instead return error.
     *   - TPNOTIME Do not timeout when waiting on full queue (TPNOBLOCK is not set).
     *   - TPSIGRSTRT Restart the system call in progress if interrupted by 
     *      signal handler. This affects only underlaying mq_* function calls.
     *   - TPREGEXMATCH Match lmid (cluster node id) and cltname by 
     *      assuming that these are regular expressions.
     * @throws AtmiTPEINVALException Environment variables not configured, see 
     *      ex_env(5) page, or invalid parameters have been passed to the function, 
     *      for example clientid, lmtid or username are set and they are invalid 
     *      regular expressions (i.e. with TPREGEXMATCH set).
     * @throws  AtmiTPESYSTEException System failure occurred during serving. 
     *      See logs i.e. user log, or debugs for more info.
     * @throws  AtmiTPEOSException System failure occurred during serving. 
     *      See logs i.e. user log, or debugs for more info.
     */
    public native void tpbroadcast(String lmid, String usrname, String cltname, 
            TypedBuffer idata, long flags);
   
    /**
     * Process received unsolicited messages and invoke callback set by
     *  tpsetunsol(). 
     * See tpchkunsol(3) manpage for more information.
     * @return Number of unsolicited messages processed
     * @throws AtmiTPESYSTEMException System failure occurred during serving. 
     *      See logs i.e. user log, or debugs for more info.
     * @throws AtmiTPEOSException System failure occurred during serving. 
     *      See logs i.e. user log, or debugs for more info.
     */
    public native int tpchkunsol();
    
    /** @} */ // end of Unsol
    
    
    
    /**
     * Queue handling
     * @defgroup Queue Enduro/X queue routines
     * @{
     */
    
    /**
     * Enqueue message to persistent storage
     * See tpenqueue(3) manpage for more information.
     * @param qspace queue space name
     * @param qname queue name
     * @param ctl queue control obj
     * @param idata data buffer
     * @param flags flags TPNOTRAN, TPSIGRSTRT, TPNOTIME, TPNOBLOCK, TPNOABORT
     * @throws  AtmiTPEINVALException data is NULL, qspace is NULL, or 
     *  nodeid and srvid is 0. Error can be generate in case if qname is 
     *  empty or NULL. ctl is NULL or data does not point to 	tpalloc() allocated buffer.
     * @throws  AtmiTPENOENTException Tmqueue server is not available.
     * @throws  AtmiTPETIMEException Service did not reply in given 
     *  time (NDRX_TOUT).
     * @throws  AtmiTPEDIAGNOSTICException More information is provided 
     *  in TPQCTL.diagnostic field.
     * @throws  AtmiTPESVCFAILException Tmqueue Service returned TPFAIL. This 
     *  is application level failure.
     * @throws  AtmiTPESVCERRException Tmqueue service got system level failure. 
     *  Server died during the message presence in service queue.
     * @throws  AtmiTPESYSTEMException Enduro/X internal error occurred. 
     *  See logs for more info.
     * @throws  AtmiTPEOSException System failure occurred during serving. 
     *  See logs i.e. user log, or debugs for more info.
     */
    public native void tpenqueue (String qspace, String qname, TPQCTL ctl, 
            TypedBuffer idata, long flags);
    
    /**
     * Enqueue message to persistent storage, extended
     * See tpenqueueex(3) manpage for more information.
     * @param nodeid Cluster node ID on which queue server is present
     * @param srvid Queue Server ID on given cluster node id
     * @param qname queue name
     * @param ctl queue control obj
     * @param idata data buffer
     * @param flags flags TPNOTRAN, TPSIGRSTRT, TPNOTIME, TPNOBLOCK, TPNOABORT
     * @throws  AtmiTPEINVALException data is NULL, qspace is NULL, or 
     *  nodeid and srvid is 0. Error can be generate in case if qname is 
     *  empty or NULL. ctl is NULL or data does not point to 	tpalloc() allocated buffer.
     * @throws  AtmiTPENOENTException Tmqueue server is not available.
     * @throws  AtmiTPETIMEException Service did not reply in given 
     *  time (NDRX_TOUT).
     * @throws  AtmiTPEDIAGNOSTICException More information is provided 
     *  in TPQCTL.diagnostic field.
     * @throws  AtmiTPESVCFAILException Tmqueue Service returned TPFAIL. This 
     *  is application level failure.
     * @throws  AtmiTPESVCERRException Tmqueue service got system level failure. 
     *  Server died during the message presence in service queue.
     * @throws  AtmiTPESYSTEMException Enduro/X internal error occurred. 
     *  See logs for more info.
     * @throws  AtmiTPEOSException System failure occurred during serving. 
     *  See logs i.e. user log, or debugs for more info.
     */
    public native void tpenqueueex (short nodeid, short srvid, String qname, TPQCTL ctl, 
            TypedBuffer idata, long flags);
   
    /** @} */ // end of Queue
    
}

/* vim: set ts=4 sw=4 et smartindent: */
