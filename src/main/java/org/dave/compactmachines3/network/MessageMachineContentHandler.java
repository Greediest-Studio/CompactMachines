package org.dave.compactmachines3.network;

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import org.dave.compactmachines3.gui.machine.GuiMachineData;

public class MessageMachineContentHandler implements IMessageHandler<MessageMachineContent, MessageMachineContent> {
    @Override
    public MessageMachineContent onMessage(MessageMachineContent message, MessageContext ctx) {
        FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
            GuiMachineData.updateGuiMachineData(message.machineSize, message.id, message.roomPos, message.machinePos, message.owner, message.customName, message.playerWhiteList, message.locked);
        });

        return null;
    }
}
