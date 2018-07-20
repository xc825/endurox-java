import static org.junit.Assert.*;
import org.junit.Test;
import org.endurox.*;
import org.endurox.exceptions.*;

public class AtmiCtxTest {

  /**
   * Test object allocation, buffer alloc
   */
  @Test
  public void newCtx() {
    AtmiCtx ctx = new AtmiCtx();
    assertNotEquals(ctx.getCtx(), 0x0);
    TypedUBF ub = (TypedUBF)ctx.tpalloc("UBF", "", 1024);
    assertNotEquals(ub, null);

    /* test sub-type NULL */
    ub = (TypedUBF)ctx.tpalloc("UBF", null, 1024);
    assertNotEquals(ub, null);
  }

  /**
   * Test invalid buffer type exception
   */
  @Test(expected = org.endurox.exceptions.AtmiTPEINVALException.class)
  public void testInvalidBuffer() {
    AtmiCtx ctx = new AtmiCtx();
    assertNotEquals(ctx.getCtx(), 0x0);
    TypedUBF ub = (TypedUBF)ctx.tpalloc(null, "", 1024);
  }

}
