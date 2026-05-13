package com.supremecrafting.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Shared helper that takes a vanilla tool's attribute set and tacks on +97
 * to entity / block interaction range — yielding an effective ~100-block reach.
 *
 * <p>Each Supreme<Tool> class supplies its own {@code ResourceLocation} ids for
 * the modifiers (so the attribute system can dedupe them properly per item).
 */
public final class SupremeToolAttributes {
    /** Bonus added to {@code ENTITY_INTERACTION_RANGE} and {@code BLOCK_INTERACTION_RANGE}. */
    public static final double REACH_BONUS = 97.0;

    private SupremeToolAttributes() {}

    public static ItemAttributeModifiers withReach(ItemAttributeModifiers base,
                                                   ResourceLocation entityReachId,
                                                   ResourceLocation blockReachId) {
        ItemAttributeModifiers.Builder b = ItemAttributeModifiers.builder();
        for (var entry : base.modifiers()) {
            b.add(entry.attribute(), entry.modifier(), entry.slot());
        }
        b.add(Attributes.ENTITY_INTERACTION_RANGE,
                new AttributeModifier(entityReachId, REACH_BONUS, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND);
        b.add(Attributes.BLOCK_INTERACTION_RANGE,
                new AttributeModifier(blockReachId, REACH_BONUS, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND);
        return b.build();
    }
}
