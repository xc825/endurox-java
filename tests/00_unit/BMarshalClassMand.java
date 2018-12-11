import org.endurox.*;

/**
 * Test basically mandatory field
 */
public class BMarshalClassMand {
             
    /* standard types: */
    @UbfField(bfldid=test.T_SHORT_FLD, ubfmin=1, ojbmin=1)
    short tshort;

    public short getTshort() {
        return tshort;
    }

    public void setTshort(short tshort) {
        this.tshort = tshort;
    }
}
