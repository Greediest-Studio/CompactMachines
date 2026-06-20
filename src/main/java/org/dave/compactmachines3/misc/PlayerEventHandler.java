package org.dave.compactmachines3.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.dave.compactmachines3.gui.machine.GuiMachineData;
import org.dave.compactmachines3.init.Blockss;
import org.dave.compactmachines3.network.MessageMachinePositions;
import org.dave.compactmachines3.network.PackageHandler;
import org.dave.compactmachines3.network.MessageWorldInfo;
import org.dave.compactmachines3.world.WorldSavedDataMachines;
import org.dave.compactmachines3.world.tools.DimensionTools;
import org.dave.compactmachines3.world.tools.TeleportationTools;

public class PlayerEventHandler {
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if(!(event.player instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        PackageHandler.instance.sendTo(new MessageWorldInfo(DimensionTools.getServerMachineWorld().getWorldInfo()), player);
        PackageHandler.instance.sendTo(MessageMachinePositions.initWithWorldSavedData(), player);
    }

    @SubscribeEvent
    public static void onPlayerSpawn(PlayerEvent.PlayerRespawnEvent event) {
        if(event.player.getEntityWorld().provider.getDimension() != ConfigurationHandler.Settings.dimensionId) {
            return;
        }

        int bedLocations = WorldSavedDataMachines.getInstance().getBedLocation(event.player);
        boolean bedInCM = bedLocations != -1;

        if(bedInCM && ConfigurationHandler.MachineSettings.allowRespawning) {
            TeleportationTools.teleportPlayerToMachine((EntityPlayerMP) event.player, bedLocations, false);
        } else {
            TeleportationTools.teleportPlayerOutOfMachineDimension((EntityPlayerMP) event.player);
            event.player.getEntityData().removeTag("compactmachines3-idHistory");
            event.player.getEntityData().removeTag("compactmachines3-coordHistory"); // Legacy
        }
    }

    @SubscribeEvent
    public static void onPlayerSleepInBed(PlayerSleepInBedEvent event) {
        if(event.getEntityPlayer().world.provider.getDimension() != ConfigurationHandler.Settings.dimensionId) {
            return;
        }

        if (!ConfigurationHandler.MachineSettings.allowRespawning) {
            event.setResult(EntityPlayer.SleepResult.NOT_POSSIBLE_HERE);
            event.getEntityPlayer().sendStatusMessage(new TextComponentTranslation("hint.compactmachines3.cant_sleep_here"), true);
            return;
        }

        if (event.getEntityPlayer().isPlayerSleeping() || !event.getEntityPlayer().isEntityAlive()) {
            event.setResult(EntityPlayer.SleepResult.OTHER_PROBLEM);
            return;
        }

        event.getEntityPlayer().sendStatusMessage(new TextComponentTranslation("hint.compactmachines3.bed_position_set"), true);
        WorldSavedDataMachines.getInstance().setBedLocation(event.getEntityPlayer());

        event.setResult(EntityPlayer.SleepResult.NOT_POSSIBLE_HERE);
    }


    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onGuiClose(GuiOpenEvent event) {
        if(event.getGui() == null) {
            GuiMachineData.canRender = false;
        }
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void renderRotationIndicator(RenderWorldLastEvent event) {
        if(!Minecraft.isGuiEnabled() || Minecraft.getMinecraft().player == null) {
            return;
        }

        EntityPlayer player = Minecraft.getMinecraft().player;
        ItemStack heldItem = player.getHeldItem(EnumHand.MAIN_HAND);
        if(heldItem.isEmpty()) {
            return;
        }

        if(!(heldItem.getItem() instanceof ItemBlock && ((ItemBlock) heldItem.getItem()).getBlock() == Blockss.fieldProjector)) {
            return;
        }

        RayTraceResult trace = Minecraft.getMinecraft().objectMouseOver;
        if(trace == null || trace.hitVec == null || trace.typeOfHit != RayTraceResult.Type.BLOCK) {
            return;
        }

        if(trace.sideHit != EnumFacing.UP) {
            return;
        }

        // Get a interpolated clientPosition so we don't get flickering when moving
        Vec3d cameraPosition = new Vec3d(
            player.lastTickPosX + (player.posX - player.lastTickPosX) * event.getPartialTicks(),
            player.lastTickPosY + (player.posY - player.lastTickPosY) * event.getPartialTicks(),
            player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.getPartialTicks()
        );

        // Get the position on the top face of the block the player is looking at, between -0.5 and +0.5 on both axis
        Vec3d hitPosition = trace.hitVec;
        hitPosition = hitPosition.subtract(new Vec3d(trace.getBlockPos()));
        hitPosition = hitPosition.subtract(0.5d, 0.5d, 0.5d);

        // Move the drawing area to one block above the block we are looking at
        BlockPos drawPosition = trace.getBlockPos().up();

        RotationTools.renderArrowOnGround(cameraPosition, hitPosition, drawPosition);
    }
}
