package org.endurox;

public class AtmiBuf {
	
  /**
   * Pointer to C ATMI Context object
   */
   private AtmiCtx ctx;
   private boolean doFinalize;
   private long len;
   private long cPtr;

   /**
    * Free up the given buffer
    * @param[in] cPtr C pointer to buffer
    */
   private native void tpfree (long cPtr);

   /**
    * Initialise ATMI Object
    * @param ctx[in] ATMI Context allocated this method
    * @param doFinalize[in] Should
    * @param cPtr[in] C pointer to allocated block
    * @param len[in] Conditional buffer length
    */
   public AtmiBuf(AtmiCtx ctx, boolean doFinalize, long cPtr, long len) {
        ctx = ctx;
        this.doFinalize = doFinalize;
        this.cPtr = cPtr;
        this.len = len;
   }

    /**
     * Finish of this ATMI buffer
     */
   @Override
   public void finalize() {
        //Call free from context?
        //Or call directly the c? I guess directly as context might be already
        //invalid
        if (doFinalize)
        {
            tpfree(cPtr);
        }
   }

    /**
     * Set the finalize flag
     */
    public void setDoFinalize(boolean b)
    {
        doFinalize = b;
    }

   static {
      System.loadLibrary("exjava"); // Load native library at runtime
                           // hello.dll (Windows) or libenduroxjava.so (Unixes)
   }
}

