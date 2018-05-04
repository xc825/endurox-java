package org.endurox.exceptions;
import org.endurox.AtmiConstants;

/**
 * TPINITFAIL exception
 */
public class AtmiTPINITFAILException extends AtmiException {
        
    public AtmiTPINITFAILException(String msg) {
        super(AtmiConstants.TPINITFAIL, msg);
    }
    
    public AtmiTPINITFAILException(String msg, Throwable throwable) {
        super(AtmiConstants.TPINITFAIL, msg, throwable);
    }
}

