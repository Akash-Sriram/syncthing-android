package com.nutomic.syncthingandroid.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.navigation3.runtime.EntryProviderScope
import com.nutomic.syncthingandroid.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "AppDownloader"

fun EntryProviderScope<SettingsRoute>.settingsAppDownloaderEntry() {
    entry<SettingsRoute.AppDownloader> {
        SettingsAppDownloaderScreen()
    }
}

data class ReleaseAsset(
    val name: String,
    val downloadUrl: String,
    val size: Long
)

data class Release(
    val tagName: String,
    val name: String,
    val prerelease: Boolean,
    val body: String,
    val publishedAt: String,
    val assets: List<ReleaseAsset>
)

enum class DownloaderState {
    LOADING,
    SUCCESS,
    ERROR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAppDownloaderScreen() {
    val context = LocalContext.current
    val navigator = LocalSettingsNavigator.current
    val scope = rememberCoroutineScope()

    val currentVersion = remember(context) {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "0.0.0.0"
        } catch (e: Exception) {
            "0.0.0.0"
        }
    }

    var downloaderState by remember { mutableStateOf(DownloaderState.LOADING) }
    var releases by remember { mutableStateOf<List<Release>>(emptyList()) }
    var errorMessage by remember { mutableStateOf("") }

    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var downloadingVersion by remember { mutableStateOf("") }

    fun loadReleases() {
        downloaderState = DownloaderState.LOADING
        scope.launch {
            try {
                val fetched = fetchReleases()
                releases = fetched
                downloaderState = DownloaderState.SUCCESS
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch releases", e)
                errorMessage = e.localizedMessage ?: "Unknown network error"
                downloaderState = DownloaderState.ERROR
            }
        }
    }

    LaunchedEffect(Unit) {
        loadReleases()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.category_downloader)) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (downloaderState) {
                DownloaderState.LOADING -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                DownloaderState.ERROR -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error: $errorMessage",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { loadReleases() }) {
                            Text("Retry")
                        }
                    }
                }
                DownloaderState.SUCCESS -> {
                    if (releases.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No releases found.")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Info, contentDescription = "Info", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Installed Version: v$currentVersion",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                            items(releases) { release ->
                                ReleaseItemCard(
                                    release = release,
                                    currentVersion = currentVersion,
                                    onDownload = {
                                        val matchedAsset = getDeviceAbiApk(release.assets)
                                        if (matchedAsset != null) {
                                            isDownloading = true
                                            downloadingVersion = release.tagName
                                            scope.launch {
                                                try {
                                                    downloadAndInstallApk(context, matchedAsset) { progress ->
                                                        downloadProgress = progress
                                                    }
                                                } catch (e: Exception) {
                                                    Log.e(TAG, "Download failed", e)
                                                } finally {
                                                    isDownloading = false
                                                    downloadProgress = 0f
                                                }
                                            }
                                        } else {
                                            Log.e(TAG, "No suitable APK asset found for device architecture")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (isDownloading) {
        Dialog(onDismissRequest = {}) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Downloading Syncthing",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = downloadingVersion,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    LinearProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${(downloadProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun ReleaseItemCard(
    release: Release,
    currentVersion: String,
    onDownload: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val isNewer = remember(release.tagName, currentVersion) {
        isUpdateAvailable(currentVersion, release.tagName)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = release.tagName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = release.publishedAt.take(10),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isNewer) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "New Update",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (release.prerelease) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFE65100))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Pre-release",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF2E7D32))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Stable",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                    contentDescription = "Toggle Changelog",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "View Release Notes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                    Text(
                        text = release.body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onDownload,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Download & Install")
            }
        }
    }
}

suspend fun fetchReleases(): List<Release> = withContext(Dispatchers.IO) {
    val url = URL("https://api.github.com/repos/researchxxl/syncthing-android/releases")
    val connection = url.openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.setRequestProperty("User-Agent", "Syncthing-Android-Downloader")
    connection.connectTimeout = 15000
    connection.readTimeout = 15000

    if (connection.responseCode == 200) {
        val text = connection.inputStream.bufferedReader().use { it.readText() }
        val jsonArray = JSONArray(text)
        val releases = mutableListOf<Release>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val tagName = obj.getString("tag_name")
            val name = obj.optString("name", tagName)
            val prerelease = obj.getBoolean("prerelease")
            val body = obj.optString("body", "")
            val publishedAt = obj.optString("published_at", "")

            val assetsArray = obj.getJSONArray("assets")
            val assets = mutableListOf<ReleaseAsset>()
            for (j in 0 until assetsArray.length()) {
                val assetObj = assetsArray.getJSONObject(j)
                val assetName = assetObj.getString("name")
                val downloadUrl = assetObj.getString("browser_download_url")
                val size = assetObj.getLong("size")
                assets.add(ReleaseAsset(assetName, downloadUrl, size))
            }
            releases.add(Release(tagName, name, prerelease, body, publishedAt, assets))
        }
        releases
    } else {
        throw IOException("HTTP error code: ${connection.responseCode}")
    }
}

