package com.amr.feature.character.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Lifecycle of the detail pane's stores, driven the way Compose drives them:
 * [DetailPaneStoreHolder.ownerFor] during composition (+ getting the ViewModel),
 * [DetailPaneStoreHolder.commit] only when that composition is applied.
 */
class DetailPaneStoreHolderTest {

    private val created = mutableListOf<Int>()
    private val cleared = mutableListOf<Int>()

    private inner class DetailVm(val id: Int) : ViewModel() {
        override fun onCleared() { cleared += id }
    }

    /** Gets (or creates) the detail ViewModel for [id] the way the screen does: keyed in the owner. */
    private fun detailIn(owner: ViewModelStoreOwner, id: Int): DetailVm {
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T =
                DetailVm(id).also { created += id } as T
        }
        return ViewModelProvider.create(owner, factory)["character-detail-$id", DetailVm::class]
    }

    /** A composition showing [id] (or nothing), then — if [applied] — its SideEffect. */
    private fun DetailPaneStoreHolder.compose(id: Int?, applied: Boolean = true): DetailVm? {
        val vm = id?.let { detailIn(ownerFor(it), it) }
        if (applied) commit(id)
        return vm
    }

    @Test
    fun appliedSwitches_keepExactlyOneLiveStore_andClearEachPreviousViewModel() {
        val holder = DetailPaneStoreHolder()

        (1..50).forEach { holder.compose(it) }

        assertEquals((1..50).toList(), created)
        assertEquals((1..49).toList(), cleared)
        assertEquals(50, holder.activeCharacterId)
        assertEquals(1, holder.liveStoreCount)
    }

    @Test
    fun recompositionWithoutChange_reusesTheViewModel_andTheSameOwner() {
        val holder = DetailPaneStoreHolder()
        val first = holder.compose(7)
        val owner = holder.ownerFor(7)

        val again = holder.compose(7)
        val third = holder.compose(7)

        assertSame(first, again)
        assertSame(first, third)
        // Stable owner: LocalViewModelStoreOwner does not change between recompositions.
        assertSame(owner, holder.ownerFor(7))
        assertEquals(listOf(7), created)
        assertEquals(emptyList(), cleared)
    }

    @Test
    fun discardedComposition_changesNothingShown_andRetryReusesThePendingStore() {
        val holder = DetailPaneStoreHolder()
        holder.compose(1)

        // A composition for 2 is built but never applied.
        val discarded = holder.compose(2, applied = false)
        assertEquals(1, holder.activeCharacterId)
        assertEquals(emptyList(), cleared)

        // The retried composition for 2 reuses the pending store: no second ViewModel.
        val applied = holder.compose(2)
        assertSame(discarded, applied)
        assertEquals(listOf(1, 2), created)
        assertEquals(listOf(1), cleared)
        assertEquals(1, holder.liveStoreCount)
    }

    @Test
    fun discardedComposition_thenSelectionReverts_keepsTheShownViewModel() {
        val holder = DetailPaneStoreHolder()
        val shown = holder.compose(1)

        holder.compose(2, applied = false)
        // The selection change is rolled back: the next applied composition shows 1 again.
        val again = holder.compose(1)

        assertSame(shown, again)
        assertEquals(listOf(1, 2), created)
        // The never-shown pending store for 2 is swept away; 1 was never cleared.
        assertEquals(listOf(2), cleared)
        assertEquals(1, holder.liveStoreCount)
    }

    @Test
    fun rapidSwitchesBeforeApply_leaveNoOrphanStores() {
        val holder = DetailPaneStoreHolder()
        holder.compose(1)

        listOf(2, 3, 4).forEach { holder.compose(it, applied = false) }
        holder.compose(5)

        assertEquals(listOf(1, 2, 3, 4, 5), created)
        assertEquals(listOf(1, 2, 3, 4), cleared.sorted())
        assertEquals(5, holder.activeCharacterId)
        assertEquals(1, holder.liveStoreCount)
    }

    @Test
    fun clearingTheSelection_clearsTheActiveDetail_andRepeatedCommitsAreSafe() {
        val holder = DetailPaneStoreHolder()
        holder.compose(3)

        holder.compose(null)
        holder.compose(null)

        assertEquals(listOf(3), cleared)
        assertNull(holder.activeCharacterId)
        assertEquals(0, holder.liveStoreCount)
    }

    @Test
    fun destinationCleared_clearsActiveAndPendingStores() {
        // Stand-in for the list destination's store, which owns the holder.
        val destinationStore = ViewModelStore()
        val destination = object : ViewModelStoreOwner {
            override val viewModelStore = destinationStore
        }
        val holderFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T =
                DetailPaneStoreHolder() as T
        }
        val holder = ViewModelProvider.create(destination, holderFactory)["pane", DetailPaneStoreHolder::class]
        holder.compose(4)
        holder.compose(5, applied = false)

        // Navigation clears the destination's store when the destination is removed.
        destinationStore.clear()

        assertEquals(listOf(4, 5), cleared.sorted())
        assertEquals(0, holder.liveStoreCount)
    }
}
