package com.nielcode.kupass.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nielcode.kupass.R
import com.nielcode.kupass.ui.theme.KupassTheme

/** Corner radius shared by vault field cards (detail view and editable fields). */
val VaultFieldShape = RoundedCornerShape(16.dp)

/**
 * Editable vault field that matches the read-only field cards on the password detail screen: a
 * `surfaceContainerLow` card with the label above the value and optional trailing actions.
 */
@Composable
fun VaultTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    isError: Boolean = false,
    supportingText: String? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val colors = MaterialTheme.colorScheme
    val outline by
        animateColorAsState(
            targetValue =
                when {
                    isError -> colors.error
                    focused -> colors.primary
                    else -> Color.Transparent
                },
            label = "field_outline",
        )

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
            cursorBrush = SolidColor(colors.primary),
            decorationBox = { innerTextField ->
                VaultFieldDecoration(
                    label = label,
                    isError = isError,
                    placeholder = placeholder.takeIf { value.isEmpty() && it.isNotEmpty() },
                    outline = outline,
                    trailingIcon = trailingIcon,
                    innerTextField = innerTextField,
                )
            },
        )
        if (supportingText != null) {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) colors.error else colors.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
    }
}

/** Card look shared with the detail screen: label above the value, trailing actions at the end. */
@Composable
private fun VaultFieldDecoration(
    label: String,
    isError: Boolean,
    placeholder: String?,
    outline: Color,
    trailingIcon: (@Composable () -> Unit)?,
    innerTextField: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier =
            Modifier.clip(VaultFieldShape)
                .background(colors.surfaceContainerLow)
                .border(width = 1.dp, color = outline, shape = VaultFieldShape)
                .padding(start = 16.dp, top = 12.dp, end = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (isError) colors.error else colors.primary,
            )
            Box(
                modifier = Modifier.defaultMinSize(minHeight = 40.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (placeholder != null) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
                innerTextField()
            }
        }
        trailingIcon?.invoke()
    }
}

@Preview
@Composable
private fun VaultTextFieldPreview() {
    KupassTheme(dynamicColor = false) {
        Column(
            modifier = Modifier.background(MaterialTheme.colorScheme.surface).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            VaultTextField(
                value = "",
                onValueChange = {},
                label = "Site",
                placeholder = "e.g. GitHub",
            )
            VaultTextField(value = "kudanilll", onValueChange = {}, label = "Username")
            VaultTextField(
                value = "short",
                onValueChange = {},
                label = "Backup password",
                isError = true,
                supportingText = "Use at least 8 characters",
            )
        }
    }
}

/** Keyboard for passwords: no suggestions or autocorrect that could learn the secret. */
val SecretKeyboardOptions =
    KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false)

/** Eye icon that shows or hides a password field. */
@Composable
fun PasswordVisibilityToggle(
    visible: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onToggle, modifier = modifier) {
        Icon(
            imageVector = if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
            contentDescription = stringResource(R.string.show_password),
        )
    }
}
