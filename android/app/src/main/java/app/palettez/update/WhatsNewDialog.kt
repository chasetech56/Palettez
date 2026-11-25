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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.palettez.ui.components.IconCircleButton
import app.palettez.ui.components.PrimaryButton
import app.palettez.ui.theme.CardRadius
import app.palettez.ui.theme.PalettezTheme

@Composable
fun WhatsNewDialog(
    releaseNote: ReleaseNote = WhatsNewRegistry.getLatestRelease(),
    onDismiss: () -> Unit,
) {
    val colors = PalettezTheme.colors
    val typography = PalettezTheme.typography

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.ground)
                .statusBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Header with Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Celebratory status badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.lime)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF151515),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "LATEST BUILD · v${releaseNote.versionName}",
                                style = typography.caption.copy(
                                    color = Color(0xFF151515),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }
                    }

                    IconCircleButton(
                        icon = Icons.Filled.Close,
                        contentDescription = "Dismiss what's new",
                        onClick = onDismiss
                    )
                }

                // 2. Headline & Congratulations
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "WHAT'S NEW",
                        style = typography.display.copy(fontSize = 44.sp, lineHeight = 46.sp),
                        color = colors.ink
                    )
                    Text(
                        text = "Congratulations! You're on the latest build of Palettez (${releaseNote.versionName}).",
                        style = typography.body.copy(color = colors.ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    )
                    Text(
                        text = "Here are the new tools and improvements crafted for your design workflow:",
                        style = typography.caption.copy(color = colors.inkMuted, fontSize = 13.sp)
                    )
                }

                // 3. Changelog Items Cards (WN-3)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    releaseNote.items.forEach { item ->
                        val (tagBg, tagText) = when (item.tag) {
                            ChangeTag.New -> Pair(colors.lime, Color(0xFF151515))
                            ChangeTag.Improved -> Pair(colors.sky, Color(0xFF151515))
                            ChangeTag.Fixed -> Pair(colors.coral, Color.White)
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(CardRadius))
                                .background(colors.surface)
                                .border(1.dp, colors.line, RoundedCornerShape(CardRadius))
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Tag badge
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(tagBg)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = item.tag.label,
                                            style = typography.caption.copy(
                                                color = tagText,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                letterSpacing = 0.5.sp
                                            )
                                        )
                                    }

                                    Text(
                                        text = item.title,
                                        style = typography.section.copy(
                                            color = colors.ink,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp
                                        )
                                    )
                                }

                                Text(
                                    text = item.description,
                                    style = typography.body.copy(
                                        color = colors.inkMuted,
                                        fontSize = 13.sp,
                                        lineHeight = 19.sp
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 4. Primary CTA Button (WN-4)
                PrimaryButton(
                    label = "EXPLORE NEW FEATURES",
                    onClick = onDismiss,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }
        }
    }
}
