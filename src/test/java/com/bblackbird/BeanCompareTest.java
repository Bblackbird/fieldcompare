package com.bblackbird;

import com.bblackbird.FieldCompare.ContextFilter;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.junit.*;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.Map.Entry;

import static com.bblackbird.FieldCompare.allFieldContextFilter;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

//import org.apache.log4j.Logger;


public class BeanCompareTest {

    //private static Logger LOGGER = Logger.getLogger (BeanCompareTest.class);

    public enum PositionType {
        BLACK, RED;
    }

    public static abstract class Base implements Serializable {
        private static final long serialversionUID = 1L;

        @Override
        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this);
        }

        @Override
        public boolean equals(Object obj) {
            return EqualsBuilder.reflectionEquals(this, obj);
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }
    }

    public static class Position extends Base {

        private static final long serialVersionUID = 1L;

        private String book;
        private String product;
        private double positionAmt;
        private List<String> traders;
        private int[] stats;
        private String[] subBooks;
        private Map<String, String> bookToProducts;
        private Set<String> bookSet;

        public Position() {
        }

        public String getBook() {
            return book;
        }

        public void setBook(String book) {
            this.book = book;
        }

        public String getProduct() {
            return product;
        }

        public void setProduct(String product) {
            this.product = product;
        }

        public double getPositionAmt() {
            return positionAmt;
        }

        public void setPositionAmt(double positionAmt) {
            this.positionAmt = positionAmt;
        }

        public List<String> getTraders() {
            return traders;
        }

        public void setTraders(List<String> traders) {
            this.traders = traders;
        }

        public int[] getStats() {
            return stats;
        }

        public void setStats(int[] stats) {
            this.stats = stats;
        }

        public String[] getSubBooks() {
            return subBooks;
        }

        public void setSubBooks(String[] subBooks) {
            this.subBooks = subBooks;
        }

        public Map<String, String> getBookToProducts() {
            return bookToProducts;
        }

        public void setBookToProducts(Map<String, String> bookToProducts) {
            this.bookToProducts = bookToProducts;
        }

        public Set<String> getBookSet() {
            return bookSet;
        }

        public void setBookSet(Set<String> bookSet) {
            this.bookSet = bookSet;
        }
    }

    public static class Portfolio extends Base {

        private static final long serialVersionUID = 1L;

        private BigDecimal totalPosition;
        private Date date;
        private long size;
        private String book;
        private Position position;
        private PositionType positionType;
        private List<String> products;
        private long[] stats;
        private String[] names;
        private Position[] arrayPositions;
        private Map<String, Double> fxRates;
        private Set<Double> rateSet;
        List<Position> positions = new ArrayList<>();

        public Portfolio() {
        }

        public BigDecimal getTotalPosition() {
            return totalPosition;
        }

        public void setTotalPosition(BigDecimal totalPosition) {
            this.totalPosition = totalPosition;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public String getBook() {
            return book;
        }

        public void setBook(String book) {
            this.book = book;
        }

        public List<Position> getPositions() {
            return positions;
        }

        public void addPosition(Position position) {
            positions.add(position);
        }

        public void removePosition(Position position) {
            positions.remove(position);
        }

        public void setPositions(List<Position> positions) {
            this.positions = positions;
        }

        public Position getPosition() {
            return position;
        }

        public void setPosition(Position position) {
            this.position = position;
        }

        public PositionType getPositionType() {
            return positionType;
        }

        public void setPositionType(PositionType positionType) {
            this.positionType = positionType;
        }

        public long[] getStats() {
            return stats;
        }

        public void setStats(long[] stats) {
            this.stats = stats;
        }

        public String[] getNames() {
            return names;
        }

        public void setNames(String[] names) {
            this.names = names;
        }

        public Position[] getArrayPositions() {
            return arrayPositions;
        }

        public void setArrayPositions(Position[] arrayPositions) {
            this.arrayPositions = arrayPositions;
        }

        public Map<String, Double> getFxRates() {
            return fxRates;
        }

        public void setFxRates(Map<String, Double> fxRates) {
            this.fxRates = fxRates;
        }

        public List<String> getProducts() {
            return products;
        }

        public void setProducts(List<String> products) {

            this.products = products;
        }

        public Set<Double> getRateSet() {
            return rateSet;
        }


        public void setRateSet(Set<Double> rateSet) {
            this.rateSet = rateSet;
        }
    }

    private static BeanCompare beanCompare;


    @BeforeClass
    public static void setUpBeforeClass() {
        beanCompare = new BeanCompare();
    }

    @AfterClass
    public static void teardownAfterClass() {
    }

    @Before
    public void setup() {
    }

    @After
    public void teardown() {
        beanCompare.clearComparators();
    }

    private static PodamFactory objFactory = new PodamFactoryImpl();

    public static <T> T getObject(Class<T> klazz) {
        return objFactory.manufacturePojo(klazz);
    }

    public static <T> T getFullObject(Class<T> klazz) {
        return objFactory.manufacturePojoWithFullData(klazz);
    }

    public static <T> T clone(Serializable value) {
        return (T) SerializationUtils.clone(value);
    }

    @Test
    public void testAll() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);

        BigDecimal totalPosition = left.getTotalPosition().add(BigDecimal.TEN);
        right.setTotalPosition(totalPosition);

        Date date = new Date();
        date.setTime(left.getDate().getTime() + 1000);
        right.setDate(date);

        String book = left.getBook() + "_DIFF";
        right.setBook(book);

        long size = left.getSize() + 10;
        right.setSize(size);

        String b = left.getPosition().getBook() + "_DIFF";
        right.getPosition().setBook(b);

        PositionType pt = left.getPositionType();
        if (pt == PositionType.RED) {
            pt = PositionType.BLACK;
        } else
            pt = PositionType.RED;
        right.setPositionType(pt);

        String product = left.getProducts().get(2) + "DIFF";
        right.getProducts().set(2, product);

        long longStat = left.getStats()[3] + 100L;
        right.getStats()[3] = longStat;

        String name = left.getNames()[2] + "DIFF";
        right.getNames()[2] = name;

        String account = left.getArrayPositions()[2].getBook() + "_DIFF";
        right.getArrayPositions()[2].setBook(account);

        String key = left.getFxRates().keySet().iterator().next();
        double rate = left.getFxRates().get(key) + 1000.00;
        right.getFxRates().put(key, rate);

        double newRate = addUniqueValue(left.getRateSet(), 99.99, 10.00);
        List<Double> sortedRateSet = new ArrayList<>(left.getRateSet());
        Collections.sort(sortedRateSet);
        int setPosition = sortedRateSet.indexOf(newRate);

        double positionAmt = left.getPositions().get(0).getPositionAmt() + 1000.00;
        right.getPositions().get(0).setPositionAmt(positionAmt);

        int stats = left.getPositions().get(0).getStats()[0] + 100;
        right.getPositions().get(0).getStats()[0] = stats;

        String subBook = left.getPositions().get(0).getSubBooks()[3] + "_DIFF";
        right.getPositions().get(0).getSubBooks()[3] = subBook;

        String trader = left.getPositions().get(4).getTraders().get(4) + "_DIFF";
        right.getPositions().get(4).getTraders().set(4, trader);

        Entry<String, String> entry = left.getPositions().get(4).getBookToProducts().entrySet().iterator().next();
        String keyBook = entry.getKey();
        String value = entry.getValue() + "_DIFF";
        right.getPositions().get(4).getBookToProducts().put(keyBook, value);

        Set<String> bookSet = left.getPositions().get(4).getBookSet();
        String newBook = addUniqueValue(bookSet, "FAKE_BOOK", "_1");
        List<String> sortedBookset = new ArrayList<>(bookSet);
        Collections.sort(sortedBookset);
        int bookSetPosition = sortedBookset.indexOf(newBook);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        List<Diff> expectedDiffs = new ArrayList<>(Arrays.asList(
                new Diff("totalPosition", BigDecimal.class, left.getTotalPosition(), right.getTotalPosition()),
                new Diff("date", Date.class, left.getDate(), right.getDate()),
                new Diff("size", long.class, left.getSize(), right.getSize()),
                new Diff("book", String.class, left.getBook(), right.getBook()),
                new Diff("position.book", String.class, left.getPosition().getBook(), right.getPosition().getBook()),
                new Diff("positionType", PositionType.class, left.getPositionType(), right.getPositionType()),
                new Diff("products.2", String.class, left.getProducts().get(2), product),
                new Diff("stats.3", long.class, left.getStats()[3], right.getStats()[3]),
                new Diff("names.2", String.class, left.getNames()[2], right.getNames()[2]),
                new Diff("arrayPositions.2.book", String.class, left.getArrayPositions()[2].getBook(), account),
                new Diff("fxRates." + key, Double.class, left.getFxRates().get(key), rate),
                new Diff("rateSet." + setPosition, Double.class, newRate, "MISSING"),
                new Diff("positions.0.positionAmt", double.class, left.getPositions().get(0).getPositionAmt(), right.getPositions().get(0).getPositionAmt()),
                new Diff("positions.0.stats.0", int.class, left.getPositions().get(0).getStats()[0], right.getPositions().get(0).getStats()[0]),
                new Diff("positions.0.subBooks.3", String.class, left.getPositions().get(0).getSubBooks()[3], right.getPositions().get(0).getSubBooks()[3]),
                new Diff("positions.4.traders.4", String.class, left.getPositions().get(4).getTraders().get(4), right.getPositions().get(4).getTraders().get(4)),
                new Diff("positions.4.bookToProducts." + keyBook, String.class, left.getPositions().get(4).getBookToProducts().get(keyBook), value),
                new Diff("positions.4.bookSet." + bookSetPosition, String.class, newBook, "MISSING")
        ));

        List<Diff> shouldBeEmpty = beanCompare.diffs(diffs, expectedDiffs);

        assertThat(shouldBeEmpty, empty());

        assertEquals(diffs, expectedDiffs);

    }

    @Test
    public void testEqualobjects() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, empty());
    }

    @Test
    public void testStringDiff() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);
        String book = left.getBook() + "_DIFF";
        right.setBook(book);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("book", String.class, left.getBook(), right.getBook()))));
    }

    @Test
    public void testStringFullDiff() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);
        String book = left.getBook() + "_DIFF";
        right.setBook(book);

        String newProduct = "DUMMY_PRODUCT";
        right.getProducts().add(newProduct);

        List<Diff> diffs = beanCompare.fullDiffs(left, right);

        assertThat(diffs, is(Arrays.asList(
                new Diff("book", String.class, left.getBook(), right.getBook()),
                new Diff("book", String.class, right.getBook(), left.getBook()),
                new Diff("products." + (right.getProducts().size() - 1), String.class, newProduct, "NULL"))));
    }

    @Test
    public void testStringLeftNull() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);

        left.setBook(null);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("book", String.class, "NULL", right.getBook()))));
    }

    @Test
    public void testStringRightNull() {

        Portfolio left = getObject(Portfolio.class);
        Portfolio right = clone(left);
        right.setBook(null);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("book", String.class, left.getBook(), "NULL"))));
    }


    @Test
    public void testStringBothNull() {

        Portfolio left = getObject(Portfolio.class);
        left.setBook(null);

        Portfolio right = clone(left);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, empty());
    }

    @Test
    public void testLongDiff() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);
        long size = left.getSize() + 10;
        right.setSize(size);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("size", long.class, left.getSize(), right.getSize()))));
    }

    @Test
    public void testBigDecimalDiff() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);
        BigDecimal totalPosition = left.getTotalPosition().add(BigDecimal.TEN);
        right.setTotalPosition(totalPosition);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("totalPosition", BigDecimal.class, left.getTotalPosition(), right.getTotalPosition()))));
    }

    @Test
    public void testBigDecimalDiff2() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);
        BigDecimal totalPosition = left.getTotalPosition().add(BigDecimal.valueOf(10 ^ 9));
        right.setTotalPosition(totalPosition);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("totalPosition", BigDecimal.class, left.getTotalPosition(), right.getTotalPosition()))));
    }

    @Test
    public void testBigDecimalDiff3() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);
        BigDecimal totalPosition = left.getTotalPosition().add(BigDecimal.valueOf(1000000.00));
        right.setTotalPosition(totalPosition);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("totalPosition", BigDecimal.class, left.getTotalPosition(), right.getTotalPosition()))));
    }

    @Test
    public void testBigDecimalLeftNull() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);

        left.setTotalPosition(null);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("totalPosition", BigDecimal.class, "NULL", right.getTotalPosition()))));

    }

    @Test
    public void testBigDecimalRightNull() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);
        right.setTotalPosition(null);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("totalPosition", BigDecimal.class, left.getTotalPosition(), "NULL"))));
    }

    @Test
    public void testBigDecimaBothNull() {

        Portfolio left = getObject(Portfolio.class);
        left.setTotalPosition(null);
        Portfolio right = clone(left);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, empty());
    }

    @Test
    public void testDate() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);
        Date date = new Date();
        date.setTime(left.getDate().getTime() + 1000);
        right.setDate(date);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("date", Date.class, left.getDate(), right.getDate()))));
    }


    @Test
    public void testDateLeftNull() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);
        left.setDate(null);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("date", Date.class, "NULL", right.getDate()))));
    }

    @Test
    public void testDateRightNull() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);

        right.setDate(null);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("date", Date.class, left.getDate(), "NULL"))));
    }

    @Test
    public void testDateBothNull() {

        Portfolio left = getObject(Portfolio.class);
        left.setDate(null);

        Portfolio right = clone(left);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, empty());
    }

    @Test
    public void testEnum() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);
        PositionType pt = left.getPositionType();
        if (pt == PositionType.RED) {
            pt = PositionType.BLACK;
        } else
            pt = PositionType.RED;
        right.setPositionType(pt);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("positionType", PositionType.class, left.getPositionType(), right.getPositionType()))));
    }

    @Test
    public void testEnumLeftNull() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);

        left.setPositionType(null);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("positionType", PositionType.class, "NULL", right.getPositionType()))));
    }

    @Test
    public void testEnumRightNull() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);

        right.setPositionType(null);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("positionType", PositionType.class, left.getPositionType(), "NULL"))));
    }

    @Test
    public void testEnumBothNull() {

        Portfolio left = getObject(Portfolio.class);
        left.setPositionType(null);

        Portfolio right = clone(left);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, empty());
    }

    @Test
    public void testUserDefinedLeftNull() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);

        left.setPosition(null);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("position", Position.class, "NULL", right.getPosition()))));
    }

    @Test
    public void testUserDefinedRightNull() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);

        right.setPosition(null);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("position", Position.class, left.getPosition(), "NULL"))));
    }

    @Test
    public void testUserDefinedBothNull() {

        Portfolio left = getObject(Portfolio.class);
        left.setPosition(null);

        Portfolio right = clone(left);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, empty());
    }


    @Test
    public void testUserDefinedobject() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);
        String book = left.getPosition().getBook() + "_DIFF";

        right.getPosition().setBook(book);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("position.book", String.class, left.getPosition().getBook(), right.getPosition().getBook()))));
    }

    @Test
    public void testUserDefinedValueLeftNull() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);

        left.getPosition().setBook(null);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("position.book", String.class, "NULL", right.getPosition().getBook()))));
    }

    @Test
    public void testUserDefinedvaluRightNull() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);

        right.getPosition().setBook(null);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("position.book", String.class, left.getPosition().getBook(), "NULL"))));
    }

    @Test
    public void testUserDefinedValueBothNull() {

        Portfolio left = getObject(Portfolio.class);
        left.getPosition().setBook(null);

        Portfolio right = clone(left);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, empty());
    }

    @Test
    public void testUserDefinedobjectInlist() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);
        double positionAmt = left.getPositions().get(0).getPositionAmt() + 1000.00;
        right.getPositions().get(0).setPositionAmt(positionAmt);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);


        assertThat(diffs, is(Arrays.asList(new Diff("positions.0.positionAmt", double.class, left.getPositions().get(0).getPositionAmt(), right.getPositions().get(0).getPositionAmt()))));
    }

    @Test
    public void testUserDefinedobjectInlist2() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);
        double positionAmt = left.getPositions().get(3).getPositionAmt() + 1000.00;
        right.getPositions().get(3).setPositionAmt(positionAmt);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("positions.3.positionAmt", double.class, left.getPositions().get(3).getPositionAmt(), right.getPositions().get(3).getPositionAmt()))));
    }

    @Test
    public void testUserDefinedobjectInListwithContextFilter() {
        Portfolio left = getObject(Portfolio.class);
        Portfolio right = clone(left);
        double positionAmt = left.getPositions().get(3).getPositionAmt() + 1000.00;
        right.getPositions().get(3).setPositionAmt(positionAmt);

        List<Diff> diffs = beanCompare.diffsWithContextFilter(left, right, allFieldContextFilter);
        assertThat(diffs, is(Arrays.asList(new Diff("positions.3.positionAmt", double.class, left.getPositions().get(3).getPositionAmt(), right.getPositions().get(3).getPositionAmt()))));
    }

    public static ContextFilter filterSpecificNestedField = fullName -> l -> r -> f -> "positions.3.positionAmt".equals(fullName) ? false : true;

    @Test
    public void testUserDefinedobjectInlistUsingContextFilterBasedonFullName() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);
        double positionAmt = left.getPositions().get(3).getPositionAmt() + 1000.00;
        right.getPositions().get(3).setPositionAmt(positionAmt);

        List<Diff> diffs = beanCompare.diffsWithContextFilter(left, right, filterSpecificNestedField);

        assertThat(diffs, empty());
    }

    @Test
    public void testUserDefinedobjectInlistUsingContextfilterBasedonFullNameNegativeTest() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);
        double positionAmt = left.getPositions().get(4).getPositionAmt() + 1000.00;
        right.getPositions().get(4).setPositionAmt(positionAmt);

        List<Diff> diffs = beanCompare.diffsWithContextFilter(left, right, filterSpecificNestedField);

        assertThat(diffs, not(empty()));
        assertThat(diffs, is(Arrays.asList(new Diff("positions.4.positionAmt", double.class, left.getPositions().get(4).getPositionAmt(), right.getPositions().get(4).getPositionAmt()))));
    }

    public static ContextFilter filterSpecificNestedValue = fullName -> l -> I -> f -> "positions.3.positionAmt".equals(fullName) ? false : true;

    @Test
    public void testUserDefinedObjectInListUsingContextFilterBasedOnValue() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);

        double positionAmt = left.getPositions().get(3).getPositionAmt() + 1000.00;

        right.getPositions().get(3).setPositionAmt(positionAmt);

        List<Diff> diffs = beanCompare.diffsWithContextFilter(left, right,
                fullName -> l -> r -> f -> {
                    return "positions.3.positionAmt".equals(fullName) ? (Double) r == positionAmt ? false : true : true;
                });

        assertThat(diffs, empty());
    }

    @Test
    public void testUserDefinedObjectInListUsingContextFilterBasedOnValue2() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);
        double positionAmt = left.getPositions().get(3).getPositionAmt() + 1000.00;
        right.getPositions().get(3).setPositionAmt(positionAmt);

        List<Diff> diffs = beanCompare.diffsWithContextFilter(left, right,
                fullName -> l -> r -> f -> {
                    return double.class.equals(f.getType()) ? (Double) r == positionAmt ? false : true : true;
                });
        assertThat(diffs, empty());
    }

    private static Set<String> defaultExcludeFieldset = Sets.newHashSet("size", "book");

    private static ContextFilter contextFilter = fullName -> l -> I -> f -> {
        return defaultExcludeFieldset.contains(f.getName()) ?
                false :
                !(fullName.startsWith("positions.") && fullName.contains(Position.class.getName()) && "TMF".equals(((Position) l).getBook()));
    };

    @Test
    public void testUserDefinedobjectInListUsingContextFilterBasedonComplexValue() {

        Portfolio left = getObject(Portfolio.class);
        left.getPositions().get(3).setBook("TMF");

        Portfolio right = clone(left);

        String diffBook = right.getBook() + "_DIFF";
        right.setBook(diffBook);

        long diffSize = right.getSize() + 10;
        right.setSize(diffSize);

        double positionAmt = left.getPositions().get(3).getPositionAmt() + 1000.00;
        right.getPositions().get(3).setPositionAmt(positionAmt);

        List<Diff> diffs = beanCompare.diffsWithContextFilter(left, right, contextFilter);

        assertThat(diffs, empty());
    }

    @Test
    public void testUserDefinedObjectInListUsingContextFilterBasedonComplexValue2() {

        Portfolio left = getObject(Portfolio.class);
        left.getPositions().get(3).setBook("TMF");

        Portfolio right = clone(left);

        double positionAmt = left.getPositions().get(3).getPositionAmt() + 1000.00;
        right.getPositions().get(3).setPositionAmt(positionAmt);

        double positionAmt2 = left.getPositions().get(4).getPositionAmt() + 1000.00;
        right.getPositions().get(4).setPositionAmt(positionAmt2);

        List<Diff> diffs = beanCompare.diffsWithContextFilter(left, right, contextFilter);

        assertThat(diffs, is(Arrays.asList(new Diff("positions.4.positionAmt", double.class, left.getPositions().get(4).getPositionAmt(), right.getPositions().get(4).getPositionAmt()))));
    }

    @Test
    public void testUserDefinedObjectInListUsingContextFilterBasedOnComplexValue3() {

        Portfolio left = getObject(Portfolio.class);
        left.getPositions().get(3).setBook("TMF");

        Portfolio right = clone(left);

        double positionAmt = left.getPositions().get(3).getPositionAmt() + 1000.00;
        right.getPositions().get(3).setPositionAmt(positionAmt);

        double positionAmt2 = left.getPositions().get(4).getPositionAmt() + 1000.00;
        right.getPositions().get(4).setPositionAmt(positionAmt2);

        List<Diff> diffs = beanCompare.diffsWithContextFilter(left, right, allFieldContextFilter);

        assertThat(diffs, is(Arrays.asList(new Diff("positions.3.positionAmt", double.class, left.getPositions().get(3).getPositionAmt(), right.getPositions().get(3).getPositionAmt()),
                new Diff("positions.4.positionAmt", double.class, left.getPositions().get(4).getPositionAmt(), right.getPositions().get(4).getPositionAmt()))));
    }

    @Test
    public void testUserDefinedobjectInListUsingContextFilterBasedonComplexValueWithLeftNull() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);

        left.getPositions().get(3).setBook(null);

        double positionAmt2 = left.getPositions().get(4).getPositionAmt() + 1000.00;
        right.getPositions().get(4).setPositionAmt(positionAmt2);

        List<Diff> diffs = beanCompare.diffsWithContextFilter(left, right, allFieldContextFilter);

        assertThat(diffs, is(Arrays.asList(new Diff("positions.3.book", String.class, "NULL", right.getPositions().get(3).getBook()),
                new Diff("positions.4.positionAmt", double.class, left.getPositions().get(4).getPositionAmt(), right.getPositions().get(4).getPositionAmt()))));
    }

    @Test
    public void testUserDefinedObjectInListUsingContextFilterBasedOnComplexValueWithRightNull() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);

        right.getPositions().get(3).setBook(null);

        double positionAmt2 = left.getPositions().get(4).getPositionAmt() + 1000.00;
        right.getPositions().get(4).setPositionAmt(positionAmt2);

        List<Diff> diffs = beanCompare.diffsWithContextFilter(left, right, allFieldContextFilter);

        assertThat(diffs, is(Arrays.asList(new Diff("positions.3.book", String.class, left.getPositions().get(3).getBook(), "NULL"),
                new Diff("positions.4.positionAmt", double.class, left.getPositions().get(4).getPositionAmt(), right.getPositions().get(4).getPositionAmt()))));

    }

    @Test
    public void testUserDefinedobjectInListUsingContextfilterBasedOnComplexvalueWithBothNull() {

        Portfolio left = getObject(Portfolio.class);
        left.getPositions().get(3).setBook(null);

        Portfolio right = clone(left);

        double positionAmt2 = left.getPositions().get(4).getPositionAmt() + 1000.00;
        right.getPositions().get(4).setPositionAmt(positionAmt2);

        List<Diff> diffs = beanCompare.diffsWithContextFilter(left, right, allFieldContextFilter);

        assertThat(diffs, is(Arrays.asList(new Diff("positions.4.positionAmt", double.class, left.getPositions().get(4).getPositionAmt(), right.getPositions().get(4).getPositionAmt()))));
    }

    @Test
    public void testLongArray() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);
        long longstat = left.getStats()[3] + 100L;
        right.getStats()[3] = longstat;

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("stats.3", long.class, left.getStats()[3], right.getStats()[3]))));
    }

    @Test
    public void testStringArray() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);
        String name = left.getNames()[2] + "DIFF";
        right.getNames()[2] = name;

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("names.2", String.class, left.getNames()[2], right.getNames()[2]))));
    }

    @Test
    public void testStringArrayRightNull() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);
        right.getNames()[2] = null;

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("names.2", String.class, left.getNames()[2], "NULL"))));
    }

    @Test
    public void teststringArrayLeftNull() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);

        left.getNames()[2] = null;
        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, is(Arrays.asList(new Diff("names.2", String.class, "NULL", right.getNames()[2]))));
    }

    @Test
    public void testStringArrayBothNull() {
        Portfolio left = getObject(Portfolio.class);
        left.getNames()[2] = null;
        Portfolio right = clone(left);
        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, empty());
    }


    @Test
    public void testStringList() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);
        String product = left.getProducts().get(2) + "DIFF";
        right.getProducts().set(2, product);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("products.2", String.class, left.getProducts().get(2), product))));
    }

    @Test
    public void testStringList2() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);
        String product = left.getProducts().get(2) + "DIFF";
        left.getProducts().set(2, product);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("products.2", String.class, product, right.getProducts().get(2)))));
    }

    @Test
    public void testStringListRightNull() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);
        right.getProducts().set(2, null);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("products.2", String.class, left.getProducts().get(2), "NULL"))));
    }

    @Test
    public void testStringListLeftNull() {
        Portfolio left = getObject(Portfolio.class);
        Portfolio right = clone(left);
        left.getProducts().set(2, null);
        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("products.2", String.class, "NULL", right.getProducts().get(2)))));
    }

    @Test
    public void testStringListBothNull() {

        Portfolio left = getObject(Portfolio.class);

        left.getProducts().set(2, null);
        Portfolio right = clone(left);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, empty());
    }


    @Test
    public void testPositionArray() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);
        String book = left.getArrayPositions()[2].getBook() + "_DIFF";
        right.getArrayPositions()[2].setBook(book);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("arrayPositions.2.book", String.class, left.getArrayPositions()[2].getBook(), book))));
    }

    @Test
    public void testPositionArrayRightNull() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);

        right.getArrayPositions()[2].setBook(null);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("arrayPositions.2.book", String.class, left.getArrayPositions()[2].getBook(), "NULL"))));
    }

    @Test
    public void testPositionArrayLeftNull() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);
        left.getArrayPositions()[2].setBook(null);
        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, is(Arrays.asList(new Diff("arrayPositions.2.book", String.class, "NULL", right.getArrayPositions()[2].getBook()))));
    }

    @Test
    public void testPositionArrayBothNull() {

        Portfolio left = getObject(Portfolio.class);

        left.getArrayPositions()[2].setBook(null);
        Portfolio right = clone(left);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, empty());
    }


    @Test
    public void testMap() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);

        String key = left.getFxRates().keySet().iterator().next();

        double rate = left.getFxRates().get(key) + 1000.00;

        right.getFxRates().put(key, rate);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("fxRates." + key, Double.class, left.getFxRates().get(key), rate))));
    }

    @Test
    public void testMapRightNull() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);

        String key = left.getFxRates().keySet().iterator().next();
        right.getFxRates().put(key, null);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("fxRates." + key, Double.class, left.getFxRates().get(key), "NULL"))));
    }

    @Test
    public void testMapLeftNull() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);

        String key = left.getFxRates().keySet().iterator().next();
        left.getFxRates().put(key, null);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("fxRates." + key, Double.class, "NULL", right.getFxRates().get(key)))));
    }


    @Test
    public void testMapBothNull() {

        Portfolio left = getObject(Portfolio.class);
        String key = left.getFxRates().keySet().iterator().next();
        left.getFxRates().put(key, null);
        Portfolio right = clone(left);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, empty());
    }


    @Test
    public void testSet() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);

        double newRate = addUniqueValue(left.getRateSet(), 99.99, 10.00);

        List<Double> sortedRateSet = new ArrayList<>(left.getRateSet());
        Collections.sort(sortedRateSet);
        int position = sortedRateSet.indexOf(newRate);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("rateSet." + position, Double.class, newRate, "MISSING"))));
    }

    private double addUniqueValue(Set<Double> set, double value, double increment) {

        int size = set.size();
        double localValue = value;
        while (true) {
            set.add(localValue);
            if (set.size() > size)
                return localValue;
            localValue += increment;
        }
    }

    @Test
    public void testIntArrayInUserDefinedObjectInList() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);
        int stats = left.getPositions().get(0).getStats()[0] + 100;
        right.getPositions().get(0).getStats()[0] = stats;

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("positions.0.stats.0", int.class, left.getPositions().get(0).getStats()[0], right.getPositions().get(0).getStats()[0]))));
    }

    @Test
    public void testIntArrayInUserDefinedoOjectInListLeftNull() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);
        left.getPositions().get(0).setStats(null);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, is(Arrays.asList(new Diff("positions.0.stats", int[].class, "NULL", right.getPositions().get(0).getStats()))));
    }

    @Test
    public void testIntArrayInUserDefinedObjectInListRightNull() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);
        right.getPositions().get(0).setStats(null);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, is(Arrays.asList(new Diff("positions.0.stats", int[].class, left.getPositions().get(0).getStats(), "NULL"))));
    }

    @Test
    public void testIntArrayInUserDefinedobjectInListBothNull() {

        Portfolio left = getObject(Portfolio.class);
        left.getPositions().get(0).setStats(null);

        Portfolio right = clone(left);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, empty());
    }

    @Test
    public void teststringArrayInUserDefinedobjectInListLeftNull() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);
        left.getPositions().get(0).setSubBooks(null);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("positions.0.subBooks", String[].class, "NULL", right.getPositions().get(0).getSubBooks()))));
    }

    @Test
    public void testStringArrayInUserDefinedObjectInListRightNull() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);
        right.getPositions().get(0).setSubBooks(null);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("positions.0.subBooks", String[].class, left.getPositions().get(0).getSubBooks(), "NULL"))));
    }

    @Test
    public void testStringArrayInUserDefinedobjectInListBothNull() {

        Portfolio left = getObject(Portfolio.class);
        left.getPositions().get(0).setSubBooks(null);

        Portfolio right = clone(left);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, empty());
    }

    @Test
    public void testStringArrayInUserDefinedobjectInlist() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);
        String subBook = left.getPositions().get(0).getSubBooks()[3] + "_DIFF";
        right.getPositions().get(0).getSubBooks()[3] = subBook;

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("positions.0.subBooks.3", String.class, left.getPositions().get(0).getSubBooks()[3], right.getPositions().get(0).getSubBooks()[3]))));
    }

    @Test
    public void testStringArrayInUserDefinedObjectInListValueLeftNull() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);
        left.getPositions().get(0).getSubBooks()[3] = null;

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("positions.0.subBooks.3", String.class, "NULL", right.getPositions().get(0).getSubBooks()[3]))));
        ;
    }

    @Test
    public void testStringArrayInUserDefinedObjectInListValueRightNull() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);
        right.getPositions().get(0).getSubBooks()[3] = null;

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("positions.0.subBooks.3", String.class, left.getPositions().get(0).getSubBooks()[3], "NULL"))));
    }

    @Test
    public void testStringArrayInUserDefinedObjectInlistValueBothNull() {

        Portfolio left = getObject(Portfolio.class);
        left.getPositions().get(0).getSubBooks()[3] = null;

        Portfolio right = clone(left);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, empty());
    }

    @Test
    public void testStringListInUserDefinedObjectInList() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);
        String trader = left.getPositions().get(4).getTraders().get(4) + "_DIFF";
        right.getPositions().get(4).getTraders().set(4, trader);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("positions.4.traders.4", String.class, left.getPositions().get(4).getTraders().get(4), right.getPositions().get(4).getTraders().get(4)))));
    }

    @Test
    public void testMapInUserDefinedObjectInListLeftNull() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);

        left.getPositions().get(4).setBookToProducts(null);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("positions.4.bookToProducts", HashMap.class, "NULL", right.getPositions().get(4).getBookToProducts()))));
    }


    @Test
    public void testMapInUserDefinedObjectInListRightNull() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);
        right.getPositions().get(4).setBookToProducts(null);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("positions.4.bookToProducts", HashMap.class, left.getPositions().get(4).getBookToProducts(), "NULL"))));
    }

    @Test
    public void testMapInUserDefinedObjectInListBothNull() {

        Portfolio left = getObject(Portfolio.class);
        left.getPositions().get(4).setBookToProducts(null);

        Portfolio right = clone(left);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, empty());
    }

    @Test
    public void testMapInUserDefinedObjectInlist() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);

        Entry<String, String> entry = left.getPositions().get(4).getBookToProducts().entrySet().iterator().next();

        String key = entry.getKey();
        String value = entry.getValue() + "_DIFF";

        right.getPositions().get(4).getBookToProducts().put(key, value);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("positions.4.bookToProducts." + key, String.class, left.getPositions().get(4).getBookToProducts().get(key), value))));
    }

    @Test
    public void testMapInUserDefinedObjectInListValueLeftNull() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);

        Entry<String, String> entry = left.getPositions().get(4).getBookToProducts().entrySet().iterator().next();

        String key = entry.getKey();
        left.getPositions().get(4).getBookToProducts().put(key, null);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("positions.4.bookToProducts." + key, String.class, "NULL", right.getPositions().get(4).getBookToProducts().get(key)))));
    }

    @Test
    public void testMapInUserDefinedObjectInListValueRightNull() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);

        Entry<String, String> entry = left.getPositions().get(4).getBookToProducts().entrySet().iterator().next();

        String key = entry.getKey();
        right.getPositions().get(4).getBookToProducts().put(key, null);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("positions.4.bookToProducts." + key, String.class, left.getPositions().get(4).getBookToProducts().get(key), "NULL"))));
    }

    @Test
    public void testMapInUserDefinedObjectInListValueBothNull() {

        Portfolio left = getObject(Portfolio.class);

        Entry<String, String> entry = left.getPositions().get(4).getBookToProducts().entrySet().iterator().next();

        String key = entry.getKey();
        left.getPositions().get(4).getBookToProducts().put(key, null);

        Portfolio right = clone(left);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, empty());
    }

    @Test
    public void testSetInUserDefinedObjectInList() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);

        Set<String> bookSet = left.getPositions().get(4).getBookSet();
        String newBook = addUniqueValue(bookSet, "FAKE_BOOK", "_1");

        List<String> sortedBookSet = new ArrayList<>(bookSet);
        Collections.sort(sortedBookSet);
        int position = sortedBookSet.indexOf(newBook);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);

        assertThat(diffs, is(Arrays.asList(new Diff("positions.4.bookSet." + position, String.class, newBook, "MISSING"))));
    }

    private String addUniqueValue(Set<String> set, String value, String increment) {

        int size = set.size();
        String localValue = value;
        while (true) {
            set.add(localValue);
            if (set.size() > size)
                return localValue;
            localValue += increment;
        }
    }

    @Test
    public void testOrderedListComparison() {

        Position left = getObject(Position.class);

        Position right = clone(left);

        List<String> traders = left.getTraders();
        Collections.shuffle(traders);

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, not(empty()));

        beanCompare.addComparator(String.class, (l, r) -> l.compareTo(r));

        diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, empty());
    }

    @Test
    public void testOrderedStringArrayComparison() {

        Position left = getObject(Position.class);

        Position right = clone(left);

        String[] subBooks = left.getSubBooks();
        String tmp = subBooks[0];
        subBooks[0] = subBooks[1];
        subBooks[1] = tmp;

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, not(empty()));

        beanCompare.addComparator(String.class, Comparator.naturalOrder());

        diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, empty());
    }

    @Test
    public void testOrderedArrayIntComparison() {

        Position left = getObject(Position.class);

        Position right = clone(left);

        int[] stats = left.getStats();
        int tmp = stats[0];
        stats[0] = stats[1];
        stats[1] = tmp;

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, not(empty()));

        beanCompare.addComparator(int.class, Comparator.naturalOrder());

        diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, empty());
    }

    @Test
    public void testOrderedTopLevelPrimitiveIntegerArrayComparison() {

        int[] left = getObject(int[].class);

        int[] right = clone(left);

        int tmp = left[0];
        left[0] = left[1];
        left[1] = tmp;

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, not(empty()));

        beanCompare.addComparator(int.class, Comparator.naturalOrder());

        diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, empty());
    }

    @Test
    public void testOrderedTopLevelIntegerArrayComparison() {

        Integer[] left = getObject(Integer[].class);

        Integer[] right = clone(left);

        Integer tmp = left[0];
        left[0] = left[1];
        left[1] = tmp;

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, not(empty()));

        beanCompare.addComparator(Integer.class, Comparator.naturalOrder());

        diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, empty());
    }

    @Test
    public void testOrderedArrayLongComparison() {

        Portfolio left = getObject(Portfolio.class);

        Portfolio right = clone(left);

        long[] stats = left.getStats();
        long tmp = stats[0];
        stats[0] = stats[1];
        stats[1] = tmp;

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, not(empty()));

        beanCompare.addComparator(long.class, Comparator.naturalOrder());

        diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, empty());
    }

    @Test
    public void testOrderedTopLevelPrimitiveLongArrayComparison() {

        long[] left = getObject(long[].class);

        long[] right = clone(left);

        long tmp = left[0];
        left[0] = left[1];
        left[1] = tmp;

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, not(empty()));

        beanCompare.addComparator(long.class, Comparator.naturalOrder());

        diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, empty());
    }

    @Test
    public void testOrderedTopLevelLongArrayComparison() {

        Long[] left = getObject(Long[].class);

        Long[] right = clone(left);

        Long tmp = left[0];
        left[0] = left[1];
        left[1] = tmp;

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, not(empty()));

        beanCompare.addComparator(Long.class, Comparator.naturalOrder());

        diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, empty());
    }

    @Test
    public void testOrderedTopLevelPrimitiveShortArrayComparison() {

        short[] left = getObject(short[].class);

        short[] right = clone(left);

        short tmp = left[0];
        left[0] = left[1];
        left[1] = tmp;

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, not(empty()));

        beanCompare.addComparator(short.class, Comparator.naturalOrder());

        diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, empty());
    }

    @Test
    public void testOrderedTopLevelShortArrayComparison() {

        Short[] left = getObject(Short[].class);

        Short[] right = clone(left);

        Short tmp = left[0];
        left[0] = left[1];
        left[1] = tmp;

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, not(empty()));

        beanCompare.addComparator(Short.class, Comparator.naturalOrder());

        diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, empty());
    }

    @Test
    public void testOrderedTopLevelPrimitiveFloatArrayComparison() {

        float[] left = getObject(float[].class);

        float[] right = clone(left);

        float tmp = left[0];
        left[0] = left[1];
        left[1] = tmp;

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, not(empty()));

        beanCompare.addComparator(float.class, Comparator.naturalOrder());

        diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, empty());
    }

    @Test
    public void testOrderedTopLevelFloatArrayComparison() {

        Float[] left = getObject(Float[].class);

        Float[] right = clone(left);

        Float tmp = left[0];
        left[0] = left[1];
        left[1] = tmp;

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, not(empty()));

        beanCompare.addComparator(Float.class, Comparator.naturalOrder());

        diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, empty());
    }

    @Test
    public void testOrderedTopLevelPrimitiveDoubleArrayComparison() {

        double[] left = getObject(double[].class);

        double[] right = clone(left);

        double tmp = left[0];
        left[0] = left[1];
        left[1] = tmp;

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, not(empty()));

        beanCompare.addComparator(double.class, (l,r) -> l.compareTo(r));

        diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, empty());
    }

    @Test
    public void testOrderedTopLevelDoubleArrayComparison() {

        Double[] left = getObject(Double[].class);

        Double[] right = clone(left);

        Double tmp = left[0];
        left[0] = left[1];
        left[1] = tmp;

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, not(empty()));

        beanCompare.addComparator(Double.class, Comparator.naturalOrder());

        diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, empty());
    }

    @Test
    public void testOrderedTopLevelPrimitiveBooleanArrayComparison() {

        boolean[] left = getObject(boolean[].class);
        left[0] = !left[0];

        boolean[] right = clone(left);

        boolean tmp = left[0];
        left[0] = left[1];
        left[1] = tmp;

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, not(empty()));

        beanCompare.addComparator(boolean.class, Comparator.naturalOrder());

        diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, empty());
    }

    @Test
    public void testOrderedTopLevelBooleanArrayComparison() {

        Boolean[] left = getObject(Boolean[].class);
        left[0] = !left[0];

        Boolean[] right = clone(left);

        Boolean tmp = left[0];
        left[0] = left[1];
        left[1] = tmp;

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, not(empty()));

        beanCompare.addComparator(Boolean.class, Comparator.naturalOrder());

        diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, empty());
    }

    @Test
    public void testOrderedTopLevelPrimitiveByteArrayComparison() {

        byte[] left = getObject(byte[].class);

        byte[] right = clone(left);

        byte tmp = left[0];
        left[0] = left[1];
        left[1] = tmp;

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, not(empty()));

        beanCompare.addComparator(byte.class, Comparator.naturalOrder());

        diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, empty());
    }

    @Test
    public void testOrderedTopLevelByteArrayComparison() {

        Byte[] left = getObject(Byte[].class);

        Byte[] right = clone(left);

        Byte tmp = left[0];
        left[0] = left[1];
        left[1] = tmp;

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, not(empty()));

        beanCompare.addComparator(Byte.class, Comparator.naturalOrder());

        diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, empty());
    }

    @Test
    public void testOrderedTopLevelPrimitiveCharArrayComparison() {

        Character[] tmpArray = getObject(Character[].class);

        char[] left = new char[tmpArray.length];
        for(int i = 0; i<tmpArray.length; i++) {
            left[i] = tmpArray[i];
        }

        char[] right = clone(left);

        char tmp = left[0];
        left[0] = left[1];
        left[1] = tmp;

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, not(empty()));

        beanCompare.addComparator(char.class, Comparator.naturalOrder());

        diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, empty());
    }

    @Test
    public void testOrderedTopLevelCharacterArrayComparison() {

        Character[] left = getObject(Character[].class);

        Character[] right = clone(left);

        Character tmp = left[0];
        left[0] = left[1];
        left[1] = tmp;

        List<Diff> diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, not(empty()));

        beanCompare.addComparator(Character.class, Comparator.naturalOrder());

        diffs = beanCompare.diffs(left, right, f -> true);
        assertThat(diffs, empty());
    }

}
