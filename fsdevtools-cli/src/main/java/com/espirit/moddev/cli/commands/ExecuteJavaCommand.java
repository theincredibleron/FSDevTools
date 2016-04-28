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

import org.mdkt.compiler.InMemoryJavaCompiler;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

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
            className = packageName + "." + className;
            Class<?> cls = InMemoryJavaCompiler.compile(className, source);

            Object instance = cls.newInstance();
            if(instance instanceof Runnable) {
                ((Runnable) instance).run();
            }
        }

        return null;
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
