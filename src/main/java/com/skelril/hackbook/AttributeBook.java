package com.skelril.hackbook;

import net.minecraft.server.v1_7_R1.EntityInsentient;
import net.minecraft.server.v1_7_R1.GenericAttributes;
import net.minecraft.server.v1_7_R1.IAttribute;
import org.bukkit.craftbukkit.v1_7_R1.entity.CraftLivingEntity;
import org.bukkit.entity.LivingEntity;

/**
 * Created by wyatt on 12/28/13.
 */
public class AttributeBook {

    public enum Attribute {

        MAX_HEALTH(GenericAttributes.a),
        FOLLOW_RANGE(GenericAttributes.b),
        KNOCKBACK_RESISTANCE(GenericAttributes.c),
        MOVEMENT_SPEED(GenericAttributes.d),
        ATTACK_DAMAGE(GenericAttributes.e);

        public IAttribute attribute;

        Attribute(IAttribute attribute) {

            this.attribute = attribute;
        }

    }

    public static double getAttribute(LivingEntity entity, Attribute attribute) {

        try {
            EntityInsentient nmsEntity = getNMSEntity(entity);

            return nmsEntity.getAttributeInstance(attribute.attribute).getValue();
        } catch (Exception ex) {
            ex.printStackTrace();
            return -1;
        }
    }

    public static void setAttribute(LivingEntity entity, Attribute attribute, double value) {

        try {
            EntityInsentient nmsEntity = getNMSEntity(entity);

            nmsEntity.getAttributeInstance(attribute.attribute).setValue(value);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static EntityInsentient getNMSEntity(LivingEntity entity) {

        return ((EntityInsentient) ((CraftLivingEntity) entity).getHandle());
    }
}