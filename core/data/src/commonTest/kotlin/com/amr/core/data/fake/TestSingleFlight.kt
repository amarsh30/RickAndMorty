package com.amr.core.data.fake

import com.amr.core.data.concurrency.SingleFlight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Test-only [SingleFlight]: shared work runs in the caller's thread (Unconfined),
 * so repository tests stay deterministic with fakes and gates. Production code
 * always receives the ApplicationScope-backed instance from Koin.
 */
fun testSingleFlight(): SingleFlight =
    SingleFlight(CoroutineScope(SupervisorJob() + Dispatchers.Unconfined))
