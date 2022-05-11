package test;

@Dummy
public abstract class EntityL2<T> extends EntityL1<T> implements Testable<String>, Testable2<Integer> {
    private String field1;
    private int field2;
    private T field3;

    public String getField1() {
        return field1;
    }

    public void setField1(String field1) {
        this.field1 = field1;
    }

    public int getField2() {
        return field2;
    }

    public void setField2(int field2) {
        this.field2 = field2;
    }

    public T getField3() {
        return field3;
    }

    public void setField3(T field3) {
        this.field3 = field3;
    }

    @Override
    public void test(String value) {

    }
}