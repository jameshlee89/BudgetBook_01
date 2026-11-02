package com.budgetbook.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.budgetbook.app.ui.BudgetBookRoot
import com.budgetbook.app.ui.MainViewModel
import com.budgetbook.app.ui.theme.BudgetBookTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as BudgetBookApp).container
        setContent {
            BudgetBookTheme {
                val viewModel: MainViewModel = viewModel(factory = MainViewModel.factory(container))
                BudgetBookRoot(viewModel = viewModel, activity = this)
            }
        }
    }
}
