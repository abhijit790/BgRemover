package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.example.data.database.HistoryEntity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ui.BackgroundType
import com.example.ui.MainViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val history by viewModel.history.collectAsStateWithLifecycle()

    val backgroundColors = remember {
        listOf(
            Color.White,
            Color.Black,
            Color(0xFFFF5964), // Crimson Coral
            Color(0xFF35A7FF), // Bright Blue
            Color(0xFF386150), // Olive Deep
            Color(0xFFFFC857), // Golden Marigold
            Color(0xFF8B5CF6), // Cosmic Violet
            Color(0xFF10B981)  // Mint Emerald
        )
    }

    val backgroundGradients = remember {
        listOf(
            listOf(Color(0xFFE0C3FC), Color(0xFF8EC5FC)), // Bubblegum pastel
            listOf(Color(0xFFF6D365), Color(0xFFFDA085)), // Sunset Warmth
            listOf(Color(0xFFFF9A9E), Color(0xFFFECFEF)), // Cotton Candy
            listOf(Color(0xFF111827), Color(0xFF312E81)), // Deep Void Indigo
            listOf(Color(0xFF00F2FE), Color(0xFF4FACFE)), // Frozen Ice
            listOf(Color(0xFF84FAB0), Color(0xFF8FD3F4))  // Forest Sky
        )
    }

    // Photo pickers
    val editImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.selectImage(it) }
    }

    val customBgPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.selectCustomBackgroundImage(it) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = "App Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Background Remover",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            letterSpacing = (-0.5).sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleHistory() },
                        modifier = Modifier.testTag("history_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (history.isNotEmpty()) {
                                    Badge { Text(history.size.toString()) }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "History Toggle",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val isLandscape = maxWidth > maxHeight

            if (isLandscape) {
                // Wide Screen Layout
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        EditorCanvas(
                            viewModel = viewModel,
                            onPickImage = {
                                editImagePicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        InteractiveControlsCard(
                            viewModel = viewModel,
                            backgroundColors = backgroundColors,
                            backgroundGradients = backgroundGradients,
                            onPickCustomBg = {
                                customBgPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onPickNewImage = {
                                editImagePicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )
                    }
                }
            } else {
                // Portrait Layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        EditorCanvas(
                            viewModel = viewModel,
                            onPickImage = {
                                editImagePicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )
                    }

                    InteractiveControlsCard(
                        viewModel = viewModel,
                        backgroundColors = backgroundColors,
                        backgroundGradients = backgroundGradients,
                        onPickCustomBg = {
                            customBgPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                        },
                        onPickNewImage = {
                            editImagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }
            }

            // Processing Overlay with Scanning Animation
            if (viewModel.isProcessing) {
                ProcessingOverlay(progressText = viewModel.progressText)
            }

            // Error Dialog
            viewModel.errorMessage?.let { error ->
                AlertDialog(
                    onDismissRequest = { viewModel.clearActiveEditor() }, // reset state to allow retrying
                    confirmButton = {
                        Button(onClick = { viewModel.clearActiveEditor() }) {
                            Text("OK")
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Warning, contentDescription = "Alert", tint = MaterialTheme.colorScheme.error)
                            Text("Processing Failed")
                        }
                    },
                    text = { Text(error) }
                )
            }

            // Slide Up Conversion History Shelf Dialog
            if (viewModel.isHistoryExpanded) {
                HistoryShelf(
                    history = history,
                    onClose = { viewModel.toggleHistory() },
                    onLoadRecord = { viewModel.loadHistoryRecord(it) },
                    onDeleteRecord = { viewModel.deleteHistoryRecord(it) },
                    onClearAll = { viewModel.clearAllHistory() }
                )
            }
        }
    }
}

/**
 * The core design canvas card supporting interactive sliders, loading scanning, and drag states.
 */
