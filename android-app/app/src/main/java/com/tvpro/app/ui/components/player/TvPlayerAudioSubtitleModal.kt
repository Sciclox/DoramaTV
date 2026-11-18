package com.tvpro.app.ui.components.player

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.exoplayer.ExoPlayer
import com.tvpro.app.ui.theme.BebasNeue
import com.tvpro.app.ui.theme.Inter
import com.tvpro.core.data.model.StreamItem
import com.tvpro.core.data.model.SubtitleTrackData

@Composable
fun TvPlayerAudioSubtitleModal(
    show: Boolean,
    onDismiss: () -> Unit,
    isEmbedStream: Boolean,
    currentStream: StreamItem,
    availableStreams: List<StreamItem>,
    currentStreamIndex: Int,
    exoPlayer: ExoPlayer,
    exoAudioTracks: List<ExoAudioTrackItem>,
    exoSubtitleTracks: List<ExoSubtitleTrackItem>,
    nativeWebAudioTracks: List<String>,
    nativeWebSubtitleTracks: List<String>,
    selectedWebAudioTrackIdx: Int,
    selectedWebSubtitleTrackIdx: Int,
    subtitlesEnabled: Boolean,
    selectedSubtitleOption: Int,
    activeAudioTrackFocusRequester: FocusRequester,
    onSwitchServer: (Int, Boolean) -> Unit,
    onApplyWebAudioTrack: (Int) -> Unit,
    onApplyWebSubtitle: (Boolean, Int) -> Unit,
    onSelectExoSubtitle: (ExoSubtitleTrackItem?) -> Unit,
    onSelectSubtitleTrackData: (SubtitleTrackData) -> Unit,
    onApplySubtitlePreference: (Boolean, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!show) return

    val warmIvory = Color(0xFFF5F2EB)
    val audioScrollState = rememberScrollState()
    val subScrollState = rememberScrollState()

    val hasSubtitles = if (isEmbedStream) {
        nativeWebSubtitleTracks.isNotEmpty() || (currentStream.subtitle == 1) || currentStream.safeSubtitles.isNotEmpty()
    } else {
        exoSubtitleTracks.isNotEmpty() || (currentStream.subtitle == 1) || currentStream.safeSubtitles.isNotEmpty()
    }

    LaunchedEffect(Unit) {
        audioScrollState.scrollTo(0)
        subScrollState.scrollTo(0)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xF206080C))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            }
            .padding(horizontal = 56.dp, vertical = 42.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.widthIn(max = 940.dp)
        ) {
            // Header superior del modal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Subtitles,
                            contentDescription = null,
                            tint = warmIvory,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "AUDIO Y SUBTÍTULOS",
                            color = warmIvory,
                            fontFamily = BebasNeue,
                            fontSize = 32.sp,
                            letterSpacing = 2.2.sp
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "·   CONFIGURACIÓN DE PISTAS",
                            color = Color(0xFF9CA3AF),
                            fontFamily = Inter,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 2.0.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Selecciona el idioma de audio preferido o activa y desactiva los subtítulos disponibles en este video.",
                        color = Color(0xFF9CA3AF),
                        fontFamily = Inter,
                        fontSize = 13.5.sp
                    )
                }

                // Píldora derecha: Estado de Subtítulos dinámico
                Row(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF10141D))
                        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(50))
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    !hasSubtitles -> Color(0xFF6B7280)
                                    subtitlesEnabled -> Color(0xFF2DD4BF)
                                    else -> Color(0xFF6B7280)
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = when {
                            !hasSubtitles -> "SUBTÍTULOS: NO DISPONIBLES"
                            subtitlesEnabled -> "SUBTÍTULOS: ACTIVADOS"
                            else -> "SUBTÍTULOS: DESACTIVADOS"
                        },
                        color = warmIvory,
                        fontFamily = Inter,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.6.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Diseño en 2 Columnas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // COLUMNA 1: IDIOMA Y PISTAS DE AUDIO
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(355.dp),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0C0F15))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(22.dp)
                    ) {
                        Text(
                            text = "IDIOMA Y AUDIO",
                            color = Color(0xFF9CA3AF),
                            fontFamily = Inter,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.0.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(audioScrollState),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val displayAudioOptions = remember(
                                exoAudioTracks,
                                nativeWebAudioTracks,
                                selectedWebAudioTrackIdx,
                                isEmbedStream,
                                availableStreams,
                                currentStreamIndex,
                                currentStream
                            ) {
                                val options = mutableListOf<DisplayLanguageOption>()

                                fun isStreamSpanish(s: StreamItem): Boolean {
                                    val src = (s.source ?: "").lowercase()
                                    val isExplicitEnglish = src.contains("subtitulado") || src.contains("subtitulos") ||
                                            src.contains("inglés") || src.contains("ingles") || src.contains("english") || src.contains("vo ")
                                    if (isExplicitEnglish && !src.contains("dual") && !src.contains("multi")) return false

                                    if (src.contains("latino") || src.contains("castellano") || src.contains("español") || src.contains("espanol") || src.contains("spanish")) {
                                        return true
                                    }
                                    val urlLower = s.url.lowercase()
                                    if ((urlLower.contains("latino") || urlLower.contains("castellano")) && !urlLower.contains("sub_en")) return true
                                    return false
                                }

                                fun isStreamEnglish(s: StreamItem): Boolean {
                                    val src = (s.source ?: "").lowercase()
                                    val isExplicitSpanish = (src.contains("latino") || src.contains("castellano") || src.contains("español") || src.contains("espanol")) &&
                                            !src.contains("dual") && !src.contains("multi")
                                    if (isExplicitSpanish) return false

                                    if (src.contains("subtitulado") || src.contains("subtitulos") || src.contains("inglés") || src.contains("ingles") || src.contains("english") || src.contains("original")) {
                                        return true
                                    }
                                    val urlLower = s.url.lowercase()
                                    if (urlLower.contains("english") || urlLower.contains("subtitulado") || urlLower.contains("sub_en")) return true
                                    return false
                                }

                                fun isWebSpanish(label: String): Boolean {
                                    val raw = label.lowercase()
                                    return raw.contains("es") || raw.contains("spa") || raw.contains("lat") || raw.contains("castell")
                                }

                                fun isWebEnglish(label: String): Boolean {
                                    val raw = label.lowercase()
                                    return raw.contains("en") || raw.contains("eng") || raw.contains("orig") || raw.contains("ing")
                                }

                                if (!isEmbedStream && exoAudioTracks.isNotEmpty()) {
                                    val exoSpanish = exoAudioTracks.filter { it.isSpanish }
                                    val exoEnglish = exoAudioTracks.filter { it.isEnglish }
                                    val exoOther = exoAudioTracks.filter { !it.isSpanish && !it.isEnglish }

                                    if (exoSpanish.isNotEmpty()) {
                                        exoSpanish.forEach { track ->
                                            options.add(
                                                DisplayLanguageOption(
                                                    id = "exo_es_${track.trackIndex}",
                                                    title = track.name,
                                                    subtitle = if (exoAudioTracks.size == 1) "Doblaje oficial latino  ·  Audio integrado" else "Doblaje Español Latino  ·  Audio Nativo",
                                                    isSelected = track.isSelected,
                                                    onSelect = {
                                                        try {
                                                            val override = TrackSelectionOverride(
                                                                track.group.mediaTrackGroup,
                                                                listOf(track.trackIndex)
                                                            )
                                                            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                                                .buildUpon()
                                                                .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                                                                .addOverride(override)
                                                                .build()
                                                        } catch (e: Exception) {
                                                            e.printStackTrace()
                                                        }
                                                    }
                                                )
                                            )
                                        }
                                    } else {
                                        val spanishStreamIdx = availableStreams.indexOfFirst { isStreamSpanish(it) }
                                        if (spanishStreamIdx >= 0) {
                                            options.add(
                                                DisplayLanguageOption(
                                                    id = "stream_es_$spanishStreamIdx",
                                                    title = "Español Latino",
                                                    subtitle = "Doblaje oficial latino  ·  Transmisión HD",
                                                    isSelected = (currentStreamIndex == spanishStreamIdx),
                                                    onSelect = {
                                                        if (currentStreamIndex != spanishStreamIdx) {
                                                            onSwitchServer(spanishStreamIdx, false)
                                                        }
                                                    }
                                                )
                                            )
                                        }
                                    }

                                    if (exoEnglish.isNotEmpty()) {
                                        exoEnglish.forEach { track ->
                                            options.add(
                                                DisplayLanguageOption(
                                                    id = "exo_en_${track.trackIndex}",
                                                    title = track.name,
                                                    subtitle = if (exoAudioTracks.size == 1) "Audio original en inglés  ·  Audio integrado" else "Audio Original / Inglés  ·  Audio Nativo",
                                                    isSelected = track.isSelected,
                                                    onSelect = {
                                                        try {
                                                            val override = TrackSelectionOverride(
                                                                track.group.mediaTrackGroup,
                                                                listOf(track.trackIndex)
                                                            )
                                                            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                                                .buildUpon()
                                                                .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                                                                .addOverride(override)
                                                                .build()
                                                        } catch (e: Exception) {
                                                            e.printStackTrace()
                                                        }
                                                    }
                                                )
                                            )
                                        }
                                    } else {
                                        val englishStreamIdx = availableStreams.indexOfFirst { isStreamEnglish(it) }
                                        if (englishStreamIdx >= 0) {
                                            options.add(
                                                DisplayLanguageOption(
                                                    id = "stream_en_$englishStreamIdx",
                                                    title = "Inglés",
                                                    subtitle = "Audio original en inglés  ·  Transmisión HD",
                                                    isSelected = (currentStreamIndex == englishStreamIdx),
                                                    onSelect = {
                                                        if (currentStreamIndex != englishStreamIdx) {
                                                            onSwitchServer(englishStreamIdx, false)
                                                        }
                                                    }
                                                )
                                            )
                                        }
                                    }

                                    exoOther.forEach { track ->
                                        val isGeneric = track.name.startsWith("Audio ", ignoreCase = true) ||
                                                track.name.equals("und", ignoreCase = true) ||
                                                track.name.isBlank()
                                        if (!isGeneric) {
                                            options.add(
                                                DisplayLanguageOption(
                                                    id = "exo_other_${track.trackIndex}",
                                                    title = track.name,
                                                    subtitle = if (track.name.isNotBlank()) "Audio en ${track.name.lowercase()}  ·  Audio Nativo" else "Pista de audio adicional  ·  Audio Nativo",
                                                    isSelected = track.isSelected,
                                                    onSelect = {
                                                        try {
                                                            val override = TrackSelectionOverride(
                                                                track.group.mediaTrackGroup,
                                                                listOf(track.trackIndex)
                                                            )
                                                            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                                                .buildUpon()
                                                                .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                                                                .addOverride(override)
                                                                .build()
                                                        } catch (e: Exception) {
                                                            e.printStackTrace()
                                                        }
                                                    }
                                                )
                                            )
                                        }
                                    }
                                } else if (isEmbedStream && nativeWebAudioTracks.isNotEmpty()) {
                                    var webSpanishIdx = nativeWebAudioTracks.indexOfFirst { isWebSpanish(it) }
                                    var webEnglishIdx = nativeWebAudioTracks.indexOfFirst { isWebEnglish(it) }

                                    if (webSpanishIdx < 0 && webEnglishIdx < 0 && nativeWebAudioTracks.size == 1) {
                                        if (isStreamEnglish(currentStream)) webEnglishIdx = 0 else webSpanishIdx = 0
                                    }

                                    if (webSpanishIdx >= 0) {
                                        options.add(
                                            DisplayLanguageOption(
                                                id = "web_es_$webSpanishIdx",
                                                title = "Español Latino",
                                                subtitle = "Doblaje Español Latino  ·  Audio Web",
                                                isSelected = (selectedWebAudioTrackIdx == webSpanishIdx),
                                                onSelect = {
                                                    onApplyWebAudioTrack(webSpanishIdx)
                                                }
                                            )
                                        )
                                    } else {
                                        val spanishStreamIdx = availableStreams.indexOfFirst { isStreamSpanish(it) }
                                        if (spanishStreamIdx >= 0) {
                                            options.add(
                                                DisplayLanguageOption(
                                                    id = "stream_es_$spanishStreamIdx",
                                                    title = "Español Latino",
                                                    subtitle = "Doblaje oficial latino  ·  Transmisión HD",
                                                    isSelected = (currentStreamIndex == spanishStreamIdx),
                                                    onSelect = {
                                                        if (currentStreamIndex != spanishStreamIdx) {
                                                            onSwitchServer(spanishStreamIdx, false)
                                                        }
                                                    }
                                                )
                                            )
                                        }
                                    }

                                    if (webEnglishIdx >= 0) {
                                        options.add(
                                            DisplayLanguageOption(
                                                id = "web_en_$webEnglishIdx",
                                                title = "Inglés",
                                                subtitle = "Audio Original en Inglés  ·  Audio Web",
                                                isSelected = (selectedWebAudioTrackIdx == webEnglishIdx),
                                                onSelect = {
                                                    onApplyWebAudioTrack(webEnglishIdx)
                                                }
                                            )
                                        )
                                    } else {
                                        val englishStreamIdx = availableStreams.indexOfFirst { isStreamEnglish(it) }
                                        if (englishStreamIdx >= 0) {
                                            options.add(
                                                DisplayLanguageOption(
                                                    id = "stream_en_$englishStreamIdx",
                                                    title = "Inglés",
                                                    subtitle = "Audio original en inglés  ·  Transmisión HD",
                                                    isSelected = (currentStreamIndex == englishStreamIdx),
                                                    onSelect = {
                                                        if (currentStreamIndex != englishStreamIdx) {
                                                            onSwitchServer(englishStreamIdx, false)
                                                        }
                                                    }
                                                )
                                            )
                                        }
                                    }
                                } else {
                                    val currentIsExplicitEnglish = isStreamEnglish(currentStream)
                                    val currentIsExplicitSpanish = isStreamSpanish(currentStream)
                                    val isEnglishActive = currentIsExplicitEnglish && !currentIsExplicitSpanish
                                    val isSpanishActive = !isEnglishActive

                                    val spanishStreamIdx = if (isSpanishActive) currentStreamIndex else availableStreams.indexOfFirst { isStreamSpanish(it) }
                                    val hasSpanish = isSpanishActive || spanishStreamIdx >= 0

                                    val englishStreamIdx = if (isEnglishActive) currentStreamIndex else availableStreams.indexOfFirst { isStreamEnglish(it) }
                                    val hasEnglish = isEnglishActive || englishStreamIdx >= 0

                                    if (hasSpanish) {
                                        options.add(
                                            DisplayLanguageOption(
                                                id = "fallback_es",
                                                title = "Español Latino",
                                                subtitle = if (isSpanishActive) "Doblaje oficial latino  ·  Audio integrado" else "Doblaje oficial latino",
                                                isSelected = isSpanishActive,
                                                onSelect = {
                                                    if (spanishStreamIdx >= 0 && spanishStreamIdx != currentStreamIndex) {
                                                        onSwitchServer(spanishStreamIdx, false)
                                                    }
                                                }
                                            )
                                        )
                                    }

                                    if (hasEnglish) {
                                        options.add(
                                            DisplayLanguageOption(
                                                id = "fallback_en",
                                                title = "Inglés",
                                                subtitle = if (isEnglishActive) "Audio original en inglés  ·  Audio integrado" else "Audio original en inglés",
                                                isSelected = isEnglishActive,
                                                onSelect = {
                                                    if (englishStreamIdx >= 0 && englishStreamIdx != currentStreamIndex) {
                                                        onSwitchServer(englishStreamIdx, false)
                                                    }
                                                }
                                            )
                                        )
                                    }

                                    if (options.isEmpty()) {
                                        options.add(
                                            DisplayLanguageOption(
                                                id = "fallback_single",
                                                title = "Español Latino",
                                                subtitle = "Audio estéreo integrado de la transmisión",
                                                isSelected = true,
                                                onSelect = {}
                                            )
                                        )
                                    }
                                }

                                val deduplicated = mutableListOf<DisplayLanguageOption>()
                                for (opt in options) {
                                    val existingIdx = deduplicated.indexOfFirst { it.title.trim().equals(opt.title.trim(), ignoreCase = true) }
                                    if (existingIdx >= 0) {
                                        if (!deduplicated[existingIdx].isSelected && opt.isSelected) {
                                            deduplicated[existingIdx] = opt
                                        }
                                    } else {
                                        deduplicated.add(opt)
                                    }
                                }

                                if (deduplicated.isNotEmpty() && deduplicated.none { it.isSelected }) {
                                    deduplicated[0] = deduplicated[0].copy(isSelected = true)
                                }

                                var hasActiveTrack = false
                                deduplicated.map { opt ->
                                    if (opt.isSelected) {
                                        if (!hasActiveTrack) {
                                            hasActiveTrack = true
                                            opt
                                        } else {
                                            opt.copy(isSelected = false)
                                        }
                                    } else {
                                        opt
                                    }
                                }
                            }

                            displayAudioOptions.forEachIndexed { optIdx, option ->
                                var isOptFocused by remember { mutableStateOf(false) }
                                val shouldFocusThisOpt = if (displayAudioOptions.any { it.isSelected }) option.isSelected else (optIdx == 0)

                                Button(
                                    onClick = {
                                        option.onSelect()
                                        onDismiss()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(
                                            if (shouldFocusThisOpt) Modifier.focusRequester(activeAudioTrackFocusRequester)
                                            else Modifier
                                        )
                                        .border(
                                            border = if (isOptFocused) BorderStroke(2.dp, warmIvory)
                                            else if (option.isSelected) BorderStroke(1.5.dp, warmIvory.copy(alpha = 0.55f))
                                            else BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .onFocusChanged { isOptFocused = it.isFocused }
                                        .onKeyEvent { keyEvent ->
                                            if (keyEvent.type == KeyEventType.KeyDown &&
                                                (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                                            ) {
                                                option.onSelect()
                                                onDismiss()
                                                true
                                            } else false
                                        },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isOptFocused) warmIvory
                                        else if (option.isSelected) Color(0xFF171C26)
                                        else Color(0xFF11151E),
                                        contentColor = if (isOptFocused) Color.Black else warmIvory
                                    ),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = option.title,
                                                fontFamily = Inter,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = if (isOptFocused) Color.Black else warmIvory
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = option.subtitle,
                                                fontFamily = Inter,
                                                fontSize = 11.sp,
                                                color = if (isOptFocused) Color.Black.copy(alpha = 0.7f) else Color(0xFF9CA3AF)
                                            )
                                        }
                                        if (option.isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .background(if (isOptFocused) Color(0xFF0A0D13) else warmIvory)
                                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = "ACTIVO",
                                                    fontFamily = Inter,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (isOptFocused) warmIvory else Color(0xFF0A0D13),
                                                    fontSize = 9.5.sp,
                                                    letterSpacing = 1.0.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // COLUMNA 2: SUBTÍTULOS
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(355.dp),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0C0F15))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(22.dp)
                    ) {
                        Text(
                            text = "SUBTÍTULOS (CC)",
                            color = Color(0xFF9CA3AF),
                            fontFamily = Inter,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.0.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        if (!hasSubtitles) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF10141D))
                                    .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(14.dp))
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF181E2B)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Subtitles,
                                            contentDescription = null,
                                            tint = Color(0xFF6B7280),
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "SIN SUBTÍTULOS EN ESTE VIDEO",
                                        color = warmIvory,
                                        fontFamily = Inter,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        letterSpacing = 1.2.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Esta transmisión cuenta con audio integrado y no incluye pistas de subtítulos en esta fuente.",
                                        color = Color(0xFF9CA3AF),
                                        fontFamily = Inter,
                                        fontSize = 11.5.sp,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(subScrollState),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Opción: Desactivados
                                var isOffFocused by remember { mutableStateOf(false) }
                                val isOffSelected = !subtitlesEnabled

                                Button(
                                    onClick = {
                                        if (isEmbedStream) {
                                            onApplyWebSubtitle(false, -1)
                                        } else {
                                            onSelectExoSubtitle(null)
                                        }
                                        onDismiss()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            border = if (isOffFocused) BorderStroke(2.dp, warmIvory)
                                            else if (isOffSelected) BorderStroke(1.5.dp, warmIvory.copy(alpha = 0.55f))
                                            else BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .onFocusChanged { isOffFocused = it.isFocused }
                                        .onKeyEvent { keyEvent ->
                                            if (keyEvent.type == KeyEventType.KeyDown &&
                                                (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                                            ) {
                                                if (isEmbedStream) {
                                                    onApplyWebSubtitle(false, -1)
                                                } else {
                                                    onSelectExoSubtitle(null)
                                                }
                                                onDismiss()
                                                true
                                            } else false
                                        },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isOffFocused) warmIvory
                                        else if (isOffSelected) Color(0xFF171C26)
                                        else Color(0xFF11151E),
                                        contentColor = if (isOffFocused) Color.Black else warmIvory
                                    ),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Desactivados",
                                                fontFamily = Inter,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = if (isOffFocused) Color.Black else warmIvory
                                            )
                                            Text(
                                                text = "Ocultar los subtítulos en pantalla",
                                                fontFamily = Inter,
                                                fontSize = 11.sp,
                                                color = if (isOffFocused) Color.Black.copy(alpha = 0.72f) else Color(0xFF9CA3AF)
                                            )
                                        }
                                        if (isOffSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .background(if (isOffFocused) Color(0xFF0A0D13) else warmIvory)
                                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = "ACTIVO",
                                                    fontFamily = Inter,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (isOffFocused) warmIvory else Color(0xFF0A0D13),
                                                    fontSize = 9.5.sp,
                                                    letterSpacing = 1.0.sp
                                                )
                                            }
                                        }
                                    }
                                }

                                if (!isEmbedStream) {
                                    if (exoSubtitleTracks.isNotEmpty()) {
                                        exoSubtitleTracks.forEach { subItem ->
                                            var isItemSubFocused by remember { mutableStateOf(false) }
                                            val isItemSubSelected = subtitlesEnabled && subItem.isSelected

                                            Button(
                                                onClick = {
                                                    onSelectExoSubtitle(subItem)
                                                    onDismiss()
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(
                                                        border = if (isItemSubFocused) BorderStroke(2.dp, warmIvory)
                                                        else if (isItemSubSelected) BorderStroke(1.5.dp, warmIvory.copy(alpha = 0.55f))
                                                        else BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .onFocusChanged { isItemSubFocused = it.isFocused }
                                                    .onKeyEvent { keyEvent ->
                                                        if (keyEvent.type == KeyEventType.KeyDown &&
                                                            (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                                                        ) {
                                                            onSelectExoSubtitle(subItem)
                                                            onDismiss()
                                                            true
                                                        } else false
                                                    },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isItemSubFocused) warmIvory
                                                    else if (isItemSubSelected) Color(0xFF171C26)
                                                    else Color(0xFF11151E),
                                                    contentColor = if (isItemSubFocused) Color.Black else warmIvory
                                                ),
                                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = subItem.name,
                                                            fontFamily = Inter,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 14.sp,
                                                            color = if (isItemSubFocused) Color.Black else warmIvory
                                                        )
                                                        Text(
                                                            text = when {
                                                                subItem.isSpanish -> "Subtítulos en español sincronizados"
                                                                subItem.name.contains("portug", ignoreCase = true) -> "Subtítulos en portugués sincronizados"
                                                                subItem.isEnglish -> "Subtítulos en inglés sincronizados"
                                                                else -> "Subtítulos en idioma original"
                                                            },
                                                            fontFamily = Inter,
                                                            fontSize = 11.sp,
                                                            color = if (isItemSubFocused) Color.Black.copy(alpha = 0.72f) else Color(0xFF9CA3AF)
                                                        )
                                                    }
                                                    if (isItemSubSelected) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(50))
                                                                .background(if (isItemSubFocused) Color(0xFF0A0D13) else warmIvory)
                                                                .padding(horizontal = 10.dp, vertical = 3.dp)
                                                        ) {
                                                            Text(
                                                                text = "ACTIVO",
                                                                fontFamily = Inter,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = if (isItemSubFocused) warmIvory else Color(0xFF0A0D13),
                                                                fontSize = 9.5.sp,
                                                                letterSpacing = 1.0.sp
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else if (currentStream.safeSubtitles.isNotEmpty()) {
                                        currentStream.safeSubtitles.forEachIndexed { sIdx, subData ->
                                            var isItemSubFocused by remember { mutableStateOf(false) }
                                            val isItemSubSelected = subtitlesEnabled && (
                                                    if (subData.language.startsWith("en", ignoreCase = true)) selectedSubtitleOption == 2
                                                    else selectedSubtitleOption == 1 || selectedSubtitleOption == (sIdx + 1)
                                                    )

                                            Button(
                                                onClick = {
                                                    onSelectSubtitleTrackData(subData)
                                                    onDismiss()
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(
                                                        border = if (isItemSubFocused) BorderStroke(2.dp, warmIvory)
                                                        else if (isItemSubSelected) BorderStroke(1.5.dp, warmIvory.copy(alpha = 0.55f))
                                                        else BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .onFocusChanged { isItemSubFocused = it.isFocused }
                                                    .onKeyEvent { keyEvent ->
                                                        if (keyEvent.type == KeyEventType.KeyDown &&
                                                            (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                                                        ) {
                                                            onSelectSubtitleTrackData(subData)
                                                            onDismiss()
                                                            true
                                                        } else false
                                                    },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isItemSubFocused) warmIvory
                                                    else if (isItemSubSelected) Color(0xFF171C26)
                                                    else Color(0xFF11151E),
                                                    contentColor = if (isItemSubFocused) Color.Black else warmIvory
                                                ),
                                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = subData.label,
                                                            fontFamily = Inter,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 14.sp,
                                                            color = if (isItemSubFocused) Color.Black else warmIvory
                                                        )
                                                        Text(
                                                            text = when {
                                                                subData.language.startsWith("es", ignoreCase = true) -> "Subtítulos en español sincronizados"
                                                                subData.language.startsWith("pt", ignoreCase = true) || subData.label.contains("portug", ignoreCase = true) -> "Subtítulos en portugués sincronizados"
                                                                subData.language.startsWith("en", ignoreCase = true) -> "Subtítulos en inglés sincronizados"
                                                                else -> "Subtítulos en idioma original"
                                                            },
                                                            fontFamily = Inter,
                                                            fontSize = 11.sp,
                                                            color = if (isItemSubFocused) Color.Black.copy(alpha = 0.72f) else Color(0xFF9CA3AF)
                                                        )
                                                    }
                                                    if (isItemSubSelected) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(50))
                                                                .background(if (isItemSubFocused) Color(0xFF0A0D13) else warmIvory)
                                                                .padding(horizontal = 10.dp, vertical = 3.dp)
                                                        ) {
                                                            Text(
                                                                text = "ACTIVO",
                                                                fontFamily = Inter,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = if (isItemSubFocused) warmIvory else Color(0xFF0A0D13),
                                                                fontSize = 9.5.sp,
                                                                letterSpacing = 1.0.sp
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    if (nativeWebSubtitleTracks.isNotEmpty()) {
                                        nativeWebSubtitleTracks.forEachIndexed { sIdx, subLabel ->
                                            var isWebSubFocused by remember { mutableStateOf(false) }
                                            val isWebSubSelected = subtitlesEnabled && (selectedWebSubtitleTrackIdx == sIdx)

                                            Button(
                                                onClick = {
                                                    onApplyWebSubtitle(true, sIdx)
                                                    onDismiss()
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(
                                                        border = if (isWebSubFocused) BorderStroke(2.dp, warmIvory)
                                                        else if (isWebSubSelected) BorderStroke(1.5.dp, warmIvory.copy(alpha = 0.55f))
                                                        else BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .onFocusChanged { isWebSubFocused = it.isFocused }
                                                    .onKeyEvent { keyEvent ->
                                                        if (keyEvent.type == KeyEventType.KeyDown &&
                                                            (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                                                        ) {
                                                            onApplyWebSubtitle(true, sIdx)
                                                            onDismiss()
                                                            true
                                                        } else false
                                                    },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isWebSubFocused) warmIvory
                                                    else if (isWebSubSelected) Color(0xFF171C26)
                                                    else Color(0xFF11151E),
                                                    contentColor = if (isWebSubFocused) Color.Black else warmIvory
                                                ),
                                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = subLabel,
                                                            fontFamily = Inter,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 14.sp,
                                                            color = if (isWebSubFocused) Color.Black else warmIvory
                                                        )
                                                        Text(
                                                            text = "Pista de subtítulos del reproductor",
                                                            fontFamily = Inter,
                                                            fontSize = 11.sp,
                                                            color = if (isWebSubFocused) Color.Black.copy(alpha = 0.72f) else Color(0xFF9CA3AF)
                                                        )
                                                    }
                                                    if (isWebSubSelected) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(50))
                                                                .background(if (isWebSubFocused) Color(0xFF0A0D13) else warmIvory)
                                                                .padding(horizontal = 10.dp, vertical = 3.dp)
                                                        ) {
                                                            Text(
                                                                text = "ACTIVO",
                                                                fontFamily = Inter,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = if (isWebSubFocused) warmIvory else Color(0xFF0A0D13),
                                                                fontSize = 9.5.sp,
                                                                letterSpacing = 1.0.sp
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else if (currentStream.subtitle == 1 || currentStream.safeSubtitles.isNotEmpty()) {
                                        val webFallbackSubs = if (currentStream.safeSubtitles.isNotEmpty()) {
                                            currentStream.safeSubtitles
                                        } else {
                                            listOf(SubtitleTrackData(url = "", label = "Español", language = "es"))
                                        }
                                        webFallbackSubs.forEachIndexed { sIdx, subData ->
                                            var isApiSubFocused by remember { mutableStateOf(false) }
                                            val isApiSubSelected = subtitlesEnabled && (
                                                    if (subData.language.startsWith("en", ignoreCase = true)) selectedSubtitleOption == 2
                                                    else selectedSubtitleOption == 1 || selectedSubtitleOption == (sIdx + 1)
                                                    )

                                            Button(
                                                onClick = {
                                                    onApplySubtitlePreference(
                                                        true,
                                                        if (subData.language.startsWith("en", ignoreCase = true)) 2 else 1
                                                    )
                                                    onDismiss()
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(
                                                        border = if (isApiSubFocused) BorderStroke(2.dp, warmIvory)
                                                        else if (isApiSubSelected) BorderStroke(1.5.dp, warmIvory.copy(alpha = 0.55f))
                                                        else BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .onFocusChanged { isApiSubFocused = it.isFocused }
                                                    .onKeyEvent { keyEvent ->
                                                        if (keyEvent.type == KeyEventType.KeyDown &&
                                                            (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                                                        ) {
                                                            onApplySubtitlePreference(
                                                                true,
                                                                if (subData.language.startsWith("en", ignoreCase = true)) 2 else 1
                                                            )
                                                            onDismiss()
                                                            true
                                                        } else false
                                                    },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isApiSubFocused) warmIvory
                                                    else if (isApiSubSelected) Color(0xFF171C26)
                                                    else Color(0xFF11151E),
                                                    contentColor = if (isApiSubFocused) Color.Black else warmIvory
                                                ),
                                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = subData.label,
                                                            fontFamily = Inter,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 14.sp,
                                                            color = if (isApiSubFocused) Color.Black else warmIvory
                                                        )
                                                        Text(
                                                            text = if (subData.language.startsWith("es", ignoreCase = true)) "Subtítulos en español sincronizados" else "Subtítulos en idioma original",
                                                            fontFamily = Inter,
                                                            fontSize = 11.sp,
                                                            color = if (isApiSubFocused) Color.Black.copy(alpha = 0.72f) else Color(0xFF9CA3AF)
                                                        )
                                                    }
                                                    if (isApiSubSelected) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(50))
                                                                .background(if (isApiSubFocused) Color(0xFF0A0D13) else warmIvory)
                                                                .padding(horizontal = 10.dp, vertical = 3.dp)
                                                        ) {
                                                            Text(
                                                                text = "ACTIVO",
                                                                fontFamily = Inter,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = if (isApiSubFocused) warmIvory else Color(0xFF0A0D13),
                                                                fontSize = 9.5.sp,
                                                                letterSpacing = 1.0.sp
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        var isCloseFocused by remember { mutableStateOf(false) }
                        Button(
                            onClick = { onDismiss() },
                            modifier = Modifier
                                .align(Alignment.End)
                                .height(38.dp)
                                .onFocusChanged { isCloseFocused = it.isFocused }
                                .onKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyDown &&
                                        (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                                    ) {
                                        onDismiss()
                                        true
                                    } else false
                                },
                            border = if (isCloseFocused) BorderStroke(2.dp, warmIvory)
                            else BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCloseFocused) warmIvory else Color(0xFF141822),
                                contentColor = if (isCloseFocused) Color.Black else warmIvory
                            ),
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "CERRAR",
                                fontFamily = Inter,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
