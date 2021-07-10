package scheduler.internal

import net.minecraft.world.PersistentStateManager
import org.apache.logging.log4j.LogManager

internal fun PersistentStateManager.getOrCreate(id: String, creator: () -> TickerState): TickerState =
    getOrCreate({
        loadTickerStateFromNbt(it)
    }, creator, id);

private val Logger = LogManager.getLogger("Working Ticker")

internal fun logWarning(warning: String) = Logger.warn(warning)


