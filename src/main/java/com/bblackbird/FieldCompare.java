package com.bblackbird;

import com.google.common.base.Strings;
import com.google.common.primitives.*;

import java.io.Serializable;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FieldCompare {

    // Reflection code
    //
    private static final Field[] NO_FIELDS = {};
    /**
     * Cache for {blink classigetDeclaredfields()), allowing for fast iteration.
     */
    private static final Map<Class<?>, Field[]> declaredFieldsCache = new ConcurrentHashMap<>(256);

    public static Predicate<Field> isTransient = f -> Modifier.isTransient(f.getModifiers());
    public static Predicate<Field> isStatic = f -> Modifier.isStatic(f.getModifiers());
    public static Predicate<Field> isFinal = f -> Modifier.isFinal(f.getModifiers());
    public static Predicate<Field> isTransientOrStatic = isTransient.or(isStatic);
    public static Predicate<Field> isTransientOrStaticOrFinal = isTransient.or(isStatic).or(isFinal);

    // Extension Functions
    //
    public static ContextFilter defaultFieldFilter = fullName -> l -> r -> isTransientOrStaticOrFinal;
    public static ContextFilter allFieldContextFilter = fullName -> l -> r -> f -> true;

    public interface ContextFilter extends Function<String, Function<Object, Function<Object, Predicate<Field>>>> {
    }

    public interface CheckDiffNulls extends Function<Deque<Field>, Function<Deque<String>, Function<String, Function<Object, Function<Object, List<Diff>>>>>> {
    }

    public interface CompareFields<T> extends Function<Deque<Field>, Function<Deque<String>, Function<Predicate<Field>, Function<CheckDiffNulls, Function<ContextFilter, Function<T, Function<T, Function<Field, List<Diff>>>>>>>>> {
    }

    //region Main API
    /**
     * This is the entry point for all comparisons.
     * It return list of all differences {@link com.bblackbird.Diff}.
     * Diffs methods go through "left" argument and compare with content of "right" object.
     * Full diffs methods compare them both ways and combine all differences.
     */

    /**
     * Simplest method that defaults to catch-all context filter and filters out transient and static fields.
     */
    public <T> List<Diff> diffs(T left, T right) {
        return diffs(left, right, checkDiffNulls(), allFieldContextFilter, isTransientOrStatic.negate());
    }

    /**
     * Overload that defaults to catch-all context filter and accepts custom field filter.
     */
    public <T> List<Diff> diffs(T left, T right, Predicate<Field> fieldFilter) {
        return diffs(left, right, checkDiffNulls(), allFieldContextFilter, fieldFilter);
    }

    /**
     * Overload that defaults to catch-all context filter and accepts custom field filter, but still filters out transient and static fields.
     */
    public <T> List<Diff> diffsNoTransientOrStatic(T left, T right, Predicate<Field> filterFields) {
        return diffs(left, right, checkDiffNulls(), allFieldContextFilter, filterFields.and(isTransientOrStaticOrFinal.negate()));
    }

    /**
     * Overload that accepts custom context filter and filters out transient and static fields.
     */
    public <T> List<Diff> diffsWithContextFilter(T left, T right, ContextFilter contextFilter) {
        return diffs(new ArrayDeque<>(), new ArrayDeque<>(), left, right, checkDiffNulls(), contextFilter, isTransientOrStatic.negate());
    }

    /**
     * Overload that accepts both custom context and field filters.
     */
    public <T> List<Diff> diffs(T left, T right, ContextFilter contextFilter, Predicate<Field> fieldFilter) {
        return diffs(new ArrayDeque<>(), new ArrayDeque<>(), left, right, checkDiffNulls(), contextFilter, fieldFilter);
    }

    /**
     * Overload that accepts both custom context and field filters and also function to compare for nulls.
     */
    public <T> List<Diff> diffs(T left, T right, CheckDiffNulls checkNulls, ContextFilter contextFilter, Predicate<Field> fieldFilter) {
        return diffs(new ArrayDeque<>(), new ArrayDeque<>(), left, right, checkNulls, contextFilter, fieldFilter);
    }

    /**
     * Full-diffs series of methods just combine differences both ways.
     */
    public <T> List<Diff> fullDiffs(T left, T right) {
        List<Diff> leftDiffs = diffs(left, right);
        List<Diff> rightDiffs = diffs(right, left);
        leftDiffs.addAll(rightDiffs);
        return leftDiffs;
    }

    public <T> List<Diff> fullDiffs(T left, T right, Predicate<Field> fieldFilter) {
        List<Diff> diffs = new ArrayList<Diff>();
        diffs(new ArrayDeque<>(), new ArrayDeque<>(), left, right, checkDiffNulls(), allFieldContextFilter, fieldFilter, diffs);
        diffs(new ArrayDeque<>(), new ArrayDeque<>(), right, left, checkDiffNulls(), allFieldContextFilter, fieldFilter, diffs);
        return diffs;
    }

    public <T> List<Diff> fullDiffsWithContextFilter(T left, T right, ContextFilter contextFilter) {
        List<Diff> diffs = new ArrayList<Diff>();
        diffs(new ArrayDeque<>(), new ArrayDeque<>(), left, right, checkDiffNulls(), contextFilter, isTransientOrStatic.negate(), diffs);
        diffs(new ArrayDeque<>(), new ArrayDeque<>(), right, left, checkDiffNulls(), contextFilter, isTransientOrStatic.negate(), diffs);
        return diffs;
    }

    public <T> List<Diff> fullDiffs(T left, T right, ContextFilter contextFilter, Predicate<Field> fieldFilter) {
        List<Diff> diffs = new ArrayList<Diff>();
        diffs(new ArrayDeque<>(), new ArrayDeque<>(), left, right, checkDiffNulls(), contextFilter, fieldFilter, diffs);
        diffs(new ArrayDeque<>(), new ArrayDeque<>(), right, left, checkDiffNulls(), contextFilter, fieldFilter, diffs);
        return diffs;
    }

    public <T> List<Diff> fullDiffs(T left, T right, CheckDiffNulls checkNulls, ContextFilter contextFilter, Predicate<Field> fieldFilter) {
        List<Diff> diffs = new ArrayList<Diff>();
        diffs(new ArrayDeque<>(), new ArrayDeque<>(), left, right, checkNulls, contextFilter, fieldFilter, diffs);
        diffs(new ArrayDeque<>(), new ArrayDeque<>(), right, left, checkNulls, contextFilter, fieldFilter, diffs);
        return diffs;
    }

    /**
     * Compare tvo objects of same type field by field, left to right i.e. data in right object not present in left is not accounted for.
     * This method is recursively called as object fields are traversed.
     *
     * Parent Fields stores stack of all parent field names
     * Prefix has stack of value name(s)
     * CheckDiffNulls - function used for comparing null values if any
     * Context Field Filter - allows field filtering based on context that consists of full name, values and field details
     * Field filter - based on field details
     * Object comparison supported for following types:
     * Collections - Lists, Maps, Sets and Collections only for now
     * Arrays
     * Complex objects - user defined
     * Simple objects - jdk ones
     */
    public <T> List<Diff> diffs(Deque<Field> parentFields, Deque<String> prefix, T left, T right, CheckDiffNulls checkNulls,
                                ContextFilter contextFilter, Predicate<Field> fieldFilter) {

        List<Diff> diffs = new ArrayList<Diff>();
        return diffs(parentFields, prefix, left, right, checkNulls, contextFilter, fieldFilter, diffs);

    }

    public <T> List<Diff> diffs(Deque<Field> parentFields, Deque<String> prefix, T left, T right, CheckDiffNulls checkNulls,
                                ContextFilter contextFilter, Predicate<Field> fieldFilter, List<Diff> diffs) {

        List<Diff> checkNullDiffs = checkNulls.apply(parentFields).apply(prefix).apply(getClassName(left, right)).apply(left).apply(right);
        if (!checkNullDiffs.isEmpty()) {
            diffs.addAll(checkNullDiffs);
            return diffs;
        }

        if (!checkClassNames(parentFields, prefix, getClassName(left, right), left, right, diffs).isEmpty())
            return diffs;

        if (compareObjects(left, right))
            return diffs;

        if (!parentFields.isEmpty() && !contextFilter.apply(getFullName(parentFields, prefix, left.getClass().getName())).apply(left).apply(right)
                .test(parentFields.getLast())) {
            return diffs;
        }

        if (left instanceof List<?>)
            return compare(parentFields, prefix, null, fieldFilter, checkNulls, contextFilter, (List<?>) left, (List<?>) right, diffs);
        else if (left instanceof Map<?, ?>)
            return compare(parentFields, prefix, null, fieldFilter, checkNulls, contextFilter, (Map<?, ?>) left, (Map<?, ?>) right, diffs);
        else if (left instanceof Set<?>)
            return compare(parentFields, prefix, null, fieldFilter, checkNulls, contextFilter, (Set<?>) left, (Set<?>) right, diffs);
        else if (left instanceof Collection<?>)
            return compare(parentFields, prefix, null, fieldFilter, checkNulls, contextFilter, (Collection<?>) left, (Collection<?>) right, diffs);
        else if (left.getClass().isArray()) {
            return compareAnyArray(parentFields, prefix, null, fieldFilter, checkNulls, contextFilter, left, right, diffs);
        }

        return getAllDeclaredFields(left.getClass(), fieldFilter).stream()
                .filter(f -> contextFilter.apply(getFullName(parentFields, prefix, f.getName())).apply(getFieldValueWithType(f, left))
                        .apply(getFieldValueWithType(f, right)).test(f))
                .map(f -> compareFields().apply(parentFields).apply(prefix).apply(fieldFilter).apply(checkNulls).apply(contextFilter)
                        .apply(getFieldValueWithType(f, left)).apply(getFieldValueWithType(f, right)).apply(f))
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    protected <T> boolean compareObjects(T left, T right) {
        return left == right || left.equals(right);
    }

    protected <T> CompareFields<T> compareFields() {
        return pf -> prefix -> fieldFilter -> checkNulls -> contextFilter -> left -> right -> f -> {
            List<Diff> nullDiff = checkNulls.apply(pf).apply(prefix).apply(f.getName()).apply(left).apply(right);
            if (!nullDiff.isEmpty()) {
                return nullDiff;
            }

            if (left == null || left == right || left.equals(right)) {
                return Collections.emptyList();
            }
            List<Diff> diffs = new ArrayList<Diff>();
            if (isString(left.getClass())) {
                return compare(pf, prefix, f, fieldFilter, checkNulls, contextFilter, (String) left, (String) right, diffs);
            }

            if (isPrimitiveType(left.getClass())) {
                return compareAnyPrimitiveType(pf, prefix, f, fieldFilter, checkNulls, contextFilter, left, right, diffs);
            }

            if (left instanceof Enum<?>) {
                return compare(pf, prefix, f, fieldFilter, checkNulls, contextFilter, (Enum<?>) left, (Enum<?>) right, diffs);
            }

            if (f.getType().isAssignableFrom(BigDecimal.class))
                return compare(pf, prefix, f, fieldFilter, checkNulls, contextFilter, (BigDecimal) left, (BigDecimal) right, diffs);

            if (f.getType().isAssignableFrom(List.class))
                return compare(pf, prefix, f, fieldFilter, checkNulls, contextFilter, (List<?>) left, (List<?>) right, diffs);
            else if (f.getType().isAssignableFrom(Map.class))
                return compare(pf, prefix, f, fieldFilter, checkNulls, contextFilter, (Map<?, ?>) left, (Map<?, ?>) right, diffs);
            else if (f.getType().isAssignableFrom(Set.class))
                return compare(pf, prefix, f, fieldFilter, checkNulls, contextFilter, (Set<?>) left, (Set<?>) right, diffs);
            else if (f.getType().isAssignableFrom(Collection.class))
                return compare(pf, prefix, f, fieldFilter, checkNulls, contextFilter, (Collection<?>) left, (Collection<?>) right, diffs);
            else if (f.getType().isArray()) {
                return compareAnyArray(pf, prefix, f, fieldFilter, checkNulls, contextFilter, left, right, diffs);
            }

            return compare(pf, prefix, f, fieldFilter, checkNulls, contextFilter, left, right, diffs);
        };

    }
    //endregion

    //region Precondition Checkers
    /**
     * Precondition checkers
     */
    protected CheckDiffNulls checkDiffNulls() {
        return pf -> prefix -> name -> l -> r -> checkDiffNulls(pf, prefix, name, l, r);
    }

    protected List<Diff> createDiff(Deque<Field> pf, Deque<String> prefix, String fieldName, Class<?> type, Object left, Object right) {
        return Collections.singletonList(new Diff(getFullName(pf, prefix, fieldName), type, left, right));
    }

    /**
     *
     * This null check is used in case of "complex" types in order to make less verbose output.
     */
    protected List<Diff> checkComplexTypeNulls(Deque<Field> pf, Deque<String> prefix, String fieldName, Object left, Object right) {

        if (left == null && right == null) {
            return createDiff(pf, prefix, fieldName, null, "NULL", "NULL");
        } else if (left != null && right == null) {
            return createDiff(pf, prefix, fieldName, left.getClass(), "NON-NULL", "NULL");
        } else if (left == null && right != null) {
            return createDiff(pf, prefix, fieldName, right.getClass(), "NULL", "NON-NULL");
        }
        return Collections.emptyList();
    }

    /**
     * Method responsible how to deal with null comparisons for both simple and non-simple types.
     */
    protected <T> List<Diff> checkDiffNulls(Deque<Field> pf, Deque<String> prefix, String fieldName, T left, T right) {

        if(!isSimpleType(getClassType(left, right)))
            return checkComplexTypeNulls(pf, prefix, fieldName, left, right);

        if (left == null && right == null) {
            //diffs.add(new Diff (getFullName (pf, field), "NULL", "NULL"));
            return Collections.emptyList();
        } else if (left != null && right == null) {
            return createDiff(pf, prefix, fieldName, left.getClass(), left, "NULL");
        } else if (left == null && right != null) {
            return createDiff(pf, prefix, fieldName, right.getClass(), "NULL", right);
        }
        return Collections.emptyList();
    }

    /**
     *  Check if object types compared are the same.
     */
    protected List<Diff> checkClassNames(Deque<Field> pf, Deque<String> prefix, String fieldName, Object left, Object right, List<Diff> diffs) {
        if (!left.getClass().getName().equals(right.getClass().getName())) {
            diffs.add(new Diff(getFullName(pf, prefix, fieldName), left.getClass(), left.getClass().getName(), right.getClass().getName()));
        }
        return diffs;
    }

    /**
     * Optional logging methods
     */
    protected void log(Deque<Field> pf, Deque<String> prefix, Field f, Object left, Object right) {
    }

    protected void log(Deque<Field> pf, Deque<String> prefix, Field f, Object left, Object right, Exception e) {
    }
    //endregion

    //region Comparison Methods
    /**
     * Field value comparison methods
     */

    /**
     * Collections comparison methods
     */
    protected List<Diff> compare(Deque<Field> pf, Deque<String> prefix, Field f, Predicate<Field> fieldFilter, CheckDiffNulls checkNulls,
                                 ContextFilter contextFilter, Map<?, ?> left, Map<?, ?> right, List<Diff> diffs) {
        List<Diff> checkNullDiffs = checkNulls.apply(pf).apply(prefix).apply(getClassName(left, right)).apply(left).apply(right);
        if (!checkNullDiffs.isEmpty()) {
            diffs.addAll(checkNullDiffs);
            return diffs;
        }
        if (left == null || left == right || left.equals(right)) {
            return diffs;
        }
        pf.addLast(f);
        left.forEach((k, v) -> {
            prefix.addLast(k.toString());
            if(right.containsKey(k)) {
                Object rightValue = right.get(k);
                compare(pf, prefix, null, fieldFilter, checkNulls, contextFilter, v, rightValue, diffs);
            } else {
                diffs.add(new Diff(getFullName(pf, prefix), v.getClass(), v, "MISSING"));
            }
            prefix.removeLast();
        });
        pf.removeLast();
        return diffs;
    }

    public String item;

    private Field getItemField() {
        try {
            return FieldCompare.class.getField("item");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Field getValidField(Field f) {

        if (f != null)
            return f;

        return getItemField();
    }

    protected List<Diff> compare(Deque<Field> pf, Deque<String> prefix, Field f, Predicate<Field> fieldFilter, CheckDiffNulls checkNulls,
                                 ContextFilter contextFilter, List<?> left, List<?> right, List<Diff> diffs) {

        Set<Object>  rightSet = right.stream().collect(Collectors.toSet());

        left = sortListIfRequired(left, f, l -> new ArrayList(l), () -> null);
        right = sortListIfRequired(right, f, l -> new ArrayList(l), () -> null);

        Field validField = getValidField(f);
        if (validField != null)
            pf.addLast(validField);

        for (int i = 0; i < left.size(); i++) {
            Object vLeft = left.get(i);

            if(rightSet.contains(vLeft)) {
                continue;
            }

            prefix.addLast(String.valueOf(i));
            if (i < right.size()) {
                Object vRight = right.get(i);
                compare(pf, prefix, null, fieldFilter, checkNulls, contextFilter, vLeft, vRight, diffs);
            } else {
                diffs.add(new Diff(getFullName(pf, prefix), vLeft.getClass(), vLeft, "NULL"));
            }
            prefix.removeLast();
        }
        if (validField != null)
            pf.removeLast();
        return diffs;
    }

    protected List<Diff> compare(Deque<Field> pf, Deque<String> prefix, Field f, Predicate<Field> fieldFilter, CheckDiffNulls checkNulls,
                                 ContextFilter contextFilter, Set<?> left, Set<?> right, List<Diff> diffs) {

        Field validField = getValidField(f);
        if (validField != null)
            pf.addLast(validField);

        List<Object> localDiffs = new ArrayList<>();
        for (Object elem : left) {
            if (!right.contains(elem)) {
                localDiffs.add(elem);
            }
        }

        List<?> sortedSet = sortSet(left, f, s -> new ArrayList(s), () -> null);

        for (Object elem : localDiffs) {
            int counter = sortedSet.indexOf(elem);
            prefix.addLast(String.valueOf(counter));
            diffs.add(new Diff(getFullName(pf, prefix), elem.getClass(), elem, "MISSING"));
            prefix.removeLast();
        }
        if (validField != null)
            pf.removeLast();
        return diffs;
    }

    protected List<Diff> compare(Deque<Field> pf, Deque<String> prefix, Field f, Predicate<Field> fieldFilter, CheckDiffNulls checkNulls,
                                 ContextFilter contextfilter, Collection<?> left, Collection<?> right, List<Diff> diffs) {

        Set<Object>  rightSet = right.stream().collect(Collectors.toSet());

        left = sortCollectionIfRequired(left, f, l -> new ArrayList(l), () -> null);
        right = sortCollectionIfRequired(right, f, l -> new ArrayList(l), () -> null);

        Field validField = getValidField(f);
        if (validField != null)
            pf.addLast(validField);

        Iterator<?> leftIter = left.iterator();
        Iterator<?> rightIter = right.iterator();
        for (int i = 0; i < left.size(); i++) {
            Object vLeft = leftIter.next();

            if(rightSet.contains(vLeft)) {
                continue;
            }

            prefix.addLast(String.valueOf(i));
            if (i < right.size()) {
                Object vRight = rightIter.next();
                compare(pf, prefix, null, fieldFilter, checkNulls, contextfilter, vLeft, vRight, diffs);
            } else {
                diffs.add(new Diff(getFullName(pf, prefix, (Field) null), vLeft.getClass(), vLeft, "NULL"));
            }
            prefix.removeLast();
        }
        if (validField != null)
            pf.removeLast();
        return diffs;
    }

    /**
     * Object comparison method
     */

    public int compare(Object left, Object right) {

        if (left == null || left == right || left.equals(right)) {
            return 0;
        }

        if (left instanceof Comparable<?>) {
            return ((Comparable) left).compareTo(right);
        }

        return 1;
    }

    protected List<Diff> compare(Deque<Field> pf, Deque<String> prefix, Field f, Predicate<Field> fieldFilter, CheckDiffNulls checkNulls,
                                 ContextFilter contextFilter, Object left, Object right, List<Diff> diffs) {
        List<Diff> checkNullDiffs = checkNulls.apply(pf).apply(prefix).apply("").apply(left).apply(right);
        if (!checkNullDiffs.isEmpty()) {
            diffs.addAll(checkNullDiffs);
            return diffs;
        }
        if (compare(left, right) == 0) {
            return diffs;
        }

        if (isSimpleType(left.getClass())) {
            diffs.add(new Diff(getFullName(pf, prefix, f), f != null ? f.getType() : left.getClass(), left, right));
        } else {
            if (f != null) {
                pf.addLast(f);
                prefix.addLast("");
            }
            List<Diff> collDiffs = diffs(pf, prefix, left, right, checkNulls, contextFilter, fieldFilter);
            diffs.addAll(collDiffs);
            if (f != null) {
                pf.removeLast();
                prefix.removeLast();
            }
            if (!collDiffs.isEmpty())
                log(pf, prefix, f, left, right);
        }
        return diffs;
    }

    /**
     * Support for String type
     */

    public int compare(String left, String right) {
        return left.compareTo(right);
    }

    public List<Diff> compare(Deque<Field> pf, Deque<String> prefix, Field f, Predicate<Field> fieldFilter, CheckDiffNulls checknulls,
                              ContextFilter contextFilter, String left, String right, List<Diff> diffs) {
        if (compare(left, right) != 0) {
            diffs.add(new Diff(getFullName(pf, prefix, f), f != null ? f.getType() : left.getClass(), left, right));
        }
        return diffs;
    }

    /**
     * Support for Big Decimal type
     */

    public int compare(BigDecimal left, BigDecimal right) {
        BigDecimal leftClean = left.stripTrailingZeros();
        BigDecimal rightClean = right.stripTrailingZeros();
        return leftClean.compareTo(rightClean);
    }

    protected List<Diff> compare(Deque<Field> pf, Deque<String> prefix, Field f, Predicate<Field> fieldFilter, CheckDiffNulls checkNulls,
                                 ContextFilter contextFilter, BigDecimal left, BigDecimal right, List<Diff> diffs) {
        try {
            if (compare(left, right) != 0) {
                diffs.add(new Diff(getFullName(pf, prefix, f), f.getType(), left, right));
                log(pf, prefix, f, left, right);
            }
        } catch (Exception e) {
            log(pf, prefix, f, left, right, e);
        }
        return diffs;
    }

    /**
     * Support for Enum types
     */

    public int compare(Enum<?> left, Enum<?> right) {
        return left.ordinal() - right.ordinal();
    }

    protected List<Diff> compare(Deque<Field> pf, Deque<String> prefix, Field f, Predicate<Field> fieldFilter, CheckDiffNulls checkNulls,
                                 ContextFilter contextFilter, Enum<?> left, Enum<?> right, List<Diff> diffs) {
        if (compare(left, right) != 0) {
            diffs.add(new Diff(getFullName(pf, prefix, f), f != null ? f.getType() : left.getClass(), left, right));
        }
        return diffs;
    }

    /**
     * Support for primitive types
     */

    public int compare(int left, int right) {
        return Integer.compare(left, right);
    }

    protected List<Diff> compare(Deque<Field> pf, Deque<String> prefix, Field f, Predicate<Field> fieldFilter, CheckDiffNulls checkNulls,
                                 ContextFilter contextFilter, int left, int right, List<Diff> diffs) {
        if (compare(left, right) != 0) {
            diffs.add(new Diff(getFullName(pf, prefix, f), int.class, left, right));
        }
        return diffs;
    }

    public int compare(long left, long right) {
        return Long.compare(left, right);
    }

    protected List<Diff> compare(Deque<Field> pf, Deque<String> prefix, Field f, Predicate<Field> fieldFilter, CheckDiffNulls checkNulls,
                                 ContextFilter contextFilter, long left, long right, List<Diff> diffs) {
        if (compare(left, right) != 0) {
            diffs.add(new Diff(getFullName(pf, prefix, f), long.class, left, right));
        }
        return diffs;
    }

    public int compare(short left, short right) {
        return Short.compare(left, right);
    }

    protected List<Diff> compare(Deque<Field> pf, Deque<String> prefix, Field f, Predicate<Field> fieldFilter, CheckDiffNulls checkNulls,
                                 ContextFilter contextFilter, short left, short right, List<Diff> diffs) {

        if (compare(left, right) != 0) {
            diffs.add(new Diff(getFullName(pf, prefix, f), short.class, left, right));
        }
        return diffs;
    }

    public int compare(float left, float right) {
        return Double.compare(left, right);
    }

    protected List<Diff> compare(Deque<Field> pf, Deque<String> prefix, Field f, Predicate<Field> fieldFilter, CheckDiffNulls checkNulls,
                                 ContextFilter contextFilter, float left, float right, List<Diff> diffs) {
        if (compare(left, right) != 0) {
            diffs.add(new Diff(getFullName(pf, prefix, f), float.class, left, right));
        }
        return diffs;
    }

    public int compare(double left, double right) {
        return Double.compare(left, right);
    }

    protected List<Diff> compare(Deque<Field> pf, Deque<String> prefix, Field f, Predicate<Field> fieldFilter, CheckDiffNulls checkNulls,
                                 ContextFilter contextFilter, double left, double right, List<Diff> diffs) {
        if (compare(left, right) != 0) {
            diffs.add(new Diff(getFullName(pf, prefix, f), double.class, left, right));
        }
        return diffs;
    }

    public int compare(boolean left, boolean right) {
        return Boolean.compare(left, right);
    }

    protected List<Diff> compare(Deque<Field> pf, Deque<String> prefix, Field f, Predicate<Field> fieldFilter, CheckDiffNulls checkNulls,
                                 ContextFilter contextFilter, boolean left, boolean right, List<Diff> diffs) {

        if (compare(left, right) != 0) {

            diffs.add(new Diff(getFullName(pf, prefix, f), boolean.class, left, right));
        }
        return diffs;
    }

    public int compare(char left, char right) {
        return Character.compare(left, right);
    }

    protected List<Diff> compare(Deque<Field> pf, Deque<String> prefix, Field f, Predicate<Field> fieldFilter, CheckDiffNulls checkNulls,
                                 ContextFilter contextFilter, char left, char right, List<Diff> diffs) {

        if (compare(left, right) != 0) {
            diffs.add(new Diff(getFullName(pf, prefix, f), char.class, left, right));
        }
        return diffs;
    }

    public int compare(byte left, byte right) {
        return Byte.compare(left, right);
    }

    protected List<Diff> compare(Deque<Field> pf, Deque<String> prefix, Field f, Predicate<Field> ieldFilter, CheckDiffNulls checkNulls, ContextFilter contextfilter, byte left, byte right, List<Diff> diffs) {

        if (compare(left, right) != 0) {
            diffs.add(new Diff(getFullName(pf, prefix, f), byte.class, left, right));
        }
        return diffs;
    }

    /**
     * Primitive wrappers
     */

    public int compare(Integer left, Integer right) {
        return Integer.compare(left, right);
    }

    protected List<Diff> compare(Deque<Field> pf, Deque<String> prefix, Field f, Predicate<Field> fieldFilter, CheckDiffNulls checkNulls,
                                 ContextFilter contextFilter, Integer left, Integer right, List<Diff> diffs) {
        if (compare(left, right) != 0) {
            diffs.add(new Diff(getFullName(pf, prefix, f), Integer.class, left, right));
        }
        return diffs;
    }

    public int compare(Long left, Long right) {
        return Long.compare(left, right);
    }

    protected List<Diff> compare(Deque<Field> pf, Deque<String> prefix, Field f, Predicate<Field> fieldFilter, CheckDiffNulls checkNulls,
                                 ContextFilter contextfilter, Long left, Long right, List<Diff> diffs) {

        if (compare(left, right) != 0) {
            diffs.add(new Diff(getFullName(pf, prefix, f), Long.class, left, right));
        }
        return diffs;
    }

    public int compare(Short left, Short right) {
        return Short.compare(left, right);
    }

    protected List<Diff> compare(Deque<Field> pf, Deque<String> prefix, Field f, Predicate<Field> fieldFilter, CheckDiffNulls checkNulls,
                                 ContextFilter contextFilter, Short left, Short right, List<Diff> diffs) {

        if (compare(left, right) != 0) {
            diffs.add(new Diff(getFullName(pf, prefix, f), Short.class, left, right));
        }
        return diffs;
    }

    public int compare(Float left, Float right) {
        return Float.compare(left, right);
    }

    protected List<Diff> compare(Deque<Field> pf, Deque<String> prefix, Field f, Predicate<Field> fieldFilter, CheckDiffNulls checkNulls,
                                 ContextFilter contextFilter, Float left, Float right, List<Diff> diffs) {

        if (compare(left, right) != 0) {
            diffs.add(new Diff(getFullName(pf, prefix, f), Float.class, left, right));
        }
        return diffs;
    }

    public int compare(Double left, Double right) {
        return Double.compare(left, right);
    }

    protected List<Diff> compare(Deque<Field> pf, Deque<String> prefix, Field f, Predicate<Field> fieldFilter, CheckDiffNulls checkNulls,
                                 ContextFilter contextFilter, Double left, Double right, List<Diff> diffs) {

        if (compare(left, right) != 0) {
            diffs.add(new Diff(getFullName(pf, prefix, f), Double.class, left, right));
        }
        return diffs;
    }

    public int compare(Boolean left, Boolean right) {
        return Boolean.compare(left, right);
    }

    protected List<Diff> compare(Deque<Field> pf, Deque<String> prefix, Field f, Predicate<Field> fieldFilter, CheckDiffNulls checkNulls,
                                 ContextFilter contextFilter, Boolean left, Boolean right, List<Diff> diffs) {

        if (compare(left, right) != 0) {

            diffs.add(new Diff(getFullName(pf, prefix, f), Boolean.class, left, right));
        }
        return diffs;
    }

    public int compare(Character left, Character right) {
        return Character.compare(left, right);
    }

    protected List<Diff> compare(Deque<Field> pf, Deque<String> prefix, Field f, Predicate<Field> fieldFilter, CheckDiffNulls checkNulls,
                                 ContextFilter contextFilter, Character left, Character right, List<Diff> diffs) {

        if (compare(left, right) != 0) {
            diffs.add(new Diff(getFullName(pf, prefix, f), Character.class, left, right));
        }
        return diffs;
    }

    public int compare(Byte left, Byte right) {
        return Byte.compare(left, right);
    }

    protected List<Diff> compare(Deque<Field> pf, Deque<String> prefix, Field f, Predicate<Field> ieldFilter, CheckDiffNulls checkNulls, ContextFilter contextfilter, Byte left, Byte right, List<Diff> diffs) {

        if (compare(left, right) != 0) {
            diffs.add(new Diff(getFullName(pf, prefix, f), Byte.class, left, right));
        }
        return diffs;
    }

    /**
     * Primitive array types support - only because of java generics limitations
     */

    protected List<Diff> compare(Deque<Field> pf, Deque<String> prefix, Field f, Predicate<Field> fieldFilter, CheckDiffNulls checkNulls, ContextFilter contextFilter, short[] left, short[] right, List<Diff> diffs) {

        Set<Object>  rightSet = convertToSet(right);

        left = sortArray(left, f, a -> a.clone(), () -> null);
        right = sortArray(right, f, a -> a.clone(), () -> null);

        for (int i = 0; i < left.length; i++) {

            short vLeft = left[i];

            if(rightSet.contains(vLeft)){
                continue;
            }

            prefix.addLast(String.valueOf(i));
            if (i < right.length) {

                short vRight = right[i];

                compare(pf, prefix, f, fieldFilter, checkNulls, contextFilter, vLeft, vRight, diffs);
            } else {
                diffs.add(new Diff(getFullName(pf, prefix, f), short.class, vLeft, "NULL"));
            }
            prefix.removeLast();
        }
        return diffs;
    }

    protected Set<Object> convertToSet( final short[] arr) {
        return IntStream.range(0, arr.length)
                .mapToObj(i -> arr[i])
                .collect(Collectors.toSet());
    }

    protected List<Diff> compare(Deque<Field> pf, Deque<String> prefix, Field f, Predicate<Field> fieldFilter, CheckDiffNulls checkNulls, ContextFilter contextFilter, int[] left, int[] right, List<Diff> diffs) {

        Set<Object>  rightSet = Arrays.stream(right).boxed().collect(Collectors.toSet());

        left = sortArray(left, f, a -> a.clone(), () -> null);
        right = sortArray(right, f, a -> a.clone(), () -> null);

        for (int i = 0; i < left.length; i++) {

            int vLeft = left[i];

            if(rightSet.contains(vLeft)){
                continue;
            }

            prefix.addLast(String.valueOf(i));
            if (i < right.length) {

                int vRight = right[i];

                compare(pf, prefix, f, fieldFilter, checkNulls, contextFilter, vLeft, vRight, diffs);
            } else {

                diffs.add(new Diff(getFullName(pf, prefix, f), int.class, vLeft, "NULL"));
            }
            prefix.removeLast();
        }
        return diffs;
    }

    protected List<Diff> compare(Deque<Field> pf, Deque<String> prefix, Field f, Predicate<Field> fieldFilter, CheckDiffNulls checkNulls, ContextFilter contextFilter, long[] left, long[] right, List<Diff> diffs) {

        Set<Object>  rightSet = Arrays.stream(right).boxed().collect(Collectors.toSet());

        left = sortArray(left, f, a -> a.clone(), () -> null);
        right = sortArray(right, f, a -> a.clone(), () -> null);

        for (int i = 0; i < left.length; i++) {
            long vLeft = left[i];

            if(rightSet.contains(vLeft)){
                continue;
            }

            prefix.addLast(String.valueOf(i));
            if (i < right.length) {
                long vRight = right[i];
                compare(pf, prefix, f, fieldFilter, checkNulls, contextFilter, vLeft, vRight, diffs);
            } else {
                diffs.add(new Diff(getFullName(pf, prefix, f), long.class, vLeft, "NULL"));
            }
            prefix.removeLast();
        }
        return diffs;
    }

    protected List<Diff> compare(Deque<Field> pf, Deque<String> prefix, Field f, Predicate<Field> fieldFilter, CheckDiffNulls checkNulls,
                                 ContextFilter contextFilter, boolean[] left, boolean[] right, List<Diff> diffs) {

        Set<Object>  rightSet = convertToSet(right);

        left = sortArray(left, f, a -> a.clone(), () -> null);
        right = sortArray(right, f, a -> a.clone(), () -> null);

        for (int i = 0; i < left.length; i++) {
            boolean vLeft = left[i];

            if(rightSet.contains(vLeft)){
                continue;
            }

            prefix.addLast(String.valueOf(i));
            if (i < right.length) {
                boolean vRight = right[i];
                compare(pf, prefix, f, fieldFilter, checkNulls, contextFilter, vLeft, vRight, diffs);
            } else {
                diffs.add(new Diff(getFullName(pf, prefix, f), boolean.class, vLeft, "NULL"));
            }
            prefix.removeLast();
        }
        return diffs;
    }

    protected Set<Object> convertToSet( final boolean[] arr) {
        return IntStream.range(0, arr.length)
                .mapToObj(i -> arr[i])
                .collect(Collectors.toSet());
    }

    protected List<Diff> compare(Deque<Field> pf, Deque<String> prefix, Field f, Predicate<Field> fieldFilter, CheckDiffNulls checkNulls,
                                 ContextFilter contextFilter, byte[] left, byte[] right, List<Diff> diffs) {

        Set<Object>  rightSet = convertToSet(right);

        left = sortArray(left, f, a -> a.clone(), () -> null);
        right = sortArray(right, f, a -> a.clone(), () -> null);

        for (int i = 0; i < left.length; i++) {
            byte vLeft = left[i];

            if(rightSet.contains(vLeft)){
                continue;
            }

            prefix.addLast(String.valueOf(i));
            if (i < right.length) {
                byte vRight = right[i];
                compare(pf, prefix, f, fieldFilter, checkNulls, contextFilter, vLeft, vRight, diffs);
            } else {
                diffs.add(new Diff(getFullName(pf, prefix, f), byte.class, vLeft, "NULL"));
            }
            prefix.removeLast();
        }
        return diffs;
    }

    protected Set<Object> convertToSet( final byte[] arr) {
        return IntStream.range(0, arr.length)
                .mapToObj(i -> arr[i])
                .collect(Collectors.toSet());
    }

    protected List<Diff> compare(Deque<Field> pf, Deque<String> prefix, Field f, Predicate<Field> fieldFilter, CheckDiffNulls checkNulls,
                                 ContextFilter contextFilter, char[] left, char[] right, List<Diff> diffs) {

        Set<Object>  rightSet = convertToSet(right);

        left = sortArray(left, f, a -> a.clone(), () -> null);
        right = sortArray(right, f, a -> a.clone(), () -> null);

        for (int i = 0; i < left.length; i++) {
            char vLeft = left[i];

            if(rightSet.contains(vLeft)){
                continue;
            }

            prefix.addLast(String.valueOf(i));
            if (i < right.length) {
                char vRight = right[i];
                compare(pf, prefix, f, fieldFilter, checkNulls, contextFilter, vLeft, vRight, diffs);
            } else {
                diffs.add(new Diff(getFullName(pf, prefix, f), char.class, vLeft, "NULL"));
            }
            prefix.removeLast();
        }
        return diffs;
    }

    protected Set<Object> convertToSet( final char[] arr) {
        return IntStream.range(0, arr.length)
                .mapToObj(i -> arr[i])
                .collect(Collectors.toSet());
    }

    protected List<Diff> compare(Deque<Field> pf, Deque<String> prefix, Field f, Predicate<Field> fieldFilter, CheckDiffNulls checkNulls,
                                 ContextFilter contextfilter, double[] left, double[] right, List<Diff> diffs) {

        Set<Object>  rightSet = Arrays.stream(right).boxed().collect(Collectors.toSet());

        left = sortArray(left, f, a -> a.clone(), () -> null);
        right = sortArray(right, f, a -> a.clone(), () -> null);

        for (int i = 0; i < left.length; i++) {
            double vLeft = left[i];

            if(rightSet.contains(vLeft)){
                continue;
            }

            prefix.addLast(String.valueOf(i));
            if (i < right.length) {
                double vRight = right[i];
                compare(pf, prefix, f, fieldFilter, checkNulls, contextfilter, vLeft, vRight, diffs);
            } else {
                diffs.add(new Diff(getFullName(pf, prefix, f), double.class, vLeft, "NULL"));
            }
            prefix.removeLast();
        }
        return diffs;
    }

    protected List<Diff> compare(Deque<Field> pf, Deque<String> prefix, Field f, Predicate<Field> fieldFilter, CheckDiffNulls checkNulls,
                                 ContextFilter contextFilter, float[] left, float[] right, List<Diff> diffs) {

        Set<Object>  rightSet = convertToSet(right);

        left = sortArray(left, f, a -> a.clone(), () -> null);
        right = sortArray(right, f, a -> a.clone(), () -> null);

        for (int i = 0; i < left.length; i++) {
            float vLeft = left[i];

            if(rightSet.contains(vLeft)){
                continue;
            }

            prefix.addLast(String.valueOf(i));
            if (i < right.length) {
                float vRight = right[i];
                compare(pf, prefix, f, fieldFilter, checkNulls, contextFilter, vLeft, vRight, diffs);
            } else {
                diffs.add(new Diff(getFullName(pf, prefix, f), float.class, vLeft, "NULL"));
            }
            prefix.removeLast();
        }
        return diffs;
    }

    protected Set<Object> convertToSet( final float[] arr) {
        return IntStream.range(0, arr.length)
                .mapToObj(i -> arr[i])
                .collect(Collectors.toSet());
    }

    // End of primitive array support

    protected List<Diff> compare(Deque<Field> pf, Deque<String> prefix, Field f, Predicate<Field> fieldFilter, CheckDiffNulls checkNulls,
                                 ContextFilter contextFilter, Object[] left, Object[] right, List<Diff> diffs) {

        return compareArray(pf, prefix, f, fieldFilter, checkNulls, contextFilter, left, right, diffs);
    }

    protected <T> List<Diff> compareArray(Deque<Field> pf, Deque<String> prefix, Field f, Predicate<Field> fieldFilter, CheckDiffNulls checkNulls,
                                          ContextFilter contextFilter, T[] left, T[] right, List<Diff> diffs) {

        Set<Object>  rightSet = Arrays.asList(right).stream().collect(Collectors.toSet());

        left = sortArray(left, f, a -> a.clone(), () -> null);
        right = sortArray(right, f, a -> a.clone(), () -> null);

        if (f != null)
            pf.addLast(f);
        for (int i = 0; i < left.length; i++) {
            Object vLeft = left[i];

            if(rightSet.contains(vLeft)){
                continue;
            }

            prefix.addLast(String.valueOf(i));
            if (i < right.length) {
                Object vRight = right[i];
                // This requires setting null for field as those are not primitive types.
                compare(pf, prefix, null, fieldFilter, checkNulls, contextFilter, vLeft, vRight, diffs);
            } else {
                diffs.add(new Diff(getFullName(pf, prefix, f), vLeft.getClass(), vLeft, "NULL"));
            }
            prefix.removeLast();
        }
        if (f != null)
            pf.removeLast();

        return diffs;
    }

    protected <T> List<Diff> compareAnyArray(Deque<Field> pf, Deque<String> prefix, Field f, Predicate<Field> fieldFilter, CheckDiffNulls checkNulls,
                                             ContextFilter contextFilter, T left, T right, List<Diff> diffs) {

        if (left instanceof boolean[]) {
            return compare(pf, prefix, f, fieldFilter, checkNulls, contextFilter, (boolean[]) left, (boolean[]) right, diffs);
        }
        if (left instanceof byte[]) {
            return compare(pf, prefix, f, fieldFilter, checkNulls, contextFilter, (byte[]) left, (byte[]) right, diffs);
        }
        if (left instanceof char[]) {
            return compare(pf, prefix, f, fieldFilter, checkNulls, contextFilter, (char[]) left, (char[]) right, diffs);
        }
        if (left instanceof double[]) {
            return compare(pf, prefix, f, fieldFilter, checkNulls, contextFilter, (double[]) left, (double[]) right, diffs);
        }
        if (left instanceof float[]) {
            return compare(pf, prefix, f, fieldFilter, checkNulls, contextFilter, (float[]) left, (float[]) right, diffs);
        }
        if (left instanceof int[]) {
            return compare(pf, prefix, f, fieldFilter, checkNulls, contextFilter, (int[]) left, (int[]) right, diffs);
        }
        if (left instanceof long[]) {
            return compare(pf, prefix, f, fieldFilter, checkNulls, contextFilter, (long[]) left, (long[]) right, diffs);
        }
        if (left instanceof short[]) {
            return compare(pf, prefix, f, fieldFilter, checkNulls, contextFilter, (short[]) left, (short[]) right, diffs);
        }

        return compare(pf, prefix, f, fieldFilter, checkNulls, contextFilter, (Object[]) left, (Object[]) right, diffs);
    }

    protected <T> List<Diff> compareAnyPrimitiveType(Deque<Field> pf, Deque<String> prefix, Field f, Predicate<Field> fieldFilter, CheckDiffNulls checkNulls,
                                                     ContextFilter contextFilter, T left, T right, List<Diff> diffs) {

        if (f.getType().isAssignableFrom(Integer.TYPE)) {
            return compare(pf, prefix, f, fieldFilter, checkNulls, contextFilter, ((Integer) left).intValue(), ((Integer) right).intValue(), diffs);
        } else if (f.getType().isAssignableFrom(Long.TYPE)) {
            return compare(pf, prefix, f, fieldFilter, checkNulls, contextFilter, ((Long) left).longValue(), ((Long) right).longValue(), diffs);
        } else if (f.getType().isAssignableFrom(Short.TYPE)) {
            return compare(pf, prefix, f, fieldFilter, checkNulls, contextFilter, ((Short) left).shortValue(), ((Short) right).shortValue(), diffs);
        } else if (f.getType().isAssignableFrom(Double.TYPE)) {
            return compare(pf, prefix, f, fieldFilter, checkNulls, contextFilter, ((Double) left).doubleValue(), ((Double) right).doubleValue(), diffs);
        } else if (f.getType().isAssignableFrom(Float.TYPE)) {
            return compare(pf, prefix, f, fieldFilter, checkNulls, contextFilter, ((Float) left).floatValue(), ((Float) right).floatValue(), diffs);
        } else if (f.getType().isAssignableFrom(Boolean.TYPE)) {
            return compare(pf, prefix, f, fieldFilter, checkNulls, contextFilter, ((Boolean) left).booleanValue(), ((Boolean) right).booleanValue(), diffs);
        } else if (f.getType().isAssignableFrom(Character.TYPE)) {
            return compare(pf, prefix, f, fieldFilter, checkNulls, contextFilter, ((Character) left).charValue(), ((Character) right).charValue(), diffs);
        } else if (f.getType().isAssignableFrom(Byte.TYPE)) {
            return compare(pf, prefix, f, fieldFilter, checkNulls, contextFilter, ((Byte) left).byteValue(), ((Byte) right).byteValue(), diffs);
        }

        return compare(pf, prefix, f, fieldFilter, checkNulls, contextFilter, left, right, diffs);
    }
    //endregion

    //region Ordering Registry
    /**
     * Ordering registry
     */

    private Map<Class<?>, Comparator> comparatorsMap = new HashMap<>();

    public void clearComparators() {
        comparatorsMap.clear();
    }

    public <T> void addComparator(Class<T> type, Comparator<T> comparator) {
        comparatorsMap.put(type, comparator);
    }

    public <T> Comparator<T> getComparator(Class<T> type) {
        return comparatorsMap.get(type);
    }

    public static Class<?> getCollectionType(Field field, Collection<?> collection) {
        if (field != null)
            return getCollectionType(field);
        return getCollectionType(collection);
    }

    public static Class<?> getCollectionType(Field field) {
        ParameterizedType paramType = (ParameterizedType) field.getGenericType();
        Class<?> type = (Class<?>) paramType.getActualTypeArguments()[0];
        return type;
    }

    public static Class<?> getCollectionType(Collection<?> collection) {
        if (collection.isEmpty())
            return null;
        return collection.iterator().next().getClass();
    }

    public <T> boolean hasComparator(Class<T> type) {
        return comparatorsMap.containsKey(type);
    }

    public <T> Collection<T> sortCollectionIfRequired(Collection<T> collection, Field collectionField, Function<Collection<T>, List<T>> cloneFunc, Supplier<Comparator<T>> defaultComparator) {
        Class<?> type = getCollectionType(collectionField, collection);
        if (hasComparator(type))
            return orderCollection(collection, (Class<T>) type, cloneFunc, defaultComparator);
        return collection;
    }

    public <T> List<T> sortListIfRequired(List<T> collection, Field collectionField, Function<Collection<T>, List<T>> cloneFunc, Supplier<Comparator<T>> defaultComparator) {
        Class<?> type = getCollectionType(collectionField, collection);
        if (hasComparator(type))
            return orderList(collection, (Class<T>) type, cloneFunc, defaultComparator);
        return collection;
    }

    public <T> Collection<T> sortCollection(Collection<T> collection, Field collectionField, Function<Collection<T>, List<T>> cloneFunc, Supplier<Comparator<T>> defaultComparator) {
        Class<?> type = getCollectionType(collectionField, collection);
        return orderCollection(collection, (Class<T>) type, cloneFunc, defaultComparator);
    }

    public <T> List<T> sortList(List<T> list, Field collectionField, Function<Collection<T>, List<T>> cloneFunc, Supplier<Comparator<T>> defaultComparator) {
        return (List<T>) sortCollection(list, collectionField, cloneFunc, defaultComparator);
    }

    public <T> List<T> sortSet(Set<T> set, Field collectionField, Function<Collection<T>, List<T>> cloneFunc, Supplier<Comparator<T>> defaultComparator) {
        return (List<T>) sortCollection(set, collectionField, cloneFunc, defaultComparator);
    }

    protected <T> Collection<T> orderCollection(Collection<T> collection, Class<T> type, Function<Collection<T>, List<T>> cloneFunc, Supplier<Comparator<T>> defaultComparator) {

        Comparator<T> comparator = defaultComparator.get();

        if (hasComparator(type)) {
            comparator = (Comparator<T>) getComparator(type);
        }

        List<T> clone = cloneFunc.apply(collection);

        clone.sort(comparator);

        return clone;
    }

    protected <T> List<T> orderList(List<T> list, Class<T> type, Function<Collection<T>, List<T>> cloneFunc, Supplier<Comparator<T>> defaultComparator) {
        return (List<T>) orderCollection(list, type, cloneFunc, defaultComparator);
    }

    protected <T> List<T> orderSet(Set<T> set, Class<T> type, Function<Collection<T>, List<T>> cloneFunc, Supplier<Comparator<T>> defaultComparator) {
        return (List<T>) orderCollection(set, type, cloneFunc, defaultComparator);
    }

    public static <T> Class<T> getArrayType(T[] array) {
        return (Class<T>) array.getClass().getComponentType();
    }

    public static <T> Class<T> getArrayType(Field field) {
        return (Class<T>) field.getType().getComponentType();
    }

    public static <T> Class<T> getArrayType(T[] array, Field field) {
        Class<T> type = null;
        if (field == null) {
            type = getArrayType(array);
        } else {
            type = getArrayType(field);
        }
        return type;
    }

    public <T> T[] sortArray(T[] array, Field field, Function<T[], T[]> cloneFunc, Supplier<Comparator<T>> defaultComparator) {
        Class<?> type = getArrayType(array, field);
        if (hasComparator(type))
            return orderArray(array, (Class<T>) type, cloneFunc, defaultComparator);
        return array;
    }

    protected <T> T[] orderArray(T[] array, Class<T> type, Function<T[], T[]> cloneFunc, Supplier<Comparator<T>> defaultComparator) {

        Comparator<T> comparator = defaultComparator.get();

        if (hasComparator(type)) {
            comparator = (Comparator<T>) getComparator(type);
        }

        T[] cloneArray = cloneFunc.apply(array);

        Arrays.sort(cloneArray, comparator);

        return cloneArray;

    }

    public short[] sortArray(short[] array, Field field, Function<short[], short[]> cloneFunc, Supplier<Comparator<Short>> defaultComparator) {
        if (hasComparator(short.class) || hasComparator(Short.class)) {
            short[] clone = cloneFunc.apply(array);
            List<Short> arrayList = Shorts.asList(clone);
            orderList(arrayList, Short.class, l -> (List<Short>) l, defaultComparator);
            return clone;

        }
        return array;
    }

    public int[] sortArray(int[] array, Field field, Function<int[], int[]> cloneFunc, Supplier<Comparator<Integer>> defaultComparator) {
        if (hasComparator(int.class) || hasComparator(Integer.class)) {
            int[] clone = cloneFunc.apply(array);
            List<Integer> arrayList = Ints.asList(clone);
            orderList(arrayList, Integer.class, l -> (List<Integer>) l, defaultComparator);
            return clone;

        }
        return array;
    }

    public long[] sortArray(long[] array, Field field, Function<long[], long[]> cloneFunc, Supplier<Comparator<Long>> defaultComparator) {
        if (hasComparator(long.class) || hasComparator(Long.class)) {
            long[] clone = cloneFunc.apply(array);
            List<Long> arrayList = Longs.asList(clone);
            orderList(arrayList, Long.class, l -> (List<Long>) l, defaultComparator);
            return clone;

        }
        return array;
    }

    public float[] sortArray(float[] array, Field field, Function<float[], float[]> cloneFunc, Supplier<Comparator<Float>> defaultComparator) {
        if (hasComparator(float.class) || hasComparator(Long.class)) {
            float[] clone = cloneFunc.apply(array);
            List<Float> arrayList = Floats.asList(clone);
            orderList(arrayList, Float.class, l -> (List<Float>) l, defaultComparator);
            return clone;

        }
        return array;
    }

    public double[] sortArray(double[] array, Field field, Function<double[], double[]> cloneFunc, Supplier<Comparator<Double>> defaultComparator) {
        if (hasComparator(double.class) || hasComparator(Double.class)) {
            double[] clone = cloneFunc.apply(array);
            List<Double> arrayList = Doubles.asList(clone);
            orderList(arrayList, Double.class, l -> (List<Double>) l, defaultComparator);
            return clone;

        }
        return array;
    }

    public boolean[] sortArray(boolean[] array, Field field, Function<boolean[], boolean[]> cloneFunc, Supplier<Comparator<Boolean>> defaultComparator) {
        if (hasComparator(boolean.class) || hasComparator(Boolean.class)) {
            boolean[] clone = cloneFunc.apply(array);
            List<Boolean> arrayList = Booleans.asList(clone);
            orderList(arrayList, Boolean.class, l -> (List<Boolean>) l, defaultComparator);
            return clone;

        }
        return array;
    }

    public byte[] sortArray(byte[] array, Field field, Function<byte[], byte[]> cloneFunc, Supplier<Comparator<Byte>> defaultComparator) {
        if (hasComparator(byte.class) || hasComparator(Byte.class)) {
            byte[] clone = cloneFunc.apply(array);
            List<Byte> arrayList = Bytes.asList(clone);
            orderList(arrayList, Byte.class, l -> (List<Byte>) l, defaultComparator);
            return clone;

        }
        return array;
    }

    public char[] sortArray(char[] array, Field field, Function<char[], char[]> cloneFunc, Supplier<Comparator<Character>> defaultComparator) {
        if (hasComparator(char.class) || hasComparator(Character.class)) {
            char[] clone = cloneFunc.apply(array);
            List<Character> arrayList = Chars.asList(clone);
            orderList(arrayList, Character.class, l -> (List<Character>) l, defaultComparator);
            return clone;

        }
        return array;
    }
    //endregion

    //region Utility Methods
    /**
     * Utility methods
     */

    private static final Collection<Class<?>> EXTENDABLE_SIMPLE_TYPES = Arrays.asList(
            BigDecimal.class,
            BigInteger.class,
            CharSequence.class, // String, StringBuilder, etc.
            Calendar.class, // GregorianCalendar, etc.
            Date.class, // java.sql.Date, java.util.Date, java.util.Time, etc.
            Enum.class // enums... duh
    );

    private static final List<Class<? extends Serializable>> FINAL_SIMPLE_TYPES = Arrays.asList(
            Class.class,
            URI.class,
            URL.class,
            Locale.class,
            UUID.class
    );

    public static boolean isComparableType(final Class<?> clazz)
    {
        return Comparable.class.isAssignableFrom(clazz);
    }

    public boolean isPrimitiveType(Class<?> clazz) {
        return clazz.isPrimitive() || Primitives.isWrapperType(clazz);
    }

    public boolean isString(Class<?> clazz) {
        return clazz == String.class;
    }

    public boolean isJdk(Class<?> clazz) {
        return clazz.getName().startsWith("java.");
    }

    public boolean isSimpleType(final Class<?> clazz)
    {
        if (clazz == null)
        {
            return false;
        }
        else if (isPrimitiveType(clazz))
        {
            return true;
        }

        for (final Class<?> type : FINAL_SIMPLE_TYPES)
        {
            if (type.equals(clazz))
            {
                return true;
            }
        }
        for (final Class<?> type : EXTENDABLE_SIMPLE_TYPES)
        {
            if (type.isAssignableFrom(clazz))
            {
                return true;
            }
        }
        return false;
    }

    public boolean isCollection(Class<?> clazz) {
        return clazz.isAssignableFrom(List.class) || clazz.isAssignableFrom(Map.class) || clazz.isAssignableFrom(Set.class) || clazz.isAssignableFrom(Collection.class);
    }

    public <T> String getClassName(T left, T right) {
        return left != null ? left.getClass().getSimpleName() : right != null ? right.getClass().getSimpleName() : "ALL";
    }

    public <T> Class<?> getClassType(T left, T right) {
        return left != null ? left.getClass() : right != null ? right.getClass() : null;
    }

    // Used for null checking
    //
    protected String getFullName(Deque<Field> pf, Deque<String> prefix, String fieldName) {

        if (pf.isEmpty()) {

            if (prefix.isEmpty())
                return Strings.isNullOrEmpty(fieldName) ? "" : fieldName;

            String postfix = Strings.isNullOrEmpty(fieldName) ? "" : "." + fieldName;
            return toString(prefix, ".") + postfix;
        }

        if (prefix.isEmpty()) {

            String fullName = getFullName(pf);

            return Strings.isNullOrEmpty(fieldName) ? fullName : fullName + "." + fieldName;
        } else {

            String fullName = getFullName(pf, prefix);
            return Strings.isNullOrEmpty(fieldName) ? fullName : fullName + "." + fieldName;
        }
    }

    protected String getFullName(Deque<Field> pf, Deque<String> prefix, Field f) {

        if (pf.isEmpty()) {

            if (prefix.isEmpty())
                return f != null ? f.getName() : "";
            String pfs = f != null ? f.getName() : "";
            return pfs + "." + toString(prefix, ".");
        }

        if (prefix.isEmpty()) {
            if (f == null) {
                return getFullName(pf);
            }
            Deque<Field> newList = new ArrayDeque<>(pf);
            newList.add(f);
            return getFullName(newList);
        } else if (f == null) {
            return getFullName(pf, prefix);
        } else {
            Deque<Field> newList = new ArrayDeque<>(pf);
            newList.add(f);
            return getFullName(newList, prefix);
        }
    }

    protected String toString(Collection<String> strings, String separator) {
        return strings.stream().collect(Collectors.joining(separator));
    }

    protected String getFullName(Deque<Field> pf, Deque<String> prefixes) {
        Iterator<String> prefixesIter = prefixes.iterator();
        return pf.stream()
                .map(f -> {
                    String preFix = prefixesIter.hasNext() ? prefixesIter.next() : "";
                    return f.getName() + (Strings.isNullOrEmpty(preFix) ? "" : "." + preFix);
                })
                .collect(Collectors.joining("."));
    }

    protected String getFullName(Deque<Field> pf) {
        return pf.stream().map(f -> f.getName()).collect(Collectors.joining("."));
    }
    //endregion

    //region Reflection Methods
    /**
     * Reflection related code
     */

    public static List<Field> getAllDeclaredFields(Class<?> clazz, Predicate<Field> filter) {

        List<Field> fields = new ArrayList<>();
        getDeclaredFields(clazz, f -> fields.add(f), f -> filter.test(f));
        return fields;
    }

    /**
     * This variant retrieves (0link ClassigetDeclaredfields () } from a local cache
     * in order to avoid the JVM's SecurityManager check and defensive array copying.
     *
     * @param clazz the class to introspect
     * @return the cached array of fields
     * @see Class#getDeclaredFields()
     */

    private static Field[] getDeclaredFields(Class<?> clazz) {

        Field[] result = declaredFieldsCache.get(clazz);
        if (result == null) {
            result = clazz.getDeclaredFields();
            declaredFieldsCache.put(clazz, (result.length == 0 ? NO_FIELDS : result));
        }
        return result;
    }

    /**
     * Invoke the given callback on all fields in the target class, going up the
     * class hierarchy to get all declared fields.
     *
     * @param clazz the target class to analyze
     * @param fc    the callback to invoke for each field
     * @param ff    the filter that determines the fields to apply the callback to
     */

    public static void getDeclaredFields(Class<?> clazz, Consumer<Field> fc, Predicate<Field> ff) {
        // Keep backing up the inheritance hierarchy.
        Class<?> targetClass = clazz;
        do {
            Field[] fields = getDeclaredFields(targetClass);
            for (Field field : fields) {
                if (ff != null && !ff.test(field)) {
                    continue;
                }
                try {
                    fc.accept(field);
                } catch (Exception ex) {
                    if (ex instanceof IllegalAccessException)
                        throw new IllegalStateException("Not allowed to access field '" + field.getName() + "': " + ex);
                    throw ex;
                }
            }
            targetClass = targetClass.getSuperclass();
        } while (targetClass != null && targetClass != Object.class);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getFieldValueWithType(Field field, T input) {
        makeAccessible(field);
        return (T) getField(field, input);
    }

    /**
     * Make the given field accessible, explicitly setting it accessible if necessary.
     * The {@code setAccessible(true)} method is only called
     * when actually necessary, to avoid unnecessary conflicts vith a JVM * SecurityManager (if active).
     *
     * @param field the field to make accessible
     * @see java.lang.reflect.Field#setAccessible
     */

    public static void makeAccessible(Field field) {
        if ((!Modifier.isPublic(field.getModifiers()) ||
                !Modifier.isPublic(field.getDeclaringClass().getModifiers()) ||
                Modifier.isFinal(field.getModifiers())) && !field.isAccessible()) {
            field.setAccessible(true);
        }
    }

    /**
     * Get the field represented by the supplied {@link Field field object} on the
     * specified {@link Object target object}. In accordance vith {@link Field#get(Object)}
     * semantics, the returned value is automatically vrapped if the underlying field
     * has a primitive type.
     * <p>Throva exceptions are handled via a call to {@link #handleReflectionException (Exception)}.
     *
     * @param field  the field to get
     * @param target the target object from which to get the field
     * @return the field's current value
     */

    public static Object getField(Field field, Object target) {
        try {
            return field.get(target);
        } catch (IllegalAccessException ex) {
            handleReflectionException(ex);
            throw new IllegalStateException("Unexpected reflection exception -" + ex.getClass().getName() + ": " + ex.getMessage());
        }
    }

    /**
     * Handle the given reflection exoeption. Should only be called if no
     * checked exception is expected to be thrown by the target method.
     * <p>Throws the underlying RuntimeException or Error in case of an
     * InvocationTargetException with such a root cause. Throws an
     * IllegalStateException with appropriate message also
     *
     * @param ex the reflection exception to handle
     */

    public static void handleReflectionException(Exception ex) {
        if (ex instanceof NoSuchMethodException) {
            throw new IllegalStateException("Method not found: " + ex.getMessage());
        }
        if (ex instanceof IllegalAccessException) {
            throw new IllegalStateException("Could not access method : " + ex.getMessage());
        }
        if (ex instanceof InvocationTargetException) {
            handleInvocationTargetException((InvocationTargetException) ex);
        }
        if (ex instanceof RuntimeException) {
            throw (RuntimeException) ex;
        }
        throw new UndeclaredThrowableException(ex);
    }


    /**
     * Handle the given invocation target exception. Should only be called if no
     * checked exception is expected to be thrown by the target method.
     * <p>Throws the underlying RuntimeException or Error in case of such a root
     * cause. Throws an IllegalStateException else.
     *
     * @param ex the invocation target exception to handle
     */

    public static void handleInvocationTargetException(InvocationTargetException ex) {
        rethrowRuntimeException(ex.getTargetException());
    }

    /**
     * Rethrow the given {@link Throwable exception}, which is presumably the
     * <em>target exception</em> of an (@liak InvocationTargetException}. Should
     * only be called if no checked exception is expected to be thrown by the target method.
     * <p>Rethrows the underlying exception cast to an {@link RuntimeException} or
     * {@link Error} if appropriate; otherwise, throws an
     * {@link IllegalStateException}.
     *
     * @param ex the exception to rethrow
     * @throws RuntimeException the rethrown exception
     */

    public static void rethrowRuntimeException(Throwable ex) {
        if (ex instanceof RuntimeException) {
            throw (RuntimeException) ex;
        }
        if (ex instanceof Error) {
            throw (Error) ex;
        }
        throw new UndeclaredThrowableException(ex);
    }
    //endregion

}