@Composable
fun EditorCanvas(
    viewModel: MainViewModel,
    onPickImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val originalPath = viewModel.originalImagePath
    val removedPath = viewModel.removedImagePath

    Card(
        modifier = modifier
            .fillMaxSize()
            .shadow(4.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (originalPath == null) {
                // Pre-Upload Empty State with Matte Glass Styling
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(18.dp))
                        .border(
                            2.dp,
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    Color.Transparent
                                )
                            ),
                            RoundedCornerShape(18.dp)
                        )
                        .clickable { onPickImage() }
                        .padding(24.dp)
                        .testTag("image_picker_area"),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Select Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Touch to choose an image",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Supported: PNG, JPEG (Up to 12MB)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            } else {
                // Interactive active sandbox
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(18.dp))
                ) {
                    if (removedPath == null) {
                        // Original loaded, waiting for extraction trigger
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = File(originalPath),
                                contentDescription = "Original Selection",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )

                            // Quick trigger float button
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 16.dp)
                                    .shadow(6.dp, CircleShape),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clickable { viewModel.removeBackground() }
                                        .padding(horizontal = 24.dp, vertical = 12.dp)
                                        .testTag("remove_bg_direct"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Done,
                                        contentDescription = "Extract background icon",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Text(
                                        text = "✨ Remove Background",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    } else {
                        // Background Extraction Completed. Render Interactive Dual View Slider.
                        CompareSliderLayout(
                            viewModel = viewModel,
                            originalPath = originalPath,
                            removedPath = removedPath
                        )
                    }
                }
            }
        }
    }
}

/**
 * Renders the processed transparent PNG with dynamic backgrounds applied underneath,
 * and handles horizontal draping overlays of the original image for pristine comparison.
 */
@Composable
fun CompareSliderLayout(
    viewModel: MainViewModel,
    originalPath: String,
    removedPath: String,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        var sliderPos by remember { mutableFloatStateOf(viewModel.sliderPosition) }

        // Sync slider position periodically to maintain VM coordination
        LaunchedEffect(viewModel.sliderPosition) {
            sliderPos = viewModel.sliderPosition
        }

        // Custom shape clipping original to left side of dragging divisor
        val horizontalClip = remember(sliderPos) {
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density
                ): Outline {
                    val path = Path().apply {
                        addRect(Rect(0f, 0f, size.width * sliderPos, size.height))
                    }
                    return Outline.Generic(path)
                }
            }
        }

        // 1. Processed Layer (Background styled dynamically beneath transparent PNG)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Draw checkers for transparency
            val darkTheme = isSystemInDarkTheme()
            Canvas(modifier = Modifier.fillMaxSize()) {
                val squareSize = 10.dp.toPx()
                val numCols = (size.width / squareSize).toInt() + 1
                val numRows = (size.height / squareSize).toInt() + 1
                for (c in 0 until numCols) {
                    for (r in 0 until numRows) {
                        val color = if ((c + r) % 2 == 0) {
                            if (darkTheme) Color(0xFF1D222B) else Color(0xFFEEEEEE)
                        } else {
                            if (darkTheme) Color(0xFF14181F) else Color(0xFFDDDDDD)
                        }
                        drawRect(
                            color = color,
                            topLeft = Offset(c * squareSize, r * squareSize),
                            size = Size(squareSize, squareSize)
                        )
                    }
                }
            }

            // Fill with dynamic background type requested
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        when (viewModel.activeBgType) {
                            BackgroundType.TRANSPARENT -> Modifier
                            BackgroundType.SOLID -> Modifier.background(viewModel.activeSolidColor)
                            BackgroundType.GRADIENT -> Modifier.background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        viewModel.activeGradientStart,
                                        viewModel.activeGradientEnd
                                    )
                                )
                            )
                            BackgroundType.CUSTOM_IMAGE -> Modifier // Drawn inside below if path exists
                        }
                    )
            ) {
                // If custom background selected, draw underneath foreground
                if (viewModel.activeBgType == BackgroundType.CUSTOM_IMAGE && viewModel.customBgImagePath != null) {
                    AsyncImage(
                        model = File(viewModel.customBgImagePath!!),
                        contentDescription = "Custom BG Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )
                }

                // Foreground transparent processed PNG
                AsyncImage(
                    model = File(removedPath),
                    contentDescription = "Background Removed Picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // 2. Original Layer (Clipped to the left side)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(horizontalClip)
        ) {
            AsyncImage(
                model = File(originalPath),
                contentDescription = "Original Background Picture",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        // 3. Draggable partition Divider with interactive Handle
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(48.dp)
                .align(Alignment.CenterStart)
                .offset(x = (maxWidth * sliderPos) - 24.dp)
                .pointerInput(widthPx) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val delta = dragAmount.x / widthPx
                        val newPos = (sliderPos + delta).coerceIn(0f, 1f)
                        sliderPos = newPos
                        viewModel.sliderPosition = newPos
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Thin glowing center line
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(Color.White.copy(alpha = 0.9f))
            )

            // Neon dynamic slider handle button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .border(2.dp, Color.White, CircleShape)
                    .shadow(4.dp, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy((-1).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown, // rotate or left direction placeholder
                        contentDescription = "Left",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .size(16.dp)
                            .pointerInput(Unit) {}
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp, // right direction placeholder
                        contentDescription = "Right",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .size(16.dp)
                            .pointerInput(Unit) {}
                    )
                }
            }
        }
    }
}

