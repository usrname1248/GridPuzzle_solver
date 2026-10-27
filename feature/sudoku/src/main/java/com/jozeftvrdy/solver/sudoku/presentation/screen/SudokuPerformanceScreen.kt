package com.jozeftvrdy.solver.sudoku.presentation.screen


import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.jozeftvrdy.solver.core.textrecognition.HandwrittenRecognizerModel
import com.jozeftvrdy.solver.sudoku.domain.ProcessSudokuImageUseCase
import com.jozeftvrdy.solver.sudoku.model.SudokuPosition
import com.jozeftvrdy.solver.sudoku.presentation.component.BorderSide
import com.jozeftvrdy.solver.sudoku.presentation.component.SudokuField
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import java.io.File
import java.io.FileOutputStream
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.math.roundToInt

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SudokuPerformanceScreen(
    processSudokuImageUseCase: ProcessSudokuImageUseCase = koinInject()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var loadedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Ready. Import or capture a Sudoku image.") }
    var progressText by remember { mutableStateOf("") }
    var top5Results by remember { mutableStateOf<List<Pair<HandwrittenRecognizerModel, Int>>>(emptyList()) }
    var isCameraMode by remember { mutableStateOf(false) }

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    // System File/Photo Picker (No permissions needed on Android 10+)
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = runCatching {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }.getOrNull()

            if (bitmap != null) {
                loadedBitmap = bitmap
                statusMessage = "Loaded file image (${bitmap.width}x${bitmap.height} px)"
                top5Results = emptyList()
                progressText = ""
            } else {
                statusMessage = "Failed to load image from selected file."
            }
        }
    }

    if (isCameraMode) {
        if (cameraPermissionState.status.isGranted) {
            SudokuTemporaryCameraView(
                onPhotoCaptured = { photoBitmap ->
                    loadedBitmap = photoBitmap
                    statusMessage = "Captured camera image (${photoBitmap.width}x${photoBitmap.height} px)"
                    top5Results = emptyList()
                    progressText = ""
                    isCameraMode = false
                },
                onCancel = {
                    isCameraMode = false
                }
            )
        } else {
            SudokuTemporaryCameraPermissionContent(
                permissionState = cameraPermissionState,
                onCancel = {
                    isCameraMode = false
                }
            )
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Sudoku Parameter Grid Search",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = statusMessage,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                if (progressText.isNotEmpty()) {
                    Text(
                        text = progressText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            pickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Text("Import file")
                    }

                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            isCameraMode = true
                        }
                    ) {
                        Text("Import camera")
                    }

                    Button(
                        modifier = Modifier.weight(1f),
                        enabled = loadedBitmap != null,
                        onClick = {
                            val bitmapToExport = loadedBitmap ?: return@Button
                            val result = exportBitmapToDownloads(context, bitmapToExport)
                            statusMessage = if (result.isSuccess) {
                                "Exported to Downloads/GridPuzzle!"
                            } else {
                                "Export failed: ${result.exceptionOrNull()?.message}"
                            }
                        }
                    ) {
                        Text("Export file")
                    }
                }

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = loadedBitmap != null && !isProcessing,
                    onClick = {
                        val bitmapToProcess = loadedBitmap ?: return@Button
                        isProcessing = true
                        statusMessage = "Running parallel parameter search..."
                        progressText = "Starting..."

                        scope.launch {
                            val results: SnapshotStateList<Pair<HandwrittenRecognizerModel, Int>> = mutableStateListOf()
                            runParameterSearch(
                                useCase = processSudokuImageUseCase,
                                bitmap = bitmapToProcess,
                                onProgress = { current, total ->
                                    val percent = (current * 100) / total
                                    progressText = "Progress: $current / $total ($percent%)"
                                },
                                onResultsUpdated = {
                                    results.clear()
                                    results.addAll(it)
                                    top5Results = results
                                }
                            )

                            Log.w("SudokuSearch", "--- TOP 5 PARAMETER SEARCH RESULTS ---")
                            results.forEachIndexed { index, (params, matches) ->
                                val percentage = (matches * 1000 / 81) / 10.0f
                                Log.w(
                                    "SudokuSearch",
                                    "#${index + 1}: Score = $matches/81 ($percentage%) | " +
                                            "assetName = ${params.assetName}, " +
                                            "library = ${params.library}"
                                )
                            }

                            isProcessing = false
                            statusMessage = "Search completed! Top 5 combinations shown below."
                        }
                    }
                ) {
                    if (isProcessing) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Text("Processing Grid Search...")
                        }
                    } else {
                        Text("Process Parameter Search")
                    }
                }

                loadedBitmap?.let { bitmap ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Image Preview:", fontWeight = FontWeight.SemiBold)
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Loaded Preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }

                if (top5Results.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "🏆 Top 5 Parameter Results (out of 81 tiles)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    top5Results.forEachIndexed { index, (params, matches) ->
                        val percentage = (matches * 1000 / 81) / 10.0f
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            tonalElevation = 4.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "#${index + 1} - Match Score: $matches / 81 ($percentage%)",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (matches == 81) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Params:",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = " • assetName: ${params.assetName}\n" +
                                            " • library: ${params.library}",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun calculateBorderSide(position: SudokuPosition) : List<BorderSide> = buildList {
    if (position.y % 3 == 0) {
        add(BorderSide.Bottom)
    }

    if ((position.y - 1) % 3 == 0) {
        add(BorderSide.Top)
    }

    if (position.x % 3 == 0) {
        add(BorderSide.Right)
    }

    if ((position.x - 1) % 3 == 0) {
        add(BorderSide.Left)
    }
}

@Composable
private fun SudokuTemporaryCameraView(
    onPhotoCaptured: (Bitmap) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isCapturing by remember { mutableStateOf(false) }

    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE or CameraController.VIDEO_CAPTURE)
        }
    }

    val positions = remember {
        buildList {
            for (y in 1..9) {
                for (x in 1..9) {
                    add(SudokuPosition(x, y))
                }
            }
        }.toImmutableList()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CameraPreview(
            controller = controller,
            modifier = Modifier.fillMaxSize()
        )

        SudokuField(
            modifier = Modifier.fillMaxWidth(0.9f),
            positions = positions,
            provideBorderSide = ::calculateBorderSide,
            provideBackgroundColor = { Color.Transparent }
        ) { }

        Row(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = onCancel
            ) {
                Text("Cancel")
            }

            Button(
                modifier = Modifier.weight(1f),
                enabled = !isCapturing,
                onClick = {
                    isCapturing = true
                    scope.launch(Dispatchers.Main) {
                        val originalPicture = takePhoto(controller, context).getOrNull()
                        isCapturing = false
                        if (originalPicture != null) {
                            onPhotoCaptured(originalPicture)
                        }
                    }
                }
            ) {
                if (isCapturing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Take photo")
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun SudokuTemporaryCameraPermissionContent(
    permissionState: PermissionState,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Camera permission is required to capture photos directly.",
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { permissionState.launchPermissionRequest() }
        ) {
            Text("Grant camera permission")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onCancel
        ) {
            Text("Cancel")
        }
    }
}

private suspend fun takePhoto(
    controller: LifecycleCameraController,
    context: Context,
): Result<Bitmap> = suspendCancellableCoroutine { continuation ->
    controller.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                super.onCaptureSuccess(image)

                val cropRect = image.cropRect
                val matrix = Matrix().apply {
                    postRotate(image.imageInfo.rotationDegrees.toFloat())
                }
                val rotatedBitmap = Bitmap.createBitmap(
                    image.toBitmap(),
                    cropRect.left,
                    cropRect.top,
                    cropRect.width(),
                    cropRect.height(),
                    matrix,
                    true
                )

                continuation.resume(Result.success(rotatedBitmap))
            }

            override fun onError(exception: ImageCaptureException) {
                super.onError(exception)
                continuation.resume(Result.failure(exception))
            }
        }
    )
}

