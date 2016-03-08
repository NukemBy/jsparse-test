package test.jsparse;

import com.google.common.collect.ImmutableList;
import java.io.File;
import java.net.URL;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.openjdk.jmh.annotations.Benchmark;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerInput;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.SourceFile;
import com.google.javascript.rhino.InputId;
import com.google.javascript.rhino.Node;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class MyBenchmark {

    static String js5script;

    static String js6script;
    
    @Benchmark
    public void testGccJS5() {
        CompilerOptions options = new CompilerOptions();
        
        options.ideMode = true;
        options.recordFunctionInformation = true;
        options.setParseJsDocDocumentation(true);
        options.setPreserveJsDocWhitespace(true);
        
        Compiler compiler = new Compiler();
        
        compiler.initOptions(options);
        options.setLanguageIn(CompilerOptions.LanguageMode.ECMASCRIPT5);

        List<SourceFile> input = ImmutableList.of(SourceFile.fromCode("js5script.js", js5script));
        
        compiler.compile(Collections.emptyList(), input, options);
        
        JSError errors[] = compiler.getErrors();
        JSError warnings[] = compiler.getWarnings();
        CompilerInput ci = compiler.getInput(new InputId("js5script.js"));
        Node root = ci.getAstRoot(compiler);
        
        root.getJSType();
    }

    @Benchmark
    public void testGccES6() {
        CompilerOptions options = new CompilerOptions();
        
        options.ideMode = true;
        options.recordFunctionInformation = true;
        options.setParseJsDocDocumentation(true);
        options.setPreserveJsDocWhitespace(true);
        
        Compiler compiler = new Compiler();
        
        compiler.initOptions(options);
        options.setLanguageIn(CompilerOptions.LanguageMode.ECMASCRIPT6);
        options.setLanguageOut(CompilerOptions.LanguageMode.ECMASCRIPT5);

        List<SourceFile> input = ImmutableList.of(SourceFile.fromCode("es6script.js", js5script));
        
        compiler.compile(ES6_COMPILER_EXTERNS, input, options);
        
        JSError errors[] = compiler.getErrors();
        JSError warnings[] = compiler.getWarnings();
        CompilerInput ci = compiler.getInput(new InputId("es6script.js"));
        Node root = ci.getAstRoot(compiler);
        
        root.getJSType();
    }

    static void collectTestData() throws Exception {
        js5script = getTestFile("less-rhino-1.7.5.js",
                                "https://raw.githubusercontent.com/less/less.js/v1.7.5/dist/less-rhino-1.7.5.js");
    }

    static String getTestFile(String fileName, String url) throws Exception {
        File localPath = new File("./.tmp/" + fileName);
        String content;

        if( localPath.exists() ) {
            content = FileUtils.readFileToString(localPath);
        }
        else {
            content = IOUtils.toString(new URL(url).openStream());
            FileUtils.writeStringToFile(localPath, content);
        }

        return content;
    }

    private final static List<SourceFile> ES5_COMPILER_EXTERNS = new ArrayList<>(10);
    private final static List<SourceFile> ES6_COMPILER_EXTERNS = new ArrayList<>(10);
    
    static {
        try {
            collectTestData();
            initExterns();
        }
        catch( Exception ex ) {
            throw new RuntimeException(ex);
        }
    }

    static void initExterns() throws Exception {
        Set<String> neededExterns = new LinkedHashSet<>();
        Map<String, String> externs = new HashMap<>();
        
        neededExterns.add("es3.js");
        neededExterns.add("es5.js");
        neededExterns.add("es6.js");
        
        try( InputStream rsrcStream = MyBenchmark.class.getClassLoader().getResourceAsStream("externs.zip") ) {
            try( ZipInputStream zip = new ZipInputStream(rsrcStream) ) {
                ZipEntry ze;

                while( ( ze = zip.getNextEntry() ) != null ) {
                    File file = new File(ze.getName());
                    String fn = file.getName();

                    if( neededExterns.contains(fn) ) {
                        externs.put(fn, IOUtils.toString(zip, "UTF-8"));
                    }
                }                
            }
        }

        ES5_COMPILER_EXTERNS.add(SourceFile.fromCode("none.js", ""));
        
        for( String neededExtern : neededExterns ) {
            ES6_COMPILER_EXTERNS.add(SourceFile.fromCode(neededExtern, externs.get(neededExtern)));
        }
    }
}
