package com.catedra.tpinativo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.catedra.tpinativo.navigation.AppNavigation
import com.catedra.tpinativo.ui.theme.TPINativoTheme
import com.catedra.tpinativo.viewmodel.HabitosViewModel
import com.catedra.tpinativo.viewmodel.HabitosViewModelFactory
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    private lateinit var habitosViewModel: HabitosViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        habitosViewModel = ViewModelProvider(
            this,
            HabitosViewModelFactory(this)
        )[HabitosViewModel::class.java]

        setContent {
            TPINativoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var userId by remember {
                        mutableStateOf(FirebaseAuth.getInstance().currentUser?.uid ?: "")
                    }

                    AppNavigation(
                        viewModel = habitosViewModel,
                        userId = userId,
                        onUsuarioLogueado = { nuevoId -> userId = nuevoId }
                    )
                }
            }
        }
    }
}