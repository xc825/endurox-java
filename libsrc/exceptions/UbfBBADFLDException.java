package org.endurox;

/**
 * BBADFLD exception
 */
public class UbfBBADFLDException extends UbfException {
        
    public UbfBBADFLDException(String msg) {
        super(AtmiConstants.BBADFLD, msg);
    }
    
    public UbfBBADFLDException(String msg, Throwable throwable) {
        super(AtmiConstants.BBADFLD, msg, throwable);
    }
}


