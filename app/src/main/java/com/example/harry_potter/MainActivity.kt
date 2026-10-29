package com.example.harry_potter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.harry_potter.ui.theme.HarrypotterTheme
import com.example.harry_potter.userinterface.SpellbookApp
import com.example.harry_potter.viewmodel.BattleViewModel
import com.example.harry_potter.viewmodel.SpellViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HarrypotterTheme {
                val application = application as SpellApplication
                val spellViewModel: SpellViewModel = viewModel(
                    factory = SpellViewModel.Factory(
                        application.authRepository,
                        application.spellRepository,
                        application.templateRepository,
                        application.sessionManager
                    )
                )
                val battleViewModel: BattleViewModel = viewModel(
                    factory = BattleViewModel.Factory(application.battleRepository, application.templateRepository)
                )
                SpellbookApp(spellViewModel, battleViewModel)
            }
        }
    }
}
