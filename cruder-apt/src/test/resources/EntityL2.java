package test;

@Dummy
public class EntityL2 extends EntityL1<Long> {
    private String field1;
    private int field2;

    public String getField1() {
        return field1;
    }

    public void setField1(String field1) {
        this.field1 = field1;
    }
}