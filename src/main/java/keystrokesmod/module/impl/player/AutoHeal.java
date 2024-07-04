package keystrokesmod.module.impl.player;

import akka.japi.Pair;
import keystrokesmod.Raven;
import keystrokesmod.event.*;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.combat.AutoClicker;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.script.classes.Vec3;
import keystrokesmod.utility.*;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.input.Mouse;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.minecraft.util.EnumFacing.DOWN;

public class AutoHeal extends Module {
    private final SliderSetting maxHealth;
	private final SliderSetting rescueRange;
	public static boolean isRescuing;
	public static boolean isKey1Pressed;
	public static boolean isBlocking;
	private static boolean foundRescueTarget;
	
    public AutoHeal() {
        super("AutoRescue", category.player);
        //this.registerSetting(new DescriptionSetting("Help you rescue teamate in zombie game."));
        this.registerSetting(maxHealth = new SliderSetting("Rescue when health above", 6, 0, 20, 1));
		this.registerSetting(rescueRange = new SliderSetting("Rescue range", 2.0, 1.0, 3.0, 0.1));
    }
	
	public void onEnable() {
		isRescuing = false;
    }

    public void onDisable() {
		//mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SNEAKING));
		isRescuing = false;
		//mc.thePlayer.sendChatMessage("Stop Snaking.");
    }
	
	private void setFoundRescueTarget(boolean val){ foundRescueTarget=val; }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
		foundRescueTarget = false;
		
		mc.theWorld.loadedEntityList.stream()
                .filter(Objects::nonNull)
                .filter(entity -> entity != mc.thePlayer)
                .filter(entity -> entity instanceof EntityLivingBase)
				.map(entity -> new Pair<>(entity, mc.thePlayer.getDistanceSqToEntity(entity)))
				.sorted((p1, p2) -> p2.second().compareTo(p1.second()))
				.forEach(pair -> {
                    // need a more accurate distance check as this can ghost on hypixel
                    if (pair.first() instanceof EntityPlayer && pair.second() <= rescueRange.getInput() && ((EntityLivingBase)pair.first()).isPlayerSleeping()) {
						setFoundRescueTarget(true);
                    }
                });
		if(mc.thePlayer.isInvisible()) setFoundRescueTarget(false);
		if(foundRescueTarget){
			AutoClicker.rescuing = true;
			if(!isRescuing && mc.thePlayer.getHealth()>=maxHealth.getInput()){
				//mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
				//mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SNEAKING));
				if (Utils.nullCheck() && mc.inGameHasFocus) 
					KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), true);
				isRescuing = true;
				//mc.thePlayer.sendChatMessage("Start Snaking.");
			}
		}else{
			if(isRescuing){
				//mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SNEAKING));
				AutoClicker.rescuing = false;
				isRescuing = false;
				KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
				KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
				mc.thePlayer.inventory.currentItem = 4;
				//mc.thePlayer.sendChatMessage("Stop Snaking.");
			}
		}
		
	}
}
