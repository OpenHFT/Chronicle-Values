//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

package net.openhft.chronicle.values;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.util.WeakIdentityHashMap;

import java.lang.ref.WeakReference;
import java.lang.reflect.Modifier;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Map;

/**
 * Generates runtime implementations for Chronicle Values.
 * Stripped down version of classes
 * https://github.com/google/guice/blob/9867f9c2142355ae958f9eeb8fb96811082c8812/core/src/com/google/inject/internal/InternalFlags.java
 * and
 * https://github.com/google/guice/blob/09fec22916264e76d95333b81c69030b3b713e64/core/src/com/google/inject/internal/BytecodeGen.java
 * <p>
 * When loading classes, we need to be careful of:
 * <ul>
 * <li><strong>Memory leaks.</strong> Generated classes need to be garbage collected in long-lived
 * applications. Once an injector and any instances it created can be garbage collected, the
 * corresponding generated classes should be collectable.
 * <li><strong>Visibility.</strong> Containers like {@code OSGi} use class loader boundaries
 * to enforce modularity at runtime.
 * </ul>
 * <p>
 * For each generated class we use three distinct class loaders:
 * <ul>
 * <li><strong>User's class loader.</strong> The loader that loaded the application
 * class being implemented. Using it allows access to protected and
 * package-scoped members.</li>
 * <li><strong>Values's class loader.</strong> The loader that loaded the Chronicle
 * Values library.</li>
 * <li><strong>Bridge class loader.</strong> A child of the user's loader that hosts
 * generated classes. It delegates to the user's loader for application classes
 * and to the Values loader for library helpers.</li>
 * </ul>
 * The bridge loader only has weak references in our cache, so once the user's
 * loader is discarded the generated classes and their loader can be collected.
 * This prevents the class-loader leaks often seen with reflection based
 * generation.
 * <p>
 * Custom class loading can be disabled with the system property
 * {@code chronicle_values_custom_class_loading}. The
 * {@link CustomClassLoadingOption#BRIDGE BRIDGE} option is the default and uses
 * the above strategy. The {@link CustomClassLoadingOption#OFF OFF} option loads
 * classes in the calling loader and relies on the JVM's standard delegation.
 *
 * @author mcculls@gmail.com (Stuart McCulloch)
 * @author jessewilson@google.com (Jesse Wilson)
 */
final class BytecodeGen {

    static final ClassLoader VALUES_CLASS_LOADER = canonicalize(BytecodeGen.class.getClassLoader());
    /**
     * ie. "net.openhft.chronicle.values"
     */
    static final String VALUES_PACKAGE =
            BytecodeGen.class.getName().replaceFirst("\\.values\\..*$", ".values");
    /**
     * ie. "net.openhft.chronicle.bytes"
     */
    static final String BYTES_PACKAGE =
            Bytes.class.getName().replaceFirst("\\.bytes\\..*$", ".bytes");
    private static final CustomClassLoadingOption CUSTOM_CLASS_LOADING =
            parseCustomClassLoadingOption();
    /**
     * Weak cache of bridge class loaders that make the Chronicle Values implementation
     * classes visible to various code-generated proxies of client classes.
     */
    private static final Map<ClassLoader, WeakReference<ClassLoader>> CLASS_LOADER_CACHE =
            new WeakIdentityHashMap<>();

    public static CustomClassLoadingOption getCustomClassLoadingOption() {
        return CUSTOM_CLASS_LOADING;
    }

    private static CustomClassLoadingOption parseCustomClassLoadingOption() {
        return getSystemOption("chronicle_values_custom_class_loading",
                CustomClassLoadingOption.BRIDGE, CustomClassLoadingOption.OFF);
    }

    /**
     * Reads an enumerated system property as a privileged action.
     *
     * @param name         property key to look up
     * @param defaultValue result when the property is absent or invalid
     * @param secureValue  result when a security manager forbids access
     * @return resolved option
     */
    private static <T extends Enum<T>> T getSystemOption(final String name, T defaultValue,
                                                         T secureValue) {
        Class<T> enumType = defaultValue.getDeclaringClass();
        String value = null;
        try {
            value = doPrivileged(() -> Jvm.getProperty(name));
            return (value != null && value.length() > 0) ? Enum.valueOf(enumType, value) :
                    defaultValue;
        } catch (SecurityException e) {
            return secureValue;
        } catch (IllegalArgumentException e) {
            Jvm.warn().on(BytecodeGen.class,
                    value + " is not a valid flag value for " + name + ". "
                            + " Values must be one of " + Arrays.asList(enumType.getEnumConstants()));
            return defaultValue;
        }
    }

    @SuppressWarnings({"deprecation", "removal"})
    private static <T> T doPrivileged(PrivilegedAction<T> stringPrivilegedAction) {
        return java.security.AccessController.doPrivileged(stringPrivilegedAction);
    }

    private static ClassLoader getFromClassLoaderCache(ClassLoader typeClassLoader) {
        synchronized (CLASS_LOADER_CACHE) {
            return CLASS_LOADER_CACHE.compute(typeClassLoader, (k, ref) -> {
                if (ref == null || ref.get() == null) {
                    Jvm.debug().on(BytecodeGen.class,
                            "Creating a bridge ClassLoader for " + typeClassLoader);
                    return doPrivileged(() -> new WeakReference<>(new BridgeClassLoader(typeClassLoader)));
                } else {
                    return ref;
                }
            }).get();
        }
    }

