/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * This file is part of Aurora.
 *
 * Aurora is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Aurora is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with Aurora.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.skelril.aurora.shard.instance.Catacombs.instruction;

import com.skelril.OpenBoss.EntityDetail;
import com.skelril.aurora.combat.bosses.instruction.CalculatedHealthInstruction;
import com.skelril.aurora.shard.instance.Catacombs.CatacombEntityDetail;

public class CatacombsHealthInstruction extends CalculatedHealthInstruction {

    private final int baseHP;

    public CatacombsHealthInstruction(int baseHP) {
        this.baseHP = baseHP;
    }

    @Override
    public double getHealth(EntityDetail detail) {
        return CatacombEntityDetail.getFrom(detail).getWave() * baseHP;
    }
}