package app.palettez.update

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.palettez.ui.components.IconCircleButton
import app.palettez.ui.components.PrimaryButton
import app.palettez.ui.theme.CardRadius
import app.palettez.ui.theme.PalettezTheme

@Composable
fun UpdateAvailableDialog(
    remote: RemoteReleaseInfo,
    onDownloadClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = PalettezTheme.colors
    val typography = PalettezTheme.typography

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.ground)
                .statusBarsPadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.lime)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.SystemUpdate,
                                contentDescription = null,
                                tint = Color(0xFF151515),
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = "UPDATE AVAILABLE · v${remote.versionName}",
                                style = typography.caption.copy(
                                    color = Color(0xFF151515),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.5.sp,
                                ),
                            )
                        }
                    }

                    IconCircleButton(
                        icon = Icons.Filled.Close,
                        contentDescription = "Dismiss update prompt",
                        onClick = onDismiss,
                    )
                }

                // Headline
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "NEW BUILD READY",
                        style = typography.display.copy(fontSize = 44.sp, lineHeight = 46.sp),
                        color = colors.ink,
                    )
                    Text(
                        text = "Palettez ${remote.versionName} (Build ${remote.versionCode}) is live on the official homepage.",
                        style = typography.body.copy(color = colors.ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                    )
                    Text(
                        text = "Opens chasetech56.github.io/Palettez in your browser to download the signed APK directly.",
                        style = typography.caption.copy(color = colors.inkSecondary, fontSize = 13.sp),
                    )
                }

                // Release notes card
                if (remote.releaseNotes.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(CardRadius))
                            .background(colors.surface)
                            .border(1.dp, colors.line.copy(alpha = 0.5f), RoundedCornerShape(CardRadius))
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "WHAT'S IN THIS UPDATE",
                            style = typography.section.copy(fontSize = 18.sp),
                            color = colors.ink,
                        )
                        remote.releaseNotes.forEach { note ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 6.dp)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(colors.lime),
                                )
                                Text(
                                    text = note,
                                    style = typography.body.copy(fontSize = 14.sp, lineHeight = 20.sp),
                                    color = colors.ink,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Primary CTA — opens homepage in browser (WN-8)
                PrimaryButton(
                    label = "DOWNLOAD ON HOMEPAGE →",
                    onClick = onDownloadClick,
                )

                Text(
                    text = "Remind me later",
                    style = typography.body.copy(
                        color = colors.inkSecondary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onDismiss() }
                        .padding(vertical = 12.dp),
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
