package app.desperse.ui.screens.create.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import app.desperse.ui.components.DesperseBottomSheet
import app.desperse.ui.theme.DesperseSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditionDetailsSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    priceDisplay: String,
    currency: String,
    maxSupplyEnabled: Boolean,
    maxSupplyDisplay: String,
    pricingEnabled: Boolean,
    onPriceChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onMaxSupplyToggle: (Boolean) -> Unit,
    onMaxSupplyChange: (String) -> Unit
) {
    DesperseBottomSheet(
        isOpen = isOpen,
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DesperseSpacing.lg)
                .padding(bottom = DesperseSpacing.xxl)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(DesperseSpacing.lg)
        ) {
            Text(
                text = "Edition Details",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            EditionOptionsCard(
                priceDisplay = priceDisplay,
                currency = currency,
                maxSupplyEnabled = maxSupplyEnabled,
                maxSupplyDisplay = maxSupplyDisplay,
                pricingEnabled = pricingEnabled,
                onPriceChange = onPriceChange,
                onCurrencyChange = onCurrencyChange,
                onMaxSupplyToggle = onMaxSupplyToggle,
                onMaxSupplyChange = onMaxSupplyChange
            )

            Spacer(modifier = Modifier.height(DesperseSpacing.sm))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Text(
                    "Done",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
