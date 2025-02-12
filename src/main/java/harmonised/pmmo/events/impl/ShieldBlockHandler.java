package harmonised.pmmo.events.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import harmonised.pmmo.api.APIUtils;
import harmonised.pmmo.api.enums.EventType;
import harmonised.pmmo.api.enums.ReqType;
import harmonised.pmmo.core.Core;
import harmonised.pmmo.features.party.PartyUtils;
import harmonised.pmmo.util.Messenger;
import harmonised.pmmo.util.TagUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;

public class ShieldBlockHandler {

	@SuppressWarnings("resource")
	public static void handle(ShieldBlockEvent event) {
		if (!(event.getEntity() instanceof Player)) return;
		if (event.getDamageSource().getEntity() == null) return;
		
		Player player = (Player) event.getEntity();
		Core core = Core.get(player.level());
		ItemStack shield = player.getUseItem();
		Entity attacker = event.getDamageSource().getEntity();

		if (!core.isActionPermitted(ReqType.WEAPON, shield, player)) {
			event.setCanceled(true);
			Messenger.sendDenialMsg(ReqType.WEAPON, player, shield.getDisplayName());
			return;
		}
		boolean serverSide = !player.level().isClientSide;
		CompoundTag hookOutput = new CompoundTag();
		if (serverSide) {
			hookOutput = core.getEventTriggerRegistry().executeEventListeners(EventType.SHIELD_BLOCK, event, new CompoundTag());
			if (hookOutput.getBoolean(APIUtils.IS_CANCELLED)) {
				event.setCanceled(true);
				return;
			}
		}
		//Process perks
		hookOutput.putFloat(APIUtils.DAMAGE_IN, event.getBlockedDamage());
		hookOutput = TagUtils.mergeTags(hookOutput, core.getPerkRegistry().executePerk(EventType.SHIELD_BLOCK, player, hookOutput));
		if (serverSide) {
			Map<String, Long> xpAward = new HashMap<>();
			core.getExperienceAwards(EventType.SHIELD_BLOCK, attacker, player, hookOutput).forEach((skill, value) -> {
				xpAward.put(skill, (long) (value * event.getBlockedDamage()));
			});;			
			List<ServerPlayer> partyMembersInRange = PartyUtils.getPartyMembersInRange((ServerPlayer) player);
			core.awardXP(partyMembersInRange, xpAward);
		}
	}
}
