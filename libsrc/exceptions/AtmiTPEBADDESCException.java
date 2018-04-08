package org.endurox;

/**
 * TPEBADDESC exception
 */
public class AtmiTPEBADDESCException extends AtmiException {
        
    public AtmiTPEBADDESCException(String msg) {
        super(AtmiConstants.TPEBADDESC, msg);
    }
    
    public AtmiTPEBADDESCException(String msg, Throwable throwable) {
        super(AtmiConstants.TPEBADDESC, msg, throwable);
    }
}


