package org.endurox;

/**
 * NETOUT exception
 */
public class NstdNETOUTException extends NstdException {
        
    public NstdNETOUTException(String msg) {
        super(AtmiConstants.NETOUT, msg);
    }
    
    public NstdNETOUTException(String msg, Throwable throwable) {
        super(AtmiConstants.NETOUT, msg, throwable);
    }
}


