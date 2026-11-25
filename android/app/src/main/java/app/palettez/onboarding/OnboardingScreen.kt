package app.palettez.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.palettez.settings.AppTheme
import app.palettez.ui.components.IconCircleButton
import app.palettez.ui.theme.PalettezTheme

@Composable
fun OnboardingScreen(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    isReplay: Boolean = false,
) {
    var step by remember { mutableIntStateOf(1) }
    val colors = PalettezTheme.colors
    val typography = PalettezTheme.typography

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ground)
    ) {
        when (step) {
            1 -> OnboardingSlide1(
                onSkip = onComplete,
                onNext = { step = 2 }
            )
            2 -> OnboardingSlide2(
                onBack = { step = 1 },
                onNext = { step = 3 }
            )
            3 -> OnboardingSlide3(
                currentTheme = currentTheme,
                onThemeSelected = onThemeSelected,
                onBack = { step = 2 },
                onFinish = onComplete
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Slide 1: Welcome
// ---------------------------------------------------------------------------
@Composable
private fun OnboardingSlide1(
    onSkip: () -> Unit,
    onNext: () -> Unit,
) {
    val colors = PalettezTheme.colors
    val typography = PalettezTheme.typography

    Box(modifier = Modifier.fillMaxSize()) {
        // Skip pill top right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 16.dp, end = 20.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(colors.surface)
                .clickable { onSkip() }
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Skip",
                style = typography.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                color = colors.ink
            )
        }

        // Illustration Collage
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(440.dp)
                .padding(top = 40.dp)
        ) {
            // Purple card
            Box(
                modifier = Modifier
                    .offset(x = 24.dp, y = 60.dp)
                    .size(width = 170.dp, height = 200.dp)
                    .rotate(-7f)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color(0xFF6A58CC))
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Aa",
                        fontSize = 86.sp,
                        lineHeight = 90.sp,
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic,
                        fontFamily = FontFamily.Serif,
                        color = Color.White
                    )
                    Text(
                        text = "Playfair Display",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Yellow card
            Box(
                modifier = Modifier
                    .offset(x = 180.dp, y = 40.dp)
                    .size(width = 175.dp, height = 165.dp)
                    .rotate(6f)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color(0xFFF4C542))
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = "#F4C542",
                        style = typography.display.copy(fontSize = 28.sp, lineHeight = 32.sp),
                        color = Color(0xFF151515),
                        maxLines = 1,
                        softWrap = false,
                    )
                    Text(
                        text = "0 · 19 · 73 · 4",
                        style = typography.value.copy(fontSize = 11.sp),
                        color = Color(0xFF151515)
                    )
                }
            }

            // Blue grid card
            Box(
                modifier = Modifier
                    .offset(x = 160.dp, y = 220.dp)
                    .size(width = 180.dp, height = 160.dp)
                    .rotate(-3f)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color(0xFFAFCBEA))
                    .padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        repeat(4) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .background(Color(0x33FF3B6B))
                            )
                        }
                    }
                }
            }

            // Coral circle
            Box(
                modifier = Modifier
                    .offset(x = 40.dp, y = 280.dp)
                    .size(104.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE2603F)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aa",
                    style = typography.display.copy(fontSize = 42.sp),
                    color = Color.White
                )
            }

            // Lime dot
            Box(
                modifier = Modifier
                    .offset(x = 130.dp, y = 270.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFA7D86E))
            )
        }

        // Bottom content
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "PALETTEZ",
                style = typography.display.copy(fontSize = 60.sp, lineHeight = 62.sp),
                color = colors.ink
            )
            Text(
                text = "Colors, grids and type, free on your phone.",
                style = typography.body.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
                color = colors.ink
            )
            Text(
                text = "Pull palettes from photos, build layout grids and browse every Google Font, with no account and no ads.",
                style = typography.body.copy(fontSize = 15.sp, lineHeight = 22.sp),
                color = colors.inkMuted
            )

            // Step dots
            StepIndicator(currentStep = 1, totalSteps = 3)

            Spacer(Modifier.height(4.dp))

            // Get Started button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(colors.ink)
                    .clickable { onNext() },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "GET STARTED",
                        style = typography.buttonLabel.copy(fontSize = 22.sp),
                        color = colors.onInk
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = colors.onInk,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Slide 2: Colors from any photo
// ---------------------------------------------------------------------------
@Composable
private fun OnboardingSlide2(
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val colors = PalettezTheme.colors
    val typography = PalettezTheme.typography

    Column(modifier = Modifier.fillMaxSize()) {
        // Top stage
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
                .background(Color(0xFFE2603F))
        ) {
            // Giant COLOR COLOR text
            Text(
                text = "COLOR\nCOLOR",
                style = typography.display.copy(fontSize = 140.sp, lineHeight = 130.sp),
                color = Color(0x1F151515),
                modifier = Modifier
                    .offset(x = (-10).dp, y = 20.dp)
            )

            // Sunset card
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(width = 240.dp, height = 190.dp)
                    .rotate(-4f)
                    .shadow(16.dp, RoundedCornerShape(26.dp))
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xFFF6E7CB))
            ) {
                // Sun
                Box(
                    modifier = Modifier
                        .offset(x = 140.dp, y = 20.dp)
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF2A65A))
                )
                // Hills
                Box(
                    modifier = Modifier
                        .offset(x = (-40).dp, y = 88.dp)
                        .size(width = 230.dp, height = 100.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE4572E))
                )
                Box(
                    modifier = Modifier
                        .offset(x = 100.dp, y = 96.dp)
                        .size(width = 250.dp, height = 110.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF8C5383))
                )
                // Water
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 30.dp)
                        .fillMaxWidth()
                        .height(18.dp)
                        .background(Color(0xFF1B998B))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(30.dp)
                        .background(Color(0xFF2E4057))
                )
            }

            // Swatches row
            val swatchColors = listOf(
                Color(0xFFF6E7CB), Color(0xFFE4572E), Color(0xFF8C5383),
                Color(0xFFF2A65A), Color(0xFF2E4057), Color(0xFF1B998B)
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 68.dp, start = 30.dp, end = 30.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                swatchColors.forEach { c ->
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(c)
                            .border(3.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                    )
                }
            }

            // Copied toast pill
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF151515))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFFA7D86E),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Copied #E4572E",
                        style = typography.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                        color = Color.White
                    )
                }
            }
        }

        // Bottom area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "COLORS FROM\nANY PHOTO",
                style = typography.display.copy(fontSize = 44.sp, lineHeight = 48.sp),
                color = colors.ink
            )
            Text(
                text = "Pick a photo or snap one. Palettez finds its main colors and gives you HEX, RGB and CMYK values to copy with one tap.",
                style = typography.body.copy(fontSize = 15.sp, lineHeight = 22.sp),
                color = colors.inkMuted
            )

            // Guarantee card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.surface)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = colors.ink,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Your images never leave your phone.",
                        style = typography.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                        color = colors.ink
                    )
                }
            }

            StepIndicator(currentStep = 2, totalSteps = 3)

            // Nav buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(colors.surface)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.ink,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(colors.ink)
                        .clickable { onNext() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "NEXT",
                            style = typography.buttonLabel.copy(fontSize = 22.sp),
                            color = colors.onInk
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = colors.onInk,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Slide 3: Grids & Google Fonts
// ---------------------------------------------------------------------------
@Composable
private fun OnboardingSlide3(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit,
) {
    val colors = PalettezTheme.colors
    val typography = PalettezTheme.typography

    Column(modifier = Modifier.fillMaxSize()) {
        // Top stage
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
                .background(Color(0xFF1D2320))
        ) {
            // Grid artboard
            Box(
                modifier = Modifier
                    .offset(x = 24.dp, y = 40.dp)
                    .size(width = 180.dp, height = 230.dp)
                    .rotate(-5f)
                    .shadow(16.dp, RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(6) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .background(Color(0x2EFF3B6B))
                                .border(1.dp, Color(0x66FF3B6B))
                        )
                    }
                }
            }

            // Font Card 1 (Playfair Display)
            Box(
                modifier = Modifier
                    .offset(x = 180.dp, y = 36.dp)
                    .size(width = 160.dp, height = 90.dp)
                    .rotate(4f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFFF4C542))
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "Playfair Display",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF151515)
                    )
                    Text(
                        text = "Aa Gg",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 38.sp,
                        lineHeight = 44.sp,
                        color = Color(0xFF151515)
                    )
                }
            }

            // Font Card 2 (Caveat / Script)
            Box(
                modifier = Modifier
                    .offset(x = 196.dp, y = 136.dp)
                    .size(width = 150.dp, height = 86.dp)
                    .rotate(-3f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFFA7D86E))
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "Caveat",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF151515)
                    )
                    Text(
                        text = "Hello!",
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic,
                        fontSize = 38.sp,
                        lineHeight = 44.sp,
                        color = Color(0xFF151515)
                    )
                }
            }

            // Font Card 3 (Bebas Neue / Headline)
            Box(
                modifier = Modifier
                    .offset(x = 176.dp, y = 230.dp)
                    .size(width = 175.dp, height = 86.dp)
                    .rotate(3f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF6A58CC))
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "Bebas Neue",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "HEADLINE",
                        style = typography.display.copy(fontSize = 34.sp, lineHeight = 38.sp),
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }

            // NEW badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 24.dp, bottom = 20.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFA7D86E))
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "NEW fonts added automatically",
                    style = typography.body.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                    color = Color(0xFF151515)
                )
            }
        }

        // Bottom area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "GRIDS & EVERY\nGOOGLE FONT",
                style = typography.display.copy(fontSize = 44.sp, lineHeight = 48.sp),
                color = colors.ink
            )
            Text(
                text = "Build layout grids for posts, slides and print. Browse the whole Google Fonts library, which updates when new families are released.",
                style = typography.body.copy(fontSize = 15.sp, lineHeight = 21.sp),
                color = colors.inkMuted
            )

            // Choose your look
            Text(
                text = "Choose your look",
                style = typography.body.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                color = colors.ink
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppTheme.entries.forEach { themeOption ->
                    val selected = currentTheme == themeOption
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(colors.surface)
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) colors.ink else colors.line.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .clickable { onThemeSelected(themeOption) },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Swatch
                            when (themeOption) {
                                AppTheme.Light -> {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEFE9DF))
                                            .border(1.5.dp, Color(0xFFCFC6B8), CircleShape)
                                    )
                                }
                                AppTheme.Dark -> {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1D2320))
                                    )
                                }
                                AppTheme.System -> {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.horizontalGradient(
                                                    0.5f to Color(0xFFEFE9DF),
                                                    0.5f to Color(0xFF1D2320)
                                                )
                                            )
                                            .border(1.5.dp, Color(0xFFCFC6B8), CircleShape)
                                    )
                                }
                            }
                            Text(
                                text = themeOption.label,
                                style = typography.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                                color = colors.ink
                            )
                        }
                    }
                }
            }

            StepIndicator(currentStep = 3, totalSteps = 3)

            // Nav buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(colors.surface)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.ink,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(Color(0xFFA7D86E))
                        .clickable { onFinish() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "START DESIGNING",
                        style = typography.buttonLabel.copy(fontSize = 22.sp),
                        color = Color(0xFF151515)
                    )
                }
            }
        }
    }
}

@Composable
private fun StepIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    val colors = PalettezTheme.colors
    Row(
        modifier = modifier
            .semantics { contentDescription = "Step $currentStep of $totalSteps" }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (i in 1..totalSteps) {
            val isActive = i == currentStep
            Box(
                modifier = Modifier
                    .size(width = if (isActive) 28.dp else 8.dp, height = 8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isActive) colors.ink else colors.line)
            )
        }
    }
}
