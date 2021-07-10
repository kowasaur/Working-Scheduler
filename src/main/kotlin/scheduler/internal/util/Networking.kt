package scheduler.internal.util

import drawer.readFrom
import drawer.write
import io.netty.buffer.Unpooled
import kotlinx.serialization.KSerializer
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.network.PacketContext
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.PacketByteBuf
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier
import net.minecraft.world.World
import java.util.*


/*******
 * Fabric Api Wrappers
 ********/

private fun PlayerEntity.sendPacket(packetId: Identifier, packetBuilder: PacketByteBuf.() -> Unit) {
    val packet = PacketByteBuf(Unpooled.buffer()).apply(packetBuilder)
    ServerPlayNetworking.send(this as ServerPlayerEntity?, packetId, packet)
}

private fun sendPacketToServer(packetId: Identifier, packetBuilder: PacketByteBuf.() -> Unit) {
    val packet = PacketByteBuf(Unpooled.buffer()).apply(packetBuilder)
    ClientPlayNetworking.send(packetId, packet)

}

/******************************
 * Automatic Serializer Wrappers
 ******************************/


internal fun CommonModInitializationContext.registerC2S(
    serializer: KSerializer<out InternalC2SPacket<*>>
) {
    ServerPlayNetworking.registerGlobalReceiver(
        Identifier(modId, serializer.packetId)
    ) { server, player, handler, buf, responseSender ->
        serializer.readFrom(buf).use(player.world as World)
    }
}

internal fun ClientModInitializationContext.registerS2C(vararg serializers: KSerializer<out InternalS2CPacket<*>>) {
    for (serializer in serializers) registerS2C(serializer)
}


internal fun ClientModInitializationContext.registerS2C(
    serializer: KSerializer<out InternalS2CPacket<*>>
) {
    ClientPlayNetworking.registerGlobalReceiver(
        Identifier(modId, serializer.packetId)
    ) { client, handler, buf, responseSender ->
        serializer.readFrom(buf).use(client.world as World)
    }
}

internal fun CommonModInitializationContext.registerC2S(
    vararg serializers: KSerializer<out InternalC2SPacket<*>>
) {
    for (serializer in serializers) registerC2S(serializer)
}


/**
 * Sends a packet from the server to the client for all the players in the stream.
 */
internal fun <T : InternalS2CPacket<T>> PlayerEntity.sendPacket(packet: T) {
    sendPacket(packetId = Identifier(packet.modId, packet.serializer.packetId)) {
        packet.serializer.write(packet, this, context = packet.serializationContext)
    }
}

/**
 * Sends a packet from the server to the client for all the players in the stream.
 */
internal fun <T : InternalC2SPacket<T>> sendPacketToServer(packet: T) {
    sendPacketToServer(Identifier(packet.modId, packet.serializer.packetId)) {
        packet.serializer.write(packet, this, context = packet.serializationContext)
    }
}

internal val PacketContext.world: World get() = player.world


private val <T : Packet<out T>> KSerializer<out T>.packetId get() = descriptor.serialName.lowercase(Locale.getDefault())


internal interface Packet<T : Packet<T>> {


    val serializer: KSerializer<T>

    val modId: String
    fun use(world: World)
    val serializationContext: SerializersModule get() = EmptySerializersModule
}

internal interface InternalC2SPacket<T : Packet<T>> : Packet<T>
internal interface InternalS2CPacket<T : Packet<T>> : Packet<T>

