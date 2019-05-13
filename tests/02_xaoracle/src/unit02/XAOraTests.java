import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.Test;
import static org.junit.Assert.*;
import org.endurox.*;

/**
 * OracleDB XA Tests
 */
public class XAOraTests {
    
    /**
     * Delete all records from db
     */
    public void deleteAll(AtmiCtx ctx) {
        
        boolean tranStarted = false;
        
        if (ctx.tpgetlev() == 0) {
            
            tranStarted = true;
            ctx.tpbegin(60, 0);
        }
        
        /* delete all if error then abort... */
        String sql = "delete from EXJTEST";
        
        try {
          Statement stmt = ctx.getConnection().createStatement();
          stmt.executeUpdate(sql);
          System.out.println("Record deleted successfully");
          
          stmt.close();
          
        } catch (SQLException e) {
          ctx.tplogex(AtmiConst.LOG_ERROR, "Failed to delete: ".concat(sql), e);
        
          if (tranStarted) {
              ctx.tpabort(0);
          }
          
          throw new RuntimeException(e);
        }
        
        if (tranStarted) {
            try {
                
            } 
            catch (Exception e) {
                
                ctx.tplogex(AtmiConst.LOG_ERROR, "Failed to get getTransactionTimeout", e);
                
            }
            ctx.tpcommit(0);
        }
    }
    
    
    /**
     * Check count in database
     * @param ctx Atmi Context
     * @param match number of records to match
     */
    public void chkCount(AtmiCtx ctx, int match) {
        
        boolean tranStarted = false;
        
        if (ctx.tpgetlev() == 0) {
            
            tranStarted = true;
            ctx.tpbegin(60, 0);
        }
        
        /* delete all if error then abort... */
        String sql = "select count(*) as count from EXJTEST";
        
        try {
            
            Statement stmt = ctx.getConnection().createStatement();

            ResultSet rs3 = stmt.executeQuery(sql);
            
            rs3.next();

            int count = rs3.getInt("count");
            
            assertEquals(match, count);
            stmt.close();
            
        } catch (SQLException e) {
            ctx.tplogex(AtmiConst.LOG_ERROR, "Failed to get count: ".concat(sql), e);

            if (tranStarted) {
                ctx.tpabort(0);
            }

            throw new RuntimeException(e);
        }
        
        if (tranStarted) {
            ctx.tpcommit(0);
        }
    }
             
    /**
     * Perform Basic XA Tests
     */
    @Test
    public void basicXA() {
        
        Connection conn = null;
        AtmiCtx ctx = new AtmiCtx();
        assertNotEquals(ctx.getCtx(), 0x0);
       
        TypedUbf ub = (TypedUbf)ctx.tpalloc("UBF", "", 1024);
        assertNotEquals(ub, null);

        boolean leaktest = false;
        int leaktestSec = 0;
        StopWatch w = new StopWatch();
        
        String leaktestSecStr = System.getenv("NDRXJ_LEAKTEST"); 
        
        if (null!=leaktestSecStr)
        {
            leaktestSec = Integer.parseInt(leaktestSecStr);
            leaktest = true;
            
            //Nothing to test at the moment
            if (!System.getenv("NDRXJ_LEAKTEST_NAME").equals("basicXA")) {
                return;
            }
        }
        
        /**
         * TODO: Have long term test for memory management.
         * ideally we would time terminated tests, for example 5 min...?
         * thus we need a stop watch construction to have in java..
         */
        for (int i=0; ((i<1000) || (leaktest && w.deltaSec() < leaktestSec)); i++)
        {
            /* TODO: Do the logic */
            ub = (TypedUbf)ctx.tpalloc("UBF", "", 1024);
            assertNotEquals(ub, null);
            
            ctx.tpopen();
            /* create some test table */
            conn = ctx.getConnection();
            assertNotEquals(null, conn);
            
            /* empty up the table... */
            deleteAll(ctx);
             
            /* run the transaction */
            ctx.tpbegin(160, 0);
            assertEquals(1, ctx.tpgetlev());

            for (int j=0; j<100; j++)
            {
                /* call the server, insert some data */
                ub.Bchg(test.T_LONG_FLD, 0, (long)j);
                ub.Bchg(test.T_STRING_FLD, 0, String.format("Name %d", j));
                ub.Bchg(test.T_STRING_2_FLD, 0, String.format("City %d", j));

                /* well we shall suspend our side here... */
                ub = (TypedUbf)ctx.tpcall("DoTran", ub, AtmiConst.TPTRANSUSPEND);
            }
            
            /* as we resume tran, get the count... */
            chkCount(ctx, 100);
            
            /* lets abort */
            ctx.tpabort(0);
            
            /* there should be 0 after abort */
            chkCount(ctx, 0);
            
            
            /* todo perform abort */

            /* get the count, shall be 0 */

            /* run again tran */

            /* check, shall be 0 */

            ctx.tpclose();
            
        }
        
        ub.cleanup();
        ctx.cleanup();
    }
    
}