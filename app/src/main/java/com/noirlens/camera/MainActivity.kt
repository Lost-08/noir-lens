package com.noirlens.camera

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor
import kotlin.math.roundToInt

private val Ink = Color(0xFF08090B)
private val Panel = Color(0xFF15171B)
private val Gold = Color(0xFFC5A46D)
private val Muted = Color(0xFF9A9DA5)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(8, 9, 11)
        window.navigationBarColor = android.graphics.Color.rgb(8, 9, 11)
        setContent { NoirLensApp() }
    }
}

@Composable
private fun NoirLensApp() {
    val context = LocalContext.current
    var permission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permission = it }
    var grid by remember { mutableStateOf(true) }
    var flash by remember { mutableStateOf(false) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var exposure by remember { mutableFloatStateOf(0f) }
    var selectedFilter by remember { mutableStateOf("NATURAL") }
    var filterIntensity by remember { mutableFloatStateOf(0.8f) }
    var mode by remember { mutableStateOf("PHOTO") }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var capture by remember { mutableStateOf<ImageCapture?>(null) }
    var pickedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var brightness by remember { mutableFloatStateOf(1f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var warmth by remember { mutableFloatStateOf(0f) }
    var showEditor by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("READY TO SHOOT") }
    LaunchedEffect(flash, capture) {
        capture?.flashMode = if (flash) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
    }
    val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } }
                .onSuccess { pickedBitmap = it; showEditor = it != null }
                .onFailure { Toast.makeText(context, "Couldn't open this image", Toast.LENGTH_SHORT).show() }
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(background = Ink, surface = Panel, primary = Gold, onBackground = Color.White, onSurface = Color.White)) {
        if (showEditor && pickedBitmap != null) {
            EditorScreen(bitmap = pickedBitmap!!, brightness = brightness, onBrightness = { brightness = it }, saturation = saturation, onSaturation = { saturation = it }, contrast = contrast, onContrast = { contrast = it }, warmth = warmth, onWarmth = { warmth = it }, onBack = { showEditor = false }, onSave = {
                val source = pickedBitmap!!
                val output = gradeBitmap(source, brightness, saturation, contrast, warmth)
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "NoirLens_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/NoirLens")
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                val saved = uri?.let { context.contentResolver.openOutputStream(it)?.use { stream -> output.compress(Bitmap.CompressFormat.JPEG, 95, stream) } } ?: false
                Toast.makeText(context, if (saved) "Graded photo saved to Pictures/NoirLens" else "Couldn't save photo", Toast.LENGTH_LONG).show()
            })
        } else {
            Column(Modifier.fillMaxSize().background(Ink).padding(horizontal = 18.dp)) {
                Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("NOIR LENS", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                        Text("PRO CAMERA  •  OFFLINE EDITING", color = Muted, fontSize = 9.sp, letterSpacing = 1.4.sp)
                    }
                    IconButton(onClick = { grid = !grid }) { Icon(Icons.Default.GridOn, "Toggle grid", tint = if (grid) Gold else Muted) }
                    IconButton(onClick = { galleryPicker.launch("image/*") }) { Icon(Icons.Default.PhotoLibrary, "Open photo editor", tint = Color.White) }
                }
                Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color(0xFF202329))) {
                    if (permission) {
                        CameraPreview(onCameraReady = { cam, imageCapture -> camera = cam; capture = imageCapture })
                        if (grid) RuleOfThirds(Modifier.fillMaxSize())
                        Column(Modifier.align(Alignment.TopStart).padding(14.dp)) {
                            Text("● LIVE", color = Color(0xFFDB796F), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("${zoom.format(1)}×", color = Color.White, fontSize = 12.sp)
                        }
                        Text("${if (exposure > 0) "+" else ""}${exposure.format(1)} EV", Modifier.align(Alignment.TopEnd).padding(14.dp), color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CameraAlt, null, tint = Gold, modifier = Modifier.size(54.dp))
                            Spacer(Modifier.height(12.dp)); Text("Camera access needed", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Allow camera access to use the viewfinder.", color = Muted, fontSize = 12.sp)
                            Spacer(Modifier.height(12.dp)); Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) { Text("Enable camera") }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    SmallTool(Icons.Default.FlashOn, if (flash) "FLASH ON" else "FLASH OFF", flash) {
                        val cam = camera
                        if (cam == null) {
                            Toast.makeText(context, "Camera is starting", Toast.LENGTH_SHORT).show()
                        } else if (!cam.cameraInfo.hasFlashUnit()) {
                            flash = false
                            capture?.flashMode = ImageCapture.FLASH_MODE_OFF
                            Toast.makeText(context, "This camera has no flash unit", Toast.LENGTH_LONG).show()
                        } else {
                            flash = !flash
                            capture?.flashMode = if (flash) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
                        }
                    }
                    SmallTool(Icons.Default.Tune, "EDIT PHOTO", false) { galleryPicker.launch("image/*") }
                    SmallTool(Icons.Default.AutoAwesome, "AI TOOLS", false) { Toast.makeText(context, "AI enhancement is not available yet; a real model/service must be integrated.", Toast.LENGTH_LONG).show() }
                }
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("EXPOSURE", color = Muted, fontSize = 10.sp, letterSpacing = 1.2.sp, modifier = Modifier.width(82.dp))
                    Slider(value = exposure, onValueChange = { exposure = it; applyExposure(camera, it) }, valueRange = -2f..2f, steps = 7, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = Gold, activeTrackColor = Gold, inactiveTrackColor = Panel), enabled = permission)
                    Text("${if (exposure > 0f) "+" else ""}${exposure.format(1)}", color = Gold, fontSize = 11.sp, modifier = Modifier.width(43.dp))
                }
                Text("LOOK  ·  FILTER IS APPLIED TO THE SAVED PHOTO", color = Muted, fontSize = 9.sp, letterSpacing = 1.1.sp, modifier = Modifier.padding(top = 2.dp, bottom = 5.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf(
                        "NATURAL", "NOIR", "SILVER", "MATTE B&W", "WARM FILM",
                        "PORTRA", "GOLDEN", "SUNSET", "TEAL", "TEAL ORANGE",
                        "VINTAGE", "RETRO", "FADE", "COOL", "ARCTIC",
                        "MOODY", "CINEMA", "DRAMA", "AMBER", "ROSE",
                        "BLOOM", "FOREST", "OLIVE", "OCEAN", "MIDNIGHT",
                        "BLUE HOUR", "DESERT", "CREAM", "PASTEL", "POP",
                        "CRISP", "HIGH KEY", "LOW KEY", "SEPIA", "CROSS PROCESS",
                        "FROST", "AUTUMN", "SPRING", "MONO WARM", "MONO COOL"
                    ).forEach { look ->
                        FilterPill(look, selectedFilter == look) { selectedFilter = look }
                    }
                }
                if (selectedFilter != "NATURAL") {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("INTENSITY", color = Muted, fontSize = 9.sp, modifier = Modifier.width(66.dp))
                        Slider(value = filterIntensity, onValueChange = { filterIntensity = it }, valueRange = 0f..1f, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = Gold, activeTrackColor = Gold))
                        Text("${(filterIntensity * 100).roundToInt()}%", color = Gold, fontSize = 10.sp, modifier = Modifier.width(35.dp))
                    }
                }
                Text("ZOOM  ${zoom.format(1)}×", color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp, modifier = Modifier.padding(top = 5.dp))
                Slider(value = zoom, onValueChange = { zoom = it; camera?.cameraControl?.setZoomRatio(it) }, valueRange = 1f..4f, colors = SliderDefaults.colors(thumbColor = Gold, activeTrackColor = Gold, inactiveTrackColor = Panel), enabled = permission)
                Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = { galleryPicker.launch("image/*") }, modifier = Modifier.size(52.dp).border(1.dp, Color(0xFF3A3D43), RoundedCornerShape(15.dp))) { Icon(Icons.Default.Photo, "Choose photo", tint = Color.White) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(76.dp).border(3.dp, Color.White, CircleShape).padding(6.dp).clip(CircleShape).background(Gold).clickable(enabled = permission) {
                            val imageCapture = capture
                            if (imageCapture == null) { Toast.makeText(context, "Camera is starting", Toast.LENGTH_SHORT).show() }
                            else saveCapture(context, imageCapture, ContextCompat.getMainExecutor(context), selectedFilter, filterIntensity) { message -> status = message; Toast.makeText(context, message, Toast.LENGTH_LONG).show() }
                        }, contentAlignment = Alignment.Center) { Icon(Icons.Default.CameraAlt, null, tint = Ink, modifier = Modifier.size(29.dp)) }
                        Text(status, color = Muted, fontSize = 8.sp, letterSpacing = 1.sp, modifier = Modifier.padding(top = 5.dp))
                    }
                    IconButton(onClick = { mode = if (mode == "PHOTO") "PRO" else "PHOTO" }, modifier = Modifier.size(52.dp).border(1.dp, if (mode == "PRO") Gold else Color(0xFF3A3D43), RoundedCornerShape(15.dp))) { Text(mode, color = if (mode == "PRO") Gold else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                }
                if (mode == "PRO") Text("PRO MODE: exposure compensation and zoom are active where supported. ISO/shutter/RAW controls vary by device and are not yet exposed in this first build.", color = Muted, fontSize = 10.sp, lineHeight = 14.sp, modifier = Modifier.padding(bottom = 12.dp))
            }
        }
    }
}