/**
 * Saves a Bitmap to public Downloads/GridPuzzle/ directory.
 */
private fun exportBitmapToDownloads(
    context: Context,
    bitmap: Bitmap,
    filename: String = "sudoku_test_${System.currentTimeMillis()}.jpg"
): Result<Uri> = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IllegalStateException("Failed to create MediaStore entry")

        resolver.openOutputStream(uri)?.use { outputStream ->
            val success = bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            if (!success) throw IllegalStateException("Failed to compress bitmap")
        }
        uri
    } else {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val targetDir = File(downloadsDir, "GridPuzzle").apply { mkdirs() }
        val file = File(targetDir, filename)

        FileOutputStream(file).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        }
        Uri.fromFile(file)
    }
}

private suspend fun runParameterSearch(
    useCase: ProcessSudokuImageUseCase,
    bitmap: Bitmap,
    onProgress: (current: Int, total: Int) -> Unit,
    onResultsUpdated: (List<Pair<HandwrittenRecognizerModel, Int>>) -> Unit
) = withContext(Dispatchers.Default) {

    val expectedResult = listOf(
        0, 8, 2, 0, 0, 0, 0, 5, 1,
        0, 4, 7, 0, 5, 0, 0, 6, 0,
        5, 9, 0, 0, 0, 8, 0, 3, 2,
        0, 0, 0, 0, 9, 0, 0, 0, 5,
        4, 1, 0, 0, 8, 6, 2, 9, 0,
        0, 5, 0, 2, 0, 0, 6, 8, 0,
        0, 3, 0, 0, 6, 7, 0, 0, 9,
        0, 0, 0, 9, 0, 0, 0, 7, 3,
        0, 0, 0, 0, 0, 3, 8, 2, 6,
    )

    // 1. Build ranges
    val cropEdgesValues = generateSequence(0.02f) { (it + 0.02f).takeIf { val1 -> val1 <= 0.125f } }
        .map { (it * 100).roundToInt() / 100f }
        .toList()

    val firstPadValues = generateSequence(1.00f) { (it + 0.05f).takeIf { val2 -> val2 <= 1.21f } }
        .map { (it * 100).roundToInt() / 100f }
        .toList()

    val tileCountValues = (3..5).toList()

    val secondPadValues = generateSequence(1.5f) { (it + 0.5f).takeIf { val3 -> val3 <= 2.6f } }
        .map { (it * 100).roundToInt() / 100f }
        .toList()

    val allParams = HandwrittenRecognizerModel.entries

    val totalIterations = allParams.size // 270 iterations
    val resultsList = Collections.synchronizedList(mutableListOf<Pair<HandwrittenRecognizerModel, Int>>())
    val completedCounter = AtomicInteger(0)

    // 2. Parallel Execution using Flow flatMapMerge (max 4 concurrent active workers)
    @OptIn(ExperimentalCoroutinesApi::class)
    allParams.asFlow()
        .flatMapMerge(concurrency = 12) { params ->
            flow {
                val returnedResults = useCase(bitmap, params).associateBy { it.position }
                var matchesCount = 0

                for (y in 1..9) {
                    for (x in 1..9) {
                        val returnedValue = returnedResults[SudokuPosition(x, y)]?.value ?: 0
                        val expectedValue = expectedResult[(y - 1) * 9 + (x - 1)]
                        if (returnedValue == expectedValue) {
                            matchesCount++
                        }
                    }
                }

                emit(params to matchesCount)
            }
        }
        // 4. Update the UI progress every time an item finishes
        .onEach { (params, matchesCount) ->
            val currentCompleted = completedCounter.incrementAndGet()
            // withContext(Dispatchers.Main) is automatically handled if you collect on the Main thread,
            // but adding it explicitly here guarantees safety:
            withContext(Dispatchers.Main) {
                onProgress(currentCompleted, totalIterations)
                resultsList.add(params to matchesCount)
                resultsList.sortByDescending { it.second }
                onResultsUpdated(resultsList.take(5))
            }

        }
        .collect()

    // 3. Take Top 5 Best
}
