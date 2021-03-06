/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.graal.lir.alloc.lsra.ssa;

import com.oracle.graal.compiler.common.alloc.RegisterAllocationConfig;
import com.oracle.graal.compiler.common.cfg.AbstractBlockBase;
import com.oracle.graal.debug.Debug;
import com.oracle.graal.debug.Debug.Scope;
import com.oracle.graal.lir.alloc.lsra.LinearScan;
import com.oracle.graal.lir.alloc.lsra.LinearScanEliminateSpillMovePhase;
import com.oracle.graal.lir.alloc.lsra.LinearScanLifetimeAnalysisPhase;
import com.oracle.graal.lir.alloc.lsra.LinearScanResolveDataFlowPhase;
import com.oracle.graal.lir.alloc.lsra.MoveResolver;
import com.oracle.graal.lir.gen.LIRGenerationResult;
import com.oracle.graal.lir.gen.LIRGeneratorTool.MoveFactory;
import com.oracle.graal.lir.ssa.SSAUtil;

import jdk.vm.ci.code.TargetDescription;

public final class SSALinearScan extends LinearScan {

    public SSALinearScan(TargetDescription target, LIRGenerationResult res, MoveFactory spillMoveFactory, RegisterAllocationConfig regAllocConfig, AbstractBlockBase<?>[] sortedBlocks,
                    boolean neverSpillConstants) {
        super(target, res, spillMoveFactory, regAllocConfig, sortedBlocks, neverSpillConstants);
    }

    @Override
    protected MoveResolver createMoveResolver() {
        SSAMoveResolver moveResolver = new SSAMoveResolver(this);
        assert moveResolver.checkEmpty();
        return moveResolver;
    }

    @Override
    protected LinearScanLifetimeAnalysisPhase createLifetimeAnalysisPhase() {
        return new SSALinearScanLifetimeAnalysisPhase(this);
    }

    @Override
    protected LinearScanResolveDataFlowPhase createResolveDataFlowPhase() {
        return new SSALinarScanResolveDataFlowPhase(this);
    }

    @Override
    protected LinearScanEliminateSpillMovePhase createSpillMoveEliminationPhase() {
        return new SSALinearScanEliminateSpillMovePhase(this);
    }

    @Override
    @SuppressWarnings("try")
    protected void beforeSpillMoveElimination() {
        /*
         * PHI Ins are needed for the RegisterVerifier, otherwise PHIs where the Out and In value
         * matches (ie. there is no resolution move) are falsely detected as errors.
         */
        try (Scope s1 = Debug.scope("Remove Phi In")) {
            for (AbstractBlockBase<?> toBlock : sortedBlocks()) {
                if (toBlock.getPredecessorCount() > 1) {
                    SSAUtil.removePhiIn(getLIR(), toBlock);
                }
            }
        }
    }

}
