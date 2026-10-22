package se.partee71.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics

/** Hur TalkBack får långtrycket i [longPress]. */
enum class LongPressSemantics {
    /** Elementets långtrycksåtgärd ("dubbeltryck och håll ned för att …") – [AppFilterChip]. */
    LongClick,

    /** En anpassad åtgärd i TalkBacks åtgärdsmeny (Dagbokens "Starkare" på en känsla). */
    CustomAction
}

/**
 * Långtryck som extra åtgärd på en yta vars klick ägs av något annat (en Material-komponent utan eget
 * långtryck, en knapp, en pill). `combinedClickable` går inte att lägga ovanpå utan att komponentens egen
 * klickyta (ripple, vald-semantik, inaktivt läge) försvinner – därför en egen gest. Den läses före
 * komponenten ([PointerEventPass.Initial]) och avbryts, som `detectTapGestures`, när fingret släpps, rör
 * sig mer än touch slop (en rullning som börjar på ytan går vidare till listan) eller när någon annan
 * förbrukat gesten. Först efter ett långtryck förbrukas resten av gesten, så att komponentens klick inte
 * också körs.
 *
 * TalkBack får långtrycket med [label] enligt [semantics] ([LongPressSemantics.CustomAction] kräver en
 * etikett; utan den blir det [LongPressSemantics.LongClick]). [haptic] ger en haptisk puls
 * (`HapticFeedbackType.LongPress`) när långtrycket utlöses. [onLongPress] `null` = ingen åtgärd, ytan är
 * som förut. En ny lambda vid varje komposition startar inte om gesten.
 */
fun Modifier.longPress(
    label: String?,
    onLongPress: (() -> Unit)?,
    haptic: Boolean = false,
    semantics: LongPressSemantics = LongPressSemantics.LongClick
): Modifier {
    if (onLongPress == null) return this
    val custom = semantics == LongPressSemantics.CustomAction && label != null
    return this then LongPressElement(haptic, onLongPress) then Modifier.semantics {
        if (custom) {
            customActions = listOf(
                CustomAccessibilityAction(label) {
                    onLongPress()
                    true
                }
            )
        } else {
            onLongClick(label) {
                onLongPress()
                true
            }
        }
    }
}

private data class LongPressElement(val haptic: Boolean, val onLongPress: () -> Unit) : ModifierNodeElement<LongPressNode>() {
    override fun create() = LongPressNode(haptic, onLongPress)

    override fun update(node: LongPressNode) {
        node.haptic = haptic
        node.onLongPress = onLongPress
    }
}

private class LongPressNode(var haptic: Boolean, var onLongPress: () -> Unit) :
    DelegatingNode(),
    CompositionLocalConsumerModifierNode {
    init {
        delegate(SuspendingPointerInputModifierNode { awaitEachGesture { longPress() } })
    }

    private suspend fun AwaitPointerEventScope.longPress() {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val cancelled = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            while (true) {
                val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id }
                val moved = change != null && (change.position - down.position).getDistance() > viewConfiguration.touchSlop
                if (change == null || !change.pressed || change.isConsumed || moved) break
            }
            true
        }
        if (cancelled == true) return
        if (haptic) currentValueOf(LocalHapticFeedback).performHapticFeedback(HapticFeedbackType.LongPress)
        onLongPress()
        // Resten av gesten hör till långtrycket: komponentens eget tryck ser ett avbrutet tryck.
        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            event.changes.forEach { it.consume() }
        } while (event.changes.any { it.pressed })
    }
}
