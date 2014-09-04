package pl.asie.computronics.util;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import gregtech.api.enums.ItemList;
import mods.railcraft.common.carts.EntityLocomotiveElectric;
import mods.railcraft.common.carts.EnumCart;
import mods.railcraft.common.carts.ItemLocomotive;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.Achievement;
import net.minecraftforge.common.AchievementPage;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.item.ItemExpireEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import pl.asie.computronics.Computronics;
import pl.asie.computronics.tile.TileLocomotiveRelay;

import java.util.HashMap;

/**
 * @author Vexatos
 */
public class ComputronicsAchievements {
	public HashMap<String, Achievement> achievementMap = new HashMap<String, Achievement>();
	private HashMap<Number, String> playerMap = new HashMap<Number, String>();
	private int playerIndex = 0;

	private enum EnumAchievements {

		Tape("gotTape"),
		Tape_Star("gotStarTape"),
		Tape_IG("gotIGTape"),
		Tape_IG_Dropped("droppedIGTape"),
		Locomotive("gotLoco"),
		Relay("gotRelay");

		String key;

		private EnumAchievements(String key) {
			this.key = key;
		}

		private String getKey() {
			return this.key;
		}

	}

	public ComputronicsAchievements() {

		initializeAchievements();

		AchievementPage.registerAchievementPage(new AchievementPage("Computronics", (Achievement[]) this.achievementMap.values().toArray(new Achievement[this.achievementMap.size()])));

		MinecraftForge.EVENT_BUS.register(this);
		FMLCommonHandler.instance().bus().register(this);
	}

	private void initializeAchievements() {
		this.registerAchievement(EnumAchievements.Tape, 0, 0, new ItemStack(Computronics.itemTape, 1, 0), null, false, true);
		this.registerAchievement(EnumAchievements.Tape_Star, 4, 0, new ItemStack(Computronics.itemTape, 1, 8), this.getAchievement(EnumAchievements.Tape), false, false);

		if(Loader.isModLoaded("gregtech")) {
			this.registerAchievement(EnumAchievements.Tape_IG, 8, 2, new ItemStack(Computronics.itemTape, 1, 9), this.getAchievement(EnumAchievements.Tape_Star), true, false);
			this.registerAchievement(EnumAchievements.Tape_IG_Dropped, 8, 10, ItemList.IC2_Scrap.get(1), this.getAchievement(EnumAchievements.Tape_IG), true, false);
		}

		if(Loader.isModLoaded("Railcraft")) {
			this.registerAchievement(EnumAchievements.Locomotive, 0, 4, EnumCart.LOCO_ELECTRIC.getCartItem(), null, false, true);
			this.registerAchievement(EnumAchievements.Relay, 2, 6, new ItemStack(Computronics.relaySensor), this.getAchievement(EnumAchievements.Locomotive), false, false);
		}
	}

	private Achievement registerAchievement(EnumAchievements key, int x, int y, ItemStack icon, Achievement requirement, boolean special, boolean independent) {
		Achievement achievement = new Achievement("computronics." + key.getKey(), "computronics." + key.getKey(), x, y, icon, requirement);
		if(special) {
			achievement.setSpecial();
		}
		if(independent) {
			achievement.initIndependentStat();
		}
		achievement.registerStat();
		this.achievementMap.put(key.getKey(), achievement);
		return achievement;
	}

	private void triggerAchievement(EntityPlayer player, EnumAchievements key) {
		if(player != null && this.achievementMap.containsKey(key.getKey())) {
			player.triggerAchievement(this.achievementMap.get(key.getKey()));
		}
	}

	public Achievement getAchievement(EnumAchievements key) {
		if(this.achievementMap.containsKey(key.getKey())) {
			return this.achievementMap.get(key.getKey());
		}
		return null;
	}

