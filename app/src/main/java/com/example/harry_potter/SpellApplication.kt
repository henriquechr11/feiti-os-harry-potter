package com.example.harry_potter

import android.app.Application
import com.example.harry_potter.data.AppDatabase
import com.example.harry_potter.data.AuthRepository
import com.example.harry_potter.data.BattleRepository
import com.example.harry_potter.data.SessionManager
import com.example.harry_potter.data.SpellRepository
import com.example.harry_potter.data.TemplateRepository
import com.example.harry_potter.network.HogwartsApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SpellApplication : Application() {
    lateinit var authRepository: AuthRepository
        private set
    lateinit var spellRepository: SpellRepository
        private set
    lateinit var templateRepository: TemplateRepository
        private set
    lateinit var sessionManager: SessionManager
        private set
    lateinit var battleRepository: BattleRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.create(this)
        val api = Retrofit.Builder()
            .baseUrl("https://hogwarts-api.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(HogwartsApi::class.java)

        sessionManager = SessionManager(this)
        authRepository = AuthRepository(database.userDao(), sessionManager)
        spellRepository = SpellRepository(api, database.favoriteDao())
        templateRepository = TemplateRepository(this)
        battleRepository = BattleRepository(database.battleDao())
    }
}
