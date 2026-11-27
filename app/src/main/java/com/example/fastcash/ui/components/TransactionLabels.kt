package com.example.fastcash.ui.components

import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

@Composable
fun transactionLabels(language: String): Resources {
    val context = LocalContext.current
    return remember(context, language) {
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale.forLanguageTag(language))
        context.createConfigurationContext(config).resources
    }
}
