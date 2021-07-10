package scheduler.internal

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import scheduler.internal.util.*

internal const val ModId = "workingscheduler"

@Suppress("unused")
internal fun init() = initCommon(ModId) {
    ServerTickEvents.START_WORLD_TICK.register { worldTick(it) }
    registerC2S(TickInServerPacket.serializer())
    registerC2S(CancelTickingInServerPacket.serializer())
}


@Suppress("unused")
internal fun initClient() = initClientOnly(ModId) {
    registerS2C(FinishScheduleInClientPacket.serializer())
}

