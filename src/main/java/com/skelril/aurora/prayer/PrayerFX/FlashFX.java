package com.skelril.aurora.prayer.PrayerFX;

import com.skelril.aurora.prayer.PrayerType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Author: Turtle9598
 */
public class FlashFX extends AbstractPrayer {

    private static final AbstractPrayer[] subFX = new AbstractPrayer[] {
            new InfiniteHungerFX()
    };
    private static final PotionEffect effect = new PotionEffect(PotionEffectType.SPEED, 20 * 600, 6);

    public FlashFX() {

        super(subFX, effect);
    }

    @Override
    public PrayerType getType() {

        return PrayerType.FLASH;
    }
}