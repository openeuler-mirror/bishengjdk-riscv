/*
 * Copyright (C) 2022 THL A29 Limited, a Tencent company. All rights reserved.
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

package sun.management;

import jdk.internal.platform.Container;
import jdk.internal.platform.Metrics;

import javax.management.ObjectName;
import java.lang.management.ContainerMXBean;
import java.lang.management.ManagementFactory;

public class ContainerImpl implements ContainerMXBean {
    final Metrics containerMetrics = Container.metrics();

    @Override
    public String getProvider() {
        return containerMetrics.getProvider();
    }

    @Override
    public long getCpuPeriod() {
        return containerMetrics.getCpuPeriod();
    }

    @Override
    public long getCpuQuota() {
        return containerMetrics.getCpuQuota();
    }

    @Override
    public long getCpuShares() {
        return 0;
    }

    @Override
    public long getEffectiveCpuCount() {
        return containerMetrics.getEffectiveCpuCount();
    }

    @Override
    public long getMemorySoftLimit() {
        return containerMetrics.getMemorySoftLimit();
    }

    @Override
    public long getMemoryLimit() {
        return containerMetrics.getMemoryLimit();
    }

    @Override
    public long getMemoryAndSwapLimit() {
        return containerMetrics.getMemoryAndSwapLimit();
    }

    @Override
    public ObjectName getObjectName() {
        return Util.newObjectName(ManagementFactory.CONTAINER_MXBEAN_NAME);
    }
}
