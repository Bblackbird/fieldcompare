# fieldcompare
Object comparison utility.

It is primarily meant as an aid in unit testing to pinpoint differences in data and objects used in non-trivial real-word applications.
The goal was to address 90% of the most common scenarios, and make it possible for easy customization as needed.

This snippet shows how it can be used to validate full data correctness in unit testing:

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

Please see BeanCompareTest class for more examples.

Diff object contains:
1. Full name to test value in following formats:
  * `<property instance name>[.<sub property instance name>...]` in case of simple properties
  * `<property instance name>.<key name>` in case of maps
  * `<property instance name>.<index>.<property name>` in case of collections, lists and arrays
2. Data type
3. "Left" value which is projected onto "Right" value for any differences
4. "Right" value

Please note that "Left" object is compared to "Right" object members. So in case one cares about properties in "Right" value not present in "Left", one should either switch their place and run the same diff() method again, or use fullDiff() methods.

This utility goes really hand in hand with some test data generator libraries, like the one used in unit test here:
https://mtedone.github.io/podam/
This is article that also describe usage of it:
https://medium.com/geekculture/java-unit-tests-make-easy-random-values-with-podam-2b1de8a56958

Some other popular alternatives:

https://www.baeldung.com/java-easy-random

https://github.com/j-easy/easy-random

https://github.com/DiUS/java-faker (https://www.baeldung.com/java-faker)

https://github.com/Codearte/jfairy

Some good object comparison libraries alternatives:

Jav Object Diff

https://java-object-diff.readthedocs.io/en/latest/

https://github.com/SQiShER/java-object-diff

https://www.woolha.com/tutorials/java-get-differences-between-two-objects

JaVers

https://javers.org/
