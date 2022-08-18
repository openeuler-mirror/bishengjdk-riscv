/*
 * Copyright (c) 2017, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */


/*
 * @test
 * @bug 8146115 8292083
 * @key cgroups
 * @summary Test JVM's memory resource awareness when running inside docker container
 * @requires docker.support
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.base/jdk.internal.platform
 *          java.management
 *          jdk.jartool/sun.tools.jar
 * @build AttemptOOM jdk.test.whitebox.WhiteBox PrintContainerInfo CheckOperatingSystemMXBean
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar whitebox.jar jdk.test.whitebox.WhiteBox
 * @run driver TestMemoryAwareness
 */
import jdk.test.lib.containers.docker.Common;
import jdk.test.lib.containers.docker.DockerRunOptions;
import jdk.test.lib.containers.docker.DockerTestUtils;
import jdk.test.lib.process.OutputAnalyzer;

import java.lang.management.ManagementFactory;

public class TestMemoryAwareness {
    private static final String imageName = Common.imageName("memory");

    public static void main(String[] args) throws Exception {
        if (!DockerTestUtils.canTestDocker()) {
            return;
        }

        Common.prepareWhiteBox();
        DockerTestUtils.buildJdkContainerImage(imageName);

        try {
            testMemoryLimit("100m", "104857600");
            testMemoryLimit("500m", "524288000");
            testMemoryLimit("1g", "1073741824");
            testMemoryLimit("4g", "4294967296");

            testMemorySoftLimit("500m", "524288000");
            testMemorySoftLimit("1g", "1073741824");

            // Add extra 10 Mb to allocator limit, to be sure to cause OOM
            testOOM("256m", 256 + 10);

            testOperatingSystemMXBeanAwareness(
                "100M", Integer.toString(((int) Math.pow(2, 20)) * 100),
                "150M", Integer.toString(((int) Math.pow(2, 20)) * (150 - 100))
            );
            testOperatingSystemMXBeanAwareness(
                "128M", Integer.toString(((int) Math.pow(2, 20)) * 128),
                "256M", Integer.toString(((int) Math.pow(2, 20)) * (256 - 128))
            );
            testOperatingSystemMXBeanAwareness(
                "1G", Integer.toString(((int) Math.pow(2, 20)) * 1024),
                "1500M", Integer.toString(((int) Math.pow(2, 20)) * (1500 - 1024))
            );
            testContainerMemExceedsPhysical();
        } finally {
            if (!DockerTestUtils.RETAIN_IMAGE_AFTER_TEST) {
                DockerTestUtils.removeDockerImage(imageName);
            }
        }
    }


    private static void testMemoryLimit(String valueToSet, String expectedTraceValue)
            throws Exception {

        Common.logNewTestCase("memory limit: " + valueToSet);

        DockerRunOptions opts = Common.newOpts(imageName)
            .addDockerOpts("--memory", valueToSet);

        Common.run(opts)
            .shouldMatch("Memory Limit is:.*" + expectedTraceValue);
    }

    // JDK-8292083
    // Ensure that Java ignores container memory limit values above the host's physical memory.
    //
    // let the host's physical memory be P; request 2P memory in the container.
    // set java's InitialRAMPercentage to 25%, and check the calculated InitialHeapSize
    // to see if that was calculated relative to P (P/4) or 2P (P/2).
    private static void testContainerMemExceedsPhysical()
            throws Exception {

        com.sun.management.OperatingSystemMXBean os = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        long realMem = os.getTotalMemorySize();
        long badMem = 2 * realMem;

        Common.logNewTestCase("bad memory limit: " + badMem);

        DockerRunOptions opts = Common.newOpts(imageName);
        opts.addDockerOpts("--memory", Long.toString(badMem));
        opts.addJavaOpts("-XX:InitialRAMPercentage=25.0");
        opts.addJavaOpts("-XX:+PrintFlagsFinal");

        // one significant digit, to avoid precision/rounding issues
        String goodDigit = Long.toString(realMem/4).substring(0,1);
        Common.run(opts)
            .shouldMatch("size_t InitialHeapSize.*= "+goodDigit);
    }