fun getDeviceAbiApk(assets: List<ReleaseAsset>): ReleaseAsset? {
    val abis = Build.SUPPORTED_ABIS
    for (abi in abis) {
        val targetAsset = assets.find { asset ->
            asset.name.endsWith(".apk") && (asset.name.contains("_$abi") || asset.name.contains(abi))
        }
        if (targetAsset != null) {
            return targetAsset
        }
    }
    return assets.find { it.name.endsWith(".apk") }
}

suspend fun downloadAndInstallApk(
    context: Context,
    asset: ReleaseAsset,
    onProgress: (Float) -> Unit
) = withContext(Dispatchers.IO) {
    val cacheDir = context.cacheDir
    val tempFile = File(cacheDir, "syncthing_update.apk")
    if (tempFile.exists()) {
        tempFile.delete()
    }

    val url = URL(asset.downloadUrl)
    val connection = url.openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.connectTimeout = 15000
    connection.readTimeout = 30000

    val totalSize = if (asset.size > 0) asset.size else connection.contentLength.toLong()

    connection.inputStream.use { input ->
        tempFile.outputStream().use { output ->
            val buffer = ByteArray(8192)
            var bytesTotal = 0L
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                output.write(buffer, 0, read)
                bytesTotal += read
                if (totalSize > 0) {
                    onProgress(bytesTotal.toFloat() / totalSize)
                }
            }
        }
    }

    withContext(Dispatchers.Main) {
        val apkUri = FileProvider.getUriForFile(
            context,
            context.packageName + ".provider",
            tempFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

fun cleanVersion(version: String): String {
    var clean = version.trim().lowercase()
    if (clean.startsWith("v")) {
        clean = clean.substring(1)
    }
    val dashIndex = clean.indexOf("-")
    if (dashIndex != -1) {
        clean = clean.substring(0, dashIndex)
    }
    val plusIndex = clean.indexOf("+")
    if (plusIndex != -1) {
        clean = clean.substring(0, plusIndex)
    }
    return clean.replace(Regex("[^0-9.]"), "")
}

fun isUpdateAvailable(currentVersion: String, releaseVersion: String): Boolean {
    val currentClean = cleanVersion(currentVersion)
    val releaseClean = cleanVersion(releaseVersion)

    val currentParts = currentClean.split(".").mapNotNull { it.toIntOrNull() }
    val releaseParts = releaseClean.split(".").mapNotNull { it.toIntOrNull() }

    val maxLength = maxOf(currentParts.size, releaseParts.size)
    for (i in 0 until maxLength) {
        val currentPart = currentParts.getOrElse(i) { 0 }
        val releasePart = releaseParts.getOrElse(i) { 0 }
        if (releasePart > currentPart) {
            return true
        } else if (currentPart > releasePart) {
            return false
        }
    }
    return false
}
