/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.combat.bosses.instruction;

import com.skelril.OpenBoss.InstructionResult;
import com.skelril.OpenBoss.condition.BindCondition;
import com.skelril.OpenBoss.instruction.BindInstruction;
import org.bukkit.entity.LivingEntity;

public class NamedBindInstruction implements BindInstruction {

    private final InstructionResult<BindInstruction> next;

    private final String name;

    public NamedBindInstruction(String name) {
        this(null, name);
    }

    public NamedBindInstruction(InstructionResult<BindInstruction> next, String name) {
        this.next = next;
        this.name = name;
    }

    @Override
    public InstructionResult<BindInstruction> process(BindCondition condition) {
        LivingEntity anEntity = condition.getBoss().getEntity();
        anEntity.setCustomName(name);
        return next;
    }
}