@Composable
private fun CameraPreview(onCameraReady: (Camera, ImageCapture) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    AndroidView(factory = { ctx ->
        val view = PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER; implementationMode = PreviewView.ImplementationMode.COMPATIBLE }
        val providerFuture = ProcessCameraProvider.getInstance(ctx)
        providerFuture.addListener({
            runCatching {
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
                val imageCapture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build()
                provider.unbindAll()
                val cam = provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
                onCameraReady(cam, imageCapture)
            }
        }, ContextCompat.getMainExecutor(ctx))
        view
    }, modifier = Modifier.fillMaxSize())
}

@Composable
private fun RuleOfThirds(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = Stroke(width = 1.dp.toPx())
        for (i in 1..2) {
            val x = size.width * i / 3f; val y = size.height * i / 3f
            drawLine(Color.White.copy(alpha = 0.42f), Offset(x, 0f), Offset(x, size.height), stroke.width)
            drawLine(Color.White.copy(alpha = 0.42f), Offset(0f, y), Offset(size.width, y), stroke.width)
        }
    }
}

@Composable
@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) Ink else Color.White,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        modifier = Modifier.clip(RoundedCornerShape(18.dp))
            .background(if (selected) Gold else Panel)
            .border(1.dp, if (selected) Gold else Color(0xFF34373D), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp)
    )
}

