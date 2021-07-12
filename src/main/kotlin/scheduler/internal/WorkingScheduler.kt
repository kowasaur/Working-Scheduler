package scheduler.internal

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import scheduler.internal.util.*

internal const val ModId = "workingscheduler"

@Suppress("unused")
internal fun init() = initCommon(ModId) {
    ServerTickEvents.START_WORLD_TICK.register { worldTick(it) }
    registerC2S(PACKET_ID_TICK_IN_SERVER, TickInServerPacket::factory)
    registerC2S(PACKET_ID_CANCEL_TICKING_IN_SERVER, CancelTickingInServerPacket::factory)
}


@Suppress("unused")
internal fun initClient() = initClientOnly(ModId) {
    registerS2C(PACKET_ID_FINISH_SCHEDULE_IN_CLIENT, FinishScheduleInClientPacket::factory)
}

