package org.endurox;

/**
 * NEINVALKEY exception
 */
public class NstdNEINVALKEYException extends NstdException {
        
    public NstdNEINVALKEYException(String msg) {
        super(AtmiConstants.NEINVALKEY, msg);
    }
    
    public NstdNEINVALKEYException(String msg, Throwable throwable) {
        super(AtmiConstants.NEINVALKEY, msg, throwable);
    }
}


