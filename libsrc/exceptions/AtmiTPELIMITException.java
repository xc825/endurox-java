package org.endurox;

/**
 * TPELIMIT exception
 */
public class AtmiTPELIMITException extends AtmiException {
        
    public AtmiTPELIMITException(String msg) {
        super(AtmiConstants.TPELIMIT, msg);
    }
    
    public AtmiTPELIMITException(String msg, Throwable throwable) {
        super(AtmiConstants.TPELIMIT, msg, throwable);
    }
}


