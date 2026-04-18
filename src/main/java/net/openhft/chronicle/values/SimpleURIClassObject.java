/*
 * Copyright 2013-2026 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.JavaFileObject;
import java.io.*;
import java.net.URI;
import java.nio.CharBuffer;
/**
 * Lightweight {@link JavaFileObject} backed by a {@link URI}.
 * <p>
 * {@link MyJavaFileManager} creates instances so the Java compiler can read
 * already compiled classes from the classpath. Only read operations are
 * implemented.
 */

class SimpleURIClassObject implements JavaFileObject {

    /**
     * Location of the class bytecode.
     */
    final URI uri;
    /**
     * The class being supplied to the compiler.
     */
    final Class<?> c;

    /**
     * Creates a wrapper around the given class resource.
     *
     * @param uri location of the {@code .class} file
     * @param c   the class being wrapped
     */
    protected SimpleURIClassObject(URI uri, Class<?> c) {
        this.uri = uri;
        this.c = c;
    }

    @Override
    public URI toUri() {
        return uri;
    }

    @Override
    public String getName() {
        return this.toUri().toString();
    }

    /**
     * Opens the underlying resource for reading.
     *
     * @return stream supplying the class bytes
     * @throws IOException if the resource cannot be opened
     */
    @Override
    public InputStream openInputStream() throws IOException {
        return uri.toURL().openStream();
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
        CharSequence charContent = this.getCharContent(ignoreEncodingErrors);
        if (charContent == null)
            throw new UnsupportedOperationException();
        if (charContent instanceof CharBuffer) {
            CharBuffer buffer = (CharBuffer) charContent;
            if (buffer.hasArray())
                return new CharArrayReader(buffer.array());
        }
        return new StringReader(charContent.toString());
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Writer openWriter() throws IOException {
        return new OutputStreamWriter(this.openOutputStream());
    }

    @Override
    public long getLastModified() {
        return 0L;
    }

    @Override
    public boolean delete() {
        return false;
    }
    /**
     * This object always represents a compiled class.
     *
     * @return {@link Kind#CLASS}
     */
    @Override
    public Kind getKind() {
        return Kind.CLASS;
    }
    @Override
    public boolean isNameCompatible(String simpleName, Kind kind) {
        String baseName = simpleName + kind.extension;
        return kind.equals(this.getKind()) && toUri().toString().endsWith(baseName);
    }

    @Override
    public NestingKind getNestingKind() {
        return null;
    }

    @Override
    public Modifier getAccessLevel() {
        return null;
    }

    @Override
    public String toString() {
        return getClass().getName() + "[" + this.toUri() + "]";
    }
}
