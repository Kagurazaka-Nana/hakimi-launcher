package com.minecraft.launcher.model.version.arguments.jvm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minecraft.launcher.model.version.arguments.Arguments;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JvmDeserializerTest {

    Arguments args;
    List<Jvm> j;

    {
        try {
            String testJson = """
                    {"jvm": [
                       {
                         "rules": [
                           {
                             "action": "allow",
                             "os": {
                               "name": "osx"
                             }
                           }
                         ],
                         "value": [
                           "-XstartOnFirstThread"
                         ]
                       },
                       {
                         "rules": [
                           {
                             "action": "allow",
                             "os": {
                               "name": "windows"
                             }
                           }
                         ],
                         "value": "-XX:HeapDumpPath=MojangTricksIntelDriversForPerformance_javaw.exe_minecraft.exe.heapdump"
                       },
                       {
                         "rules": [
                           {
                             "action": "allow",
                             "os": {
                               "arch": "x86"
                             }
                           }
                         ],
                         "value": "-Xss1M"
                       },
                       "--sun-misc-unsafe-memory-access=allow",
                       "--enable-native-access=ALL-UNNAMED",
                       "-Djava.library.path=${natives_directory}/java",
                       "-Djna.tmpdir=${natives_directory}/jna",
                       "-Dorg.lwjgl.system.SharedLibraryExtractPath=${natives_directory}/lwjgl",
                       "-Dio.netty.native.workdir=${natives_directory}/netty",
                       "-Dminecraft.launcher.brand=${launcher_name}",
                       "-Dminecraft.launcher.version=${launcher_version}",
                       "-cp",
                       "${classpath}"
                     ]}
                    """;
            args = new ObjectMapper().readValue(testJson, Arguments.class);
            j = args.getJvm();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testArrayValueWithOsRule() {
        // index 0：数组 value + os:name=osx 规则
        Jvm osx = j.get(0);
        assertFalse(osx.getRules().isEmpty());
        assertInstanceOf(List.class, osx.getValue());
        assertEquals(1, osx.getValue().size());
        assertEquals(List.of("-XstartOnFirstThread"), osx.getValue());
    }

    @Test
    void testSingleValueWithOsRule() {
        // index 1：单 value + os:name=windows 规则
        Jvm heap = j.get(1);
        assertFalse(heap.getRules().isEmpty());
        assertEquals(1, heap.getValue().size());
        assertEquals("-XX:HeapDumpPath=MojangTricksIntelDriversForPerformance_javaw.exe_minecraft.exe.heapdump",
                heap.getValue().getFirst());
    }

    @Test
    void testArchRuleValue() {
        // index 2：单 value + os:arch=x86 规则
        Jvm xss = j.get(2);
        assertFalse(xss.getRules().isEmpty());
        assertEquals(1, xss.getValue().size());
        assertEquals("-Xss1M", xss.getValue().getFirst());
    }

    @Test
    void testPlainStringJvmArgs() {
        // index 3+：纯字符串参数统一收口为单元素 List<String>，且 rules 为 null
        Jvm unsafe = j.get(3);
        assertNull(unsafe.getRules());
        assertEquals(List.of("--sun-misc-unsafe-memory-access=allow"), unsafe.getValue());

        Jvm nativeAccess = j.get(4);
        assertNull(nativeAccess.getRules());
        assertEquals(List.of("--enable-native-access=ALL-UNNAMED"), nativeAccess.getValue());
    }

    @Test
    void testPlainStringWithToken() {
        // index 6：带 ${...} token 的纯字符串参数
        Jvm jnaTmpdir = j.get(6);
        assertNull(jnaTmpdir.getRules());
        assertEquals(1, jnaTmpdir.getValue().size());
        assertEquals("-Djna.tmpdir=${natives_directory}/jna",
                jnaTmpdir.getValue().getFirst());
    }

    @Test
    void testPlainPlaceholderArgs() {
        // 末尾的 -cp 与 ${classpath} 两个纯字符串
        Jvm cp = j.get(11);
        assertNull(cp.getRules());
        assertEquals(List.of("-cp"), cp.getValue());

        Jvm classpath = j.get(12);
        assertNull(classpath.getRules());
        assertEquals(List.of("${classpath}"), classpath.getValue());
    }

}
