package me.SamzyDev;

import org.lwjgl.input.Keyboard;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import java.io.*;
import java.net.*;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Objects;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.event.ClickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import java.lang.Thread;

@Mod(modid = AnimatedClockMod.MODID, version = AnimatedClockMod.VERSION)
public class AnimatedClockMod
{
    public static final String MODID = "animatedclockmod";
    public static final String VERSION = "1.0.0";
    
    KeyBinding macro, direction, oldDirection, attack;
    boolean toggled = false;
    boolean turning = false;
    int tickAmount = 0;
    
    @EventHandler
    public void init(FMLInitializationEvent event)
    {
    	FMLCommonHandler.instance().bus().register(this);
        MinecraftForge.EVENT_BUS.register(this);
        macro = new KeyBinding("Enable Animated Clock", Keyboard.KEY_C, "Animated Clock Mod");
        direction = Minecraft.getMinecraft().gameSettings.keyBindLeft;
        oldDirection = Minecraft.getMinecraft().gameSettings.keyBindForward;
        attack = Minecraft.getMinecraft().gameSettings.keyBindAttack;
        ClientRegistry.registerKeyBinding(macro);
    }
    
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event)
    {
    	if(!toggled || event.phase != Phase.START || Minecraft.getMinecraft().theWorld == null || Minecraft.getMinecraft().thePlayer == null)
    		return;
    	
    	Minecraft mc = Minecraft.getMinecraft();
    	World world = mc.theWorld;
    	EntityPlayerSP player = mc.thePlayer;
    	GameSettings gamesettings = mc.gameSettings;
    	
    	tickAmount++;
    	if(tickAmount > 200)
    	{
    		tickAmount = 0;
	    	toggled = false;
			holdDownKey(attack, false);
			if(direction.isKeyDown()) holdDownKey(direction, false);
			direction = Minecraft.getMinecraft().gameSettings.keyBindLeft;
	        oldDirection = Minecraft.getMinecraft().gameSettings.keyBindForward;
	    	player.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + "Animated Clock: " + EnumChatFormatting.DARK_PURPLE + "No Netherwart looked at in 10 seconds, clock disabled."));
	    	return;
    	}
    	
    	if(turning)
    	{
    		float yaw = player.rotationYaw;
    		if(yaw < 18 && yaw > -18)
    		{
    			player.rotationYaw = 0;
    			turning = false;
    			direction = gamesettings.keyBindForward;
    			oldDirection = gamesettings.keyBindRight;
    		}
    		else
    		{
    			player.setPositionAndRotation(player.posX, player.posY, player.posZ, yaw - 18, player.rotationPitch);
    		}
    		return;
    	}
    	
    	MovingObjectPosition lookingAt = Minecraft.getMinecraft().objectMouseOver;
		if (lookingAt != null && lookingAt.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
		    BlockPos pos = lookingAt.getBlockPos();
		    Block blockLookingAt = Minecraft.getMinecraft().theWorld.getBlockState(pos).getBlock();
		    String blockName = blockLookingAt.getRegistryName();
		    
		    if(blockName.contains("wart"))
		    {
		    	tickAmount = 0;
		    }
		    if(blockName.contains("lantern"))
		    {
				turning = true;
		    }
		}
    	
    	if(direction.compareTo(gamesettings.keyBindForward) == 0 && player.lastTickPosZ == player.posZ)
		{
			if(oldDirection.compareTo(gamesettings.keyBindLeft) == 0)
			{
				oldDirection = direction;
				direction = gamesettings.keyBindRight;
			}
			else if(oldDirection.compareTo(gamesettings.keyBindRight) == 0)
			{
				oldDirection = direction;
				direction = gamesettings.keyBindLeft;
			}
		}
		
    	else if(direction.compareTo(gamesettings.keyBindForward) != 0 && player.lastTickPosX == player.posX)
		{
			oldDirection = direction;
			direction = gamesettings.keyBindForward;
		}
		
		if(oldDirection.isKeyDown()) holdDownKey(oldDirection, false);
		if(!direction.isKeyDown()) holdDownKey(direction, true);
    }
    
    public void holdDownKey(KeyBinding key, boolean state)
	{
		KeyBinding.setKeyBindState(key.getKeyCode(), state);
		KeyBinding.onTick(key.getKeyCode());
	}

    static JsonElement getJson(String jsonUrl) {
		try {
			URL url = new URL(jsonUrl);
			URLConnection conn = url.openConnection();
			conn.setRequestProperty("Connection", "close");
			return new JsonParser().parse(new InputStreamReader(conn.getInputStream()));
		} catch (Exception e) {
			return null;
		}
	}
	
	@SubscribeEvent
	public void OnServerConnect(FMLNetworkEvent.ClientConnectedToServerEvent event) throws Exception
	{
		Minecraft m1c = Minecraft.getMinecraft();
		if (m1c.getCurrentServerData() == null) return;
		if (m1c.getCurrentServerData().serverIP.toLowerCase().contains("hypixel.") == false) return;
		new Thread(() -> {
			try {
				while (m1c.thePlayer == null) {
					//Yes, I'm too lazy to code something proper so I'm busy-waiting, shut up.
					//It usually waits for less than half a second
					Thread.sleep(100);
				}
				Thread.sleep(3000);
		String latestVersion = getJson("https://api.github.com/repos/YungSamzy/ACM/releases")
		.getAsJsonArray().get(0).getAsJsonObject().get("tag_name").getAsString();
		if (!Objects.equals(latestVersion, AnimatedClockMod.VERSION)) {
			ChatComponentText update = new ChatComponentText(EnumChatFormatting.GREEN + "" + EnumChatFormatting.BOLD + "  [UPDATE]  ");
				update.setChatStyle(update.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/YungSamzy/ACM/releases/latest")));
			Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + "Animated Clock: " +  EnumChatFormatting.DARK_PURPLE + "An update (" + latestVersion + ") is available. ").appendSibling(update));
		}
	}catch(Exception e)
	{

	}
	}).start();
	}

    @SubscribeEvent
    public void onKey(KeyInputEvent event)
    {
    	if(macro.isPressed())
    	{
    		if(!toggled)
    		{
    			if(Minecraft.getMinecraft().thePlayer.getHorizontalFacing().getName().toLowerCase().equals("north"))
    			{
    				Minecraft.getMinecraft().thePlayer.rotationYaw = -180;
    			}
    			else if(Minecraft.getMinecraft().thePlayer.getHorizontalFacing().getName().toLowerCase().equals("south"))
    			{
    				Minecraft.getMinecraft().thePlayer.rotationYaw = 0;
    			}
    			else
    			{
    				Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + "Animated Clock: " + EnumChatFormatting.DARK_PURPLE + "Face North or South to enable clock."));
    				return;
    			}
    			toggled = true;
    			holdDownKey(attack, true);
    			Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + "Animated Clock: " + EnumChatFormatting.DARK_PURPLE + "Clock Enabled."));
    		}
    		else
    		{
    			toggled = false;
    			tickAmount = 0;
    			holdDownKey(attack, false);
    			if(direction.isKeyDown()) holdDownKey(direction, false);
    			direction = Minecraft.getMinecraft().gameSettings.keyBindLeft;
    	        oldDirection = Minecraft.getMinecraft().gameSettings.keyBindForward;
    			Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + "Animated Clock: " + EnumChatFormatting.DARK_PURPLE + "Clock Disabled."));
    		}
    	}
    }
}
