package com.amr.feature.character.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

/**
 * Owns the one [ViewModelStore] used by the detail shown inside the character
 * list destination (two-pane, or single-pane after a resize).
 *
 * Without it, every selected character's detail ViewModel would live in the
 * list destination's own store until that destination is destroyed, i.e. grow
 * with every selection.
 *
 * Composition-safe in two steps:
 * - [ownerFor] (during composition) never changes which store is active; it only
 *   returns the active store or creates/reuses a *pending* one for that id. So a
 *   discarded composition changes nothing that is shown, and retrying the same
 *   id reuses the pending store (no second ViewModel, no second refresh).
 * - [commit] (after the composition is applied) promotes the shown id's store and
 *   clears every other store, so at most one stays alive and none is left behind.
 *
 * Each store has one fixed owner object, so [ownerFor] is stable across
 * recompositions. This holder is itself a ViewModel of the list destination: it
 * survives configuration changes and tab switches with it, and clears its stores
 * when the destination is cleared. It never touches Navigation's or the
 * Activity's stores.
 */
internal class DetailPaneStoreHolder : ViewModel() {

    private class Entry(val characterId: Int) : ViewModelStoreOwner {
        override val viewModelStore = ViewModelStore()
    }

    private var active: Entry? = null
    private val pending = mutableMapOf<Int, Entry>()

    /** Character whose store is active (shown), if any. */
    val activeCharacterId: Int? get() = active?.characterId

    /** Stores currently alive (active + pending), for lifecycle checks. */
    internal val liveStoreCount: Int get() = (if (active != null) 1 else 0) + pending.size

    /** Owner for [characterId]'s detail; safe to call during composition. */
    fun ownerFor(characterId: Int): ViewModelStoreOwner =
        active?.takeIf { it.characterId == characterId }
            ?: pending.getOrPut(characterId) { Entry(characterId) }

    /**
     * Called once a composition is applied, with the id it showed (null = none):
     * that id's store becomes active, all other stores are cleared.
     */
    fun commit(shownCharacterId: Int?) {
        val next = shownCharacterId?.let { id ->
            active?.takeIf { it.characterId == id } ?: pending.remove(id)
        }
        if (active !== next) active?.viewModelStore?.clear()
        pending.values.forEach { it.viewModelStore.clear() }
        pending.clear()
        active = next
    }

    override fun onCleared() = commit(null)
}