	@SubscribeEvent
	public void onCrafting(PlayerEvent.ItemCraftedEvent event) {
		ItemStack stack = event.crafting;
		EntityPlayer player = event.player;
		if(player == null || stack == null) {
			return;
		}
		if(stack.getItem() == Computronics.itemTape) {
			switch(stack.getItemDamage()){
				case 9:{
					this.triggerAchievement(player, EnumAchievements.Tape_IG);
					break;
				}
				case 4:
				case 8:{
					this.triggerAchievement(player, EnumAchievements.Tape_Star);
					break;
				}
				default:{
					this.triggerAchievement(player, EnumAchievements.Tape);
					break;
				}
			}
		} else if(stack.getItem() instanceof ItemLocomotive
			&& ItemLocomotive.getModel(stack).equals(ItemLocomotive.getModel(EnumCart.LOCO_ELECTRIC.getCartItem()))) {
			this.triggerAchievement(player, EnumAchievements.Locomotive);
		}
	}

	@SubscribeEvent
	public void onLeftClickEntity(AttackEntityEvent event) {
		if(Loader.isModLoaded("Railcraft") && event != null && event.target != null
			&& event.target instanceof EntityLocomotiveElectric) {

			EntityPlayer player = event.entityPlayer;
			EntityLocomotiveElectric loco = (EntityLocomotiveElectric) event.target;

			if(player == null) {
				return;
			}

			ItemStack stack = player.getCurrentEquippedItem();

			if(stack == null) {
				return;
			}

			if(player.isSneaking() && stack.getItem() == Computronics.relaySensor
				&& stack.hasTagCompound()) {

				NBTTagCompound data = stack.getTagCompound();
				if(data != null && data.hasKey("bound") && data.getBoolean("bound")) {

					int x = data.getInteger("relayX");
					int y = data.getInteger("relayY");
					int z = data.getInteger("relayZ");
					if(loco.worldObj.getTileEntity(x, y, z) != null
						&& loco.worldObj.getTileEntity(x, y, z) instanceof TileLocomotiveRelay) {

						TileLocomotiveRelay relay = (TileLocomotiveRelay) loco.worldObj.getTileEntity(x, y, z);
						if(loco.dimension == relay.getWorldObj().provider.dimensionId
							&& loco.getDistance(relay.xCoord, relay.yCoord, relay.zCoord) <= Computronics.LOCOMOTIVE_RELAY_RANGE) {

							this.triggerAchievement(player, EnumAchievements.Relay);
						}
					}
				}
			}
		}
	}

	@SubscribeEvent
	public void onItemDropped(ItemTossEvent event) {
		if(event == null || event.player == null || event.entityItem == null) {
			return;
		}
		EntityPlayer player = event.player;
		EntityItem item = event.entityItem;
		ItemStack stack = item.getEntityItem();

		if(stack != null && stack.getItem() == Computronics.itemTape && stack.getItemDamage() == 9) {
			if(!stack.hasTagCompound()) {
				stack.setTagCompound(new NBTTagCompound());
			}
			NBTTagCompound data = stack.getTagCompound();
			NBTTagCompound eventdata = new NBTTagCompound();
			eventdata.setString("player", player.getDisplayName());
			eventdata.setInteger("index", playerIndex);
			data.setTag("dropevent", eventdata);
			stack.setTagCompound(data);
			playerMap.put(playerIndex, player.getDisplayName());
			playerIndex++;
		}
	}

	@SubscribeEvent
	public void onItemDespawn(ItemExpireEvent event) {
		if(event == null || event.entityItem == null) {
			return;
		}
		EntityItem item = event.entityItem;
		ItemStack stack = item.getEntityItem();
		if(stack != null && stack.getItem() == Computronics.itemTape && stack.getItemDamage() == 9) {
			if(stack.hasTagCompound()) {
				NBTTagCompound data = stack.getTagCompound();
				if(data.hasKey("dropevent")) {
					NBTTagCompound eventdata = data.getCompoundTag("dropevent");
					String playername = null;
					if(eventdata.hasKey("player")) {
						playername = eventdata.getString("player");
					}
					if(playername != null && playerMap.containsValue(playername)) {
						Integer index = null;
						if(eventdata.hasKey("index")) {
							index = eventdata.getInteger("index");
						}
						this.triggerAchievement(item.worldObj.getPlayerEntityByName(playername), EnumAchievements.Tape_IG_Dropped);
						playerMap.remove(index);
						eventdata.removeTag("player");
						eventdata.removeTag("index");
						data.removeTag("dropevent");

					}
				}
			}
		}
	}
}