@Composable
private fun SmallTool(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).background(if (selected) Color(0xFF27231C) else Color.Transparent).padding(horizontal = 10.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = if (selected) Gold else Color.White, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(5.dp)); Text(label, color = if (selected) Gold else Muted, fontSize = 8.sp, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun EditorScreen(bitmap: Bitmap, brightness: Float, onBrightness: (Float) -> Unit, saturation: Float, onSaturation: (Float) -> Unit, contrast: Float, onContrast: (Float) -> Unit, warmth: Float, onWarmth: (Float) -> Unit, onBack: () -> Unit, onSave: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Ink).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) }
            Column(Modifier.weight(1f)) { Text("COLOR STUDIO", color = Color.White, fontWeight = FontWeight.Black, letterSpacing = 2.sp); Text("OFFLINE PHOTO EDITOR", color = Muted, fontSize = 9.sp) }
            Button(onClick = onSave, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink)) { Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(5.dp)); Text("Save") }
        }
        Spacer(Modifier.height(14.dp))
        androidx.compose.foundation.Image(bitmap.asImageBitmap(), null, Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(18.dp)), contentScale = androidx.compose.ui.layout.ContentScale.Fit, colorFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(editorMatrix(brightness, saturation, contrast, warmth)))
        Spacer(Modifier.height(16.dp))
        Text("COLOR ADJUSTMENTS", color = Gold, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
        EditSlider("Brightness", brightness, 0.4f..1.8f, onBrightness)
        EditSlider("Contrast", contrast, 0.5f..1.8f, onContrast)
        EditSlider("Saturation", saturation, 0f..2f, onSaturation)
        EditSlider("Warmth", warmth, -0.5f..0.5f, onWarmth)
        Text("Adjustments preview live. Save creates a graded copy; the original remains unchanged.", color = Muted, fontSize = 10.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 6.dp, bottom = 8.dp))
    }
}

@Composable
private fun EditSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, fontSize = 12.sp, modifier = Modifier.width(86.dp))
        Slider(value = value, onValueChange = onChange, valueRange = range, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = Gold, activeTrackColor = Gold))
        Text(value.format(2), color = Muted, fontSize = 10.sp, modifier = Modifier.width(42.dp))
    }
}

private fun editorMatrix(brightness: Float, saturation: Float, contrast: Float, warmth: Float): androidx.compose.ui.graphics.ColorMatrix {
    val s = saturation
    val sr = 0.213f * (1 - s); val sg = 0.715f * (1 - s); val sb = 0.072f * (1 - s)
    val b = (brightness - 1f) * 255f
    val c = contrast; val t = (1f - c) * 128f + b
    val w = warmth * 45f
    return androidx.compose.ui.graphics.ColorMatrix(floatArrayOf(
        c*(sr+s)+0f, c*sg, c*sb, 0f, t+w,
        c*sr, c*(sg+s), c*sb, 0f, t,
        c*sr, c*sg, c*(sb+s), 0f, t-w,
        0f,0f,0f,1f,0f
    ))
}

