package frb.axeron.manager.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import frb.axeron.manager.R
import frb.axeron.manager.ui.viewmodel.PluginViewModel
import frb.axeron.manager.ui.viewmodel.PrivilegeViewModel

@Composable
@Preview
fun StatusCard() {
    val isDark = isSystemInDarkTheme()
    val colorScheme = colorScheme
    val fadeColor = when {
        isDark -> colorScheme.surfaceVariant
        else -> colorScheme.surfaceVariant
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = colorScheme.surfaceContainer
        )
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(10.dp, 30.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Icon(
                    modifier = Modifier
                        .size(145.dp),
                    painter = painterResource(R.drawable.ic_axeron),
                    contentDescription = null,
                    tint = colorScheme.primary.copy(alpha = 0.15f)
                )
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                fadeColor.copy(alpha = 0.0f),
                                fadeColor.copy(alpha = 0.55f)
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .matchParentSize()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.home_running),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    ExtraLabel(
                        text = "Shell",
                        style = ExtraLabelDefaults.style.copy(
                            allCaps = false
                        )
                    )
                }

                Text(
                    text = "Version: 20 | Pid: 20",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(Modifier.weight(1f))

                Text("Hello")
            }
        }
    }
}

@Composable
@Preview
fun PreviewCard() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PluginCard(Modifier.weight(1f))
        PrivilegeCard(Modifier.weight(1f))
    }
}

@Composable
@Preview
fun PluginCard(
    modifier: Modifier = Modifier,
    pluginViewModel: PluginViewModel = viewModel(),
) {
    val countTotal = pluginViewModel.plugins.size
    val colorScheme = colorScheme

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (countTotal <= 1) {
                        stringResource(R.string.plugin)
                    } else {
                        stringResource(R.string.plugin_plural)
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurfaceVariant
                )

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colorScheme.primaryContainer)
                        .border(1.dp, colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(16.dp),
                        imageVector = Icons.Filled.Extension,
                        tint = colorScheme.primary,
                        contentDescription = null
                    )
                }
            }

            Text(
                text = "$countTotal",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 38.sp
                ),
                fontWeight = FontWeight.Black,
                color = colorScheme.onSurface
            )
        }
    }
}

@Composable
@Preview
fun PrivilegeCard(
    modifier: Modifier = Modifier,
    privilegeViewModel: PrivilegeViewModel = viewModel(),
) {
    val countTotal = privilegeViewModel.privilegedCount
    val colorScheme = colorScheme

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (countTotal <= 1) {
                        stringResource(R.string.privilege)
                    } else {
                        stringResource(R.string.privilege_plural)
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurfaceVariant
                )

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colorScheme.tertiaryContainer)
                        .border(1.dp, colorScheme.tertiary.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(16.dp),
                        imageVector = Icons.Filled.AdminPanelSettings,
                        tint = colorScheme.tertiary,
                        contentDescription = null
                    )
                }
            }

            Text(
                text = "$countTotal",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 38.sp
                ),
                fontWeight = FontWeight.Black,
                color = colorScheme.onSurface
            )
        }
    }
}
