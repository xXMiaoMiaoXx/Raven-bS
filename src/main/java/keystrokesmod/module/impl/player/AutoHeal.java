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
	private final ButtonSetting dualRescueMode;
	public static boolean isRescuing;
	public static boolean isKey1Pressed;
	public static boolean isBlocking;
	public static byte latestState;
	private long lastReviveTime;
	private static boolean foundRescueTarget;
	private static boolean prevFoundRescueTarget;
	private static boolean rescuedTarget;
	
    public AutoHeal() {
        super("AutoRescue", category.player);
        this.registerSetting(maxHealth = new SliderSetting("Rescue when health above", 6, 0, 20, 1));
		this.registerSetting(rescueRange = new SliderSetting("Rescue range", 2.0, 1.0, 3.0, 0.1));
		this.registerSetting(dualRescueMode = new ButtonSetting("Dual mode", false));
    }
	
	public void onEnable() {
		isRescuing = false;
		latestState = 0;
		rescuedTarget = false;
		AutoClicker.rescuing = false;
		prevFoundRescueTarget = false;
		lastReviveTime = System.currentTimeMillis() - 1500;
    }

    public void onDisable() {
		isRescuing = false;
		latestState = 0;
		AutoClicker.rescuing = false;
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
		if(!dualRescueMode.isToggled()){
			if(mc.thePlayer.isInvisible()) setFoundRescueTarget(false);
			if(foundRescueTarget){
				AutoClicker.rescuing = true;
				if(!isRescuing && mc.thePlayer.getHealth()>=maxHealth.getInput()){
					if (Utils.nullCheck() && mc.inGameHasFocus) 
						KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), true);
					isRescuing = true;
				}
			}else{
				AutoClicker.rescuing = false;
				if(isRescuing){
					isRescuing = false;
					KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
					KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
					mc.thePlayer.inventory.currentItem = 4;
				}
			}
		}else{
			if(mc.thePlayer.isInvisible()){
				latestState = 2;
				//mc.thePlayer.sendChatMessage("Stop rescuing：invis");
				AutoClicker.rescuing = false;
				isRescuing = false;
				rescuedTarget = false;
				KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
				KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
			}else{
				if(mc.thePlayer.getHealth() >= (mc.thePlayer.getMaxHealth()*0.65)){
					latestState = 0;
					//mc.thePlayer.sendChatMessage("Stop rescuing：safe");
				}
				if(latestState == 2){
					latestState = 1;
					lastReviveTime = System.currentTimeMillis();
				}
				if(latestState == 1){
					if(System.currentTimeMillis()-lastReviveTime >= 1100){
						if(!foundRescueTarget && prevFoundRescueTarget){
							if(isRescuing){
								//mc.thePlayer.sendChatMessage("Stop rescuing.");
								AutoClicker.rescuing = false;
								isRescuing = false;
								rescuedTarget = true;
								KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
								KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
								mc.thePlayer.inventory.currentItem = 4;
							}
						}else if(foundRescueTarget && !prevFoundRescueTarget){
							KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), true);
						}else{
							if(!rescuedTarget){
								if(!isRescuing /*&& mc.thePlayer.getHealth()>=maxHealth.getInput()*/){
									AutoClicker.rescuing = true;
									isRescuing = true;
								}
							}//else mc.thePlayer.sendChatMessage("Rescued target, waiting for death.");
						}
						prevFoundRescueTarget = foundRescueTarget;
					}else{
						//mc.thePlayer.sendChatMessage("Stop rescuing：firing");
						AutoClicker.rescuing = false;
						isRescuing = false;
					}
				}
				if(latestState == 0){
					if(foundRescueTarget){
						AutoClicker.rescuing = true;
						if(!isRescuing /*&& mc.thePlayer.getHealth()>=maxHealth.getInput()*/){
							if (Utils.nullCheck() && mc.inGameHasFocus) 
								KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), true);
							isRescuing = true;
						}
					}else{
						AutoClicker.rescuing = false;
						if(isRescuing){
							isRescuing = false;
							KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
							KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
							mc.thePlayer.inventory.currentItem = 4;
						}
					}
				}
			}
			
		}
		
	}
}