    /**
     * Attempts to canonicalize null references to the system class loader.
     * May return null if for some reason the system loader is unavailable.
     */
    private static ClassLoader canonicalize(ClassLoader classLoader) {
        return classLoader != null ? classLoader : SystemBridgeHolder.SYSTEM_BRIDGE.getParent();
    }

    /**
     * Returns the class loader to host generated classes for {@code type}.
     */
    public static ClassLoader getClassLoader(Class<?> type) {
        return getClassLoader(type, type.getClassLoader());
    }

    private static ClassLoader getClassLoader(Class<?> type, ClassLoader delegate) {

        // simple case: do nothing!
        if (getCustomClassLoadingOption() == CustomClassLoadingOption.OFF) {
            return delegate;
        }

        // java.* types can be seen everywhere
        if (type.getName().startsWith("java.")) {
            return VALUES_CLASS_LOADER;
        }

        delegate = canonicalize(delegate);

        // no need for a bridge if using same class loader, or it's already a bridge
        if (delegate == VALUES_CLASS_LOADER || delegate instanceof BridgeClassLoader) {
            return delegate;
        }

        // don't try bridging private types as it won't work
        if (Visibility.forType(type) == Visibility.PUBLIC) {
            if (delegate != SystemBridgeHolder.SYSTEM_BRIDGE.getParent()) {
                // delegate guaranteed to be non-null here
                return getFromClassLoaderCache(delegate);
            }
            // delegate may or may not be null here
            return SystemBridgeHolder.SYSTEM_BRIDGE;
        }

        return delegate; // last-resort: do nothing!
    }

    /**
     * The options for Values custom class loading.
     */
    public enum CustomClassLoadingOption {
        /**
         * No custom class loading
         */
        OFF,
        /**
         * Automatically bridge between class loaders (Default)
         */
        BRIDGE
    }

    /**
     * The required visibility of a user's class from a Values-generated class. Visibility of
     * package-private members depends on the loading classloader: only if two classes were loaded
     * by the same classloader can they see each other's package-private members. We need to be
     * careful when choosing which classloader to use for generated classes. We prefer our bridge
     * classloader, since it's OSGi-safe and doesn't leak Metaspace. But often we cannot due to
     * visibility.
     */
    public enum Visibility {

        /**
         * Indicates that Values-generated classes only need to call and override public members of
         * the target class. These generated classes may be loaded by our bridge classloader.
         */
        PUBLIC {
            @Override
            public Visibility and(Visibility that) {
                return that;
            }
        },

        /**
         * Indicates that Values-generated classes need to call or override package-private members.
         * These generated classes must be loaded in the same classloader as the target class. They
         * won't work with OSGi, and won't get garbage collected until the target class' classloader
         * is garbage collected.
         */
        SAME_PACKAGE {
            @Override
            public Visibility and(Visibility that) {
                return this;
            }
        };

        public static Visibility forType(Class<?> type) {
            return (type.getModifiers() & (Modifier.PROTECTED | Modifier.PUBLIC)) != 0
                    ? PUBLIC
                    : SAME_PACKAGE;
        }

        public abstract Visibility and(Visibility that);
    }

    // initialization-on-demand...
    private static class SystemBridgeHolder {
        static final BridgeClassLoader SYSTEM_BRIDGE = new BridgeClassLoader();
    }

    /**
     * Class loader for Values-generated classes. It bridges between the user's
     * class loader and Values's class loader so that generated code can access
     * both application and library classes.
     */
    static class BridgeClassLoader extends ClassLoader {

        /**
         * Creates a bridge using the system class loader as the parent.
         */
        BridgeClassLoader() {
            // use system loader as parent
        }

        /**
         * Creates a bridge whose parent is the supplied user's class loader.
         *
         * @param usersClassLoader parent loader for user classes
         */
        BridgeClassLoader(ClassLoader usersClassLoader) {
            super(usersClassLoader);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve)
                throws ClassNotFoundException {

            if (name.startsWith("sun.reflect")) {
                // these reflection classes must be loaded from bootstrap class loader
                return SystemBridgeHolder.SYSTEM_BRIDGE.classicLoadClass(name, resolve);
            }

            if (name.startsWith(VALUES_PACKAGE) || name.startsWith(BYTES_PACKAGE)) {
                if (null == VALUES_CLASS_LOADER) {
                    // use special system bridge to load classes from bootstrap class loader
                    return SystemBridgeHolder.SYSTEM_BRIDGE.classicLoadClass(name, resolve);
                }
                try {
                    Class<?> clazz = VALUES_CLASS_LOADER.loadClass(name);
                    if (resolve) {
                        resolveClass(clazz);
                    }
                    return clazz;
                } catch (Throwable e) {
                    // fall-back to classic delegation
                }
            }

            return classicLoadClass(name, resolve);
        }

        // make the classic delegating loadClass method visible
        Class<?> classicLoadClass(String name, boolean resolve)
                throws ClassNotFoundException {
            return super.loadClass(name, resolve);
        }
    }
}
