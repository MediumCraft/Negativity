package com.elikill58.negativity.api.packets.packet.playin;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.elikill58.negativity.api.item.ItemStack;
import com.elikill58.negativity.api.packets.PacketType;
import com.elikill58.negativity.api.packets.nms.PacketSerializer;
import com.elikill58.negativity.api.packets.packet.NPacketPlayIn;
import com.elikill58.negativity.universal.Version;

public class NPacketPlayInSetCreativeSlot implements NPacketPlayIn {

	public int slot;
	@Nullable
	public ItemStack item;
	
	public NPacketPlayInSetCreativeSlot() {

	}

	@Override
	public void read(PacketSerializer serializer, Version version) {
		this.slot = serializer.readShort();
		this.item = serializer.readItemStack(version);
	}

	@Override
	public PacketType getPacketType() {
		return PacketType.Client.SET_CREATIVE_SLOT;
	}
}
