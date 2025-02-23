package com.elikill58.negativity.common.server;

import java.io.ByteArrayOutputStream;

import com.elikill58.negativity.api.NegativityPlayer;
import com.elikill58.negativity.api.block.Block;
import com.elikill58.negativity.api.entity.Player;
import com.elikill58.negativity.api.events.EventListener;
import com.elikill58.negativity.api.events.EventPriority;
import com.elikill58.negativity.api.events.Listeners;
import com.elikill58.negativity.api.events.channel.GameChannelNegativityMessageEvent;
import com.elikill58.negativity.api.events.player.PlayerChangeWorldEvent;
import com.elikill58.negativity.api.events.player.PlayerConnectEvent;
import com.elikill58.negativity.api.events.player.PlayerMoveEvent;
import com.elikill58.negativity.api.events.plugins.ProxyPluginListEvent;
import com.elikill58.negativity.api.impl.server.CompensatedWorld;
import com.elikill58.negativity.api.item.Materials;
import com.elikill58.negativity.api.packets.packet.playout.NPacketPlayOutCustomPayload;
import com.elikill58.negativity.universal.Adapter;
import com.elikill58.negativity.universal.ProxyCompanionManager;
import com.elikill58.negativity.universal.Scheduler;
import com.elikill58.negativity.universal.logger.Debug;
import com.elikill58.negativity.universal.pluginMessages.ClientModsListMessage;
import com.elikill58.negativity.universal.pluginMessages.NegativityMessage;
import com.elikill58.negativity.universal.pluginMessages.NegativityMessagesManager;
import com.elikill58.negativity.universal.pluginMessages.PlayerVersionMessage;
import com.elikill58.negativity.universal.pluginMessages.ProxyPingMessage;

public class GameEventsManager implements Listeners {

	@EventListener
	public void onProxyPlugin(ProxyPluginListEvent e) {
		Adapter.getAdapter().getOnlinePlayers().forEach(this::sendVersionRequest);
	}

	@EventListener
	public void onJoin(PlayerConnectEvent e) {
		Player p = e.getPlayer();
		if (ProxyCompanionManager.isIntegrationEnabled()) {
			sendVersionRequest(p);
		} else if (!ProxyCompanionManager.searchedCompanion) {
			Scheduler.getInstance().runDelayed(() -> ProxyCompanionManager.sendProxyPing(p), 20);
		}
	}

	private void sendVersionRequest(Player p) {
		try {
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			stream.write(NegativityMessagesManager.CHANNEL_ID.getBytes("UTF8"));
			stream.write(0);
			p.sendPacket(new NPacketPlayOutCustomPayload("REGISTER", stream.toByteArray()));
			p.sendPacket(new NPacketPlayOutCustomPayload(NegativityMessagesManager.CHANNEL_ID, NegativityMessagesManager.writeMessage(new PlayerVersionMessage(p.getUniqueId(), null))));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@EventListener
	public void onChannelMessage(GameChannelNegativityMessageEvent e) {
		Player p = e.getPlayer();
		NegativityMessage message = e.getMessage();
		Adapter.getAdapter().debug(Debug.GENERAL, "Message: " + (message == null ? null : message.messageId() + ": " + message.getClass().getSimpleName()));
		if (message instanceof ProxyPingMessage) {
			ProxyPingMessage pingMessage = (ProxyPingMessage) message;
			ProxyCompanionManager.foundCompanion(pingMessage);
		} else if (message instanceof ClientModsListMessage) {
			ClientModsListMessage modsMessage = (ClientModsListMessage) message;
			NegativityPlayer np = NegativityPlayer.getNegativityPlayer(p);
			if (!modsMessage.getMods().isEmpty()) {
				np.mods.clear();
				np.mods.putAll(modsMessage.getMods());
			}
		} else if (message instanceof PlayerVersionMessage) {
			p.setPlayerVersion(((PlayerVersionMessage) message).getVersion());
		} else if (message != null) {
			Adapter.getAdapter().getLogger().warn("Received unexpected plugin message " + message.getClass().getName());
		}
	}

	@EventListener(priority = EventPriority.POST)
	public void onMove(PlayerMoveEvent e) {
		if (!e.isMovePosition() || e.isCancelled())
			return;
		Player p = e.getPlayer();
		NegativityPlayer np = NegativityPlayer.getNegativityPlayer(p);
		Block below = p.getLocation().clone().sub(0, 1, 0).getBlock();
		if (below.getType().equals(Materials.SLIME_BLOCK)) {
			np.isUsingSlimeBlock = true;
		} else if (np.isUsingSlimeBlock && (p.isOnGround() && !below.getType().equals(Materials.AIR)))
			np.isUsingSlimeBlock = false;
	}

	@EventListener
	public void changeWorld(PlayerChangeWorldEvent e) {
		Player p = e.getPlayer();
		p.setInternalWorld(new CompensatedWorld(p, e.getServerWorld()));
	}
}
