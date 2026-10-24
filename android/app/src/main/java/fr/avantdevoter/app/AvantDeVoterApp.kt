package fr.avantdevoter.app

import android.app.Application
import fr.avantdevoter.app.data.AppDatabase
import fr.avantdevoter.app.data.Prefs
import fr.avantdevoter.app.data.Repository
import fr.avantdevoter.app.sync.SyncWorker

class AvantDeVoterApp : Application() {

    lateinit var repository: Repository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = Repository(this, AppDatabase.create(this), Prefs(this))
        SyncWorker.planifier(this)
    }
}
