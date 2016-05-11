<h1>Chronicle-Values
<a href="https://maven-badges.herokuapp.com/maven-central/net.openhft/chronicle-values">
<img src="https://maven-badges.herokuapp.com/maven-central/net.openhft/chronicle-values/badge.svg" />
</a></h1>

Poor man's value types, Java 8+

Generation of constantly-sized flyweight accessors to [Chronicle Bytes](
https://github.com/OpenHFT/Chronicle-Bytes) and simple bean-style on-heap implementations from
interfaces. Interfaces, that could be processed by Chronicle-Values generation, are called **value
interfaces**.

**Project status: Alpha**, the feature matrix (see below) is very wast and not fully implemented
yet, please write the value interface according to the specification below, with the **unit tests
for your interface**. If the generation from the value interface (according to spec) doesn't work,
please report the case via issues on Github.

## Value interface specification

An important pre-requirement for value interfaces: they should belong to some package with
a non-empty name, not the default package.

Simple example:

```java
package test;

interface Balance {
    long getDollars();
    void setDollars(long dollars);
    long addDollars(long addition);

    int getCents();
    void setCents(int cents);
}
```

is processed to either a flyweight of 12 bytes, or a bean class with `long dollars;` and
`int cents;` fields.

### Supported field types

#### All Java primitives

`int`, `long`, `float`, `double`, `byte`, `boolean`, `char`, `short`

#### `String` or `CharSequence`

Must be annotated with `@MaxUtf8Length()`, like:

```java
interface Client {
    CharSequence getName();
    void setName(@MaxUtf8Length(20) CharSequence name);

    CharSequence getStateCode();
    void setStateCode(@NotNull @MaxUtf8Length(2) CharSequence stateCode);
}
```

#### Any Java `enum` type

```
interface Order {
    enum State {NEW, CANCELLED, FILLED}

    State getState();
    void setState(@NotNull State state);
}
```

#### `java.util.Date`

#### Another value interface

This allows to build nested structures:

```java
interface Point {
    double getX();
    void setX(double x);

    double getY();
    void setY(double y);
}

interface Circle {
    Point getCenter();
    void getUsingCenter(Point using);
    void setCenter(Point center);

    double getRadius();
    void setRadius(double radius);
}
```

Self-references are forbidden.

#### Array fields

Of any of the above types, with special syntax: `-At` suffix and first parameter of all methods should be `int index`.

```java
interface SomeStats {
    @Array(length=100)
    long getPercentFreqAt(int index);
    void setPercentFreqAt(int index, long percentFreq);
    long addPercentFreqAt(int index, long addition);
}
```

### More than 2 operations with fields

#### Simple get/set

`[get]<FieldName>[At]`, `[set]<FieldName>[At]`, `is<FieldName>[At]` - simple get/set. For `boolean`
fields, `isFoo()` Java bean syntax variation is supported. Also, `get-` and `set-` prefixes could be
omitted, e. g.
```java
interface Point {
    double x();
    void x(double x);

    double y();
    void y(double y);
}
```

#### Volatile get/set

`getVolatile<FieldName>[At]`, `setVolatile<FieldName>[At]`

#### "Ordered" set

`setOrdered<FieldName>[At]` - ordered write operation, the same as behind `AtomicInteger.lazySet()`

#### Simple add

`type add<FieldName>[At]([int index, ]type addition)` - equivalent of
```java
    int foo = getFoo();
    foo += addition;
    setFoo(foo);
    return foo;
```
works only with numeric primitive field types: `byte`, `char`, `short`, `int`, `long`, `double`,
`float`

#### Atomic add

`type addAtomic<FieldName>[At]([int index, ]type addition)` - same as `add`, operates via atomic
operations, works only with numeric primitive field types.

#### Compare-and-swap

`boolean compareAndSwap<FieldName>[At]([int index, ]type expectedValue, type newValue)` - atomic
field value exchange, returns `true` if successfully swapped the value. Works only with primitive,
`enum` and `Date` field types.

#### getUsing

`getUsing<FieldName>[At]([int index, ]Type using)` - for `String`, `CharSequence` or another value
interface field types. Reads the value into the given on-heap object. Primarily useful for
retrieving data from flyweight implementations without creating garbage.

If the field type is `String` or `CharSequence`, `using` parameter type must be `StringBuilder`.
Return type of the `getUsing` method in this case might be `CharSequence`, `StringBuilder`, `String`
or `void`, if this char sequence field is marked as `@NotNull`. Semantically this method is
equivalent to
```java
CharSequence getUsingName(StringBuilder using) {
    using.setLength(0);
    CharSequence name = getName();
    if (name != null) {
       using.append(name);
       return using;
    } else {
       return null;
    }
}
```

Note that the `StringBuilder` is cleared via `setLength(0)` before reusing.

If the field type is another value interface field, `using` parameter type is the value interface,
the return type of the method could be the interface or `void`. See `getUsingCenter(Point using)` in
the example above.

### Field configuration via annotations

#### Field ordering in flyweight layout

Field order is unspecified. To ensure some order, put `@Group` annotations on any of field's methods,
for example:

```java
interface Complex {
    @Group(1)
    double real();
    void real(double real);

    @Group(2)
    double image();
    void image(double image);
}
```

Groups are ordered in the ascending order of their argument numbers. In the above case, the
generated flyweight implementation will place `real` field at 0-7 bytes and `image` field at 8-15
bytes from it's offset.

#### Field nullability

By default, `enum` and `String`/`CharSequence` fields are nullable. Annotate them with
`@net.openhft.chronicle.values.NotNull` to forbid `null` values:

```java
interface Instrument {
    CharSequence getSymbol();
    void setSymbol(@NotNull @MaxUtf8Length(5) CharSequence symbol);
}
```

#### Numeric field ranges

Annotate numeric fields with `@Range(min=, max=)` to save space in flyweight implementation, e. g.

```java
interface Transaction {
    int getSecondFromDayStart();
    void setSecondFromDayStart(@Range(min = 0, max = 24 * 60 * 60) int secondFromDayStart);
}
```

The field `SecondFromDayStart` could take only 17 bits in bytes, instead of 32.

#### Field alignment

For flyweight implementation, you might need to align certain fields, to ensure some properties of
reads and writes. For example, you might want to ensure, that a certain field doesn't cross cache
line boundary:

```java
interface Message {
    ...many fields

    @Align(dontCross=64)
    long getImportantField();
    void setImportantField(long importantValue);
}
```

See `@Align` and `@Array` annotations [Javadocs](http://javadoc.io/doc/net.openhft/chronicle-values)
for more information.

## Use

```java
// flyweight
Point offHeapPoint = Values.newDirectReference(Point.class);
((Byteable) offHeapPoint).bytesStore(bytesStore, offset, 16);
offHeapPoint.setX(0);
offHeapPoint.setY(0);

// on-heap
Point onHeapPoint = Values.newHeapInstance(Point.class);
onHeapPoint.setX(1)
onHeapPoint.setY(2);
```

The generated on-heap and flyweight classes *do* implement:
 - `Copyable<Point>`, to allow easy data exchange: `onHeapPoint.copyFrom(offHeapPoint)`
 - `BytesMarshallable` from [Chronicle Bytes](https://github.com/OpenHFT/Chronicle-Bytes)
 - Proper `equals()`, `hashCode()` and `toString()`
 - `Byteable`, but on-heap implementation is dummy, throws `UnsupportedOperationException`

For convenience, you could make the value interface to extend the above utility interfaces,
to avoid casting:

```java
interface Point extends Byteable, BytesMarshallable, Copyable { ... }

Point offHeapPoint = Values.newDirectReference(Point.class);
// no cast
offHeapPoint.bytesStore(bytesStore, offset, offHeapPoint.maxSize());
```

## [Javadocs](http://javadoc.io/doc/net.openhft/chronicle-values)