private fun gradeBitmap(src: Bitmap, brightness: Float, saturation: Float, contrast: Float, warmth: Float): Bitmap {
    val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    val c = contrast; val b = (brightness - 1f) * 255f; val w = warmth * 45f
    val cm = ColorMatrix(floatArrayOf(c,0f,0f,0f,b+w, 0f,c,0f,0f,b, 0f,0f,c,0f,b-w, 0f,0f,0f,1f,0f))
    val sat = ColorMatrix().apply { setSaturation(saturation) }
    cm.postConcat(sat)
    val canvas = android.graphics.Canvas(out)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG).apply { colorFilter = ColorMatrixColorFilter(cm) }
    canvas.drawBitmap(src, 0f, 0f, paint)
    return out
}

private fun saveCapture(context: android.content.Context, imageCapture: ImageCapture, executor: Executor, filter: String, intensity: Float, onResult: (String) -> Unit) {
    val name = "NoirLens_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
    val values = ContentValues().apply { put(MediaStore.Images.Media.DISPLAY_NAME, name); put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg"); put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/NoirLens") }
    val options = ImageCapture.OutputFileOptions.Builder(context.contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values).build()
    imageCapture.takePicture(options, executor, object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
            val uri = outputFileResults.savedUri
            if (uri == null || filter == "NATURAL" || intensity <= 0f) {
                onResult("PHOTO SAVED")
            } else {
                Thread({
                    val applied = runCatching { applyPhotoFilter(context, uri, filter, intensity) }.getOrDefault(false)
                    executor.execute { onResult(if (applied) "PHOTO SAVED · $filter" else "PHOTO SAVED · FILTER FAILED") }
                }, "NoirLens-Filter").start()
            }
        }
        override fun onError(exception: ImageCaptureException) { onResult("CAPTURE FAILED: ${exception.message ?: "unknown error"}") }
    })
}

