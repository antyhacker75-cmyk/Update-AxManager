package frb.axeron.manager.ui.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import frb.axeron.manager.R
import frb.axeron.manager.ui.component.SearchAppBar
import frb.axeron.manager.ui.component.UseLifecycle
import frb.axeron.manager.ui.viewmodel.ViewModelGlobal

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun PrivilegeScreen(
    navigator: DestinationsNavigator,
    viewModelGlobal: ViewModelGlobal
) {
    val privilegeViewModel = viewModelGlobal.privilegeViewModel
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val listState = rememberLazyListState()

    UseLifecycle(
        {
            privilegeViewModel.loadInstalledApps(false)
        }
    )

    Scaffold(
        topBar = {
            SearchAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.privilege_manager),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                searchLabel = stringResource(R.string.search_label_apps),
                searchText = privilegeViewModel.search,
                onSearchTextChange = { privilegeViewModel.search = it },
                onClearClick = { privilegeViewModel.search = "" },
                scrollBehavior = scrollBehavior,
//                onBackClick = {
//                    navigator.popBackStack()
//                },
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            modifier = Modifier.padding(paddingValues),
            isRefreshing = privilegeViewModel.isRefreshing,
            onRefresh = {
                privilegeViewModel.loadInstalledApps()
            }
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = remember {
                    PaddingValues(
                        bottom = 120.dp
                    )
                }
            ) {
                items(
                    privilegeViewModel.privilegeList,
                    key = { it.packageName + it.uid }
                ) { app ->
                    ElevatedCard(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        ListItem(
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent
                            ),
                            headlineContent = {
                                Text(
                                    text = app.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = app.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            leadingContent = {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(app.packageInfo)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = app.label,
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = app.isAdded,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            privilegeViewModel.grant(app.packageInfo.applicationInfo!!.uid)
                                        } else {
                                            privilegeViewModel.revoke(app.packageInfo.applicationInfo!!.uid)
                                        }
                                    }
                                )
                            }
                        )
                    }
                }
            }

        }
    }
}