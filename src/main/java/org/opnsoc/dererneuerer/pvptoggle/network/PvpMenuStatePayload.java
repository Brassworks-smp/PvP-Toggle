package org.opnsoc.dererneuerer.pvptoggle.network;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.opnsoc.dererneuerer.pvptoggle.Pvptoggle;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record PvpMenuStatePayload(
        boolean pvpOff,
        long pendingUntil,
        int configuredDelayMinutes,
        int blockedCount,
        int pendingBlockedCount,
        List<PlayerEntry> players
) implements CustomPacketPayload {
    private static final int MAX_PLAYERS = 512;
    private static final int MAX_NAME_BYTES = 64;

    public static final Type<PvpMenuStatePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Pvptoggle.MODID, "pvp_menu_state")
    );

    public static final StreamCodec<ByteBuf, PvpMenuStatePayload> STREAM_CODEC = StreamCodec.of(
            PvpMenuStatePayload::encode,
            PvpMenuStatePayload::decode
    );

    public PvpMenuStatePayload {
        players = List.copyOf(players);
    }

    private static void encode(ByteBuf buffer, PvpMenuStatePayload payload) {
        buffer.writeBoolean(payload.pvpOff);
        buffer.writeLong(payload.pendingUntil);
        buffer.writeInt(payload.configuredDelayMinutes);
        buffer.writeInt(payload.blockedCount);
        buffer.writeInt(payload.pendingBlockedCount);
        buffer.writeInt(payload.players.size());

        for (PlayerEntry player : payload.players) {
            buffer.writeLong(player.id.getMostSignificantBits());
            buffer.writeLong(player.id.getLeastSignificantBits());
            writeString(buffer, player.name);
            buffer.writeByte(player.relation.ordinal());
            buffer.writeBoolean(player.canAttack);
            buffer.writeLong(player.pendingUntil);
        }
    }

    private static PvpMenuStatePayload decode(ByteBuf buffer) {
        boolean pvpOff = buffer.readBoolean();
        long pendingUntil = buffer.readLong();
        int delay = buffer.readInt();
        int blocked = buffer.readInt();
        int pendingBlocked = buffer.readInt();
        int count = buffer.readInt();
        if (count < 0 || count > MAX_PLAYERS) {
            throw new DecoderException("Invalid PvP menu player count: " + count);
        }

        List<PlayerEntry> players = new ArrayList<>(count);
        Relation[] relations = Relation.values();
        for (int index = 0; index < count; index++) {
            UUID id = new UUID(buffer.readLong(), buffer.readLong());
            String name = readString(buffer);
            int relationIndex = Byte.toUnsignedInt(buffer.readByte());
            Relation relation = relationIndex < relations.length ? relations[relationIndex] : Relation.ALLOWED;
            boolean canAttack = buffer.readBoolean();
            long playerPendingUntil = buffer.readLong();
            players.add(new PlayerEntry(id, name, relation, canAttack, playerPendingUntil));
        }

        return new PvpMenuStatePayload(pvpOff, pendingUntil, delay, blocked, pendingBlocked, players);
    }

    private static void writeString(ByteBuf buffer, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_NAME_BYTES) {
            throw new IllegalArgumentException("PvP menu player name is too long");
        }
        buffer.writeInt(bytes.length);
        buffer.writeBytes(bytes);
    }

    private static String readString(ByteBuf buffer) {
        int length = buffer.readInt();
        if (length < 0 || length > MAX_NAME_BYTES) {
            throw new DecoderException("Invalid PvP menu player name length: " + length);
        }
        byte[] bytes = new byte[length];
        buffer.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record PlayerEntry(UUID id, String name, Relation relation, boolean canAttack, long pendingUntil) {
    }

    public enum Relation {
        ALLOWED,
        BLOCKED,
        PENDING
    }
}