/**
 * Renders design choices, export triggers, and clean modular config cards.
 */
@Composable
fun InteractiveControlsCard(
    viewModel: MainViewModel,
    backgroundColors: List<Color>,
    backgroundGradients: List<List<Color>>,
    onPickCustomBg: () -> Unit,
    onPickNewImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isApiKeyVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // API Key Section (Accordion design)
        ElevatedCard(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isApiKeyVisible = !isApiKeyVisible },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Config Icon",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "remove.bg Configuration",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Icon(
                        imageVector = if (isApiKeyVisible) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Accordion state"
                    )
                }

                AnimatedVisibility(
                    visible = isApiKeyVisible,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = viewModel.apiKey,
                            onValueChange = { viewModel.apiKey = it },
                            label = { Text("remove.bg API Key") },
                            placeholder = { Text("Enter Key...") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("api_key_field"),
                            trailingIcon = {
                                if (viewModel.apiKey.isNotBlank()) {
                                    IconButton(onClick = { viewModel.apiKey = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            }
                        )

                        Text(
                            text = "Provided with i5u7iHxNPJzfwYwNMrMth6Ew for instant use.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )

                        // Size choosing
                        Divider()
                        Text(
                            text = "Extraction Resolution",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = viewModel.selectedSize == "preview",
                                onClick = { viewModel.selectedSize = "preview" },
                                label = { Text("Preview (Free/0.25 credit)") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = viewModel.selectedSize == "auto",
                                onClick = { viewModel.selectedSize = "auto" },
                                label = { Text("Auto / Full (1 credit)") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Active Creative Studio (Rendered only when a removed image exists)
        if (viewModel.removedImagePath != null) {
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "🎨 Background Sandbox",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Tab buttons
                    ScrollableTabRow(
                        selectedTabIndex = viewModel.activeBgType.ordinal,
                        edgePadding = 0.dp,
                        divider = {},
                        indicator = {}
                    ) {
                        BackgroundType.values().forEach { type ->
                            Tab(
                                selected = viewModel.activeBgType == type,
                                onClick = { viewModel.activeBgType = type },
                                text = {
                                    Text(
                                        text = type.name.lowercase().capitalize(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                },
                                modifier = Modifier
                                    .padding(vertical = 4.dp, horizontal = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (viewModel.activeBgType == type) MaterialTheme.colorScheme.primaryContainer
                                        else Color.Transparent
                                    )
                            )
                        }
                    }

                    // Bottom Custom Panels
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (viewModel.activeBgType) {
                            BackgroundType.TRANSPARENT -> {
                                Text(
                                    text = "Transparent chequered PNG backing.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                            BackgroundType.SOLID -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    backgroundColors.forEach { color ->
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(color, CircleShape)
                                                .border(
                                                    width = if (viewModel.activeSolidColor == color) 3.dp else 1.dp,
                                                    color = if (viewModel.activeSolidColor == color) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                    shape = CircleShape
                                                )
                                                .clickable { viewModel.activeSolidColor = color }
                                                .padding(4.dp)
                                        )
                                    }
                                }
                            }
                            BackgroundType.GRADIENT -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    backgroundGradients.forEach { gradient ->
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(gradient[0], gradient[1])
                                                    )
                                                )
                                                .border(
                                                    width = if (viewModel.activeGradientStart == gradient[0]) 3.dp else 0.dp,
                                                    color = if (viewModel.activeGradientStart == gradient[0]) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    viewModel.activeGradientStart = gradient[0]
                                                    viewModel.activeGradientEnd = gradient[1]
                                                }
                                        )
                                    }
                                }
                            }
                            BackgroundType.CUSTOM_IMAGE -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (viewModel.customBgImagePath != null) {
                                        Box(
                                            modifier = Modifier
                                                .height(80.dp)
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                                        ) {
                                            AsyncImage(
                                                model = File(viewModel.customBgImagePath!!),
                                                contentDescription = "Active Custom BG",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = onPickCustomBg,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Add PG")
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Select custom BG Image")
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Divider()

                    // Primary sandbox triggers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.saveResultToDeviceGallery { success ->
                                    val text = if (success) {
                                        "Saved completed file successfully to Pictures Gallery!"
                                    } else {
                                        "Error saving file to MediaStore gallery."
                                    }
                                    Toast.makeText(context, text, Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.Done, contentDescription = "Save Icon")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Download Photo", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.clearActiveEditor() },
                            modifier = Modifier
                                .weight(0.8f)
                                .testTag("reset_button"),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset Icon")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        } else if (viewModel.originalImagePath != null) {
            // Original exists but background has not been extracted yet
            Button(
                onClick = { viewModel.removeBackground() },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("extract_big_trigger_button")
                    .padding(vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Icon(Icons.Default.Done, contentDescription = "Extraction trigger")
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Extract Background",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

/**
 * Clean scanning laser animations over a glowing blurred background to signify extraction.
 */
@Composable
fun ProcessingOverlay(
    progressText: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxSize()
            .clickable(enabled = false) {}, // Intercept touch events
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.65f)
        ),
        shape = RoundedCornerShape(0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "laser")
            val positionFactor by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "factor"
            )

            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                // Spinning loading wheel
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(80.dp)
                )

                // Laser Scan Line
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val y = positionFactor * size.height
                    val verticalRadius = 25.dp.toPx()
                    val brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFFa78bfa).copy(alpha = 0.4f),
                            Color(0xFFa78bfa),
                            Color(0xFFa78bfa).copy(alpha = 0.4f),
                            Color.Transparent
                        ),
                        startY = y - verticalRadius,
                        endY = y + verticalRadius
                    )

                    drawRect(
                        brush = brush,
                        topLeft = Offset(0f, y - verticalRadius),
                        size = Size(size.width, verticalRadius * 2)
                    )

                    // Precise focus beam core line
                    drawLine(
                        color = Color(0xFFa78bfa),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = progressText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Processing on remove.bg cloud engine...",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Historical bottom shelf showing local conversions in indexed cards.
 */
@Composable
fun HistoryShelf(
    history: List<HistoryEntity>,
    onClose: () -> Unit,
    onLoadRecord: (HistoryEntity) -> Unit,
    onDeleteRecord: (HistoryEntity) -> Unit,
    onClearAll: () -> Unit
) {
    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .testTag("history_shelf_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Conversion History",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleLarge
                    )

                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close Shelf")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (history.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Empty History",
                            tint = Color.LightGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No saved conversions yet.",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Gray,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(history) { record ->
                            HistoryItemRow(
                                record = record,
                                onClick = { onLoadRecord(record) },
                                onDelete = { onDeleteRecord(record) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = onClearAll,
                        modifier = Modifier.fillMaxWidth().testTag("clear_history_direct"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Discard")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear All Offline History")
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItemRow(
    record: HistoryEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Images side by side
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                    ) {
                        AsyncImage(
                            model = File(record.originalPath),
                            contentDescription = "Original Thumb",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "arrow", modifier = Modifier.size(16.dp))

                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                    ) {
                        AsyncImage(
                            model = File(record.removedPath),
                            contentDescription = "Removed Thumb",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Column {
                    Text(
                        text = "Success removal",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "ID: ${record.id} • ${formatTimestamp(record.timestamp)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete record",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
