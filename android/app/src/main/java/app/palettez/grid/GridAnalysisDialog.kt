package app.palettez.grid

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.automirrored.outlined.ViewQuilt
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.palettez.layout.LayoutEngine
import app.palettez.ui.components.IconCircleButton
import app.palettez.ui.components.PrimaryButton
import app.palettez.ui.theme.PalettezTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun GridAnalysisDialog(
    bitmap: Bitmap,
    onDismiss: () -> Unit,
    onApplyToColumns: (GridSpec) -> Unit,
    onOpenInLayouts: (LayoutEngine.LayoutTemplate) -> Unit,
) {
    val colors = PalettezTheme.colors
    val typography = PalettezTheme.typography

    // Compute analysis on background thread
    var analysisResult by remember(bitmap) { mutableStateOf<GridAnalysisResult?>(null) }
    LaunchedEffect(bitmap) {
        analysisResult = withContext(Dispatchers.Default) {
            GridAnalysisEngine.analyze(bitmap)
        }
    }

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
                    Column {
                        Text(
                            text = "GRID ANALYSIS",
                            style = typography.display.copy(fontSize = 32.sp, lineHeight = 36.sp),
                            color = colors.ink
                        )
                        Text(
                            text = "On-device layout reverse engineering",
                            style = typography.caption,
                            color = colors.inkMuted
                        )
                    }
                    IconCircleButton(
                        icon = Icons.Filled.Close,
                        contentDescription = "Close grid analysis",
                        onClick = onDismiss
                    )
                }

                // 2. Visual Overlay Stage (GA-5)
                val imgRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(imgRatio.coerceIn(0.5f, 2.0f))
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.line, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Base Image
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Source poster",
                        modifier = Modifier.fillMaxSize()
                    )

                    // Grid Overlay Layer
                    val result = analysisResult
                    if (result != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.25f))
                        ) {
                            // Column bands overlay
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(
                                        start = (result.estimatedMarginLeft.toFloat() / result.imageWidth * 300).dp,
                                        end = (result.estimatedMarginRight.toFloat() / result.imageWidth * 300).dp,
                                        top = (result.estimatedMarginTop.toFloat() / result.imageHeight * 300).dp,
                                        bottom = (result.estimatedMarginBottom.toFloat() / result.imageHeight * 300).dp
                                    ),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                repeat(result.estimatedColumns) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxSize()
                                            .background(colors.lime.copy(alpha = 0.22f))
                                            .border(1.dp, colors.lime.copy(alpha = 0.7f))
                                    )
                                }
                            }

                            // Detected Badge overlay
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF151515).copy(alpha = 0.88f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${result.estimatedColumns} COLS · ${result.confidence}% CONFIDENCE",
                                    style = typography.caption.copy(color = colors.lime, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                )
                            }
                        }
                    } else {
                        CircularProgressIndicator(color = colors.lime, modifier = Modifier.size(36.dp))
                    }
                }

                // 3. Analysis Inspector Details Card
                val result = analysisResult
                if (result != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.surface)
                            .padding(18.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(
                                text = "ANALYSIS RESULTS",
                                style = typography.label.copy(fontWeight = FontWeight.Bold),
                                color = colors.ink
                            )

                            // Readout Row 1: Columns & Gutter
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                InfoTile(
                                    label = "ESTIMATED COLUMNS",
                                    value = "${result.estimatedColumns}",
                                    subtext = "Symmetrical grid",
                                    modifier = Modifier.weight(1f)
                                )
                                InfoTile(
                                    label = "GUTTER WIDTH",
                                    value = "${result.estimatedGutterPx} px",
                                    subtext = "Average spacing",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Readout Row 2: Family & Aspect
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                InfoTile(
                                    label = "LAYOUT FAMILY",
                                    value = result.detectedFamily.name,
                                    subtext = "${result.confidence}% algorithm match",
                                    modifier = Modifier.weight(1f)
                                )
                                InfoTile(
                                    label = "ASPECT RATIO",
                                    value = result.detectedAspectDescription,
                                    subtext = "${result.imageWidth}×${result.imageHeight}",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Margins breakdown
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "MARGINS (L/R/T/B)",
                                    style = typography.caption.copy(color = colors.inkMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${result.estimatedMarginLeft}px left · ${result.estimatedMarginRight}px right · ${result.estimatedMarginTop}px top · ${result.estimatedMarginBottom}px bottom",
                                    style = typography.body.copy(color = colors.ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }

                    // 4. Action Buttons (GA-6)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Apply to Columns Grid
                        PrimaryButton(
                            label = "APPLY TO COLUMNS GRID",
                            onClick = {
                                onApplyToColumns(result.toGridSpec())
                            }
                        )

                        // Open in Panel Layouts
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(colors.surface)
                                .border(1.dp, colors.line, RoundedCornerShape(28.dp))
                                .clickable {
                                    onOpenInLayouts(result.recommendedTemplate)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ViewQuilt,
                                    contentDescription = null,
                                    tint = colors.ink,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "OPEN IN PANEL LAYOUTS (${result.recommendedTemplate.name.uppercase()})",
                                    style = typography.label.copy(color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoTile(
    label: String,
    value: String,
    subtext: String,
    modifier: Modifier = Modifier,
) {
    val colors = PalettezTheme.colors
    val typography = PalettezTheme.typography

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.ground)
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = typography.caption.copy(color = colors.inkMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            )
            Text(
                text = value,
                style = typography.section.copy(color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            )
            Text(
                text = subtext,
                style = typography.caption.copy(color = colors.inkMuted, fontSize = 11.sp)
            )
        }
    }
}
