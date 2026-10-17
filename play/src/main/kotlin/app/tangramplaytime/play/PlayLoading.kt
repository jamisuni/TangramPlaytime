// WO-004 TASK-020 · seam S-23 (designs/WO-004-design.md §2) · WO-003 F3 (the bundle and the store load off the
// main thread, decisions.md 2026-10-12 "Carried to WO-004 #Browsing") · decisions.md 2026-10-15 "REQ-024 A2 and the
// launch window" (its title's dotted ID written without the dot; no puzzle is current yet, so no top bar) and
// "Startup, and what survives" ("blank paper
// (`play-loading`: no spinner, no text)").
package app.tangramplaytime.play

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.testTag

/**
 * What `:app` shows until the controller exists (S-23): plain paper over [modifier]'s size, with no top bar, no
 * text and no spinner, tagged `play-loading`.
 */
@Composable
fun PlayLoading(modifier: Modifier = Modifier) {
    Box(modifier.testTag(PlayTags.PLAY_LOADING).drawBehind { drawRect(PlayPalette.paper) })
}
