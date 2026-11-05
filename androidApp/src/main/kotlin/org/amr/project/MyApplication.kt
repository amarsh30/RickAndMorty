package org.amr.project

import android.app.Application
import com.amr.shared.di.androidDatabaseModule
import com.amr.shared.di.commonModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@MyApplication)

            modules(
                commonModules + androidDatabaseModule
            )
        }
    }
}