package com.example.dynamicapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dynamicapp.ui.theme.DynamicAppTheme
import com.example.dynamicform.DynamicFormScreen
import com.example.dynamicform.DynamicFormViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DynamicAppTheme {
                val viewModel: DynamicFormViewModel = viewModel()
                DynamicFormScreen(viewModel = viewModel)
            }
        }
    }
}
