package keystrokesmod.module.impl.combat;

import akka.japi.Pair;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.other.RecordClick;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.ModeSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.module.setting.utils.ModeOnly;
import keystrokesmod.utility.Reflection;
import keystrokesmod.utility.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class AutoClicker extends Module {
	public static boolean rescuing = false;
	public static boolean blocking = false;
	public static boolean isInvincible = false;
	private static boolean refillWeapon = false;
	
	private final SliderSetting shotInterval;
	private final SliderSetting weaponCount;
	private final ButtonSetting smartUseShotgun;
	public static ButtonSetting leftClick;
	private int action;
	private Random rand = null;
	private long lastClickTime, curTime;
	private boolean foundNearTarget;

    public AutoClicker() {
        super("AutoClicker", Module.category.combat, 0);
        this.registerSetting(shotInterval = new SliderSetting("Shot interval", 20, 10, 50, 1));
		this.registerSetting(weaponCount = new SliderSetting("Weapon count", 1, 1, 3, 1));
		this.registerSetting(smartUseShotgun = new ButtonSetting("Smart use shotgun", false));
    }

    public void onEnable() {
		action = 0;
		foundNearTarget = false;
		lastClickTime = System.currentTimeMillis()-50;
        this.rand = new Random();
    }

    public void onDisable() {
		KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
		KeyBinding.setKeyBindState(mc.gameSettings.keyBindsHotbar[1].getKeyCode(), false);
		KeyBinding.setKeyBindState(mc.gameSettings.keyBindsHotbar[2].getKeyCode(), false);
		KeyBinding.setKeyBindState(mc.gameSettings.keyBindsHotbar[3].getKeyCode(), false);
    }
	
	private void incAction(){
		action++;
		action %= ((int)weaponCount.getInput())*2;
	}
	
	private void setFoundNearTarget(boolean val){ foundNearTarget=val; }

    @SubscribeEvent
    public void onRenderTick(@NotNull RenderTickEvent ev) {
        if (ev.phase != Phase.END && Utils.nullCheck() && !mc.thePlayer.isEating() && mc.objectMouseOver != null) {
            if (mc.currentScreen == null && mc.inGameHasFocus) {
				curTime = System.currentTimeMillis();
				if(curTime-lastClickTime >= 50){
					lastClickTime = curTime;
				}else return;
				
				setFoundNearTarget(false);
				mc.theWorld.loadedEntityList.stream()
                .filter(Objects::nonNull)
				.filter(entity -> entity instanceof EntityLivingBase)
				.map(entity -> new Pair<>(entity, mc.thePlayer.getDistanceSqToEntity(entity)))
				.sorted((p1, p2) -> p2.second().compareTo(p1.second()))
				.forEach(pair -> {
                    // need a more accurate distance check as this can ghost on hypixel
					//mc.thePlayer.sendChatMessage(String.valueOf(pair.second()));
                    if (!(pair.first() instanceof EntityPlayer) && !(pair.first() instanceof EntityArmorStand) && pair.second() <= 40.0) {
						setFoundNearTarget(true);
                    }
                });
				
				if(rescuing){
					if(blocking) return;
					if(Utils.holdingSword()){
						blocking = true;
						KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), true);
					}else{
						mc.thePlayer.inventory.currentItem = 0;
					}
					return;
				}else{
					if(blocking){
						KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
						blocking = false;
						return;
					}
				}
				
				if(isInvincible && this.rand.nextInt(100) > 50)
					refillWeapon = true;
				else
					refillWeapon = false;
				
				switch (action) {
					case 0: 
						if(this.rand.nextInt(100) > (int)shotInterval.getInput()){
							//KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
							mc.thePlayer.inventory.currentItem = 1;
							incAction();
						}
					break;
					case 1: 
						if(this.rand.nextInt(100) > (int)shotInterval.getInput()){
							//KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), true);
							if(refillWeapon)
								KeyBinding.onTick(mc.gameSettings.keyBindAttack.getKeyCode());
							else
								KeyBinding.onTick(mc.gameSettings.keyBindUseItem.getKeyCode());
							incAction();
							if(smartUseShotgun.isToggled()){
								if(!foundNearTarget){
									incAction();
									incAction();
								}
							}
						}
					break;

					
					case 2: 
						if(this.rand.nextInt(100) > (int)shotInterval.getInput()){
							//KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
							mc.thePlayer.inventory.currentItem = 2;
							incAction();
						}
					break;
					case 3: 
						if(this.rand.nextInt(100) > (int)shotInterval.getInput()){
							//KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), true);
							if(refillWeapon)
								KeyBinding.onTick(mc.gameSettings.keyBindAttack.getKeyCode());
							else
								KeyBinding.onTick(mc.gameSettings.keyBindUseItem.getKeyCode());
							incAction();
						}
					break;
					
					case 4: 
						if(this.rand.nextInt(100) > (int)shotInterval.getInput()){
							//KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
							mc.thePlayer.inventory.currentItem = 3;
							incAction();
						}
					break;
					case 5: 
						if(this.rand.nextInt(100) > (int)shotInterval.getInput()){
							//KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), true);
							if(refillWeapon)
								KeyBinding.onTick(mc.gameSettings.keyBindAttack.getKeyCode());
							else
								KeyBinding.onTick(mc.gameSettings.keyBindUseItem.getKeyCode());
							incAction();
						}
					break;
				}
            }
        }
    }

}
