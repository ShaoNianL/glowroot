/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.glowroot.agent.tests.javaagent;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.glowroot.agent.it.harness.AppUnderTest;
import org.glowroot.agent.it.harness.Container;
import org.glowroot.agent.it.harness.impl.JavaagentContainer;
import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig.InstrumentationConfig;
import org.glowroot.wire.api.model.AgentConfigOuterClass.AgentConfig.InstrumentationConfig.CaptureKind;

public class AnalyzedClassPlanBeePlusIT {

    private static Container container;

    @BeforeAll
    public static void setUp() throws Exception {
        container = JavaagentContainer.create();
    }

    @AfterAll
    public static void tearDown() throws Exception {
        container.close();
    }

    @AfterEach
    public void afterEachTest() throws Exception {
        container.checkAndReset();
    }

    @Test
    public void shouldLogWarningInAnalyzedWorldPlanB() throws Exception {
        // given
        updateInstrumentationConfigs();
        container.addExpectedLogMessage("org.glowroot.agent.weaving.AnalyzedWorld",
                AnalyzedClassPlanBeePlusIT.class.getName()
                        + "$Y was not woven with requested advice");
        // when
        container.executeNoExpectedTrace(ShouldLogWarningInAnalyzedWorldPlanB.class);
        // then
        // container close will validate that there were no unexpected warnings or errors
    }

    private static void updateInstrumentationConfigs() throws Exception {
        InstrumentationConfig config = InstrumentationConfig.newBuilder()
                .setClassName(AnalyzedClassPlanBeePlusIT.class.getName() + "$Y")
                .setMethodName("y")
                .setMethodReturnType("")
                .setCaptureKind(CaptureKind.TIMER)
                .setTimerName("y")
                .build();
        container.getConfigService().updateInstrumentationConfigs(ImmutableList.of(config));
    }

    public static class ShouldLogWarningInAnalyzedWorldPlanB implements AppUnderTest {
        @Override
        public void executeApp() throws Exception {
            Class.forName(AnalyzedClassPlanBeePlusIT.class.getName() + "$Z", true,
                    new DelegatingClassLoader2());
        }
    }

    public static class DelegatingClassLoader extends ClassLoader {
        @Override
        protected synchronized Class<?> loadClass(String name, boolean resolve)
                throws ClassNotFoundException {

            if (name.equals(Z.class.getName())) {
                try {
                    return load(name);
                } catch (IOException e) {
                    throw new ClassNotFoundException("Error loading class", e);
                }
            } else {
                return Class.forName(name, resolve, DelegatingClassLoader.class.getClassLoader());
            }
        }
        protected Class<?> load(String name) throws IOException {
            byte[] bytes =
                    Resources.toByteArray(Resources.getResource(name.replace('.', '/') + ".class"));
            return super.defineClass(name, bytes, 0, bytes.length);
        }
        @Override
        public URL getResource(String name) {
            // don't load .class files as resources
            return null;
        }
        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            // don't load .class files as resources
            return Collections.enumeration(Collections.<URL>emptyList());
        }
    }

    public static class DelegatingClassLoader2 extends DelegatingClassLoader {
        @Override
        protected synchronized Class<?> loadClass(String name, boolean resolve)
                throws ClassNotFoundException {

            if (name.equals(AnalyzedClassPlanBeePlusIT.class.getName() + "$Y")
                    || name.equals(AnalyzedClassPlanBeePlusIT.class.getName() + "$Z")) {
                try {
                    return load(name);
                } catch (IOException e) {
                    throw new ClassNotFoundException("Error loading class", e);
                }
            } else {
                return Class.forName(name, resolve, DelegatingClassLoader.class.getClassLoader());
            }
        }
    }

    public static class Y {
        public void y() {}
    }

    public static class Z extends Y {}
}
