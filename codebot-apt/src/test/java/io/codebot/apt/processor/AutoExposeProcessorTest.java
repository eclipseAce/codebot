package io.codebot.apt.processor;

import com.google.common.io.CharStreams;
import com.karuslabs.elementary.Results;
import com.karuslabs.elementary.junit.JavacExtension;
import com.karuslabs.elementary.junit.annotations.Classpath;
import com.karuslabs.elementary.junit.annotations.Processors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.tools.JavaFileObject;
import java.io.IOException;

@ExtendWith(JavacExtension.class)
@Processors(AutoExposeProcessor.class)
@Classpath("cases.UserService")
class AutoExposeProcessorTest {
    @Test
    void compile(Results results) throws IOException {
        for (JavaFileObject generated : results.generated) {
            System.out.println(CharStreams.toString(generated.openReader(true)));
        }
    }
}