    private static void testMemorySoftLimit(String valueToSet, String expectedTraceValue)
            throws Exception {
        Common.logNewTestCase("memory soft limit: " + valueToSet);

        DockerRunOptions opts = Common.newOpts(imageName, "PrintContainerInfo");
        Common.addWhiteBoxOpts(opts);
        opts.addDockerOpts("--memory-reservation=" + valueToSet);

        Common.run(opts)
            .shouldMatch("Memory Soft Limit.*" + expectedTraceValue);
    }


    // provoke OOM inside the container, see how VM reacts
    private static void testOOM(String dockerMemLimit, int sizeToAllocInMb) throws Exception {
        Common.logNewTestCase("OOM");

        DockerRunOptions opts = Common.newOpts(imageName, "AttemptOOM")
            .addDockerOpts("--memory", dockerMemLimit, "--memory-swap", dockerMemLimit);
        opts.classParams.add("" + sizeToAllocInMb);

        // make sure we avoid inherited Xmx settings from the jtreg vmoptions
        // set Xmx ourselves instead
        System.out.println("sizeToAllocInMb is:" + sizeToAllocInMb + " sizeToAllocInMb/2 is:" + sizeToAllocInMb/2);
        String javaHeapSize = sizeToAllocInMb/2 + "m";
        opts.addJavaOptsAppended("-Xmx" + javaHeapSize);

        OutputAnalyzer out = DockerTestUtils.dockerRunJava(opts);

        if (out.getExitValue() == 0) {
            throw new RuntimeException("We exited successfully, but we wanted to provoke an OOM inside the container");
        }

        out.shouldContain("Entering AttemptOOM main")
           .shouldNotContain("AttemptOOM allocation successful")
           .shouldContain("java.lang.OutOfMemoryError");
    }

    private static void testOperatingSystemMXBeanAwareness(String memoryAllocation, String expectedMemory,
            String swapAllocation, String expectedSwap) throws Exception {
        Common.logNewTestCase("Check OperatingSystemMXBean");

        DockerRunOptions opts = Common.newOpts(imageName, "CheckOperatingSystemMXBean")
            .addDockerOpts(
                "--memory", memoryAllocation,
                "--memory-swap", swapAllocation
            )
            // CheckOperatingSystemMXBean uses Metrics (jdk.internal.platform) for
            // diagnostics
            .addJavaOpts("--add-exports")
            .addJavaOpts("java.base/jdk.internal.platform=ALL-UNNAMED");

        OutputAnalyzer out = DockerTestUtils.dockerRunJava(opts);
        out.shouldHaveExitValue(0)
           .shouldContain("Checking OperatingSystemMXBean")
           .shouldContain("OperatingSystemMXBean.getTotalPhysicalMemorySize: " + expectedMemory)
           .shouldContain("OperatingSystemMXBean.getTotalMemorySize: " + expectedMemory)
           .shouldMatch("OperatingSystemMXBean\\.getFreeMemorySize: [1-9][0-9]+")
           .shouldMatch("OperatingSystemMXBean\\.getFreePhysicalMemorySize: [1-9][0-9]+");

        // in case of warnings like : "Your kernel does not support swap limit capabilities
        // or the cgroup is not mounted. Memory limited without swap."
        // the getTotalSwapSpaceSize and getFreeSwapSpaceSize return the system
        // values as the container setup isn't supported in that case.
        try {
            out.shouldContain("OperatingSystemMXBean.getTotalSwapSpaceSize: " + expectedSwap);
        } catch(RuntimeException ex) {
            out.shouldMatch("OperatingSystemMXBean.getTotalSwapSpaceSize: [0-9]+");
        }

        try {
            out.shouldMatch("OperatingSystemMXBean\\.getFreeSwapSpaceSize: [1-9][0-9]+");
        } catch(RuntimeException ex) {
            out.shouldMatch("OperatingSystemMXBean\\.getFreeSwapSpaceSize: 0");
        }
    }

}
