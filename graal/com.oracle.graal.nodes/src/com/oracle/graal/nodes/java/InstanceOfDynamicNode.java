/*
 * Copyright (c) 2009, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.graal.nodes.java;

import static com.oracle.graal.nodeinfo.NodeCycles.CYCLES_30;
import static com.oracle.graal.nodeinfo.NodeSize.SIZE_30;

import com.oracle.graal.compiler.common.type.Stamp;
import com.oracle.graal.compiler.common.type.TypeReference;
import com.oracle.graal.graph.NodeClass;
import com.oracle.graal.graph.spi.Canonicalizable;
import com.oracle.graal.graph.spi.CanonicalizerTool;
import com.oracle.graal.nodeinfo.NodeInfo;
import com.oracle.graal.nodes.BinaryOpLogicNode;
import com.oracle.graal.nodes.LogicConstantNode;
import com.oracle.graal.nodes.LogicNode;
import com.oracle.graal.nodes.ValueNode;
import com.oracle.graal.nodes.calc.IsNullNode;
import com.oracle.graal.nodes.spi.Lowerable;
import com.oracle.graal.nodes.spi.LoweringTool;

import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaType;
import jdk.vm.ci.meta.TriState;

/**
 * The {@code InstanceOfDynamicNode} represents a type check where the type being checked is not
 * known at compile time. This is used, for instance, to intrinsify {@link Class#isInstance(Object)}
 * .
 */
@NodeInfo(cycles = CYCLES_30, size = SIZE_30)
public class InstanceOfDynamicNode extends BinaryOpLogicNode implements Canonicalizable.Binary<ValueNode>, Lowerable {
    public static final NodeClass<InstanceOfDynamicNode> TYPE = NodeClass.create(InstanceOfDynamicNode.class);

    private final boolean allowNull;

    public static LogicNode create(Assumptions assumptions, ConstantReflectionProvider constantReflection, ValueNode mirror, ValueNode object, boolean allowNull) {
        LogicNode synonym = findSynonym(assumptions, constantReflection, mirror, object, allowNull);
        if (synonym != null) {
            return synonym;
        }
        return new InstanceOfDynamicNode(mirror, object, allowNull);
    }

    protected InstanceOfDynamicNode(ValueNode mirror, ValueNode object, boolean allowNull) {
        super(TYPE, mirror, object);
        this.allowNull = allowNull;
        assert mirror.getStackKind() == JavaKind.Object || mirror.getStackKind() == JavaKind.Illegal : mirror.getStackKind();
    }

    public boolean isMirror() {
        return getMirrorOrHub().getStackKind() == JavaKind.Object;
    }

    public boolean isHub() {
        return !isMirror();
    }

    @Override
    public void lower(LoweringTool tool) {
        tool.getLowerer().lower(this, tool);
    }

    private static LogicNode findSynonym(Assumptions assumptions, ConstantReflectionProvider constantReflection, ValueNode forMirror, ValueNode forObject,
                    boolean allowNull) {
        if (forMirror.isConstant()) {
            ResolvedJavaType t = constantReflection.asJavaType(forMirror.asConstant());
            if (t != null) {
                if (t.isPrimitive()) {
                    if (allowNull) {
                        return IsNullNode.create(forObject);
                    } else {
                        return LogicConstantNode.contradiction();
                    }
                } else {
                    TypeReference type = TypeReference.createTrusted(assumptions, t);
                    if (allowNull) {
                        return InstanceOfNode.createAllowNull(type, forObject, null, null);
                    } else {
                        return InstanceOfNode.create(type, forObject);
                    }
                }
            }
        }
        return null;
    }

    public ValueNode getMirrorOrHub() {
        return this.getX();
    }

    public ValueNode getObject() {
        return this.getY();
    }

    @Override
    public LogicNode canonical(CanonicalizerTool tool, ValueNode forMirror, ValueNode forObject) {
        LogicNode result = findSynonym(tool.getAssumptions(), tool.getConstantReflection(), forMirror, forObject, allowNull);
        if (result != null) {
            return result;
        }
        return this;
    }

    public void setMirror(ValueNode newObject) {
        this.updateUsages(x, newObject);
        this.x = newObject;
    }

    public boolean allowsNull() {
        return allowNull;
    }

    @Override
    public Stamp getSucceedingStampForX(boolean negated) {
        return null;
    }

    @Override
    public Stamp getSucceedingStampForY(boolean negated) {
        return null;
    }

    @Override
    public TriState tryFold(Stamp xStamp, Stamp yStamp) {
        return TriState.UNKNOWN;
    }
}
