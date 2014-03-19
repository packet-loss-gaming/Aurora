/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.items.specialattack.attacks.melee.unleashed;

import com.skelril.aurora.events.anticheat.RapidHitEvent;
import com.skelril.aurora.items.specialattack.EntityAttack;
import com.skelril.aurora.items.specialattack.attacks.melee.MeleeSpecial;
import com.skelril.aurora.util.DamageUtil;
import org.bukkit.Effect;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class HealingLight extends EntityAttack implements MeleeSpecial {

    public HealingLight(LivingEntity owner, LivingEntity target) {
        super(owner, target);
    }

    @Override
    public void activate() {

        if (owner instanceof Player) {
            server.getPluginManager().callEvent(new RapidHitEvent((Player) owner));
        }

        owner.setHealth(Math.min(owner.getMaxHealth(), owner.getHealth() + 5));
        for (int i = 0; i < 4; i++) {
            target.getWorld().playEffect(target.getLocation(), Effect.MOBSPAWNER_FLAMES, 0);
        }

        DamageUtil.damage(owner, target, 20);
        inform("Your weapon glows dimly.");
    }
}
