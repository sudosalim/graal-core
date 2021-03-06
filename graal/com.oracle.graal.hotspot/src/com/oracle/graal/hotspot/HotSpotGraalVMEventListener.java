/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.graal.hotspot;

import java.util.ArrayList;

import com.oracle.graal.code.CompilationResult;
import com.oracle.graal.debug.Debug;
import com.oracle.graal.debug.GraalDebugConfig;
import com.oracle.graal.serviceprovider.ServiceProvider;

import jdk.vm.ci.code.CompiledCode;
import jdk.vm.ci.code.InstalledCode;
import jdk.vm.ci.hotspot.HotSpotCodeCacheProvider;
import jdk.vm.ci.hotspot.services.HotSpotVMEventListener;

@ServiceProvider(HotSpotVMEventListener.class)
public class HotSpotGraalVMEventListener extends HotSpotVMEventListener {

    private static final ArrayList<HotSpotGraalRuntime> runtimes = new ArrayList<>();

    static void addRuntime(HotSpotGraalRuntime runtime) {
        runtimes.add(runtime);
    }

    @Override
    public void notifyShutdown() {
        for (HotSpotGraalRuntime runtime : runtimes) {
            runtime.shutdown();
        }
    }

    @Override
    public void notifyInstall(HotSpotCodeCacheProvider codeCache, InstalledCode installedCode, CompiledCode compiledCode) {
        if (Debug.isDumpEnabled(Debug.BASIC_LOG_LEVEL)) {
            CompilationResult compResult = Debug.contextLookup(CompilationResult.class);
            assert compResult != null : "can't dump installed code properly without CompilationResult";
            Debug.dump(Debug.BASIC_LOG_LEVEL, installedCode, "After code installation");
        }
        if (Debug.isLogEnabled()) {
            Debug.log("%s", codeCache.disassemble(installedCode));
        }
    }

    @Override
    public void notifyBootstrapFinished() {
        for (HotSpotGraalRuntime runtime : runtimes) {
            runtime.notifyBootstrapFinished();
            if (GraalDebugConfig.Options.ClearMetricsAfterBootstrap.getValue()) {
                runtime.clearMeters();
            }
        }
    }
}
