/*
 *
 * *********************************************************************
 * fsdevtools
 * %%
 * Copyright (C) 2016 e-Spirit AG
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * *********************************************************************
 *
 */

package com.espirit.moddev.cli.commands;

import com.espirit.moddev.cli.api.command.Command;
import com.github.rvesse.airline.annotations.Option;

import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.tools.DiagnosticListener;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;

/**
 * @author e-Spirit AG
 */
@com.github.rvesse.airline.annotations.Command(name = "java", groupNames = {"execute"})
public class ExecuteJavaCommand implements Command {


    @Option(name="-file")
    private String filePath;

    @Override
    public Object call() throws Exception {

        String source = "package xxx;\n"
                        + "\n"
                        + "import com.espirit.moddev.cli.Cli;\n"
                        + "\n"
                        + "public class Test implements Runnable {\n"
                        + "\tstatic {\n"
                        + "\t\tSystem.out.println(\"hello\");\n"
                        + "\t}\n"
                        + "\tpublic Test() {\n"
                        + "\t\tSystem.out.println(\"world\");\n"
                        + "\t}\n"
                        + "\n"
                        + "\t@Override\n"
                        + "\tpublic void run() {\n"
                        + "\n"
                        + "\t\tnew Cli().execute(new String[]{\"help\"});\n"
                        + "\n"
                        + "\t\tSystem.out.println(\"ran the xxx runnable\"); \n"
                        + "\t}\n"
                        + "}";

        if(filePath != null) {
            File sourceFile = new File(filePath);
            source = readFile(sourceFile.getPath(), StandardCharsets.UTF_8);
            String packageName = getPackageNameFromJavCode(source);

            String className = sourceFile.getName().replaceAll("\\.java", "");
            String simpleClassName = className;
            className = packageName + "." + className;

            JavaCompiler javac = new EclipseCompiler();
            StandardJavaFileManager sjfm = javac.getStandardFileManager(null, null, null);
            SpecialClassLoader cl = new SpecialClassLoader();
            SpecialJavaFileManager fileManager = new SpecialJavaFileManager(sjfm, cl);
            List options = Collections.emptyList();

            List compilationUnits = Arrays.asList(new MemorySource(simpleClassName, source));
            DiagnosticListener diagnosticListener = null;
            Iterable classes = null;
            JavaCompiler.CompilationTask compile = javac.getTask(new PrintWriter(System.err), fileManager, diagnosticListener, options, classes, compilationUnits);
            boolean res = compile.call();
            Class<?> cls = cl.findClass(className);

            Object instance = cls.newInstance();
            if(instance instanceof Runnable) {
                ((Runnable) instance).run();
            }
        }

        return null;
    }
    class MemorySource extends SimpleJavaFileObject {
        private String src;
        public MemorySource(String name, String src) {
            super(URI.create("file:///" + name + ".java"), Kind.SOURCE);
            this.src = src;
        }
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return src;
        }
        public OutputStream openOutputStream() {
            throw new IllegalStateException();
        }
        public InputStream openInputStream() {
            return new ByteArrayInputStream(src.getBytes());
        }
    }
    class MemoryByteCode extends SimpleJavaFileObject {
        private ByteArrayOutputStream baos;
        public MemoryByteCode(String name) {
            super(URI.create("byte:///" + name + ".class"), JavaFileObject.Kind.CLASS);
        }
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            throw new IllegalStateException();
        }
        public OutputStream openOutputStream() {
            baos = new ByteArrayOutputStream();
            return baos;
        }
        public InputStream openInputStream() {
            throw new IllegalStateException();
        }
        public byte[] getBytes() {
            return baos.toByteArray();
        }
    }
    class SpecialClassLoader extends ClassLoader {
        private Map<String,MemoryByteCode> m = new HashMap<String, MemoryByteCode>();

        protected Class<?> findClass(String name) throws ClassNotFoundException {
            MemoryByteCode mbc = m.get(name);
            if (mbc==null){
                mbc = m.get(name.replace(".","/"));
                if (mbc==null){
                    return super.findClass(name);
                }
            }
            return defineClass(name, mbc.getBytes(), 0, mbc.getBytes().length);
        }

        public void addClass(String name, MemoryByteCode mbc) {
            m.put(name, mbc);
        }
    }

    class SpecialJavaFileManager extends ForwardingJavaFileManager {
        private SpecialClassLoader xcl;
        public SpecialJavaFileManager(StandardJavaFileManager sjfm, SpecialClassLoader xcl) {
            super(sjfm);
            this.xcl = xcl;
        }
        public JavaFileObject getJavaFileForOutput(Location location, String name, JavaFileObject.Kind kind, FileObject sibling) throws IOException {
            MemoryByteCode mbc = new MemoryByteCode(name);
            xcl.addClass(name, mbc);
            return mbc;
        }

        public ClassLoader getClassLoader(Location location) {
            return xcl;
        }
    }

    static String readFile(String path, Charset encoding)
        throws IOException
    {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }
    private String getPackageNameFromJavCode(String javaCode) {
        String packageName = "";

        int index = javaCode.indexOf("package") + 8;

        while (index < javaCode.length()) {
            char charAtIndex = javaCode.charAt(index);
            if(';' == charAtIndex) {
                break;
            } else {
                packageName += charAtIndex;
                index++;
            }
        }


        return packageName;
    }
}