private fun applyPhotoFilter(context: android.content.Context, uri: android.net.Uri, filter: String, intensity: Float): Boolean {
    val source = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return false
    val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
    val identity = ColorMatrix().array
    val look = when (filter) {
        "NOIR" -> filterMatrix(1f, 1f, 1f, 0f, 0f, 0f, 0f)
        "SILVER" -> filterMatrix(0.96f, 1.04f, 1.04f, 1.04f, 2f, 2f, 0f)
        "MATTE B&W" -> filterMatrix(0.88f, 0.94f, 0.94f, 0.94f, 18f, 18f, 0f)
        "WARM FILM" -> filterMatrix(1.02f, 1.10f, 1.02f, 0.88f, 10f, 2f, -7f)
        "PORTRA" -> filterMatrix(1.05f, 1.08f, 1.01f, 0.94f, 8f, 3f, -2f)
        "GOLDEN" -> filterMatrix(1.04f, 1.16f, 1.03f, 0.82f, 18f, 5f, -10f)
        "SUNSET" -> filterMatrix(1.08f, 1.18f, 0.98f, 0.82f, 20f, 1f, -7f)
        "TEAL" -> filterMatrix(1.02f, 0.92f, 1.03f, 1.10f, -2f, 5f, 10f)
        "TEAL ORANGE" -> filterMatrix(1.10f, 1.12f, 0.98f, 1.08f, 12f, 1f, 7f)
        "VINTAGE" -> filterMatrix(0.92f, 0.96f, 0.88f, 0.76f, 14f, 4f, -6f)
        "RETRO" -> filterMatrix(0.96f, 1.08f, 0.92f, 0.78f, 12f, 3f, -8f)
        "FADE" -> filterMatrix(0.88f, 0.86f, 0.86f, 0.86f, 26f, 26f, 26f)
        "COOL" -> filterMatrix(1.04f, 0.94f, 1.01f, 1.12f, -3f, 2f, 12f)
        "ARCTIC" -> filterMatrix(1.08f, 0.88f, 1.02f, 1.18f, -5f, 4f, 18f)
        "MOODY" -> filterMatrix(0.86f, 0.92f, 0.96f, 1.02f, -8f, -3f, 2f)
        "CINEMA" -> filterMatrix(0.94f, 1.05f, 0.98f, 1.04f, 3f, 1f, 5f)
        "DRAMA" -> filterMatrix(1.12f, 1.08f, 1.02f, 1.02f, -6f, -4f, -2f)
        "AMBER" -> filterMatrix(1.04f, 1.18f, 1.02f, 0.82f, 9f, 2f, -8f)
        "ROSE" -> filterMatrix(1.06f, 1.12f, 0.94f, 1.02f, 9f, -1f, 3f)
        "BLOOM" -> filterMatrix(0.92f, 1.08f, 1.04f, 1.08f, 12f, 8f, 10f)
        "FOREST" -> filterMatrix(1.02f, 0.94f, 1.08f, 0.92f, 0f, 5f, -3f)
        "OLIVE" -> filterMatrix(0.92f, 1.00f, 1.04f, 0.84f, 6f, 4f, -8f)
        "OCEAN" -> filterMatrix(1.08f, 0.90f, 1.02f, 1.16f, -2f, 5f, 13f)
        "MIDNIGHT" -> filterMatrix(0.82f, 0.82f, 0.90f, 1.12f, -12f, -6f, 8f)
        "BLUE HOUR" -> filterMatrix(1.02f, 0.90f, 1.00f, 1.14f, -4f, 1f, 12f)
        "DESERT" -> filterMatrix(0.94f, 1.10f, 1.02f, 0.86f, 14f, 7f, -5f)
        "CREAM" -> filterMatrix(0.86f, 1.06f, 1.02f, 0.94f, 14f, 10f, 4f)
        "PASTEL" -> filterMatrix(0.82f, 1.04f, 1.04f, 1.02f, 18f, 16f, 14f)
        "POP" -> filterMatrix(1.22f, 1.08f, 1.04f, 1.02f, 2f, 1f, 0f)
        "CRISP" -> filterMatrix(1.12f, 1.04f, 1.04f, 1.04f, 0f, 0f, 0f)
        "HIGH KEY" -> filterMatrix(0.92f, 1.06f, 1.06f, 1.06f, 20f, 20f, 20f)
        "LOW KEY" -> filterMatrix(0.92f, 0.78f, 0.78f, 0.82f, -18f, -18f, -14f)
        "SEPIA" -> filterMatrix(0.88f, 1.10f, 0.94f, 0.72f, 15f, 6f, -8f)
        "CROSS PROCESS" -> filterMatrix(1.10f, 1.10f, 0.92f, 1.10f, 8f, -2f, 10f)
        "FROST" -> filterMatrix(0.94f, 0.92f, 1.04f, 1.14f, 7f, 12f, 18f)
        "AUTUMN" -> filterMatrix(1.02f, 1.14f, 1.00f, 0.82f, 12f, 3f, -10f)
        "SPRING" -> filterMatrix(0.92f, 1.06f, 1.06f, 1.00f, 10f, 12f, 4f)
        "MONO WARM" -> filterMatrix(0.02f, 1.08f, 0.94f, 0.78f, 12f, 4f, -5f)
        "MONO COOL" -> filterMatrix(0.02f, 0.88f, 1.02f, 1.14f, -3f, 5f, 12f)
        else -> identity
    }
    val blended = FloatArray(20) { i -> identity[i] + (look[i] - identity[i]) * intensity.coerceIn(0f, 1f) }
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG).apply {
        colorFilter = ColorMatrixColorFilter(ColorMatrix(blended))
    }
    android.graphics.Canvas(output).drawBitmap(source, 0f, 0f, paint)
    val saved = context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
        output.compress(Bitmap.CompressFormat.JPEG, 95, stream)
    } ?: false
    source.recycle()
    output.recycle()
    return saved
}


private fun filterMatrix(saturation: Float, red: Float, green: Float, blue: Float, redOffset: Float, greenOffset: Float, blueOffset: Float): FloatArray {
    val matrix = ColorMatrix(floatArrayOf(
        red, 0f, 0f, 0f, redOffset,
        0f, green, 0f, 0f, greenOffset,
        0f, 0f, blue, 0f, blueOffset,
        0f, 0f, 0f, 1f, 0f
    ))
    matrix.postConcat(ColorMatrix().apply { setSaturation(saturation) })
    return matrix.array
}

private fun applyExposure(camera: Camera?, ev: Float) {
    val state = camera?.cameraInfo?.exposureState ?: return
    if (state.isExposureCompensationSupported) {
        val range = state.exposureCompensationRange
        val index = (ev / (state.exposureCompensationStep.toFloat())).roundToInt().coerceIn(range.lower, range.upper)
        camera.cameraControl.setExposureCompensationIndex(index)
    }
}

private fun Float.format(digits: Int) = String.format(Locale.US, "%1$.${digits}f", this)
