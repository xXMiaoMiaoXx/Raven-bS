package keystrokesmod.module.impl.combat;

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
import java.util.Random;

public class AutoClicker extends Module {
	public static boolean rescuing = false;
	public static boolean blocking = false;
	
	private final SliderSetting shotInterval;
	private final SliderSetting weaponCount;
	private final ButtonSetting smartUseShotgun;
	public static ButtonSetting leftClick;
	private int action;
	private Random rand = null;
	private long lastClickTime, curTime;
	private static int useShotgunIntention = 0;

    public AutoClicker() {
        super("AutoClicker", Module.category.combat, 0);
        this.registerSetting(shotInterval = new SliderSetting("Shot interval", 20, 10, 50, 1));
		this.registerSetting(weaponCount = new SliderSetting("Weapon count", 1, 1, 3, 1));
		this.registerSetting(smartUseShotgun = new ButtonSetting("Smart use shotgun", false));
    }

    public void onEnable() {
		action = 0;
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

    @SubscribeEvent
    public void onRenderTick(@NotNull RenderTickEvent ev) {
        if (ev.phase != Phase.END && Utils.nullCheck() && !mc.thePlayer.isEating() && mc.objectMouseOver != null) {
            if (mc.currentScreen == null && mc.inGameHasFocus) {
				curTime = System.currentTimeMillis();
				if(curTime-lastClickTime >= 50){
					lastClickTime = curTime;
				}else return;
				
				if(mc.thePlayer.hurtTime != 0) useShotgunIntention = 5;
				
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
							KeyBinding.onTick(mc.gameSettings.keyBindUseItem.getKeyCode());
							incAction();
							if(smartUseShotgun.isToggled()){
								if(useShotgunIntention <= 0){
									incAction();
									incAction();
								}else useShotgunIntention--;
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
							KeyBinding.onTick(mc.gameSettings.keyBindUseItem.getKeyCode());
							incAction();
						}
					break;
				}
            }
        }
    }

}
